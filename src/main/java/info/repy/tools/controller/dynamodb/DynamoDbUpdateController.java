package info.repy.tools.controller.dynamodb;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import info.repy.dynamodb.DynamoDBJsonConvert;
import info.repy.tools.controller.Config;
import info.repy.tools.controller.config.DynamoUploadConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.io.IOException;
import java.util.*;

@Controller
public class DynamoDbUpdateController {
    private static final DynamoDbClient client = DynamoDbClient.builder().region(Region.AP_NORTHEAST_1).build();

    public static final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private Config config;

    @GetMapping(path = "/dynamodb/edit")
    public ModelAndView index() {
        List<DynamoUploadConfig> conf = config.read().getDynamoUpload();
        return new ModelAndView("dynamodb/edit/index").addObject("conf", conf);
    }

    @GetMapping(path = "/dynamodb/edit/{id}")
    public ModelAndView id(@PathVariable("id") String id) throws JsonProcessingException {
        List<DynamoUploadConfig> conf = config.read().getDynamoUpload();
        DynamoUploadConfig data = conf.stream().filter((d) -> Objects.equals(d.getId(), id)).findFirst().get();
        HashMap<String, AttributeValue> map = new HashMap<>();
        map.put(data.getKeyName(), AttributeValue.builder().s(data.getKeyValue()).build());
        GetItemResponse item = client.getItem(GetItemRequest.builder().tableName(data.getTable()).key(map).build());
        JsonNode node = DynamoDBJsonConvert.toJson(item.item());
        String json = mapper.writeValueAsString(node);
        return new ModelAndView("dynamodb/edit/id").addObject("data", data).addObject("json", json);
    }

    @PostMapping(path = "/dynamodb/edit/{id}")
    public ModelAndView post(@PathVariable("id") String id, @RequestParam("json") String json) throws IOException {
        List<DynamoUploadConfig> conf = config.read().getDynamoUpload();
        DynamoUploadConfig data = conf.stream().filter((d) -> Objects.equals(d.getId(), id)).findFirst().get();
        Map<String, AttributeValue> item = DynamoDBJsonConvert.toAttributeMap(mapper.readTree(json));
        client.putItem(PutItemRequest.builder().tableName(data.getTable()).item(item).build());
        return id(id);
    }

}