package info.repy.tools.controller.s3;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Data
public class S3PostFrom {
    private String action;
    private String method;
    private String enctype;
    private ArrayList<KeyValue> hiddenInput;
    private String uploadedUrl;

    public static S3PostFrom build(
            AwsCredentials credentials,
            long expire,
            String region,
            String bucket,
            String key,
            String acl,
            String redirect
    ) {
        String accessKeyId = credentials.accessKeyId();
        String sessionToken = null;
        if (credentials instanceof AwsSessionCredentials) {
            sessionToken = ((AwsSessionCredentials) credentials).sessionToken();
        }

        long now = System.currentTimeMillis();

        ArrayList<KeyValue> map = new ArrayList<>();

        map.add(new KeyValue("success_action_redirect", redirect));
        map.add(new KeyValue("X-Amz-Algorithm", "AWS4-HMAC-SHA256"));
        map.add(new KeyValue("cache-control", "max-age=3600000"));
        map.add(new KeyValue("X-Amz-Credential", accessKeyId + "/" + date(now) + "/" + region + "/s3/aws4_request"));
        map.add(new KeyValue("X-Amz-Date", time(now)));
        if (sessionToken != null) {
            map.add(new KeyValue("X-Amz-Security-Token", sessionToken));
        }
        map.add(new KeyValue("key", key));
        map.add(new KeyValue("bucket", bucket));
        map.add(new KeyValue("acl", acl));

        //ここでpolicy生成
        List<Object> policyList = new ArrayList<>();
        for (KeyValue d: map) {
            policyList.add(new HashMap.SimpleEntry<String, String>(d.key, d.value));
        }
        policyList.add(new Object[]{"starts-with","$content-type",""});
        String policy = new Policy(datetime2(now + expire), policyList).toString();
        policy = Base64.getEncoder().encodeToString(policy.getBytes(StandardCharsets.UTF_8));
        map.add(0, new KeyValue("policy", policy));

        map.add(new KeyValue("X-Amz-Signature", getSignatureKey(credentials.secretAccessKey(), date(now), region, policy)));


        S3PostFrom ret = new S3PostFrom();
        ret.action = "https://s3." + region + ".amazonaws.com/" + bucket + "/";
        ret.method = "post";
        ret.enctype = "multipart/form-data";
        ret.hiddenInput = map;
        return ret;
    }


    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneId.of("UTC"));

    private static String time(long milli) {
        return TIME_FORMATTER.format(Instant.ofEpochMilli(milli));
    }

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneId.of("UTC"));

    private static String date(long milli) {
        return DATE_FORMATTER.format(Instant.ofEpochMilli(milli));
    }

    private static final DateTimeFormatter TIME2_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneId.of("UTC"));

    private static String datetime2(long milli) {
        return TIME2_FORMATTER.format(Instant.ofEpochMilli(milli));
    }

    public static String hex(byte[] by) {
        StringBuilder sb = new StringBuilder();
        for (int b : by) {
            sb.append(Character.forDigit(b >> 4 & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }

    public static byte[] HmacSHA256(String data, byte[] key) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String getSignatureKey(String secretKey, String dateStamp, String regionName, String policy) {
        byte[] signature;
        signature = ("AWS4" + secretKey).getBytes(StandardCharsets.UTF_8);
        System.out.println("AWS4" + secretKey);
        signature = HmacSHA256(dateStamp, signature);
        System.out.println(dateStamp + ":" + hex(signature));
        signature = HmacSHA256(regionName, signature);
        System.out.println(regionName + ":" + hex(signature));
        signature = HmacSHA256("s3", signature);
        System.out.println("s3" + ":" + hex(signature));
        signature = HmacSHA256("aws4_request", signature);
        System.out.println("aws4_request" + ":" + hex(signature));
        signature = HmacSHA256(policy, signature);
        System.out.println("policy" + ":" + hex(signature));
        return hex(signature);
    }


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class KeyValue {
        private String key;
        private String value;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Policy {
        public static final ObjectMapper mapper = new ObjectMapper();
        private String expiration;
        private List<Object> conditions;

        public String toString() {
            try {
                return mapper.writeValueAsString(this);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
    }
}

