package info.repy.tools.controller.config;

import lombok.Data;

@Data
public class S3UploadConfig {
    private String bucket;
    private String url;
}
