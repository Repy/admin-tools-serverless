package info.repy.tools.controller.config;

import lombok.Data;

import java.util.List;

@Data
public class DbBatchConfig {
    private String id = "";
    private String name = "";
    private String description = "";
    private String sql = null;
    private String initSql = null;
    private String finallySql = null;
    private boolean insertForm = false;
    private String[] params = new String[0];
}
