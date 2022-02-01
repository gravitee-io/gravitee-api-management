/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
