package info.repy.tools.controller.db;

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;
import com.opencsv.CSVReaderHeaderAware;
import com.opencsv.CSVWriterBuilder;
import com.opencsv.ICSVWriter;
import info.repy.tools.controller.Config;
import info.repy.tools.controller.config.DbBatchConfig;
import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.input.BOMInputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
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
    ) {
        List<DbBatchConfig> conf = config.read().getDbBatch();
        Optional<DbBatchConfig> sqlOpt = conf.stream().filter((data) -> {
            return Objects.equals(data.getId(), id);
        }).findFirst();
        DbBatchConfig sql = sqlOpt.get();

        try (
                BufferedInputStream input = new BufferedInputStream(file.getInputStream());
                BOMInputStream bom = new BOMInputStream(input, ByteOrderMark.UTF_8, ByteOrderMark.UTF_16LE, ByteOrderMark.UTF_16BE, ByteOrderMark.UTF_32LE, ByteOrderMark.UTF_32BE);
        ) {
            CharsetDetector detector = new CharsetDetector();
            List<String> enableCharsets = Arrays.asList("UTF-8", "Shift_JIS", "EUC-JP", "ISO-2022-JP", "UTF-16BE", "UTF-16LE");
            if (bom.hasBOM()) {
                if (bom.hasBOM(ByteOrderMark.UTF_8)) {
                    enableCharsets = Collections.singletonList(StandardCharsets.UTF_8.name());
                } else if (bom.hasBOM(ByteOrderMark.UTF_16LE)) {
                    enableCharsets = Collections.singletonList(StandardCharsets.UTF_16LE.name());
                } else if (bom.hasBOM(ByteOrderMark.UTF_16BE)) {
                    enableCharsets = Collections.singletonList(StandardCharsets.UTF_16BE.name());
                } else if (bom.hasBOM(ByteOrderMark.UTF_32LE)) {
                    throw new RuntimeException("not support utf32");
                } else if (bom.hasBOM(ByteOrderMark.UTF_32BE)) {
                    throw new RuntimeException("not support utf32");
                }
            }
            detector = detector.setText(bom);
            CharsetMatch detect = null;
            CharsetMatch[] detectAll = detector.detectAll();
            for (CharsetMatch match : detectAll) {
                for (String enableCharset : enableCharsets) {
                    if (Objects.equals(match.getName(), enableCharset)) {
                        detect = match;
                    }
                    if (detect != null) break;
                }
                if (detect != null) break;
            }
            if (detect == null) throw new RuntimeException();

            try (
                    Reader reader = detect.getReader();
                    CSVReaderHeaderAware csv = new CSVReaderHeaderAware(reader);
            ) {
                ArrayList<Map<String, String>> list = new ArrayList<>();
                Map<String, String> data;
                while ((data = csv.readMap()) != null) {
                    for (String key : data.keySet()) {
                        String value = data.get(key);
                        if (Objects.equals(value, "NULL")) {
                            data.put(key, null);
                        }
                    }
                    list.add(data);
                }
                if (sql.getInitSql() != null) {
                    this.jdbc.update(sql.getInitSql(), new HashMap<>());
                }
                this.jdbc.batchUpdate(sql.getSql(), list.toArray(new Map[]{}));
                return new ModelAndView("db/batch/id").addObject("sql", sql);
            }
        } catch (
                IOException e) {
            throw new RuntimeException(e);
        }

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
