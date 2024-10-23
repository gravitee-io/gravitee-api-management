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

import static assertions.MAPIAssertions.assertThat;

import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.category.model.ApiCategoryOrder;
import io.gravitee.apim.core.category.use_case.GetCategoryApisUseCase;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.Proxy;
import io.gravitee.definition.model.VirtualHost;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.listener.http.Path;
import io.gravitee.definition.model.v4.listener.tcp.TcpListener;
import io.gravitee.rest.api.management.v2.rest.model.CategoryApi;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

public class CategoryApiMapperTest {

    private final CategoryApiMapper mapper = Mappers.getMapper(CategoryApiMapper.class);

    private static final String API_ID = "api-id";
    private static final String API_NAME = "api-name";
    private static final String API_DESCRIPTION = "api-description";
    private static final String API_VERSION = "99";
    private static final String CAT_ID = "cat-id";

    @Test
    void should_map_v2_api_with_virtual_hosts() {
        var virtualHost1 = new VirtualHost();
        virtualHost1.setHost("host-1");
        virtualHost1.setPath("/path-2");

        var virtualHost2 = new VirtualHost();
        virtualHost2.setHost(null);
        virtualHost2.setPath("/great-path");

        var definition = io.gravitee.definition.model.Api
            .builder()
            .definitionVersion(DefinitionVersion.V2)
            .proxy(Proxy.builder().virtualHosts(List.of(virtualHost1, virtualHost2)).build())
            .build();

        var apiV2 = Api
            .builder()
            .id(API_ID)
            .definitionVersion(DefinitionVersion.V2)
            .name(API_NAME)
            .description(API_DESCRIPTION)
            .version(API_VERSION)
            .apiDefinition(definition)
            .build();

        var apiCategoryOrder = ApiCategoryOrder.builder().apiId(API_ID).categoryId(CAT_ID).order(11).build();

        var mappedResult = mapper.map(apiCategoryOrder, apiV2);

        var expectedResult = CategoryApi
            .builder()
            .id(API_ID)
            .name(API_NAME)
            .apiVersion(API_VERSION)
            .description(API_DESCRIPTION)
            .definitionVersion(io.gravitee.rest.api.management.v2.rest.model.DefinitionVersion.V2)
            .order(11)
            .accessPaths(List.of("host-1/path-2", "/great-path"))
            .build();

        assertThat(mappedResult).isNotNull().usingRecursiveAssertion().isEqualTo(expectedResult);
    }

    @Test
    void should_map_v2_api_without_virtual_hosts() {
        var definition = io.gravitee.definition.model.Api
            .builder()
            .definitionVersion(DefinitionVersion.V2)
            .proxy(Proxy.builder().virtualHosts(List.of()).build())
            .build();

        var apiV2 = Api
            .builder()
            .id(API_ID)
            .definitionVersion(DefinitionVersion.V2)
            .name(API_NAME)
            .description(API_DESCRIPTION)
            .version(API_VERSION)
            .apiDefinition(definition)
            .build();

        var apiCategoryOrder = ApiCategoryOrder.builder().apiId(API_ID).categoryId(CAT_ID).order(11).build();

        var mappedResult = mapper.map(apiCategoryOrder, apiV2);

        var expectedResult = CategoryApi
            .builder()
            .id(API_ID)
            .name(API_NAME)
            .apiVersion(API_VERSION)
            .description(API_DESCRIPTION)
            .definitionVersion(io.gravitee.rest.api.management.v2.rest.model.DefinitionVersion.V2)
            .order(11)
            .accessPaths(List.of())
            .build();

        assertThat(mappedResult).isNotNull().usingRecursiveAssertion().isEqualTo(expectedResult);
    }

    @Test
    void should_map_v4_api_with_tcp_listeners() {
        var tcpListener = TcpListener.builder().hosts(List.of("one-host", "two-host")).build();

        var definition = io.gravitee.definition.model.v4.Api
            .builder()
            .id(API_ID)
            .definitionVersion(DefinitionVersion.V4)
            .listeners(List.of(tcpListener))
            .build();

        var apiV4 = Api
            .builder()
            .id(API_ID)
            .name(API_NAME)
            .description(API_DESCRIPTION)
            .version(API_VERSION)
            .definitionVersion(DefinitionVersion.V4)
            .apiDefinitionHttpV4(definition)
            .build();

        var apiCategoryOrder = ApiCategoryOrder.builder().apiId(API_ID).categoryId(CAT_ID).order(11).build();

        var mappedResult = mapper.map(apiCategoryOrder, apiV4);

        var expectedResult = CategoryApi
            .builder()
            .id(API_ID)
            .name(API_NAME)
            .apiVersion(API_VERSION)
            .description(API_DESCRIPTION)
            .definitionVersion(io.gravitee.rest.api.management.v2.rest.model.DefinitionVersion.V4)
            .order(11)
            .accessPaths(List.of("one-host", "two-host"))
            .build();

        assertThat(mappedResult).isNotNull().usingRecursiveAssertion().isEqualTo(expectedResult);
    }

    @Test
    void should_map_v4_api_with_http_listeners() {
        var httpListener = HttpListener
            .builder()
            .paths(List.of(Path.builder().host("host-1").path("/path-2").build(), Path.builder().host(null).path("/great-path").build()))
            .build();

        var definition = io.gravitee.definition.model.v4.Api
            .builder()
            .id(API_ID)
            .definitionVersion(DefinitionVersion.V4)
            .listeners(List.of(httpListener))
            .build();

        var apiV4 = Api
            .builder()
            .id(API_ID)
            .name(API_NAME)
            .description(API_DESCRIPTION)
            .version(API_VERSION)
            .definitionVersion(DefinitionVersion.V4)
            .apiDefinitionHttpV4(definition)
            .build();

        var apiCategoryOrder = ApiCategoryOrder.builder().apiId(API_ID).categoryId(CAT_ID).order(11).build();

        var mappedResult = mapper.map(apiCategoryOrder, apiV4);

        var expectedResult = CategoryApi
            .builder()
            .id(API_ID)
            .name(API_NAME)
            .apiVersion(API_VERSION)
            .description(API_DESCRIPTION)
            .definitionVersion(io.gravitee.rest.api.management.v2.rest.model.DefinitionVersion.V4)
            .order(11)
            .accessPaths(List.of("host-1/path-2", "/great-path"))
            .build();

        assertThat(mappedResult).isNotNull().usingRecursiveAssertion().isEqualTo(expectedResult);
    }

    @Test
    void should_map_v4_api_with_tcp_and_http_listeners() {
        var tcpListener = TcpListener.builder().hosts(List.of("one-host", "two-host")).build();

        var httpListener = HttpListener
            .builder()
            .paths(List.of(Path.builder().host("host-1").path("/path-2").build(), Path.builder().host(null).path("/great-path").build()))
            .build();

        var definition = io.gravitee.definition.model.v4.Api
            .builder()
            .id(API_ID)
            .definitionVersion(DefinitionVersion.V4)
            .listeners(List.of(tcpListener, httpListener))
            .build();

        var apiV4 = Api
            .builder()
            .id(API_ID)
            .name(API_NAME)
            .description(API_DESCRIPTION)
            .version(API_VERSION)
            .definitionVersion(DefinitionVersion.V4)
            .apiDefinitionHttpV4(definition)
            .build();

        var apiCategoryOrder = ApiCategoryOrder.builder().apiId(API_ID).categoryId(CAT_ID).order(11).build();

        var mappedResult = mapper.map(apiCategoryOrder, apiV4);

        var expectedResult = CategoryApi
            .builder()
            .id(API_ID)
            .name(API_NAME)
            .apiVersion(API_VERSION)
            .description(API_DESCRIPTION)
            .definitionVersion(io.gravitee.rest.api.management.v2.rest.model.DefinitionVersion.V4)
            .order(11)
            .accessPaths(List.of("one-host", "two-host", "host-1/path-2", "/great-path"))
            .build();

        assertThat(mappedResult).isNotNull().usingRecursiveAssertion().isEqualTo(expectedResult);
    }

    @Test
    void should_map_v4_api_with_no_tcp_or_http_listeners() {
        var definition = io.gravitee.definition.model.v4.Api
            .builder()
            .id(API_ID)
            .definitionVersion(DefinitionVersion.V4)
            .listeners(List.of())
            .build();

        var apiV4 = Api
            .builder()
            .id(API_ID)
            .name(API_NAME)
            .description(API_DESCRIPTION)
            .version(API_VERSION)
            .definitionVersion(DefinitionVersion.V4)
            .apiDefinitionHttpV4(definition)
            .build();

        var apiCategoryOrder = ApiCategoryOrder.builder().apiId(API_ID).categoryId(CAT_ID).order(11).build();

        var mappedResult = mapper.map(apiCategoryOrder, apiV4);

        var expectedResult = CategoryApi
            .builder()
            .id(API_ID)
            .name(API_NAME)
            .apiVersion(API_VERSION)
            .description(API_DESCRIPTION)
            .definitionVersion(io.gravitee.rest.api.management.v2.rest.model.DefinitionVersion.V4)
            .order(11)
            .accessPaths(List.of())
            .build();

        assertThat(mappedResult).isNotNull().usingRecursiveAssertion().isEqualTo(expectedResult);
    }
}
