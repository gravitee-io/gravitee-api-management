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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.json.validation.JsonSchemaValidatorImpl;
import io.gravitee.rest.api.service.JsonSchemaService;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class JsonSchemaServiceTest {

    private final JsonSchemaService jsonSchemaService = new JsonSchemaServiceImpl(new JsonSchemaValidatorImpl());

    @Test
    public void shouldRejectInValidJsonConfigurationWithDependencies() throws IOException {
        String schema = Files.readString(Path.of("src/test/resources/io/gravitee/rest/api/management/service/http-connector.json"));
        String configuration = "{\"ssl\": {\n \"trustStore\": {\n \"type\": \"PEM\"\n }\n }}";

        assertThatThrownBy(() -> jsonSchemaService.validate(schema, configuration))
            .isInstanceOf(InvalidDataException.class)
            .hasMessage(
                "#/ssl/trustStore/type: \n" +
                "#/ssl/trustStore: required key [content] not found\n" +
                "#/ssl/trustStore: required key [path] not found\n" +
                "#/ssl/trustStore: required key [content] not found\n" +
                "#/ssl/trustStore: required key [password] not found\n" +
                "#/ssl/trustStore/type: string [PEM] does not match pattern JKS|PKCS12\n" +
                "#/ssl/trustStore: required key [path] not found\n" +
                "#/ssl/trustStore: required key [password] not found\n" +
                "#/ssl/trustStore/type: string [PEM] does not match pattern JKS|PKCS12"
            );
    }

    @Test
    public void shouldAcceptValidJsonConfigurationWithDependencies() throws IOException {
        String schema = Files.readString(Path.of("src/test/resources/io/gravitee/rest/api/management/service/http-connector.json"));
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
        assertThat(validate)
            .isEqualTo(
                "{\"http\":{\"keepAliveTimeout\":30000,\"readTimeout\":7777,\"idleTimeout\":60000,\"connectTimeout\":5000,\"maxConcurrentConnections\":100},\"ssl\":{\"trustStore\":{\"path\":\"...\",\"type\":\"PEM\"},\"hostnameVerifier\":false,\"trustAll\":false}}"
            );
    }
}
