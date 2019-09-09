package info.repy.tools.controller.config;

import lombok.Data;

@Data
public class DynamoUploadConfig {
    private String id;
    private String name;
    private String table;
    private String keyName;
    private String keyValue;
}
