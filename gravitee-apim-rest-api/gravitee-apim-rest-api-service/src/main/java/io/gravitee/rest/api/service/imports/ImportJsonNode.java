package io.gravitee.rest.api.service.imports;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

public class ImportJsonNode {

    private JsonNode jsonNode;

    public ImportJsonNode(com.fasterxml.jackson.databind.JsonNode jsonNode) {
        this.jsonNode = jsonNode;
    }

    protected boolean hasField(String fieldName) {
        return jsonNode.hasNonNull(fieldName) && StringUtils.isNotEmpty(jsonNode.get(fieldName).asText());
    }

    protected boolean hasArray(String fieldName) {
        return jsonNode.has(fieldName) && jsonNode.get(fieldName).isArray();
    }

    protected String getField(String fieldName) {
        return jsonNode.get(fieldName).asText();
    }

    protected void setField(String fieldName, String value) {
        ((ObjectNode) jsonNode).put(fieldName, value);
    }

    protected ArrayNode getArray(String fieldName) {
        return (ArrayNode) jsonNode.get(fieldName);
    }

    protected List<JsonNode> getChildNodesByName(String name) {
        List<JsonNode> nodes = new ArrayList<>();
        if (jsonNode.has(name) && jsonNode.get(name).isArray()) {
            jsonNode.get(name).forEach(nodes::add);
        }
        return nodes;
    }

    public String asText() {
        return jsonNode.asText();
    }

    public JsonNode getJsonNode() {
        return jsonNode;
    }

    @Override
    public String toString() {
        return jsonNode.toString();
    }
}
