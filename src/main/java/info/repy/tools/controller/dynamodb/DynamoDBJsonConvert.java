package info.repy.tools.controller.dynamodb;

import java.math.BigDecimal;
import java.util.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class DynamoDBJsonConvert {
    public static final ObjectMapper mapper = new ObjectMapper();

    public static JsonNode toJson(Map<String, AttributeValue> attributeValues) {
        return toJson(AttributeValue.builder().m(attributeValues).build());
    }

    public static JsonNode toJson(AttributeValue attributeValue) {
        if (attributeValue.nul() != null) return NullNode.getInstance();
        if (attributeValue.bool() != null) return BooleanNode.valueOf(attributeValue.bool());
        if (attributeValue.s() != null) return TextNode.valueOf(attributeValue.s());
        if (attributeValue.n() != null) {
            return DecimalNode.valueOf(new BigDecimal(attributeValue.n()));
        }
        if (attributeValue.l() != null && !attributeValue.l().isEmpty()) {
            ArrayNode a = mapper.createArrayNode();
            for (AttributeValue v : attributeValue.l()) {
                a.add(toJson(v));
            }
            return a;
        }
        if (attributeValue.m() != null && !attributeValue.m().isEmpty()) {
            ObjectNode a = mapper.createObjectNode();
            for (Map.Entry<String, AttributeValue> v : attributeValue.m().entrySet()) {
                a.set(v.getKey(), toJson(v.getValue()));
            }
            return a;
        }
        throw new UnsupportedOperationException();
    }

    public static Map<String, AttributeValue> toAttributeMap(JsonNode data) {
        HashMap<String, AttributeValue> item = new HashMap<>();
        for (Iterator<Map.Entry<String, JsonNode>> it = data.fields(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> f = it.next();
            item.put(f.getKey(), toAttributeValue(f.getValue()));
        }
        return item;
    }

    public static AttributeValue toAttributeValue(JsonNode data) {
        JsonNodeType type = data.getNodeType();
        switch (type) {
            case NULL:
                return AttributeValue.builder().nul(true).build();
            case BINARY:
                BinaryNode bn = (BinaryNode) data;
                return AttributeValue.builder().bs(SdkBytes.fromByteArray(bn.binaryValue())).build();
            case NUMBER:
                return AttributeValue.builder().n(data.asText()).build();
            case STRING:
                return AttributeValue.builder().s(data.asText()).build();
            case BOOLEAN:
                return AttributeValue.builder().bool(data.booleanValue()).build();
            case ARRAY:
                ArrayList<AttributeValue> list = new ArrayList<>();
                for (Iterator<JsonNode> it = data.elements(); it.hasNext(); ) {
                    JsonNode e = it.next();
                    list.add(toAttributeValue(e));
                }
                return AttributeValue.builder().l(list).build();
            case OBJECT:
                return AttributeValue.builder().m(toAttributeMap(data)).build();
            case POJO:
            case MISSING:
            default:
                throw new RuntimeException();
        }
    }

}
