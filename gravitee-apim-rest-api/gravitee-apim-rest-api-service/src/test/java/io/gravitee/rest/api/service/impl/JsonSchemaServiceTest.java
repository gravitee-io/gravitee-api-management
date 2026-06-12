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

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.json.validation.JsonSchemaValidatorImpl;
import io.gravitee.rest.api.service.JsonSchemaService;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
@MockitoSettings(strictness = Strictness.WARN)
@ExtendWith(MockitoExtension.class)
public class JsonSchemaServiceTest {

    private final JsonSchemaService jsonSchemaService = new JsonSchemaServiceImpl(new JsonSchemaValidatorImpl());

    @Test
    public void shouldRejectInValidJsonConfigurationWithDependencies() throws IOException {
        String schema = Files.readString(Path.of("src/test/resources/io/gravitee/rest/api/management/service/http-connector.json"));
        String configuration = "{\"ssl\": {\n \"trustStore\": {\n \"type\": \"PEM\"\n }\n }}";

        assertThatThrownBy(() -> jsonSchemaService.validate(schema, configuration))
            .isInstanceOf(InvalidDataException.class)
            .hasMessage("$.ssl.trustStore: must be valid to one and only one schema, but 0 are valid");
    }

    @Test
    public void shouldAcceptValidJsonConfigurationWithDependencies() throws IOException {
        String schema = Files.readString(Path.of("src/test/resources/io/gravitee/rest/api/management/service/http-connector.json"));
        String configuration = """
            {
              "ssl": {
                "trustStore": {
                  "type": "PEM",
                  "path": "..."
                 }
              },
              "http": {
                "readTimeout": 7777
              }
            }""";
        String validate = jsonSchemaService.validate(schema, configuration);
        assertThatJson(validate).isEqualTo(
            """
            {"http":{"keepAliveTimeout":30000,"readTimeout":7777,"idleTimeout":60000,"connectTimeout":5000,"maxConcurrentConnections":100},"ssl":{"trustStore":{"path":"...","type":"PEM"},"hostnameVerifier":false,"trustAll":false}}"""
        );
    }

    @Test
    public void shouldReconcileDefaultValuesWithGioExternalDefinitions() {
        String schema = """
            {
              "type": "object",
              "gioExternalDefinitions": {
                "def1": {
                  "type": "object",
                  "properties": {
                    "field1": {
                      "type": "string"
                    },
                    "field2": {
                      "type": "string",
                      "default": "default2"
                    }
                  },
                  "required": ["field1", "field2"]
                }
              },
              "properties": {
                "obj": {
                  "$ref": "#/gioExternalDefinitions/def1"
                }
              }
            }""";
        String configuration = "{\"obj\": {\"field1\": \"value1\"}}";

        String result = jsonSchemaService.validate(schema, configuration);

        assertThatJson(result).isEqualTo(
            """
            {"obj":{"field1":"value1","field2":"default2"}}"""
        );
    }
}
