package info.repy.tools.controller.s3;

import info.repy.tools.controller.Config;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;

import java.util.UUID;

@Controller
@Slf4j
public class S3UploadController {
    @Autowired
    private Config config;

    @Autowired
    private NamedParameterJdbcTemplate jdbc;

    @GetMapping(path = "/s3/upload")
    public ModelAndView get() {
        AwsCredentials credentials = DefaultCredentialsProvider.builder().build().resolveCredentials();
        String uuid = UUID.randomUUID().toString();
        S3PostFrom form = S3PostFrom.build(credentials, 600 * 1000L, Region.AP_NORTHEAST_1.toString(), config.read().getS3Upload().getBucket(), uuid, "private", config.read().getS3Upload().getUrl() + uuid);
        return new ModelAndView("s3/upload/index").addObject("config", config.read().getS3Upload()).addObject("form", form);
    }
}
