package info.repy.tools.controller.config;

import lombok.Data;

@Data
public class S3UpdateConfig {
    private String id;
    private String name;
    private String bucket;
    private String contentType;
    private String language;
    private String cacheControl;
    private String key;
}
