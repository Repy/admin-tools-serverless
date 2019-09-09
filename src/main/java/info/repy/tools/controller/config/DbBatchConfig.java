package info.repy.tools.controller.config;

import lombok.Data;

import java.util.List;

@Data
public class DbBatchConfig {
    private String id;
    private String name;
    private String sql;
    private String[] params;
}
