package info.repy.tools.controller;

import info.repy.tools.controller.config.Root;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;

@Service
public class Config {
    @Value("${info.repy.tools.file}")
    private String filename;

    private static Root data = null;

    public Root read() {
        if (data != null) return data;
        Yaml yaml = new Yaml();
        try (
                InputStream is = this.getClass().getResourceAsStream("/" + filename);
        ) {
            data = yaml.loadAs(is, Root.class);
            return data;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
