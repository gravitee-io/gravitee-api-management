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
package io.gravitee.rest.api.service;

import static org.junit.Assert.assertEquals;

import com.github.fge.jsonschema.main.JsonSchemaFactory;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import io.gravitee.rest.api.service.impl.JsonSchemaServiceImpl;
import io.gravitee.rest.api.service.spring.ServiceConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class JsonSchemaServiceTest {

    @InjectMocks
    private JsonSchemaService jsonSchemaService = new JsonSchemaServiceImpl();

    private JsonSchemaFactory jsonSchemaFactory = new ServiceConfiguration().jsonSchemaFactory();

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

    @Before
    public void before() {
        // Manually set the jsonSchemaFactory.
        ReflectionTestUtils.setField(jsonSchemaService, "jsonSchemaFactory", jsonSchemaFactory);
    }

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
        String schemaWithJavaRegexFormat = JSON_SCHEMA.replace(
            "\"type\": \"string\"\n",
            "\"type\": \"string\"\n,\"format\": \"java-regex\""
        );

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
            assertEquals("Invalid configuration : Invalid java regular expression [( INVALID regex]", e.getMessage());
            throw e;
        }
    }

    @Test
    public void shouldAcceptValidJssonConfiguration_defaultValue() throws Exception {
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
            "    \"name\"\n" +
            "  ]\n" +
            "}";

        String configuration = "{ \"valid\": true }";
        jsonSchemaService.validate(JSON_SCHEMA, configuration);
    }
}
