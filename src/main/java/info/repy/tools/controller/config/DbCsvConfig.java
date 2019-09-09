package info.repy.tools.controller.config;

import lombok.Data;

@Data
public class DbCsvConfig {
    private String id;
    private String name;
    private String sql;
    private String description;
}
