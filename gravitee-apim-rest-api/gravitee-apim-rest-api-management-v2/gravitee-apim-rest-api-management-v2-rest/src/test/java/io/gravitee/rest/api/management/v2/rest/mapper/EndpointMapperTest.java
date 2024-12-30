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
package io.gravitee.rest.api.management.v2.rest.mapper;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fixtures.EndpointFixtures;
import fixtures.EndpointModelFixtures;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.v4.endpointgroup.Endpoint;
import io.gravitee.definition.model.v4.nativeapi.NativeEndpoint;
import io.gravitee.rest.api.management.v2.rest.model.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

public class EndpointMapperTest {

    private final EndpointMapper endpointMapper = Mappers.getMapper(EndpointMapper.class);

    @Test
    void shouldMapToEndpointEntityV4() throws JsonProcessingException {
        var endpointV4 = EndpointFixtures.anEndpointV4();
        var endpointEntityV4 = endpointMapper.mapToHttpV4(endpointV4);

        assertV4EndpointsAreEquals(endpointEntityV4, endpointV4);
    }

    @Test
    void shouldMapFromEndpointEntityV4() throws JsonProcessingException {
        var endpointEntityV4 = EndpointFixtures.aModelEndpointHttpV4();
        var endpointV4 = endpointMapper.mapFromHttpV4(endpointEntityV4);

        assertV4EndpointsAreEquals(endpointEntityV4, endpointV4);
    }

    @Test
    void shouldMapToEndpointGroupEntityV4() throws JsonProcessingException {
        var endpointGroupV4 = EndpointFixtures.anEndpointGroupV4();

        var endpointGroupEntityV4 = endpointMapper.mapEndpointGroupHttpV4(endpointGroupV4);
        assertThat(endpointGroupEntityV4).isNotNull();
        assertThat(endpointGroupEntityV4.getName()).isEqualTo(endpointGroupV4.getName());
        assertThat(endpointGroupEntityV4.getType()).isEqualTo(endpointGroupV4.getType());
        assertThat(endpointGroupEntityV4.getLoadBalancer()).isNotNull();
        assertThat(endpointGroupEntityV4.getLoadBalancer().getType().name()).isEqualTo(endpointGroupV4.getLoadBalancer().getType().name());
        assertThat(endpointGroupEntityV4.getSharedConfiguration())
            .isEqualTo(new GraviteeMapper().writeValueAsString(endpointGroupV4.getSharedConfiguration()));
        assertV4EndpointsAreEquals(endpointGroupEntityV4.getEndpoints(), endpointGroupV4.getEndpoints());
        assertThat(endpointGroupEntityV4.getServices()).isNotNull(); // Tested in ServiceMapperTest
    }

    private static void assertV4EndpointsAreEquals(List<Endpoint> endpointEntityV4List, List<EndpointV4> endpointV4List)
        throws JsonProcessingException {
        assertThat(endpointEntityV4List).isNotNull().asList().hasSize(endpointV4List.size());
        var endpointEntityV4 = endpointEntityV4List.get(0);
        var endpointV4 = endpointV4List.get(0);
        assertV4EndpointsAreEquals(endpointEntityV4, endpointV4);
    }

    private static void assertV4EndpointsAreEquals(Endpoint endpointEntityV4, EndpointV4 endpointV4) throws JsonProcessingException {
        assertThat(endpointEntityV4).isNotNull();
        assertThat(endpointEntityV4.getName()).isEqualTo(endpointV4.getName());
        assertThat(endpointEntityV4.getType()).isEqualTo(endpointV4.getType());
        assertThat(endpointEntityV4.isSecondary()).isEqualTo(endpointV4.getSecondary());
        assertThat(endpointEntityV4.getTenants()).isEqualTo(endpointV4.getTenants());
        assertThat(endpointEntityV4.getWeight()).isEqualTo(endpointV4.getWeight());
        assertThat(endpointEntityV4.isInheritConfiguration()).isEqualTo(endpointV4.getInheritConfiguration());
        assertThat(endpointEntityV4.getConfiguration()).isEqualTo(new GraviteeMapper().writeValueAsString(endpointV4.getConfiguration()));
        assertThat(endpointEntityV4.getSharedConfigurationOverride())
            .isEqualTo(new GraviteeMapper().writeValueAsString(endpointV4.getSharedConfigurationOverride()));
        assertThat(endpointEntityV4.getServices()).isNotNull(); // Tested in ServiceMapperTest
    }

    @Test
    void shouldMapFromEndpointGroupEntityV4() throws JsonProcessingException {
        var endpointGroupEntityV4 = EndpointFixtures.aModelEndpointGroupHttpV4();
        var endpointGroupV4 = endpointMapper.mapEndpointGroupHttpV4(endpointGroupEntityV4);
        assertThat(endpointGroupV4).isNotNull();
        assertThat(endpointGroupV4.getName()).isEqualTo(endpointGroupEntityV4.getName());
        assertThat(endpointGroupV4.getType()).isEqualTo(endpointGroupEntityV4.getType());
        assertThat(endpointGroupV4.getLoadBalancer()).isNotNull();
        assertThat(endpointGroupV4.getLoadBalancer().getType().name()).isEqualTo(endpointGroupEntityV4.getLoadBalancer().getType().name());
        assertThat(endpointGroupV4.getSharedConfiguration())
            .isEqualTo(new GraviteeMapper().readValue(endpointGroupEntityV4.getSharedConfiguration(), LinkedHashMap.class));
        assertV4EndpointsAreEquals(endpointGroupEntityV4.getEndpoints(), endpointGroupV4.getEndpoints());
        assertThat(endpointGroupV4.getServices()).isNotNull(); // Tested in ServiceMapperTest
    }

    //////////////////// NATIVE ENDPOINTS

    @Test
    void shouldMapToNativeEndpointV4() throws JsonProcessingException {
        var endpointV4 = EndpointFixtures.anEndpointV4();
        var endpointEntityV4 = endpointMapper.mapToNativeV4(endpointV4);

        assertV4EndpointIsEqual(endpointEntityV4, endpointV4);
    }

    @Test
    void shouldMapFromNativeEndpointV4() throws JsonProcessingException {
        var endpointEntityV4 = EndpointFixtures.aModelEndpointNativeV4();
        var endpointV4 = endpointMapper.mapFromNativeV4(endpointEntityV4);

        assertV4EndpointIsEqual(endpointEntityV4, endpointV4);
    }

    @Test
    void shouldMapToNativeEndpointGroupV4() throws JsonProcessingException {
        var endpointGroupV4 = EndpointFixtures.anEndpointGroupV4();

        var endpointGroupEntityV4 = endpointMapper.mapEndpointGroupNativeV4(endpointGroupV4);
        assertThat(endpointGroupEntityV4).isNotNull();
        assertThat(endpointGroupEntityV4.getName()).isEqualTo(endpointGroupV4.getName());
        assertThat(endpointGroupEntityV4.getType()).isEqualTo(endpointGroupV4.getType());
        assertThat(endpointGroupEntityV4.getLoadBalancer()).isNotNull();
        assertThat(endpointGroupEntityV4.getLoadBalancer().getType().name()).isEqualTo(endpointGroupV4.getLoadBalancer().getType().name());
        assertThat(endpointGroupEntityV4.getSharedConfiguration())
            .isEqualTo(new GraviteeMapper().writeValueAsString(endpointGroupV4.getSharedConfiguration()));
        assertThat(endpointGroupV4.getEndpoints()).isNotNull();
        assertV4NativeEndpointsAreEqual(endpointGroupEntityV4.getEndpoints(), endpointGroupV4.getEndpoints());
    }

    @Test
    void shouldMapFromNativeEndpointGroupV4() throws JsonProcessingException {
        var endpointGroupEntityV4 = EndpointFixtures.aModelEndpointGroupNativeV4();
        var endpointGroupV4 = endpointMapper.mapEndpointGroupNativeV4(endpointGroupEntityV4);
        assertThat(endpointGroupV4).isNotNull();
        assertThat(endpointGroupV4.getName()).isEqualTo(endpointGroupEntityV4.getName());
        assertThat(endpointGroupV4.getType()).isEqualTo(endpointGroupEntityV4.getType());
        assertThat(endpointGroupV4.getLoadBalancer()).isNotNull();
        assertThat(endpointGroupV4.getLoadBalancer().getType().name()).isEqualTo(endpointGroupEntityV4.getLoadBalancer().getType().name());
        assertThat(endpointGroupV4.getSharedConfiguration())
            .isEqualTo(new GraviteeMapper().readValue(endpointGroupEntityV4.getSharedConfiguration(), LinkedHashMap.class));
        assertV4NativeEndpointsAreEqual(endpointGroupEntityV4.getEndpoints(), endpointGroupV4.getEndpoints());
        assertThat(endpointGroupV4.getServices()).isNull();
    }

    private static void assertV4NativeEndpointsAreEqual(List<NativeEndpoint> endpointEntityV4List, List<EndpointV4> endpointV4List)
        throws JsonProcessingException {
        assertThat(endpointEntityV4List).isNotNull().asList().hasSize(endpointV4List.size());
        var endpointEntityV4 = endpointEntityV4List.get(0);
        var endpointV4 = endpointV4List.get(0);
        assertV4EndpointIsEqual(endpointEntityV4, endpointV4);
    }

    private static void assertV4EndpointIsEqual(NativeEndpoint endpointEntityV4, EndpointV4 endpointV4) throws JsonProcessingException {
        assertThat(endpointEntityV4).isNotNull();
        assertThat(endpointEntityV4.getName()).isEqualTo(endpointV4.getName());
        assertThat(endpointEntityV4.getType()).isEqualTo(endpointV4.getType());
        assertThat(endpointEntityV4.isSecondary()).isEqualTo(endpointV4.getSecondary());
        assertThat(endpointEntityV4.getTenants()).isEqualTo(endpointV4.getTenants());
        assertThat(endpointEntityV4.getWeight()).isEqualTo(endpointV4.getWeight());
        assertThat(endpointEntityV4.isInheritConfiguration()).isEqualTo(endpointV4.getInheritConfiguration());
        assertThat(endpointEntityV4.getConfiguration()).isEqualTo(new GraviteeMapper().writeValueAsString(endpointV4.getConfiguration()));
        assertThat(endpointEntityV4.getSharedConfigurationOverride())
            .isEqualTo(new GraviteeMapper().writeValueAsString(endpointV4.getSharedConfigurationOverride()));
    }

    ////////////////////

    @Test
    void shouldMapToEndpointEntityV2() throws JsonProcessingException {
        var endpointV2 = EndpointFixtures.anEndpointV2();
        var endpointEntityV2 = endpointMapper.map(endpointV2);

        assertV2EndpointsAreEquals(endpointEntityV2, endpointV2);
    }

    @Test
    void shouldMapToEndpointEntityV2WithConfiguration() throws JsonProcessingException {
        var httpEndpointV2 = EndpointFixtures.anHttpEndpointV2();
        httpEndpointV2.setInherit(false);

        httpEndpointV2.setHealthCheck(EndpointFixtures.anEndpointHealthCheckService());

        var httpHeader = new HttpHeader();
        httpHeader.setName("headerName");
        httpHeader.setValue("headerValue");
        httpEndpointV2.setHeaders(List.of(httpHeader));

        var httpProxy = new HttpProxy();
        httpProxy.setHost("inheritProxy");
        httpProxy.setPort(8080);
        httpEndpointV2.setHttpProxy(httpProxy);

        var httpClientOptions = new HttpClientOptions();
        httpClientOptions.setIdleTimeout(42);
        httpEndpointV2.setHttpClientOptions(httpClientOptions);

        var httpClientSslOptions = new HttpClientSslOptions();
        httpClientSslOptions.setTrustAll(true);
        httpEndpointV2.setHttpClientSslOptions(httpClientSslOptions);

        var endpointV2 = new EndpointV2(httpEndpointV2);

        var endpointEntityV2 = endpointMapper.map(endpointV2);

        assertV2EndpointsAreEquals(endpointEntityV2, endpointV2);
    }

    @Test
    void shouldMapFromEndpointEntityV2() throws JsonProcessingException {
        var endpointEntityV2 = EndpointFixtures.aModelEndpointV2();
        var endpointV2 = endpointMapper.map(endpointEntityV2);

        assertV2EndpointsAreEquals(endpointEntityV2, endpointV2);
    }

    @Test
    void shouldMapFromEndpointEntityV2WithConfiguration() throws JsonProcessingException {
        // Init http endpoint configuration
        var httpEndpointV2Configuration = EndpointModelFixtures.aModelHttpEndpointV2();

        var httpHeader = new io.gravitee.common.http.HttpHeader();
        httpHeader.setName("headerName");
        httpHeader.setValue("headerValue");
        httpEndpointV2Configuration.setHeaders(List.of(httpHeader));

        var httpProxy = new io.gravitee.definition.model.HttpProxy();
        httpProxy.setHost("inheritProxy");
        httpProxy.setPort(8080);
        httpEndpointV2Configuration.setHttpProxy(httpProxy);

        var httpClientOptions = new io.gravitee.definition.model.HttpClientOptions();
        httpClientOptions.setIdleTimeout(42);
        httpEndpointV2Configuration.setHttpClientOptions(httpClientOptions);

        var httpClientSslOptions = new io.gravitee.definition.model.HttpClientSslOptions();
        httpClientSslOptions.setTrustAll(true);
        httpEndpointV2Configuration.setHttpClientSslOptions(httpClientSslOptions);

        // Set http endpoint configuration into endpoint.configuration
        var endpointEntityV2 = EndpointModelFixtures.aModelEndpointV2();
        endpointEntityV2.setName("Should not be mapped");
        endpointEntityV2.setType("HtTp");
        endpointEntityV2.setConfiguration(new ObjectMapper().writeValueAsString(httpEndpointV2Configuration));

        var endpointV2 = endpointMapper.map(endpointEntityV2);

        // assert nested configuration only are mapped if filled
        assertV2EndpointsAreEquals(httpEndpointV2Configuration, endpointV2);
        // Check http configuration
        var httpEndpointV2 = endpointV2.getHttpEndpointV2();
        assertThat(httpEndpointV2Configuration.getHealthCheck()).isNotNull(); // Tested in ServiceMapperTest
        assertThat(httpEndpointV2Configuration.getHttpProxy()).isNotNull();
        assertThat(httpEndpointV2Configuration.getHttpProxy().getHost()).isEqualTo(httpEndpointV2.getHttpProxy().getHost());
        assertThat(httpEndpointV2Configuration.getHttpProxy().getPort()).isEqualTo(httpEndpointV2.getHttpProxy().getPort());
        assertThat(httpEndpointV2Configuration.getHttpClientOptions()).isNotNull();
        assertThat(httpEndpointV2Configuration.getHttpClientOptions().getIdleTimeout())
            .isEqualTo(httpEndpointV2.getHttpClientOptions().getIdleTimeout().longValue());
        assertThat(httpEndpointV2Configuration.getHttpClientSslOptions()).isNotNull();
        assertThat(httpEndpointV2Configuration.getHttpClientSslOptions().isTrustAll())
            .isEqualTo(httpEndpointV2.getHttpClientSslOptions().getTrustAll());
        assertThat(httpEndpointV2Configuration.getHeaders()).isNotNull();
        assertThat(httpEndpointV2Configuration.getHeaders().get(0).getName()).isEqualTo(httpEndpointV2.getHeaders().get(0).getName());
        assertThat(httpEndpointV2Configuration.getHeaders().get(0).getValue()).isEqualTo(httpEndpointV2.getHeaders().get(0).getValue());
    }

    @Test
    void should_map_from_endpoint_entity_V2_with_gRPC_type() throws JsonProcessingException {
        // Init http endpoint configuration
        var httpEndpointV2Configuration = EndpointModelFixtures.aModelHttpEndpointV2();

        var httpHeader = new io.gravitee.common.http.HttpHeader();
        httpHeader.setName("headerName");
        httpHeader.setValue("headerValue");
        httpEndpointV2Configuration.setHeaders(List.of(httpHeader));

        // Set http endpoint configuration into endpoint.configuration
        var endpointEntityV2 = EndpointModelFixtures.aModelEndpointV2();
        endpointEntityV2.setType("gRpC");
        endpointEntityV2.setName("Should not be mapped");
        endpointEntityV2.setConfiguration(new ObjectMapper().writeValueAsString(httpEndpointV2Configuration));

        var endpointV2 = endpointMapper.map(endpointEntityV2);

        // assert nested configuration only are mapped if filled
        assertV2EndpointsAreEquals(httpEndpointV2Configuration, endpointV2);
        // Check http configuration
        var httpEndpointV2 = endpointV2.getHttpEndpointV2();
        assertThat(httpEndpointV2Configuration.getHeaders()).isNotNull();
        assertThat(httpEndpointV2Configuration.getHeaders().get(0).getName()).isEqualTo(httpEndpointV2.getHeaders().get(0).getName());
        assertThat(httpEndpointV2Configuration.getHeaders().get(0).getValue()).isEqualTo(httpEndpointV2.getHeaders().get(0).getValue());
    }

    @Test
    void shouldMapToEndpointGroupEntityV2() throws JsonProcessingException {
        var endpointGroupV2 = EndpointFixtures.anEndpointGroupV2();

        var endpointGroupEntityV2 = endpointMapper.mapEndpointGroup(endpointGroupV2);
        assertThat(endpointGroupEntityV2).isNotNull();
        assertThat(endpointGroupEntityV2.getName()).isEqualTo(endpointGroupV2.getName());
        assertV2EndpointsAreEquals(new ArrayList<>(endpointGroupEntityV2.getEndpoints()), endpointGroupV2.getEndpoints());
        assertThat(endpointGroupEntityV2.getLoadBalancer()).isNotNull();
        assertThat(endpointGroupEntityV2.getLoadBalancer().getType().name()).isEqualTo(endpointGroupV2.getLoadBalancer().getType().name());
        assertThat(endpointGroupEntityV2.getServices()).isNotNull(); // Tested in ServiceMapperTest
        assertThat(endpointGroupEntityV2.getHttpProxy()).isNull();
        assertThat(endpointGroupEntityV2.getHttpClientOptions()).isNull();
        assertThat(endpointGroupEntityV2.getHttpClientSslOptions()).isNull();
        assertThat(endpointGroupEntityV2.getHeaders()).isNull();
    }

    private static void assertV2EndpointsAreEquals(
        List<io.gravitee.definition.model.Endpoint> endpointEntityV2List,
        List<EndpointV2> endpointV2List
    ) throws JsonProcessingException {
        assertThat(endpointEntityV2List).isNotNull().asList().hasSize(endpointV2List.size());
        var endpointEntityV2 = endpointEntityV2List.iterator().next();
        var endpointV2 = endpointV2List.get(0);
        assertV2EndpointsAreEquals(endpointEntityV2, endpointV2);
    }

    private static void assertV2EndpointsAreEquals(io.gravitee.definition.model.Endpoint endpointEntityV2, EndpointV2 endpointV2)
        throws JsonProcessingException {
        var httpEndpointV2 = endpointV2.getHttpEndpointV2();
        assertThat(endpointEntityV2).isNotNull();
        assertThat(endpointEntityV2.getName()).isEqualTo(httpEndpointV2.getName());
        assertThat(endpointEntityV2.getTarget()).isEqualTo(httpEndpointV2.getTarget());
        assertThat(endpointEntityV2.getWeight()).isEqualTo(httpEndpointV2.getWeight());
        assertThat(endpointEntityV2.isBackup()).isEqualTo(httpEndpointV2.getBackup());
        assertThat(endpointEntityV2.getStatus().name()).isEqualTo(httpEndpointV2.getStatus().name());
        assertThat(endpointEntityV2.getTenants()).isEqualTo(httpEndpointV2.getTenants());
        assertThat(endpointEntityV2.getType()).isEqualTo(httpEndpointV2.getType());
        assertThat(endpointEntityV2.getInherit()).isEqualTo(httpEndpointV2.getInherit());
        // The HealthCheck is in the configuration if it's not null
        if (endpointEntityV2.getConfiguration() == null) {
            assertThat(endpointEntityV2.getHealthCheck()).isNotNull(); // Tested in ServiceMapperTest
        }
        if (endpointEntityV2.getConfiguration() != null) {
            assertThat(endpointEntityV2.getConfiguration()).isEqualTo(buildConfiguration(httpEndpointV2));
        }
    }

    private static String buildConfiguration(HttpEndpointV2 endpointV2) throws JsonProcessingException {
        var mapper = new GraviteeMapper();
        var configurationNode = mapper.valueToTree(endpointV2);

        var proxy = configurationNode.get("httpProxy");
        ((ObjectNode) configurationNode).set("proxy", proxy);
        ((ObjectNode) configurationNode).remove("httpProxy");

        var httpclient = configurationNode.get("httpClientOptions");
        ((ObjectNode) configurationNode).set("http", httpclient);
        ((ObjectNode) configurationNode).remove("httpClientOptions");

        var ssl = configurationNode.get("httpClientSslOptions");
        ((ObjectNode) configurationNode).set("ssl", ssl);
        ((ObjectNode) configurationNode).remove("httpClientSslOptions");

        var healthcheck = configurationNode.get("healthCheck");
        ((ObjectNode) configurationNode).set("healthcheck", healthcheck);
        ((ObjectNode) configurationNode).remove("healthCheck");

        return mapper.writeValueAsString(configurationNode);
    }

    @Test
    void shouldMapFromEndpointGroupEntityV2() throws JsonProcessingException {
        var endpointGroupEntityV2 = EndpointFixtures.aModelEndpointGroupV2();
        var endpointGroupV2 = endpointMapper.mapEndpointGroup(endpointGroupEntityV2);
        assertThat(endpointGroupV2).isNotNull();
        assertThat(endpointGroupV2.getName()).isEqualTo(endpointGroupEntityV2.getName());
        assertV2EndpointsAreEquals(new ArrayList<>(endpointGroupEntityV2.getEndpoints()), endpointGroupV2.getEndpoints());
        assertThat(endpointGroupV2.getLoadBalancer()).isNotNull();
        assertThat(endpointGroupV2.getLoadBalancer().getType().name()).isEqualTo(endpointGroupEntityV2.getLoadBalancer().getType().name());
        // TODO: Below should be tested deeper
        assertThat(endpointGroupV2.getServices()).isNull();
        assertThat(endpointGroupV2.getHttpProxy()).isNull();
        assertThat(endpointGroupV2.getHttpClientOptions()).isNull();
        assertThat(endpointGroupV2.getHttpClientSslOptions()).isNull();
        assertThat(endpointGroupV2.getHeaders()).isNotNull();
    }
}
