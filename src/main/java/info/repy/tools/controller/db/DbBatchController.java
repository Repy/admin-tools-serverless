package info.repy.tools.controller.db;

import com.opencsv.CSVReaderHeaderAware;
import com.opencsv.CSVWriterBuilder;
import com.opencsv.ICSVWriter;
import info.repy.tools.controller.Config;
import info.repy.tools.controller.config.DbBatchConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.*;

@Controller
public class DbBatchController {
    @Autowired
    private Config config;

    @Autowired
    private NamedParameterJdbcTemplate jdbc;

    @GetMapping(path = "/db/batch")
    public ModelAndView index() {
        List<DbBatchConfig> conf = config.read().getDbBatch();
        return new ModelAndView("db/batch/index").addObject("conf", conf);
    }

    @GetMapping(path = "/db/batch/{id}")
    public ModelAndView id(@PathVariable("id") String id) {
        List<DbBatchConfig> conf = config.read().getDbBatch();
        Optional<DbBatchConfig> sqlOpt = conf.stream().filter((data) -> {
            return Objects.equals(data.getId(), id);
        }).findFirst();
        return new ModelAndView("db/batch/id").addObject("sql", sqlOpt.get());
    }

    @PostMapping(path = "/db/batch/{id}")
    public ModelAndView post(
            @PathVariable("id") String id,
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        List<DbBatchConfig> conf = config.read().getDbBatch();
        Optional<DbBatchConfig> sqlOpt = conf.stream().filter((data) -> {
            return Objects.equals(data.getId(), id);
        }).findFirst();
        DbBatchConfig sql = sqlOpt.get();
        CSVReaderHeaderAware reader = new CSVReaderHeaderAware(new InputStreamReader(file.getInputStream()));
        ArrayList<Map<String, String>> list = new ArrayList<>();
        Map<String, String> data;
        while ((data = reader.readMap()) != null) list.add(data);
        this.jdbc.batchUpdate(sql.getSql(), list.toArray(new Map[]{}));
        return new ModelAndView("db/batch/id").addObject("sql", sql);
    }

    @GetMapping(path = "/db/batch/{id}/download", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @ResponseBody
    public byte[] download(@PathVariable("id") String id) {
        List<DbBatchConfig> conf = config.read().getDbBatch();
        Optional<DbBatchConfig> sqlOpt = conf.stream().filter((data) -> {
            return Objects.equals(data.getId(), id);
        }).findFirst();
        StringWriter sw = new StringWriter();
        try (
                ICSVWriter writer = new CSVWriterBuilder(sw).build();
        ) {
            writer.writeNext(sqlOpt.get().getParams());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return sw.toString().getBytes(Charset.forName("Windows-31J"));
    }

}
