package info.repy.tools.controller.s3;

import info.repy.tools.controller.Config;
import info.repy.tools.controller.config.S3UpdateConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.output.StringBuilderWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

@Controller
@Slf4j
public class S3UpdateController {
    public static final S3Client s3 = S3Client.builder().region(Region.AP_NORTHEAST_1).build();

    @Autowired
    private Config config;

    @GetMapping(path = "/s3/update")
    public ModelAndView list() {
        List<S3UpdateConfig> conf = config.read().getS3Update();
        return new ModelAndView("s3/update/index").addObject("conf", conf);
    }

    @GetMapping(path = "/s3/update/{id}")
    public ModelAndView get(@PathVariable("id") String id) {
        List<S3UpdateConfig> conf = config.read().getS3Update();
        S3UpdateConfig idConf = conf.stream().filter((d) -> Objects.equals(d.getId(), id)).findFirst().get();
        String data;
        try (
                BufferedReader input = new BufferedReader(new InputStreamReader(s3.getObject(GetObjectRequest.builder().bucket(idConf.getBucket()).key(idConf.getKey()).build())));
                StringBuilderWriter output = new StringBuilderWriter();
        ) {
            char[] buffer = new char[1024];
            int readbyte;
            while (-1 != (readbyte = input.read(buffer))) {
                output.write(buffer, 0, readbyte);
            }
            data = output.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new ModelAndView("s3/update/id").addObject("idConf", idConf).addObject("data", data);
    }

    @PostMapping(path = "/s3/update/{id}")
    public ModelAndView post(@PathVariable("id") String id, @RequestParam("data") String data) {
        List<S3UpdateConfig> conf = config.read().getS3Update();
        S3UpdateConfig idConf = conf.stream().filter((d) -> Objects.equals(d.getId(), id)).findFirst().get();
        s3.putObject(PutObjectRequest.builder()
                        .bucket(idConf.getBucket())
                        .key(idConf.getKey())
                        .contentType(idConf.getContentType())
                        .cacheControl(idConf.getCacheControl())
                        .build(),
                RequestBody.fromString(data, StandardCharsets.UTF_8));
        return new ModelAndView("s3/update/id").addObject("idConf", idConf).addObject("data", data);
    }

}
