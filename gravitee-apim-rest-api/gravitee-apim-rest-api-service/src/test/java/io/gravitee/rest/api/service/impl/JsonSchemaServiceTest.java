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
package io.gravitee.rest.api.service.impl;

import static org.junit.Assert.assertEquals;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import io.gravitee.rest.api.service.JsonSchemaService;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import io.gravitee.rest.api.service.impl.JsonSchemaServiceImpl;
import java.io.IOException;
import java.net.URL;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class JsonSchemaServiceTest {

    @InjectMocks
    private JsonSchemaService jsonSchemaService = new JsonSchemaServiceImpl();

    private static final String JSON_SCHEMA =
        "{\n" +
        "  \"type\": \"object\",\n" +
        "  \"id\": \"urn:jsonschema:io:gravitee:policy:test\",\n" +
        "  \"properties\": {\n" +
        "    \"name\": {\n" +
        "      \"title\": \"Name\",\n" +
        "      \"type\": \"string\"\n" +
        "    },\n" +
        "    \"valid\": {\n" +
        "      \"title\": \"Valid\",\n" +
        "      \"type\": \"boolean\",\n" +
        "      \"default\": false\n" +
        "    }\n" +
        "  },\n" +
        "  \"required\": [\n" +
        "    \"name\"\n" +
        "  ]\n" +
        "}";

    @Test
    public void shouldAcceptValidJsonConfigurationWithSchema() {
        String configuration = "{ \"name\": \"test\", \"valid\": true }";
        jsonSchemaService.validate(JSON_SCHEMA, configuration);
    }

    @Test(expected = InvalidDataException.class)
    public void shouldRejectInvalidJsonConfigurationWithSchema() throws Exception {
        String configuration = "{ \"name\": true, \"valid\": true }";
        jsonSchemaService.validate(JSON_SCHEMA, configuration);
    }

    @Test
    public void shouldAcceptValidJsonConfigurationWithCustomJavaRegexFormat() throws Exception {
        String schemaWithJavaRegexFormat = JSON_SCHEMA.replace("\"type\": \"string\"\n", "\"type\": \"string\"\n,\"format\": \"regex\"");

        String configuration = "{ \"name\": \"^.*[A-Za-z]\\\\d*$\", \"valid\": true }";
        jsonSchemaService.validate(schemaWithJavaRegexFormat, configuration);
    }

    @Test(expected = InvalidDataException.class)
    public void shouldRejectInvalidJsonConfigurationWithCustomJavaRegexFormat() throws Exception {
        try {
            String schemaWithJavaRegexFormat = JSON_SCHEMA.replace(
                "\"type\": \"string\"\n",
                "\"type\": \"string\"\n,\"format\": \"java-regex\""
            );

            String configuration = "{ \"name\": \"( INVALID regex\", \"valid\": true }";
            jsonSchemaService.validate(schemaWithJavaRegexFormat, configuration);
        } catch (InvalidDataException e) {
            assertEquals("#/name: [( INVALID regex] is not a valid regular expression", e.getMessage());
            throw e;
        }
    }

    @Test
    public void shouldAcceptValidJsonConfiguration_defaultValue() {
        final String JSON_SCHEMA =
            "{\n" +
            "  \"type\": \"object\",\n" +
            "  \"id\": \"urn:jsonschema:io:gravitee:policy:test\",\n" +
            "  \"properties\": {\n" +
            "    \"name\": {\n" +
            "      \"title\": \"Name\",\n" +
            "      \"type\": \"string\",\n" +
            "      \"default\": \"test\"\n" +
            "    },\n" +
            "    \"valid\": {\n" +
            "      \"title\": \"Valid\",\n" +
            "      \"type\": \"boolean\",\n" +
            "      \"default\": false\n" +
            "    }\n" +
            "  },\n" +
            "  \"required\": [\n" +
            "    \"name\",\n" +
            "    \"valid\"\n" +
            "  ]\n" +
            "}";

        String configuration = "{}";
        String validate = jsonSchemaService.validate(JSON_SCHEMA, configuration);
        assertEquals(validate, "{\"valid\":false,\"name\":\"test\"}");
    }

    @Test
    public void shouldAcceptValidJsonComplexConfiguration_defaultValue() {
        final String JSON_SCHEMA =
            "{\n" +
            "  \"type\": \"object\",\n" +
            "  \"properties\": {\n" +
            "    \"name\": {\n" +
            "      \"title\": \"Name\",\n" +
            "      \"type\": \"string\",\n" +
            "      \"default\": \"test\"\n" +
            "    },\n" +
            "    \"address\": {\n" +
            "      \"type\": \"object\",\n" +
            "      \"properties\": {\n" +
            "        \"city\": {\n" +
            "          \"title\": \"City\",\n" +
            "          \"type\": \"string\",\n" +
            "          \"default\": \"Lille\"\n" +
            "         }\n" +
            "       },\n" +
            "       \"required\": [\n" +
            "         \"city\"\n" +
            "        ],\n" +
            "      \"default\": {\n" +
            "        \"city\": \"Lille\"\n" +
            "       }\n" +
            "    },\n" +
            "    \"valid\": {\n" +
            "      \"title\": \"Valid\",\n" +
            "      \"type\": \"boolean\",\n" +
            "      \"default\": false\n" +
            "    }\n" +
            "  },\n" +
            "  \"required\": [\n" +
            "    \"name\",\n" +
            "    \"valid\",\n" +
            "    \"address\"\n" +
            "  ]\n" +
            "}";

        String configuration = "{\"additional-property\": true}";
        String validate = jsonSchemaService.validate(JSON_SCHEMA, configuration);
        assertEquals(validate, "{\"additional-property\":true,\"valid\":false,\"address\":{\"city\":\"Lille\"},\"name\":\"test\"}");
    }

    @Test(expected = InvalidDataException.class)
    public void shouldRejectInValidJsonConfigurationWithDependencies() throws IOException {
        URL url = Resources.getResource("io/gravitee/rest/api/management/service/http-connector.json");
        String schema = Resources.toString(url, Charsets.UTF_8);
        String configuration = "{\"ssl\": {\n \"trustStore\": {\n \"type\": \"PEM\"\n }\n }}";
        try {
            String validate = jsonSchemaService.validate(schema, configuration);
            assertEquals(validate, "{}");
        } catch (InvalidDataException e) {
            assertEquals(
                "#/ssl/trustStore/type: \n" +
                "#/ssl/trustStore: required key [content] not found\n" +
                "#/ssl/trustStore: required key [path] not found\n" +
                "#/ssl/trustStore: required key [content] not found\n" +
                "#/ssl/trustStore: required key [password] not found\n" +
                "#/ssl/trustStore/type: string [PEM] does not match pattern JKS|PKCS12\n" +
                "#/ssl/trustStore: required key [path] not found\n" +
                "#/ssl/trustStore: required key [password] not found\n" +
                "#/ssl/trustStore/type: string [PEM] does not match pattern JKS|PKCS12",
                e.getMessage()
            );
            throw e;
        }
    }

    @Test
    public void shouldAcceptValidJsonConfigurationWithDependencies() throws IOException {
        URL url = Resources.getResource("io/gravitee/rest/api/management/service/http-connector.json");
        String schema = Resources.toString(url, Charsets.UTF_8);
        String configuration =
            "{\n" +
            "  \"ssl\": {\n" +
            "    \"trustStore\": {\n" +
            "      \"type\": \"PEM\",\n" +
            "      \"path\": \"...\"\n" +
            "     }\n" +
            "  },\n" +
            "  \"http\": {\n" +
            "    \"readTimeout\": 7777\n" +
            "  }\n" +
            "}";
        String validate = jsonSchemaService.validate(schema, configuration);
        assertEquals(
            validate,
            "{\"http\":{\"readTimeout\":7777,\"idleTimeout\":60000,\"connectTimeout\":5000,\"maxConcurrentConnections\":100},\"ssl\":{\"trustStore\":{\"path\":\"...\",\"type\":\"PEM\"},\"hostnameVerifier\":false,\"trustAll\":false}}"
        );
    }

    @Test
    public void shouldAcceptValidJsonAndRemoveAdditionalProperties() {
        final String JSON_SCHEMA =
            "{\n" +
            "  \"type\": \"object\",\n" +
            "  \"properties\": {\n" +
            "    \"name\": {\n" +
            "      \"title\": \"Name\",\n" +
            "      \"type\": \"string\",\n" +
            "      \"default\": \"test\"\n" +
            "    },\n" +
            "    \"address\": {\n" +
            "      \"type\": \"object\",\n" +
            "      \"properties\": {\n" +
            "        \"city\": {\n" +
            "          \"title\": \"City\",\n" +
            "          \"type\": \"string\",\n" +
            "          \"default\": \"Lille\"\n" +
            "         }\n" +
            "       },\n" +
            "       \"required\": [\n" +
            "         \"city\"\n" +
            "        ],\n" +
            "      \"default\": {\n" +
            "        \"city\": \"Lille\"\n" +
            "       }\n" +
            "    },\n" +
            "    \"valid\": {\n" +
            "      \"title\": \"Valid\",\n" +
            "      \"type\": \"boolean\",\n" +
            "      \"default\": false\n" +
            "    }\n" +
            "  },\n" +
            "  \"required\": [\n" +
            "    \"name\",\n" +
            "    \"valid\",\n" +
            "    \"address\"\n" +
            "  ],\n" +
            "  \"additionalProperties\": false" +
            "}";

        String configuration = "{\"additional-property\": true}";
        String validate = jsonSchemaService.validate(JSON_SCHEMA, configuration);
        assertEquals(validate, "{\"valid\":false,\"address\":{\"city\":\"Lille\"},\"name\":\"test\"}");
    }

    @Test
    public void shouldAcceptValidJsonAndRemoveAdditionalAndKeepPatternProperties() {
        final String JSON_SCHEMA =
            "{\n" +
            "  \"type\": \"object\",\n" +
            "  \"properties\": {\n" +
            "    \"name\": {\n" +
            "      \"title\": \"Name\",\n" +
            "      \"type\": \"string\",\n" +
            "      \"default\": \"test\"\n" +
            "    },\n" +
            "    \"address\": {\n" +
            "      \"type\": \"object\",\n" +
            "      \"properties\": {\n" +
            "        \"city\": {\n" +
            "          \"title\": \"City\",\n" +
            "          \"type\": \"string\",\n" +
            "          \"default\": \"Lille\"\n" +
            "         }\n" +
            "       },\n" +
            "       \"required\": [\n" +
            "         \"city\"\n" +
            "        ],\n" +
            "      \"default\": {\n" +
            "        \"city\": \"Lille\"\n" +
            "       }\n" +
            "    },\n" +
            "    \"valid\": {\n" +
            "      \"title\": \"Valid\",\n" +
            "      \"type\": \"boolean\",\n" +
            "      \"default\": false\n" +
            "    }\n" +
            "  },\n" +
            "  \"required\": [\n" +
            "    \"name\",\n" +
            "    \"valid\",\n" +
            "    \"address\"\n" +
            "  ],\n" +
            "  \"additionalProperties\": false,\n" +
            "  \"patternProperties\": {\n" +
            "    \"to-keep\": true\n" +
            "  }\n" +
            "}";

        String configuration = "{\"additional-property\": true, \"to-keep\": \"value\"}";
        String validate = jsonSchemaService.validate(JSON_SCHEMA, configuration);
        assertEquals(validate, "{\"valid\":false,\"address\":{\"city\":\"Lille\"},\"name\":\"test\",\"to-keep\":\"value\"}");
    }
}
