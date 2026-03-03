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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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

    @Test
    void should_produce_empty_json_when_all_options_are_null() throws JsonProcessingException {
        var group = new EndpointGroup();
        group.setHttpClientOptions(null);
        group.setHttpClientSslOptions(null);
        group.setHeaders(null);
        group.setHttpProxy(null);

        var result = sharedConfigurationMigration.convert(group);
        JsonNode json = objectMapper.readTree(result);

        assertThat(json.has("http")).isFalse();
        assertThat(json.has("ssl")).isFalse();
        assertThat(json.has("headers")).isFalse();
        assertThat(json.has("proxy")).isFalse();
        assertThat(json.isEmpty()).isTrue();
    }

    @Test
    void should_include_http2_specific_fields_when_version_is_http2() throws JsonProcessingException {
        var group = new EndpointGroup();
        var options = new HttpClientOptions();
        options.setVersion(ProtocolVersion.HTTP_2);
        options.setClearTextUpgrade(true);
        group.setHttpClientOptions(options);

        var result = sharedConfigurationMigration.convert(group);
        JsonNode json = objectMapper.readTree(result);

        JsonNode httpNode = json.get("http");
        assertThat(httpNode).isNotNull();
        assertThat(httpNode.get("version").asText()).isEqualTo(ProtocolVersion.HTTP_2.name());
        assertThat(httpNode.has("clearTextUpgrade")).isTrue();
        assertThat(httpNode.get("clearTextUpgrade").asBoolean()).isTrue();
    }

    @Test
    void should_not_include_clearTextUpgrade_when_version_is_http11() throws JsonProcessingException {
        var group = new EndpointGroup();
        var options = new HttpClientOptions();
        options.setVersion(ProtocolVersion.HTTP_1_1);
        options.setClearTextUpgrade(true); // should be ignored for HTTP_1_1
        group.setHttpClientOptions(options);

        var result = sharedConfigurationMigration.convert(group);
        JsonNode json = objectMapper.readTree(result);

        JsonNode httpNode = json.get("http");
        assertThat(httpNode).isNotNull();
        assertThat(httpNode.get("version").asText()).isEqualTo(ProtocolVersion.HTTP_1_1.name());
        assertThat(httpNode.has("clearTextUpgrade")).isFalse();
    }

    @Nested
    class ProxyMigrationTest {

        @Test
        void should_map_proxy_with_use_system_proxy_enabled() throws JsonProcessingException {
            var group = new EndpointGroup();
            var options = new HttpClientOptions();
            options.setVersion(ProtocolVersion.HTTP_1_1);
            group.setHttpClientOptions(options);

            var proxy = new HttpProxy();
            proxy.setEnabled(true);
            proxy.setUseSystemProxy(true);
            proxy.setHost("proxy.example.com");
            proxy.setPort(8080);
            group.setHttpProxy(proxy);

            var result = sharedConfigurationMigration.convert(group);
            JsonNode json = objectMapper.readTree(result);

            JsonNode proxyNode = json.get("proxy");
            assertThat(proxyNode).isNotNull();
            assertThat(proxyNode.has("host")).isFalse();
            assertThat(proxyNode.has("port")).isFalse();
            assertThat(proxyNode.has("type")).isFalse();
            assertThat(proxyNode.has("enabled")).isFalse();
            assertThat(proxyNode.get("useSystemProxy").asBoolean()).isTrue();
        }

        @Test
        void should_not_modify_proxy_when_not_using_system_proxy() throws JsonProcessingException {
            var group = new EndpointGroup();
            var options = new HttpClientOptions();
            options.setVersion(ProtocolVersion.HTTP_1_1);
            group.setHttpClientOptions(options);

            var proxy = new HttpProxy();
            proxy.setEnabled(true);
            proxy.setUseSystemProxy(false);
            proxy.setHost("proxy.example.com");
            proxy.setPort(3128);
            group.setHttpProxy(proxy);

            var result = sharedConfigurationMigration.convert(group);
            JsonNode json = objectMapper.readTree(result);

            JsonNode proxyNode = json.get("proxy");
            assertThat(proxyNode).isNotNull();
            assertThat(proxyNode.get("host").asText()).isEqualTo("proxy.example.com");
        }

        @ParameterizedTest
        @ValueSource(strings = { "" })
        void should_set_host_to_slash_when_host_is_blank(String host) throws JsonProcessingException {
            var group = new EndpointGroup();
            var options = new HttpClientOptions();
            options.setVersion(ProtocolVersion.HTTP_1_1);
            group.setHttpClientOptions(options);

            var proxy = new HttpProxy();
            proxy.setEnabled(false);
            proxy.setUseSystemProxy(false);
            proxy.setHost(host);
            group.setHttpProxy(proxy);

            var result = sharedConfigurationMigration.convert(group);
            JsonNode json = objectMapper.readTree(result);

            JsonNode proxyNode = json.get("proxy");
            assertThat(proxyNode).isNotNull();
            assertThat(proxyNode.get("host").asText()).isEqualTo("/");
        }

        @Test
        void should_set_host_to_slash_when_host_is_null() throws JsonProcessingException {
            var group = new EndpointGroup();
            var options = new HttpClientOptions();
            options.setVersion(ProtocolVersion.HTTP_1_1);
            group.setHttpClientOptions(options);

            var proxy = new HttpProxy();
            proxy.setEnabled(false);
            proxy.setUseSystemProxy(false);
            proxy.setHost(null);
            group.setHttpProxy(proxy);

            var result = sharedConfigurationMigration.convert(group);
            JsonNode json = objectMapper.readTree(result);

            JsonNode proxyNode = json.get("proxy");
            assertThat(proxyNode).isNotNull();
            assertThat(proxyNode.get("host").asText()).isEqualTo("/");
        }
    }
}
