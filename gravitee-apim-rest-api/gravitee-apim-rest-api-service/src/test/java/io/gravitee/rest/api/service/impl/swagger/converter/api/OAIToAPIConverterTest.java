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
package io.gravitee.rest.api.service.impl.swagger.converter.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import io.gravitee.definition.model.Endpoint;
import io.gravitee.definition.model.EndpointGroup;
import io.gravitee.rest.api.model.ImportSwaggerDescriptorEntity;
import io.gravitee.rest.api.model.api.SwaggerApiEntity;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.TagService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.impl.swagger.policy.impl.PolicyOperationVisitorManagerImpl;
import io.gravitee.rest.api.service.swagger.OAIDescriptor;
import io.gravitee.rest.api.service.swagger.SwaggerDescriptor;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.servers.ServerVariable;
import io.swagger.v3.oas.models.servers.ServerVariables;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OAIToAPIConverterTest {

    private OAIToAPIConverter oaiToAPIConverter;

    @BeforeEach
    void setUp() {
        oaiToAPIConverter = new OAIToAPIConverter(
            new ImportSwaggerDescriptorEntity(),
            new PolicyOperationVisitorManagerImpl(),
            mock(GroupService.class),
            mock(TagService.class)
        );
    }

    @Test
    @DisplayName("Should return null for null input")
    void convertReturnsNullForNullInput() {
        var api = oaiToAPIConverter.convert(new ExecutionContext("DEFAULT", "DEFAULT"), new OAIDescriptor(null));
        assertThat(api).isNull();
    }

    @Test
    @DisplayName("Should return an API with Name, Version and Description based on OpenAPI info")
    void convertMapBasicFields() {
        OpenAPI openAPI = new OpenAPI();
        openAPI.setInfo(new Info().title("My API").version("1.0.0").description("A description"));
        openAPI.setServers(List.of());

        var api = oaiToAPIConverter.convert(new ExecutionContext("DEFAULT", "DEFAULT"), new OAIDescriptor(openAPI));
        assertThat(api).isNotNull();
        assertThat(api.getName()).isEqualTo("My API");
        assertThat(api.getVersion()).isEqualTo("1.0.0");
        assertThat(api.getDescription()).isEqualTo("A description");
    }

    @Test
    @DisplayName("Should return an API with Description computed from OpenAPI Name if OpenAPI Description is empty")
    void convertCreateDescriptionBasedOnNameIfEmpty() {
        OpenAPI openAPI = new OpenAPI();
        openAPI.setInfo(new Info().title("My API").version("1.0.0"));
        openAPI.setServers(List.of());

        var api = oaiToAPIConverter.convert(new ExecutionContext("DEFAULT", "DEFAULT"), new OAIDescriptor(openAPI));
        assertThat(api).isNotNull();
        assertThat(api.getName()).isEqualTo("My API");
        assertThat(api.getVersion()).isEqualTo("1.0.0");
        assertThat(api.getDescription()).isEqualTo("Description of My API");
    }

    @Test
    @DisplayName("Should return an API with Endpoint created from Server url")
    void convertMapProxyEndpoint() {
        Server server1 = new Server();
        server1.setUrl("https://api.company.com");

        OpenAPI openAPI = new OpenAPI();
        openAPI.setInfo(new Info().title("My API").version("1.0.0"));
        openAPI.setServers(List.of(server1));

        SwaggerApiEntity api = oaiToAPIConverter.convert(new ExecutionContext("DEFAULT", "DEFAULT"), new OAIDescriptor(openAPI));
        assertThat(api.getProxy().getGroups()).hasSize(1);

        EndpointGroup endpointGroup = api.getProxy().getGroups().stream().findAny().orElseThrow();
        assertThat(endpointGroup.getName()).isEqualTo("default-group");
        assertThat(endpointGroup.getEndpoints()).hasSize(1);

        Endpoint endpoint = endpointGroup.getEndpoints().stream().findAny().orElseThrow();
        assertThat(endpoint.getName()).isEqualTo("default");
        assertThat(endpoint.getTarget()).isEqualTo("https://api.company.com");
        assertThat(endpoint.getInherit()).isTrue();
    }

    @Test
    @DisplayName("Should return an API with multiple Endpoints created from Server urls and variables")
    void convertMapProxyEndpoints() {
        var server1 = new Server();
        server1.setUrl("https://api.company.com/{basePath}");
        server1.setVariables(
            new ServerVariables()
                .addServerVariable("basePath", new ServerVariable().description("Base path")._default("v1"))
                .addServerVariable("anotherVariable", new ServerVariable().description("Another Variable")._default("value"))
        );

        var server2 = new Server();
        server2.setUrl("https://api2.company.com");

        var server3 = new Server();
        server3.setUrl("https://api3.company.com/{basePath}");
        server3.setVariables(
            new ServerVariables().addServerVariable("basePath", new ServerVariable().description("Base path")._enum(List.of("v2", "v3")))
        );

        OpenAPI openAPI = new OpenAPI();
        openAPI.setInfo(new Info().title("My API").version("1.0.0"));
        openAPI.setServers(List.of(server1, server2, server3));

        var api = oaiToAPIConverter.convert(new ExecutionContext("DEFAULT", "DEFAULT"), new OAIDescriptor(openAPI));
        assertThat(api.getProxy().getGroups()).hasSize(1);

        EndpointGroup endpointGroup = api.getProxy().getGroups().stream().findAny().orElseThrow();
        assertThat(endpointGroup.getName()).isEqualTo("default-group");
        assertThat(endpointGroup.getEndpoints()).hasSize(4);

        List<Endpoint> endpoints = endpointGroup
            .getEndpoints()
            .stream()
            .sorted(Comparator.comparing(Endpoint::getName))
            .collect(Collectors.toList());
        Endpoint endpoint1 = endpoints.get(0);
        assertThat(endpoint1.getName()).isEqualTo("server1");
        assertThat(endpoint1.getTarget()).isEqualTo("https://api.company.com/v1");
        assertThat(endpoint1.getInherit()).isTrue();

        Endpoint endpoint2 = endpoints.get(1);
        assertThat(endpoint2.getName()).isEqualTo("server2");
        assertThat(endpoint2.getTarget()).isEqualTo("https://api2.company.com");
        assertThat(endpoint2.getInherit()).isTrue();

        Endpoint endpoint3 = endpoints.get(2);
        assertThat(endpoint3.getName()).isEqualTo("server3");
        assertThat(endpoint3.getTarget()).isEqualTo("https://api3.company.com/v2");
        assertThat(endpoint3.getInherit()).isTrue();

        Endpoint endpoint4 = endpoints.get(3);
        assertThat(endpoint4.getName()).isEqualTo("server4");
        assertThat(endpoint4.getTarget()).isEqualTo("https://api3.company.com/v3");
        assertThat(endpoint4.getInherit()).isTrue();
    }

    @Test
    @DisplayName("Should return an API with Path and paths mappings created from OpenAPI paths")
    void convertSetPathsAndPathMappings() {
        OpenAPI openAPI = new OpenAPI();
        openAPI.setInfo(new Info().title("My API").version("1.0.0"));
        openAPI.setServers(List.of());

        var api = oaiToAPIConverter.convert(new ExecutionContext("DEFAULT", "DEFAULT"), new OAIDescriptor(openAPI));
        assertThat(api).isNotNull();
        assertThat(api.getPaths()).isEqualTo(Map.of("/", List.of()));
        assertThat(api.getPathMappings()).isEqualTo(Set.of("/"));
    }
}
