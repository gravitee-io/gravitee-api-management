/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.service.impl;

import static junit.framework.TestCase.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.rest.api.model.JsonPatch;
import io.gravitee.rest.api.service.exceptions.JsonPatchTestFailedException;
import io.gravitee.rest.api.service.exceptions.JsonPatchUnsafeException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class JsonPatchServiceTest {

    public static final String API_DEFINITION =
        "{\n" +
        "  \"description\" : \"Gravitee.io\",\n" +
        "  \"paths\" : { },\n" +
        "  \"path_mappings\":[],\n" +
        "  \"proxy\": {\n" +
        "    \"virtual_hosts\": [{\n" +
        "      \"path\": \"/test\"\n" +
        "    }],\n" +
        "    \"strip_context_path\": false,\n" +
        "    \"preserve_host\":false,\n" +
        "    \"logging\": {\n" +
        "      \"mode\":\"CLIENT_PROXY\",\n" +
        "      \"condition\":\"condition\"\n" +
        "    },\n" +
        "    \"groups\": [\n" +
        "      {\n" +
        "        \"name\": \"default-group\",\n" +
        "        \"endpoints\": [\n" +
        "          {\n" +
        "            \"name\": \"default\",\n" +
        "            \"target\": \"http://test\",\n" +
        "            \"weight\": 1,\n" +
        "            \"backup\": true,\n" +
        "            \"type\": \"HTTP\",\n" +
        "            \"http\": {\n" +
        "              \"connectTimeout\": 5000,\n" +
        "              \"idleTimeout\": 60000,\n" +
        "              \"keepAliveTimeout\": 30000,\n" +
        "              \"keepAlive\": true,\n" +
        "              \"readTimeout\": 10000,\n" +
        "              \"pipelining\": false,\n" +
        "              \"maxConcurrentConnections\": 100,\n" +
        "              \"useCompression\": true,\n" +
        "              \"followRedirects\": false,\n" +
        "              \"encodeURI\":false\n" +
        "            }\n" +
        "          }\n" +
        "        ],\n" +
        "        \"load_balancing\": {\n" +
        "          \"type\": \"ROUND_ROBIN\"\n" +
        "        },\n" +
        "        \"http\": {\n" +
        "          \"connectTimeout\": 5000,\n" +
        "          \"idleTimeout\": 60000,\n" +
        "          \"keepAliveTimeout\": 30000,\n" +
        "          \"keepAlive\": true,\n" +
        "          \"readTimeout\": 10000,\n" +
        "          \"pipelining\": false,\n" +
        "          \"maxConcurrentConnections\": 100,\n" +
        "          \"useCompression\": true,\n" +
        "          \"followRedirects\": false,\n" +
        "          \"encodeURI\":false\n" +
        "        }\n" +
        "      }\n" +
        "    ]\n" +
        "  }\n" +
        "}\n";

    @InjectMocks
    private JsonPatchServiceImpl jsonPatchService;

    @Spy
    GraviteeMapper objectMapper = new GraviteeMapper();

    @Test
    public void shouldReplaceEndpointBackup() throws JsonProcessingException {
        Collection<JsonPatch> jsonPatches = objectMapper.readValue(
            "[\n" +
            "  {\n" +
            "    \"jsonPath\": \"$.proxy.groups[?(@.name == 'default-group')].endpoints[?(@.name == 'default')].backup\",\n" +
            "    \"value\": false\n" +
            "  }\n" +
            "]",
            new TypeReference<>() {}
        );

        String jsonPatched = jsonPatchService.execute(API_DEFINITION, jsonPatches);

        JsonNode expectedJsonNode = objectMapper.readValue(API_DEFINITION, JsonNode.class);
        ((ObjectNode) expectedJsonNode.get("proxy").get("groups").get(0).get("endpoints").get(0)).put("backup", false);

        assertEquals(expectedJsonNode.toPrettyString(), jsonPatched);
    }

    @Test
    public void shouldRemoveEndpoint() throws JsonProcessingException {
        Collection<JsonPatch> jsonPatches = objectMapper.readValue(
            "[\n" +
            "  {\n" +
            "    \"jsonPath\": \"$.proxy.groups[?(@.name == 'default-group')].endpoints[?(@.name == 'default')]\",\n" +
            "    \"operation\": \"REMOVE\"\n" +
            "  }\n" +
            "]",
            new TypeReference<>() {}
        );

        String jsonPatched = jsonPatchService.execute(API_DEFINITION, jsonPatches);

        JsonNode expectedJsonNode = objectMapper.readValue(API_DEFINITION, JsonNode.class);
        ((ArrayNode) expectedJsonNode.get("proxy").get("groups").get(0).get("endpoints")).remove(0);

        assertEquals(expectedJsonNode.toPrettyString(), jsonPatched);
    }

    @Test
    public void shouldAddEndpoint() throws JsonProcessingException {
        Collection<JsonPatch> jsonPatches = objectMapper.readValue(
            "[\n" +
            "  {\n" +
            "    \"jsonPath\": \"$.proxy.groups[?(@.name == 'default-group')].endpoints\",\n" +
            "    \"value\": { \"name\": \"new-endpoint\" },\n" +
            "    \"operation\": \"ADD\"\n" +
            "  }\n" +
            "]",
            new TypeReference<>() {}
        );

        String jsonPatched = jsonPatchService.execute(API_DEFINITION, jsonPatches);

        JsonNode expectedJsonNode = objectMapper.readValue(API_DEFINITION, JsonNode.class);
        ObjectNode endpointNode = objectMapper.createObjectNode();
        endpointNode.put("name", "new-endpoint");
        ((ArrayNode) expectedJsonNode.get("proxy").get("groups").get(0).get("endpoints")).add(endpointNode);

        assertEquals(expectedJsonNode.toPrettyString(), jsonPatched);
    }

    @Test
    public void shouldAddEndpointAndSwitchBackup() throws JsonProcessingException {
        Collection<JsonPatch> jsonPatches = objectMapper.readValue(
            "[\n" +
            "  {\n" +
            "    \"jsonPath\": \"$.proxy.groups[?(@.name == 'default-group')].endpoints\",\n" +
            "    \"value\": { \"name\": \"new-endpoint\", \"backup\": false },\n" +
            "    \"operation\": \"ADD\"\n" +
            "  },\n" +
            "  {\n" +
            "    \"jsonPath\": \"$.proxy.groups[?(@.name == 'default-group')].endpoints[?(@.name == 'default')].backup\",\n" +
            "    \"value\": false,\n" +
            "    \"operation\": \"REPLACE\"\n" +
            "  },\n" +
            "  {\n" +
            "    \"jsonPath\": \"$.proxy.groups[?(@.name == 'default-group')].endpoints[?(@.name == 'new-endpoint')].backup\",\n" +
            "    \"value\": true,\n" +
            "    \"operation\": \"REPLACE\"\n" +
            "  }\n" +
            "]",
            new TypeReference<>() {}
        );

        String jsonPatched = jsonPatchService.execute(API_DEFINITION, jsonPatches);

        JsonNode expectedJsonNode = objectMapper.readValue(API_DEFINITION, JsonNode.class);
        ((ObjectNode) expectedJsonNode.get("proxy").get("groups").get(0).get("endpoints").get(0)).put("backup", false);
        ObjectNode endpointNode = objectMapper.createObjectNode();
        endpointNode.put("name", "new-endpoint");
        endpointNode.put("backup", true);
        ((ArrayNode) expectedJsonNode.get("proxy").get("groups").get(0).get("endpoints")).add(endpointNode);

        assertEquals(expectedJsonNode.toPrettyString(), jsonPatched);
    }

    @Test
    public void shouldAddLeafPropertyIfNotExist() throws JsonProcessingException {
        Collection<JsonPatch> jsonPatches = objectMapper.readValue(
            "[\n" +
            "  {\n" +
            "    \"jsonPath\": \"$.proxy.groups[?(@.name == 'default-group')].endpoints[?(@.name == 'default')].foobar\",\n" +
            "    \"value\": 1234,\n" +
            "    \"operation\": \"REPLACE\"\n" +
            "  }\n" +
            "]",
            new TypeReference<>() {}
        );

        String jsonPatched = jsonPatchService.execute(API_DEFINITION, jsonPatches);

        JsonNode expectedJsonNode = objectMapper.readValue(API_DEFINITION, JsonNode.class);
        ((ObjectNode) expectedJsonNode.get("proxy").get("groups").get(0).get("endpoints").get(0)).put("foobar", 1234);

        assertEquals(expectedJsonNode.toPrettyString(), jsonPatched);
    }

    @Test
    public void shouldNotAddIfBranchNotExist() throws JsonProcessingException {
        Collection<JsonPatch> jsonPatches = objectMapper.readValue(
            "[\n" +
            "  {\n" +
            "    \"jsonPath\": \"$.proxy.groups[?(@.name == 'default-group')].endpoints[?(@.name == 'foobar')].foobar\",\n" +
            "    \"value\": 1234,\n" +
            "    \"operation\": \"REPLACE\"\n" +
            "  }\n" +
            "]",
            new TypeReference<>() {}
        );

        String jsonPatched = jsonPatchService.execute(API_DEFINITION, jsonPatches);

        JsonNode expectedJsonNode = objectMapper.readValue(API_DEFINITION, JsonNode.class);

        assertEquals(expectedJsonNode.toPrettyString(), jsonPatched);
    }

    @Test(expected = JsonPatchUnsafeException.class)
    public void shouldNotInjectScript() throws JsonProcessingException {
        Collection<JsonPatch> jsonPatches = objectMapper.readValue(
            "[\n" +
            "  {\n" +
            "    \"jsonPath\": \"$.proxy.groups[?(@.name == 'default-group')].endpoints[?(@.name == 'default')].foobar\",\n" +
            "    \"value\": \"<script src=”http://localhost:8080”></script>\",\n" +
            "    \"operation\": \"ADD\"\n" +
            "  }\n" +
            "]",
            new TypeReference<>() {}
        );

        jsonPatchService.execute(API_DEFINITION, jsonPatches);
    }

    @Test(expected = JsonPatchUnsafeException.class)
    public void shouldNotInjectScriptWithObject() throws JsonProcessingException {
        Collection<JsonPatch> jsonPatches = objectMapper.readValue(
            "[\n" +
            "  {\n" +
            "    \"jsonPath\": \"$.proxy.groups[?(@.name == 'default-group')].endpoints\",\n" +
            "    \"value\": { \"name\": \"new-endpoint\", \"value\": \"<script src=”http://localhost:8080”></script>\" },\n" +
            "    \"operation\": \"ADD\"\n" +
            "  }\n" +
            "]",
            new TypeReference<>() {}
        );

        jsonPatchService.execute(API_DEFINITION, jsonPatches);
    }

    @Test(expected = JsonPatchUnsafeException.class)
    public void shouldNotInjectScriptWithArray() throws JsonProcessingException {
        Collection<JsonPatch> jsonPatches = objectMapper.readValue(
            "[\n" +
            "  {\n" +
            "    \"jsonPath\": \"$.proxy.groups[?(@.name == 'default-group')].endpoints\",\n" +
            "    \"value\": { \"name\": \"new-endpoint\", \"values\": [\"<script src=”http://localhost:8080”></script>\"] },\n" +
            "    \"operation\": \"ADD\"\n" +
            "  }\n" +
            "]",
            new TypeReference<>() {}
        );

        jsonPatchService.execute(API_DEFINITION, jsonPatches);
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldThrowTechnicalManagementExceptionWithInvalidJsonPath() throws JsonProcessingException {
        Collection<JsonPatch> jsonPatches = objectMapper.readValue(
            "[\n" +
            "  {\n" +
            "    \"jsonPath\": \"$.proxy.groups[name == 'default-group']\",\n" +
            "    \"value\": 1234,\n" +
            "    \"operation\": \"REPLACE\"\n" +
            "  }\n" +
            "]",
            new TypeReference<>() {}
        );

        jsonPatchService.execute(API_DEFINITION, jsonPatches);
    }

    @Test(expected = JsonPatchTestFailedException.class)
    public void shouldThrowJsonPatchTestFailedException() throws JsonProcessingException {
        Collection<JsonPatch> jsonPatches = objectMapper.readValue(
            "[\n" +
            "  {\n" +
            "    \"jsonPath\": \"$.proxy.groups[?(@.name == 'default-group')].endpoints\",\n" +
            "    \"value\": { \"name\": \"new-endpoint\", \"backup\": false },\n" +
            "    \"operation\": \"ADD\"\n" +
            "  },\n" +
            "  {\n" +
            "    \"jsonPath\": \"$.proxy.groups[?(@.name == 'default-group')].endpoints[?(@.name == 'default')].backup\",\n" +
            "    \"value\": false,\n" +
            "    \"operation\": \"TEST\"\n" +
            "  },\n" +
            "  {\n" +
            "    \"jsonPath\": \"$.proxy.groups[?(@.name == 'default-group')].endpoints[?(@.name == 'default')].backup\",\n" +
            "    \"value\": false,\n" +
            "    \"operation\": \"REPLACE\"\n" +
            "  },\n" +
            "  {\n" +
            "    \"jsonPath\": \"$.proxy.groups[?(@.name == 'default-group')].endpoints[?(@.name == 'new-endpoint')].backup\",\n" +
            "    \"value\": true,\n" +
            "    \"operation\": \"REPLACE\"\n" +
            "  }\n" +
            "]",
            new TypeReference<>() {}
        );

        jsonPatchService.execute(API_DEFINITION, jsonPatches);
    }

    @Test
    public void shouldManageArrays() throws JsonProcessingException {
        Collection<JsonPatch> jsonPatches = objectMapper.readValue(
            "[\n" +
            " {\n" +
            "   \"jsonPath\": \"$.properties\",\n" +
            "   \"value\": \"null\",\n" +
            "   \"operation\": \"TEST\"\n" +
            "  },\n" +
            " {\n" +
            "   \"jsonPath\": \"$.properties\",\n" +
            "   \"value\": [\n" +
            "     { \"key\": \"a\", \"value\": \"0\" },\n" +
            "     { \"key\": \"b\", \"value\": \"1\" }\n" +
            "   ],\n" +
            "   \"operation\": \"REPLACE\"\n" +
            "  },\n" +
            " {\n" +
            "   \"jsonPath\": \"$.properties.length()\",\n" +
            "   \"value\": 2,\n" +
            "   \"operation\": \"TEST\"\n" +
            "  },\n" +
            " {\n" +
            "   \"jsonPath\": \"$.properties\",\n" +
            "   \"value\": { \"key\": \"c\", \"value\": 2 },\n" +
            "   \"operation\": \"ADD\"\n" +
            "  }\n" +
            "]",
            new TypeReference<>() {}
        );

        String jsonPatched = jsonPatchService.execute(API_DEFINITION, jsonPatches);

        JsonNode expectedJsonNode = objectMapper.readValue(API_DEFINITION, JsonNode.class);
        ((ObjectNode) expectedJsonNode).put("properties", objectMapper.createArrayNode());
        ObjectNode aNode = objectMapper.createObjectNode();
        aNode.put("key", "a");
        aNode.put("value", "0");
        ObjectNode bNode = objectMapper.createObjectNode();
        bNode.put("key", "b");
        bNode.put("value", "1");
        ObjectNode cNode = objectMapper.createObjectNode();
        cNode.put("key", "c");
        cNode.put("value", 2);
        ((ArrayNode) expectedJsonNode.get("properties")).add(aNode);
        ((ArrayNode) expectedJsonNode.get("properties")).add(bNode);
        ((ArrayNode) expectedJsonNode.get("properties")).add(cNode);

        assertEquals(expectedJsonNode.toPrettyString(), jsonPatched);
    }
}
