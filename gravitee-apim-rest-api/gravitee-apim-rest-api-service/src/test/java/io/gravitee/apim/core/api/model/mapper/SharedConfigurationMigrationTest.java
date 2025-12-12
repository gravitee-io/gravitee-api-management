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
package io.gravitee.apim.core.api.model.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.http.HttpHeader;
import io.gravitee.definition.model.EndpointGroup;
import io.gravitee.definition.model.HttpClientOptions;
import io.gravitee.definition.model.HttpClientSslOptions;
import io.gravitee.definition.model.HttpProxy;
import io.gravitee.definition.model.ProtocolVersion;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SharedConfigurationMigrationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private SharedConfigurationMigration sharedConfigurationMigration;

    @BeforeEach
    void setUp() {
        sharedConfigurationMigration = new SharedConfigurationMigration(objectMapper);
    }

    @Test
    void should_convert_endpoint_group() throws JsonProcessingException {
        var group = new EndpointGroup();
        var options = new HttpClientOptions();
        options.setVersion(ProtocolVersion.HTTP_1_1);
        group.setHttpClientOptions(options);
        group.setHttpClientSslOptions(new HttpClientSslOptions());
        var headers = List.of(new HttpHeader("X-Any-Header", "any header"));
        group.setHeaders(headers);
        group.setHttpProxy(new HttpProxy());

        var result = sharedConfigurationMigration.convert(group);
        JsonNode json = objectMapper.readTree(result);

        JsonNode httpNode = json.get("http");
        assertThat(httpNode).isNotNull();
        assertThat(httpNode.get("version").asText()).isEqualTo(ProtocolVersion.HTTP_1_1.name());
        assertThat(json.get("ssl")).isNotNull();
        assertThat(json.get("headers")).isNotNull();
        assertThat(json.get("headers")).hasSize(1);
        JsonNode firstHeader = json.get("headers").get(0);
        assertThat(firstHeader.get("name").asText()).isEqualTo("X-Any-Header");
        assertThat(firstHeader.get("value").asText()).isEqualTo("any header");
        assertThat(json.get("proxy")).isNotNull();
    }

    @Test
    void should_not_set_properties_if_null() throws JsonProcessingException {
        var group = new EndpointGroup();
        var options = new HttpClientOptions();
        options.setVersion(ProtocolVersion.HTTP_1_1);
        group.setHttpClientOptions(options);
        group.setHttpClientSslOptions(null);
        group.setHttpProxy(null);

        var result = sharedConfigurationMigration.convert(group);
        JsonNode json = objectMapper.readTree(result);

        assertThat(json.has("http")).isTrue();
        assertThat(json.has("ssl")).isFalse();
        assertThat(json.has("headers")).isFalse();
        assertThat(json.has("proxy")).isFalse();
    }
}
