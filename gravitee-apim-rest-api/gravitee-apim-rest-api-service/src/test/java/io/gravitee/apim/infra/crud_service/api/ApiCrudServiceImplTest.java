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
package io.gravitee.apim.infra.crud_service.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.core.model.ApiFixtures;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.infra.adapter.GraviteeJacksonMapper;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.model.ApiLifecycleState;
import io.gravitee.repository.management.model.LifecycleState;
import io.gravitee.repository.management.model.Visibility;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class ApiCrudServiceImplTest {

    ApiRepository apiRepository;

    ApiCrudServiceImpl service;

    private static final String API_ID = "api-id";

    @BeforeEach
    void setUp() {
        apiRepository = mock(ApiRepository.class);

        service = new ApiCrudServiceImpl(apiRepository);
    }

    @Nested
    class Get {

        @Test
        void should_get_an_api() throws TechnicalException {
            var existingApi = io.gravitee.repository.management.model.Api
                .builder()
                .id(API_ID)
                .definitionVersion(DefinitionVersion.V4)
                .definition(
                    """
                    {"id":"my-api","name":"My Api","type":"proxy","apiVersion":"1.0.0","definitionVersion":"4.0.0","tags":["tag1"],"listeners":[{"type":"http","entrypoints":[{"type":"http-proxy","qos":"auto","configuration":{}}],"paths":[{"path":"/http_proxy"}]}],"endpointGroups":[{"name":"default-group","type":"http-proxy","loadBalancer":{"type":"round-robin"},"sharedConfiguration":{},"endpoints":[{"name":"default-endpoint","type":"http-proxy","secondary":false,"weight":1,"inheritConfiguration":true,"configuration":{"target":"https://api.gravitee.io/echo"},"services":{}}],"services":{}}],"analytics":{"enabled":false},"flowExecution":{"mode":"default","matchRequired":false},"flows":[]}
                    """
                )
                .apiLifecycleState(ApiLifecycleState.PUBLISHED)
                .lifecycleState(LifecycleState.STARTED)
                .build();
            when(apiRepository.findById(API_ID)).thenReturn(Optional.of(existingApi));

            var result = service.get(API_ID);
            Assertions
                .assertThat(result)
                .extracting(Api::getId, Api::getApiLifecycleState, Api::getLifecycleState, Api::getDefinitionVersion)
                .containsExactly(
                    API_ID,
                    io.gravitee.apim.core.api.model.Api.ApiLifecycleState.PUBLISHED,
                    io.gravitee.apim.core.api.model.Api.LifecycleState.STARTED,
                    DefinitionVersion.V4
                );
        }

        @Test
        void should_throw_exception_if_api_not_found() throws TechnicalException {
            when(apiRepository.findById(API_ID)).thenReturn(Optional.empty());

            Assertions.assertThatThrownBy(() -> service.get(API_ID)).isInstanceOf(ApiNotFoundException.class);
        }
    }

    @Nested
    class Find {

        @Test
        void should_find_an_api() throws TechnicalException {
            var existingApi = io.gravitee.repository.management.model.Api
                .builder()
                .id(API_ID)
                .definitionVersion(DefinitionVersion.V4)
                .definition(
                    """
                    {"id":"my-api","name":"My Api","type":"proxy","apiVersion":"1.0.0","definitionVersion":"4.0.0","tags":["tag1"],"listeners":[{"type":"http","entrypoints":[{"type":"http-proxy","qos":"auto","configuration":{}}],"paths":[{"path":"/http_proxy"}]}],"endpointGroups":[{"name":"default-group","type":"http-proxy","loadBalancer":{"type":"round-robin"},"sharedConfiguration":{},"endpoints":[{"name":"default-endpoint","type":"http-proxy","secondary":false,"weight":1,"inheritConfiguration":true,"configuration":{"target":"https://api.gravitee.io/echo"},"services":{}}],"services":{}}],"analytics":{"enabled":false},"flowExecution":{"mode":"default","matchRequired":false},"flows":[]}
                    """
                )
                .apiLifecycleState(ApiLifecycleState.PUBLISHED)
                .lifecycleState(LifecycleState.STARTED)
                .build();
            when(apiRepository.findById(API_ID)).thenReturn(Optional.of(existingApi));

            var result = service.findById(API_ID);
            Assertions
                .assertThat(result)
                .isPresent()
                .get()
                .extracting(Api::getId, Api::getApiLifecycleState, Api::getLifecycleState, Api::getDefinitionVersion)
                .containsExactly(
                    API_ID,
                    io.gravitee.apim.core.api.model.Api.ApiLifecycleState.PUBLISHED,
                    io.gravitee.apim.core.api.model.Api.LifecycleState.STARTED,
                    DefinitionVersion.V4
                );
        }

        @Test
        void should_return_empty_if_api_not_found() throws TechnicalException {
            when(apiRepository.findById(API_ID)).thenReturn(Optional.empty());

            var result = service.findById(API_ID);

            Assertions.assertThat(result).isNotNull().isEmpty();
        }

        @Test
        void should_throw_when_technical_exception_occurs() throws TechnicalException {
            // Given
            when(apiRepository.findById(API_ID)).thenThrow(TechnicalException.class);

            // When
            Throwable throwable = catchThrowable(() -> service.findById(API_ID));

            // Then
            assertThat(throwable)
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessage("An error occurs while trying to find an api by id: api-id");
        }
    }

    @Nested
    class Create {

        @BeforeEach
        @SneakyThrows
        void setUp() {
            when(apiRepository.create(any())).thenAnswer(invocation -> invocation.getArgument(0));
        }

        @Test
        @SneakyThrows
        void should_create_a_v4_api() {
            var api = ApiFixtures
                .aProxyApiV4()
                .toBuilder()
                .createdAt(Instant.parse("2020-02-01T20:22:02.00Z").atZone(ZoneOffset.UTC))
                .updatedAt(Instant.parse("2020-02-02T20:22:02.00Z").atZone(ZoneOffset.UTC))
                .build();
            service.create(api);

            var captor = ArgumentCaptor.forClass(io.gravitee.repository.management.model.Api.class);
            verify(apiRepository).create(captor.capture());

            assertThat(captor.getValue())
                .usingRecursiveComparison()
                .isEqualTo(
                    io.gravitee.repository.management.model.Api
                        .builder()
                        .id("my-api")
                        .crossId("my-api-crossId")
                        .name("My Api")
                        .description("api-description")
                        .version("1.0.0")
                        .type(ApiType.PROXY)
                        .apiLifecycleState(ApiLifecycleState.PUBLISHED)
                        .lifecycleState(LifecycleState.STARTED)
                        .visibility(Visibility.PUBLIC)
                        .origin("management")
                        .background("api-background")
                        .picture("api-picture")
                        .environmentId("environment-id")
                        .categories(Set.of("category-1"))
                        .groups(Set.of("group-1"))
                        .labels(List.of("label-1"))
                        .disableMembershipNotifications(true)
                        .createdAt(Date.from(Instant.parse("2020-02-01T20:22:02.00Z")))
                        .updatedAt(Date.from(Instant.parse("2020-02-02T20:22:02.00Z")))
                        .deployedAt(Date.from(Instant.parse("2020-02-03T20:22:02.00Z")))
                        .definitionVersion(DefinitionVersion.V4)
                        .definition(
                            GraviteeJacksonMapper.getInstance().writeValueAsString(ApiFixtures.aProxyApiV4().getApiDefinitionHttpV4())
                        )
                        .build()
                );
        }

        @Test
        @SneakyThrows
        void should_create_a_v2_api() {
            var api = ApiFixtures.aProxyApiV2();
            service.create(api);

            var captor = ArgumentCaptor.forClass(io.gravitee.repository.management.model.Api.class);
            verify(apiRepository).create(captor.capture());

            assertThat(captor.getValue())
                .usingRecursiveComparison()
                .isEqualTo(
                    io.gravitee.repository.management.model.Api
                        .builder()
                        .id("my-api")
                        .crossId("my-api-crossId")
                        .name("My Api")
                        .description("api-description")
                        .version("1.0.0")
                        .type(ApiType.PROXY)
                        .apiLifecycleState(ApiLifecycleState.PUBLISHED)
                        .lifecycleState(LifecycleState.STARTED)
                        .visibility(Visibility.PUBLIC)
                        .origin("management")
                        .background("api-background")
                        .picture("api-picture")
                        .environmentId("environment-id")
                        .categories(Set.of("category-1"))
                        .groups(Set.of("group-1"))
                        .labels(List.of("label-1"))
                        .disableMembershipNotifications(true)
                        .createdAt(Date.from(Instant.parse("2020-02-01T20:22:02.00Z")))
                        .updatedAt(Date.from(Instant.parse("2020-02-02T20:22:02.00Z")))
                        .deployedAt(Date.from(Instant.parse("2020-02-03T20:22:02.00Z")))
                        .definitionVersion(DefinitionVersion.V2)
                        .definition(
                            """
                                              {"id":"my-api","name":"api-name","version":"1.0.0","gravitee":"2.0.0","execution_mode":"v3","flow_mode":"DEFAULT","proxy":{"strip_context_path":false,"preserve_host":false,"groups":[{"name":"default-group","endpoints":[{"name":"default","target":"https://api.gravitee.io/echo","weight":1,"backup":false,"type":"http1"}],"load_balancing":{"type":"ROUND_ROBIN"},"http":{"connectTimeout":5000,"idleTimeout":60000,"keepAliveTimeout":30000,"keepAlive":true,"readTimeout":10000,"pipelining":false,"maxConcurrentConnections":100,"useCompression":true,"followRedirects":false}}]},"properties":[],"tags":["tag1"]}"""
                        )
                        .build()
                );
        }

        @Test
        @SneakyThrows
        void should_create_a_federated_api() {
            var api = ApiFixtures.aFederatedApi();
            service.create(api);

            var captor = ArgumentCaptor.forClass(io.gravitee.repository.management.model.Api.class);
            verify(apiRepository).create(captor.capture());

            assertThat(captor.getValue())
                .usingRecursiveComparison()
                .isEqualTo(
                    io.gravitee.repository.management.model.Api
                        .builder()
                        .id("my-api")
                        .name("My Api")
                        .description("api-description")
                        .version("1.0.0")
                        .apiLifecycleState(ApiLifecycleState.PUBLISHED)
                        .lifecycleState(null)
                        .visibility(Visibility.PUBLIC)
                        .origin("integration")
                        .integrationId("integration-id")
                        .background("api-background")
                        .picture("api-picture")
                        .environmentId("environment-id")
                        .categories(Set.of("category-1"))
                        .groups(Set.of("group-1"))
                        .labels(List.of("label-1"))
                        .disableMembershipNotifications(true)
                        .createdAt(Date.from(Instant.parse("2020-02-01T20:22:02.00Z")))
                        .updatedAt(Date.from(Instant.parse("2020-02-02T20:22:02.00Z")))
                        .deployedAt(Date.from(Instant.parse("2020-02-03T20:22:02.00Z")))
                        .definitionVersion(DefinitionVersion.FEDERATED)
                        .definition(
                            """
                            {"id":"my-api","providerId":"provider-id","name":"My Api","apiVersion":"1.0.0","definitionVersion":"FEDERATED"}"""
                        )
                        .build()
                );
        }

        @Test
        @SneakyThrows
        void should_return_the_created_api() {
            when(apiRepository.create(any())).thenAnswer(invocation -> invocation.getArgument(0));

            var toUpdate = ApiFixtures.aProxyApiV4();
            var result = service.create(toUpdate);

            assertThat(result).isEqualTo(toUpdate);
        }

        @Test
        void should_throw_when_technical_exception_occurs() throws TechnicalException {
            // Given
            when(apiRepository.create(any())).thenThrow(TechnicalException.class);

            // When
            Throwable throwable = catchThrowable(() -> service.create(ApiFixtures.aProxyApiV4()));

            // Then
            assertThat(throwable)
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessage("An error occurs while trying to create the api: my-api");
        }
    }

    @Nested
    class Update {

        @BeforeEach
        @SneakyThrows
        void setUp() {
            when(apiRepository.update(any())).thenAnswer(invocation -> invocation.getArgument(0));
        }

        @Test
        @SneakyThrows
        void should_update_an_existing_v4_api() {
            var plan = ApiFixtures
                .aProxyApiV4()
                .toBuilder()
                .createdAt(Instant.parse("2020-02-01T20:22:02.00Z").atZone(ZoneOffset.UTC))
                .updatedAt(Instant.parse("2020-02-02T20:22:02.00Z").atZone(ZoneOffset.UTC))
                .build();
            service.update(plan);

            var captor = ArgumentCaptor.forClass(io.gravitee.repository.management.model.Api.class);
            verify(apiRepository).update(captor.capture());

            assertThat(captor.getValue())
                .usingRecursiveComparison()
                .isEqualTo(
                    io.gravitee.repository.management.model.Api
                        .builder()
                        .id("my-api")
                        .crossId("my-api-crossId")
                        .name("My Api")
                        .description("api-description")
                        .version("1.0.0")
                        .type(ApiType.PROXY)
                        .apiLifecycleState(ApiLifecycleState.PUBLISHED)
                        .lifecycleState(LifecycleState.STARTED)
                        .visibility(Visibility.PUBLIC)
                        .origin("management")
                        .background("api-background")
                        .picture("api-picture")
                        .environmentId("environment-id")
                        .categories(Set.of("category-1"))
                        .groups(Set.of("group-1"))
                        .labels(List.of("label-1"))
                        .disableMembershipNotifications(true)
                        .createdAt(Date.from(Instant.parse("2020-02-01T20:22:02.00Z")))
                        .updatedAt(Date.from(Instant.parse("2020-02-02T20:22:02.00Z")))
                        .deployedAt(Date.from(Instant.parse("2020-02-03T20:22:02.00Z")))
                        .definitionVersion(DefinitionVersion.V4)
                        .definition(
                            GraviteeJacksonMapper.getInstance().writeValueAsString(ApiFixtures.aProxyApiV4().getApiDefinitionHttpV4())
                        )
                        .build()
                );
        }

        @Test
        @SneakyThrows
        void should_update_an_existing_v2_api() {
            var api = ApiFixtures.aProxyApiV2();
            service.update(api);

            var captor = ArgumentCaptor.forClass(io.gravitee.repository.management.model.Api.class);
            verify(apiRepository).update(captor.capture());

            assertThat(captor.getValue())
                .usingRecursiveComparison()
                .isEqualTo(
                    io.gravitee.repository.management.model.Api
                        .builder()
                        .id("my-api")
                        .crossId("my-api-crossId")
                        .name("My Api")
                        .description("api-description")
                        .version("1.0.0")
                        .type(ApiType.PROXY)
                        .apiLifecycleState(ApiLifecycleState.PUBLISHED)
                        .lifecycleState(LifecycleState.STARTED)
                        .visibility(Visibility.PUBLIC)
                        .origin("management")
                        .background("api-background")
                        .picture("api-picture")
                        .environmentId("environment-id")
                        .categories(Set.of("category-1"))
                        .groups(Set.of("group-1"))
                        .labels(List.of("label-1"))
                        .disableMembershipNotifications(true)
                        .createdAt(Date.from(Instant.parse("2020-02-01T20:22:02.00Z")))
                        .updatedAt(Date.from(Instant.parse("2020-02-02T20:22:02.00Z")))
                        .deployedAt(Date.from(Instant.parse("2020-02-03T20:22:02.00Z")))
                        .definitionVersion(DefinitionVersion.V2)
                        .definition(
                            """
                              {"id":"my-api","name":"api-name","version":"1.0.0","gravitee":"2.0.0","execution_mode":"v3","flow_mode":"DEFAULT","proxy":{"strip_context_path":false,"preserve_host":false,"groups":[{"name":"default-group","endpoints":[{"name":"default","target":"https://api.gravitee.io/echo","weight":1,"backup":false,"type":"http1"}],"load_balancing":{"type":"ROUND_ROBIN"},"http":{"connectTimeout":5000,"idleTimeout":60000,"keepAliveTimeout":30000,"keepAlive":true,"readTimeout":10000,"pipelining":false,"maxConcurrentConnections":100,"useCompression":true,"followRedirects":false}}]},"properties":[],"tags":["tag1"]}"""
                        )
                        .build()
                );
        }

        @Test
        @SneakyThrows
        void should_return_the_updated_api() {
            when(apiRepository.update(any())).thenAnswer(invocation -> invocation.getArgument(0));

            var toUpdate = ApiFixtures.aProxyApiV4();
            var result = service.update(toUpdate);

            assertThat(result).isEqualTo(toUpdate);
        }

        @Test
        void should_throw_when_technical_exception_occurs() throws TechnicalException {
            // Given
            when(apiRepository.update(any())).thenThrow(TechnicalException.class);

            // When
            Throwable throwable = catchThrowable(() -> service.update(ApiFixtures.aProxyApiV4()));

            // Then
            assertThat(throwable)
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessage("An error occurs while trying to update the api: my-api");
        }
    }

    @Nested
    class Delete {

        @Test
        void should_delete_an_api() throws TechnicalException {
            service.delete("api-id");
            verify(apiRepository).delete("api-id");
        }

        @Test
        void should_throw_if_deletion_problem_occurs() throws TechnicalException {
            doThrow(new TechnicalException("exception")).when(apiRepository).delete("api-id");
            assertThatThrownBy(() -> service.delete("api-id"))
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessage("An error occurs while trying to delete the api: api-id");
            verify(apiRepository).delete("api-id");
        }
    }
}
