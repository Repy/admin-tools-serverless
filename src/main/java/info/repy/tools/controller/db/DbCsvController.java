package info.repy.tools.controller.db;

import com.opencsv.CSVWriterBuilder;
import com.opencsv.ICSVWriter;
import info.repy.tools.controller.config.DbCsvConfig;
import info.repy.tools.controller.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
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

    @GetMapping(path = "/db/csv/{id}/download", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @ResponseBody
    public byte[] download(@PathVariable("id") String id) {
        List<DbCsvConfig> conf = config.read().getDbCsv();
        Optional<DbCsvConfig> sqlOpt = conf.stream().filter((data) -> {
            return Objects.equals(data.getId(), id);
        }).findFirst();
        DbCsvConfig sql = sqlOpt.get();
        StringWriter sw = new StringWriter();

        List<String> header = new ArrayList<>();
        List<List<String>> data = this.jdbc.query(sql.getSql(), (rs, rowNum) -> {
            if (header.isEmpty()) {
                ResultSetMetaData meta = rs.getMetaData();
                for (int i = 0; i < meta.getColumnCount(); i++) {
                    String name = meta.getColumnLabel(i + 1);
                    header.add(name);
                }
            }
            List<String> row = new ArrayList<>();
            for (int i = 0; i < header.size(); i++) {
                String val = rs.getString(i + 1);
                row.add(val);
            }
            return row;
        });

        try (
                ICSVWriter writer = new CSVWriterBuilder(sw).build();
        ) {
            writer.writeNext(header.toArray(new String[0]));
            for (List<String> row : data) {
                writer.writeNext(row.toArray(new String[0]));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return sw.toString().getBytes(Charset.forName("Windows-31J"));
    }

}
