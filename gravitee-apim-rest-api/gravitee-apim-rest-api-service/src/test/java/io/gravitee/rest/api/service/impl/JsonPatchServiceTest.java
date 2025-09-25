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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

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
import io.gravitee.rest.api.service.sanitizer.HtmlSanitizer;
import java.util.Collection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.env.MockEnvironment;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class JsonPatchServiceTest {

    static final String API_DEFINITION = """
        {
          "description" : "Gravitee.io",
          "paths" : { },
          "path_mappings":[],
          "proxy": {
            "virtual_hosts": [{
              "path": "/test"
            }],
            "strip_context_path": false,
            "preserve_host":false,
            "logging": {
              "mode":"CLIENT_PROXY",
              "condition":"condition"
            },
            "groups": [
              {
                "name": "default-group",
                "endpoints": [
                  {
                    "name": "default",
                    "target": "http://test",
                    "weight": 1,
                    "backup": true,
                    "type": "HTTP",
                    "http": {
                      "connectTimeout": 5000,
                      "idleTimeout": 60000,
                      "keepAliveTimeout": 30000,
                      "keepAlive": true,
                      "readTimeout": 10000,
                      "pipelining": false,
                      "maxConcurrentConnections": 100,
                      "useCompression": true,
                      "followRedirects": false,
                      "encodeURI":false
                    }
                  }
                ],
                "load_balancing": {
                  "type": "ROUND_ROBIN"
                },
                "http": {
                  "connectTimeout": 5000,
                  "idleTimeout": 60000,
                  "keepAlive": true,
                  "readTimeout": 10000,
                  "pipelining": false,
                  "maxConcurrentConnections": 100,
                  "useCompression": true,
                  "followRedirects": false,
                  "encodeURI":false
                }
              }
            ]
          }
        }
        """;

    JsonPatchServiceImpl jsonPatchService;

    @Spy
    GraviteeMapper objectMapper = new GraviteeMapper();

    @BeforeEach
    public void init() {
        var htmlSanitizer = new HtmlSanitizer(new MockEnvironment());
        jsonPatchService = new JsonPatchServiceImpl(objectMapper, htmlSanitizer);
    }

    @Test
    void should_replace_endpoint_backup() throws JsonProcessingException {
        Collection<JsonPatch> jsonPatches = objectMapper.readValue(
            """
            [
              {
                "jsonPath": "$.proxy.groups[?(@.name == 'default-group')].endpoints[?(@.name == 'default')].backup",
                "value": false
              }
            ]""",
            new TypeReference<>() {}
        );

        String jsonPatched = jsonPatchService.execute(API_DEFINITION, jsonPatches);

        JsonNode expectedJsonNode = objectMapper.readValue(API_DEFINITION, JsonNode.class);
        ((ObjectNode) expectedJsonNode.get("proxy").get("groups").get(0).get("endpoints").get(0)).put("backup", false);

        assertThat(jsonPatched).isEqualTo(expectedJsonNode.toPrettyString());
    }

    @Test
    void should_remove_endpoint() throws JsonProcessingException {
        Collection<JsonPatch> jsonPatches = objectMapper.readValue(
            """
            [
              {
                "jsonPath": "$.proxy.groups[?(@.name == 'default-group')].endpoints[?(@.name == 'default')]",
                "operation": "REMOVE"
              }
            ]""",
            new TypeReference<>() {}
        );

        String jsonPatched = jsonPatchService.execute(API_DEFINITION, jsonPatches);

        JsonNode expectedJsonNode = objectMapper.readValue(API_DEFINITION, JsonNode.class);
        ((ArrayNode) expectedJsonNode.get("proxy").get("groups").get(0).get("endpoints")).remove(0);

        assertThat(jsonPatched).isEqualTo(expectedJsonNode.toPrettyString());
    }

    @Test
    void should_add_endpoint() throws JsonProcessingException {
        Collection<JsonPatch> jsonPatches = objectMapper.readValue(
            """
            [
              {
                "jsonPath": "$.proxy.groups[?(@.name == 'default-group')].endpoints",
                "value": { "name": "new-endpoint" },
                "operation": "ADD"
              }
            ]""",
            new TypeReference<>() {}
        );

        String jsonPatched = jsonPatchService.execute(API_DEFINITION, jsonPatches);

        JsonNode expectedJsonNode = objectMapper.readValue(API_DEFINITION, JsonNode.class);
        ObjectNode endpointNode = objectMapper.createObjectNode();
        endpointNode.put("name", "new-endpoint");
        ((ArrayNode) expectedJsonNode.get("proxy").get("groups").get(0).get("endpoints")).add(endpointNode);

        assertThat(jsonPatched).isEqualTo(expectedJsonNode.toPrettyString());
    }

    @Test
    void should_add_endpoint_and_switch_backup() throws JsonProcessingException {
        Collection<JsonPatch> jsonPatches = objectMapper.readValue(
            """
            [
              {
                "jsonPath": "$.proxy.groups[?(@.name == 'default-group')].endpoints",
                "value": { "name": "new-endpoint", "backup": false },
                "operation": "ADD"
              },
              {
                "jsonPath": "$.proxy.groups[?(@.name == 'default-group')].endpoints[?(@.name == 'default')].backup",
                "value": false,
                "operation": "REPLACE"
              },
              {
                "jsonPath": "$.proxy.groups[?(@.name == 'default-group')].endpoints[?(@.name == 'new-endpoint')].backup",
                "value": true,
                "operation": "REPLACE"
              }
            ]""",
            new TypeReference<>() {}
        );

        String jsonPatched = jsonPatchService.execute(API_DEFINITION, jsonPatches);

        JsonNode expectedJsonNode = objectMapper.readValue(API_DEFINITION, JsonNode.class);
        ((ObjectNode) expectedJsonNode.get("proxy").get("groups").get(0).get("endpoints").get(0)).put("backup", false);
        ObjectNode endpointNode = objectMapper.createObjectNode();
        endpointNode.put("name", "new-endpoint");
        endpointNode.put("backup", true);
        ((ArrayNode) expectedJsonNode.get("proxy").get("groups").get(0).get("endpoints")).add(endpointNode);

        assertThat(jsonPatched).isEqualTo(expectedJsonNode.toPrettyString());
    }

    @Test
    void should_add_leaf_property_if_not_exist() throws JsonProcessingException {
        Collection<JsonPatch> jsonPatches = objectMapper.readValue(
            """
            [
              {
                "jsonPath": "$.proxy.groups[?(@.name == 'default-group')].endpoints[?(@.name == 'default')].foobar",
                "value": 1234,
                "operation": "REPLACE"
              }
            ]""",
            new TypeReference<>() {}
        );

        String jsonPatched = jsonPatchService.execute(API_DEFINITION, jsonPatches);

        JsonNode expectedJsonNode = objectMapper.readValue(API_DEFINITION, JsonNode.class);
        ((ObjectNode) expectedJsonNode.get("proxy").get("groups").get(0).get("endpoints").get(0)).put("foobar", 1234);

        assertThat(jsonPatched).isEqualTo(expectedJsonNode.toPrettyString());
    }

    @Test
    void should_not_replace_if_branch_not_exist() throws JsonProcessingException {
        // first operation should fail silently, and the second one you go through
        Collection<JsonPatch> jsonPatches = objectMapper.readValue(
            """
            [
              {
                "jsonPath": "$.proxy.groups[?(@.name == 'default-group')].endpoints[?(@.name == 'foobar')].foobar",
                "value": 1234,
                "operation": "REPLACE"
              },
              {
                "jsonPath": "$.proxy.groups[?(@.name == 'default-group')].endpoints",
                "value": { "name": "new-endpoint" },
                "operation": "ADD"
              }
            ]""",
            new TypeReference<>() {}
        );

        String jsonPatched = jsonPatchService.execute(API_DEFINITION, jsonPatches);

        JsonNode expectedJsonNode = objectMapper.readValue(API_DEFINITION, JsonNode.class);
        ObjectNode endpointNode = objectMapper.createObjectNode();
        endpointNode.put("name", "new-endpoint");
        ((ArrayNode) expectedJsonNode.get("proxy").get("groups").get(0).get("endpoints")).add(endpointNode);

        assertThat(jsonPatched).isEqualTo(expectedJsonNode.toPrettyString());
    }

    @ParameterizedTest(name = "[{index}]")
    @ValueSource(
        strings = {
            """
            [
              {
                "jsonPath": "$.proxy.groups[?(@.name == 'default-group')].endpoints[?(@.name == 'default')].foobar",
                "value": "<script src=”http://localhost:8080”></script>",
                "operation": "ADD"
              }
            ]""",
            """
            [
              {
                "jsonPath": "$.proxy.groups[?(@.name == 'default-group')].endpoints",
                "value": { "name": "new-endpoint", "value": "<script src=”http://localhost:8080”></script>" },
                "operation": "ADD"
              }
            ]""",
            """
            [
              {
                "jsonPath": "$.proxy.groups[?(@.name == 'default-group')].endpoints",
                "value": { "name": "new-endpoint", "values": ["<script src=”http://localhost:8080”></script>"] },
                "operation": "ADD"
              }
            ]""",
        }
    )
    void should_generate_an_unsafe_error(String unsafePatch) throws JsonProcessingException {
        Collection<JsonPatch> jsonPatches = objectMapper.readValue(unsafePatch, new TypeReference<>() {});

        assertThatCode(() -> jsonPatchService.execute(API_DEFINITION, jsonPatches)).isInstanceOf(JsonPatchUnsafeException.class);
    }

    @Test
    void shouldThrowTechnicalManagementExceptionWithInvalidJsonPath() throws JsonProcessingException {
        Collection<JsonPatch> jsonPatches = objectMapper.readValue(
            """
            [
              {
                "jsonPath": "$.proxy.groups[name == 'default-group']",
                "value": 1234,
                "operation": "REPLACE"
              }
            ]""",
            new TypeReference<>() {}
        );

        assertThatCode(() -> jsonPatchService.execute(API_DEFINITION, jsonPatches)).isInstanceOf(TechnicalManagementException.class);
    }

    @Test
    void shouldThrowJsonPatchTestFailedException() throws JsonProcessingException {
        Collection<JsonPatch> jsonPatches = objectMapper.readValue(
            """
            [
              {
                "jsonPath": "$.proxy.groups[?(@.name == 'default-group')].endpoints",
                "value": { "name": "new-endpoint", "backup": false },
                "operation": "ADD"
              },
              {
                "jsonPath": "$.proxy.groups[?(@.name == 'default-group')].endpoints[?(@.name == 'default')].backup",
                "value": false,
                "operation": "TEST"
              },
              {
                "jsonPath": "$.proxy.groups[?(@.name == 'default-group')].endpoints[?(@.name == 'default')].backup",
                "value": false,
                "operation": "REPLACE"
              },
              {
                "jsonPath": "$.proxy.groups[?(@.name == 'default-group')].endpoints[?(@.name == 'new-endpoint')].backup",
                "value": true,
                "operation": "REPLACE"
              }
            ]""",
            new TypeReference<>() {}
        );

        assertThatCode(() -> jsonPatchService.execute(API_DEFINITION, jsonPatches)).isInstanceOf(JsonPatchTestFailedException.class);
    }

    @Test
    void shouldManageArrays() throws JsonProcessingException {
        Collection<JsonPatch> jsonPatches = objectMapper.readValue(
            """
            [
             {
               "jsonPath": "$.properties",
               "value": "null",
               "operation": "TEST"
              },
             {
               "jsonPath": "$.properties",
               "value": [
                 { "key": "a", "value": "0" },
                 { "key": "b", "value": "1" }
               ],
               "operation": "REPLACE"
              },
             {
               "jsonPath": "$.properties.length()",
               "value": 2,
               "operation": "TEST"
              },
             {
               "jsonPath": "$.properties",
               "value": { "key": "c", "value": 2 },
               "operation": "ADD"
              }
            ]""",
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

        assertThat(jsonPatched).isEqualTo(expectedJsonNode.toPrettyString());
    }
}
