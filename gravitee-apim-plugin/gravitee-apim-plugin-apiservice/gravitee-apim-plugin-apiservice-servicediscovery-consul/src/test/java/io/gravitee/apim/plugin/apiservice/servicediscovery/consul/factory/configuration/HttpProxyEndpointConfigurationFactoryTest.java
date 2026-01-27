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
package io.gravitee.apim.plugin.apiservice.servicediscovery.consul.factory.configuration;

import static io.gravitee.apim.plugin.apiservice.servicediscovery.consul.factory.configuration.EndpointConfigurationFactory.CONSUL_METADATA_PATH;
import static io.gravitee.apim.plugin.apiservice.servicediscovery.consul.factory.configuration.EndpointConfigurationFactory.CONSUL_METADATA_SSL;
import static org.assertj.core.api.Assertions.assertThat;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.consul.Service;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class HttpProxyEndpointConfigurationFactoryTest {

    private HttpProxyEndpointConfigurationFactory factory;

    @BeforeEach
    void setUp() {
        factory = new HttpProxyEndpointConfigurationFactory();
    }

    @Test
    void should_build_http_configuration_with_service_address() {
        Service service = new Service().setAddress("192.168.1.100").setPort(8080);

        String result = factory.buildConfiguration(service);

        JsonObject json = new JsonObject(result);
        assertThat(json.getString("target")).isEqualTo("http://192.168.1.100:8080");
    }

    @Test
    void should_build_http_configuration_with_node_address_when_service_address_is_null() {
        Service service = new Service().setNodeAddress("192.168.1.200").setPort(8080);

        String result = factory.buildConfiguration(service);

        JsonObject json = new JsonObject(result);
        assertThat(json.getString("target")).isEqualTo("http://192.168.1.200:8080");
    }

    @Test
    void should_build_http_configuration_with_node_address_when_service_address_is_blank() {
        Service service = new Service().setAddress("   ").setNodeAddress("192.168.1.200").setPort(8080);

        String result = factory.buildConfiguration(service);

        JsonObject json = new JsonObject(result);
        assertThat(json.getString("target")).isEqualTo("http://192.168.1.200:8080");
    }

    @ParameterizedTest
    @CsvSource({ "false,http://192.168.1.100:8080", "true,https://192.168.1.100:8080" })
    void should_build_http_configuration_when_ssl_metadata(String ssl, String expectedTarget) {
        Service service = new Service().setAddress("192.168.1.100").setPort(8080).setMeta(Map.of(CONSUL_METADATA_SSL, ssl));

        String result = factory.buildConfiguration(service);

        JsonObject json = new JsonObject(result);
        assertThat(json.getString("target")).isEqualTo(expectedTarget);
    }

    @ParameterizedTest
    @CsvSource(
        {
            "/api/v1,http://192.168.1.100:8080/api/v1",
            "/api/v1/,http://192.168.1.100:8080/api/v1",
            "'',http://192.168.1.100:8080",
            "/,http://192.168.1.100:8080",
            "/app/,http://192.168.1.100:8080/app",
            "/service/endpoint/,http://192.168.1.100:8080/service/endpoint",
        }
    )
    void should_build_configuration_with_path(String path, String expectedTarget) {
        Service service = new Service().setAddress("192.168.1.100").setPort(8080).setMeta(Map.of(CONSUL_METADATA_PATH, path));

        String result = factory.buildConfiguration(service);

        JsonObject json = new JsonObject(result);
        assertThat(json.getString("target")).isEqualTo(expectedTarget);
    }

    @Test
    void should_build_configuration_without_port_when_port_is_zero() {
        Service service = new Service().setAddress("192.168.1.100").setPort(0);

        String result = factory.buildConfiguration(service);

        JsonObject json = new JsonObject(result);
        assertThat(json.getString("target")).isEqualTo("http://192.168.1.100");
    }

    @Test
    void should_build_configuration_without_port_when_port_is_negative() {
        Service service = new Service().setAddress("192.168.1.100").setPort(-1);

        String result = factory.buildConfiguration(service);

        JsonObject json = new JsonObject(result);
        assertThat(json.getString("target")).isEqualTo("http://192.168.1.100");
    }

    @Test
    void should_build_configuration_when_metadata_is_null() {
        Service service = new Service().setAddress("192.168.1.100").setPort(8080).setMeta(null);

        String result = factory.buildConfiguration(service);

        JsonObject json = new JsonObject(result);
        assertThat(json.getString("target")).isEqualTo("http://192.168.1.100:8080");
    }
}
