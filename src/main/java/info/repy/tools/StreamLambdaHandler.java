package info.repy.tools;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.model.Headers;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2ProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.zip.GZIPOutputStream;

@Slf4j
public class StreamLambdaHandler {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler;

    static {
        String[] profile = new String[0];
        String env = System.getenv("SPRING_PROFILES_ACTIVE");
        if (env != null) {
            profile = env.split(",");
        }
        try {
            SpringBootLambdaContainerHandler.getContainerConfig().setDefaultContentCharset("UTF-8");
            handler = SpringBootLambdaContainerHandler.getAwsProxyHandler(ToolsApplication.class, profile);
            handler.onStartup(servletContext -> {
            });
        } catch (ContainerInitializationException e) {
            log.error("static init", e);
            throw new RuntimeException("Could not initialize Spring Boot application", e);
        }
    }

    public StreamLambdaHandler() {

    }

    public APIGatewayV2ProxyResponseEvent handleRequest(AwsProxyRequest request, Context context) {
        try {
            // log.error("request = {} ", mapper.writeValueAsString(request));
        } catch (Exception e) {
        }
        AwsProxyResponse response = handler.proxy(request, context);
        APIGatewayV2ProxyResponseEvent responseV2 = changeResponse(request, response);
        try {
            // log.error("response = {} ", mapper.writeValueAsString(response));
        } catch (Exception e) {
        }
        // AWSのサーバーレスライブラリに不具合あるので変換
        return responseV2;
    }

    public APIGatewayV2ProxyResponseEvent changeResponse(AwsProxyRequest request, AwsProxyResponse response) {
        APIGatewayV2ProxyResponseEvent responceV2 = new APIGatewayV2ProxyResponseEvent();
        //ステータスコード
        responceV2.setStatusCode(response.getStatusCode());

        Headers header = response.getMultiValueHeaders();
        HashMap<String, String[]> headerV2 = new HashMap<>();
        for (String key : header.keySet()) {
            headerV2.put(key.toLowerCase(), header.get(key).toArray(new String[0]));
        }
        headerV2.remove("content-length");
        responceV2.setMultiValueHeaders(headerV2);

        byte[] data;
        if (response.isBase64Encoded()) {
            String body = response.getBody();
            data = Base64.getMimeDecoder().decode(body);
        } else {
            data = response.getBody().getBytes(StandardCharsets.UTF_8);
        }
        response.setBody(null);
        List<String> aenc = request.getMultiValueHeaders().get("accept-encoding");
        if (data.length > 2048 && aenc != null && aenc.get(0).contains("gzip") && !headerV2.containsKey("content-encoding")) {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            try (
                    GZIPOutputStream gzip = new GZIPOutputStream(byteStream);
            ) {
                gzip.write(data);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            try {
                byteStream.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            data = byteStream.toByteArray();
            headerV2.put("content-encoding", new String[]{"gzip"});
        }
        responceV2.setBody(Base64.getEncoder().encodeToString(data));
        responceV2.setIsBase64Encoded(true);

        return responceV2;
    }
}
