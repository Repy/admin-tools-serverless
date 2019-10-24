package info.repy.tools.controller.db;

import com.opencsv.CSVWriterBuilder;
import com.opencsv.ICSVWriter;
import info.repy.tools.controller.Util;
import info.repy.tools.controller.config.DbCsvConfig;
import info.repy.tools.controller.Config;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSetMetaData;
import java.util.*;

@Controller
public class DbCsvController {
    @Autowired
    private Config config;

    @Autowired
    private NamedParameterJdbcTemplate jdbc;

    @GetMapping(path = "/db/csv")
    public ModelAndView index() {
        List<DbCsvConfig> conf = config.read().getDbCsv();
        return new ModelAndView("db/csv/index").addObject("conf", conf);
    }

    @GetMapping(path = "/db/csv/{id}")
    public ModelAndView id(@PathVariable("id") String id) {
        List<DbCsvConfig> conf = config.read().getDbCsv();
        Optional<DbCsvConfig> sqlOpt = conf.stream().filter((data) -> {
            return Objects.equals(data.getId(), id);
        }).findFirst();
        return new ModelAndView("db/csv/id").addObject("sql", sqlOpt.get());
    }

    @Data
    @AllArgsConstructor
    public static class DBData {
        List<String> headers = new ArrayList<>();
        List<List<String>> rows = new ArrayList<>();
    }

    private DBData data(DbCsvConfig sql, Map<String, String> data) {
        List<String> headers = new ArrayList<>();
        List<List<String>> rows = this.jdbc.query(sql.getSql(), data, (rs, rowNum) -> {
            if (headers.isEmpty()) {
                ResultSetMetaData meta = rs.getMetaData();
                for (int i = 0; i < meta.getColumnCount(); i++) {
                    String name = meta.getColumnLabel(i + 1);
                    headers.add(name);
                }
            }
            List<String> row = new ArrayList<>();
            for (int i = 0; i < headers.size(); i++) {
                String val = rs.getString(i + 1);
                if (val == null) val = "NULL";
                row.add(val);
            }
            return row;
        });
        return new DBData(headers, rows);
    }

    @GetMapping(path = "/db/csv/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable("id") String id, HttpServletRequest request) {
        List<DbCsvConfig> conf = config.read().getDbCsv();
        Optional<DbCsvConfig> sqlOpt = conf.stream().filter((data) -> {
            return Objects.equals(data.getId(), id);
        }).findFirst();
        DbCsvConfig sql = sqlOpt.get();

        DBData data = this.data(sql, Util.toSingleMap(request.getParameterMap()));

        // CSV
        StringWriter sw = new StringWriter();
        sw.append('\uFEFF'); // UTF-8のBOM追加
        try (
                ICSVWriter writer = new CSVWriterBuilder(sw).build();
        ) {
            writer.writeNext(data.getHeaders().toArray(new String[0]));
            for (List<String> row : data.getRows()) {
                writer.writeNext(row.toArray(new String[0]));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment;filename=\"" + sql.getId() + ".csv\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(sw.toString().getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping(path = "/db/csv/{id}/view", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @ResponseBody
    public ModelAndView view(@PathVariable("id") String id, HttpServletRequest request) {
        List<DbCsvConfig> conf = config.read().getDbCsv();
        Optional<DbCsvConfig> sqlOpt = conf.stream().filter((data) -> {
            return Objects.equals(data.getId(), id);
        }).findFirst();
        DbCsvConfig sql = sqlOpt.get();

        DBData data = this.data(sql, Util.toSingleMap(request.getParameterMap()));

        return new ModelAndView("db/csv/view").addObject("sql", sqlOpt.get()).addObject("data", data);
    }

}
