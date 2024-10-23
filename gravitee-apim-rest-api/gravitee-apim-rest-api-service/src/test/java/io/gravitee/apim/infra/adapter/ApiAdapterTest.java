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
package io.gravitee.apim.infra.adapter;

import static assertions.CoreAssertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import fixtures.core.model.ApiFixtures;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.analytics.Analytics;
import io.gravitee.definition.model.v4.endpointgroup.Endpoint;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.definition.model.v4.failover.Failover;
import io.gravitee.definition.model.v4.flow.execution.FlowExecution;
import io.gravitee.definition.model.v4.listener.entrypoint.Entrypoint;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.listener.http.Path;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.ApiLifecycleState;
import io.gravitee.repository.management.model.LifecycleState;
import io.gravitee.repository.management.model.Visibility;
import io.gravitee.rest.api.model.context.OriginContext;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiAdapterTest {

    @Nested
    class RepositoryToModel {

        @Test
        void should_convert_from_v4_repository_to_core_model() {
            var repository = apiV4().build();

            var api = ApiAdapter.INSTANCE.toCoreModel(repository);

            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(api.getId()).isEqualTo("my-id");
                soft.assertThat(api.getEnvironmentId()).isEqualTo("env-id");
                soft.assertThat(api.getGroups()).containsExactly("group-1");
                soft.assertThat(api.getCategories()).containsExactly("category-1");
                soft.assertThat(api.getLabels()).containsExactly("label-1");
                soft.assertThat(api.getCrossId()).isEqualTo("cross-id");
                soft.assertThat(api.getDescription()).isEqualTo("api-description");
                soft.assertThat(api.getVersion()).isEqualTo("1.0.0");
                soft.assertThat(api.getDefinitionVersion()).isEqualTo(DefinitionVersion.V4);
                soft.assertThat(api.getApiDefinition()).isNull();
                soft
                    .assertThat(api.getApiDefinitionHttpV4())
                    .isEqualTo(
                        io.gravitee.definition.model.v4.Api
                            .builder()
                            .id("my-id")
                            .name("api-name")
                            .apiVersion("1.0.0")
                            .definitionVersion(DefinitionVersion.V4)
                            .flowExecution(new FlowExecution())
                            .analytics(Analytics.builder().enabled(false).build())
                            .failover(
                                Failover
                                    .builder()
                                    .enabled(true)
                                    .maxRetries(7)
                                    .slowCallDuration(500)
                                    .openStateDuration(11000)
                                    .maxFailures(3)
                                    .perSubscription(false)
                                    .build()
                            )
                            .type(ApiType.PROXY)
                            .tags(Set.of("tag1"))
                            .listeners(
                                List.of(
                                    HttpListener
                                        .builder()
                                        .paths(List.of(Path.builder().path("/http_proxy").build()))
                                        .entrypoints(List.of(Entrypoint.builder().type("http-proxy").configuration("{}").build()))
                                        .build()
                                )
                            )
                            .endpointGroups(
                                List.of(
                                    EndpointGroup
                                        .builder()
                                        .name("default-group")
                                        .type("http-proxy")
                                        .sharedConfiguration("{}")
                                        .endpoints(
                                            List.of(
                                                Endpoint
                                                    .builder()
                                                    .name("default-endpoint")
                                                    .type("http-proxy")
                                                    .inheritConfiguration(true)
                                                    .configuration("{\"target\":\"https://api.gravitee.io/echo\"}")
                                                    .build()
                                            )
                                        )
                                        .build()
                                )
                            )
                            .flows(List.of())
                            .build()
                    );
                soft.assertThat(api.getType()).isEqualTo(ApiType.PROXY);
                soft.assertThat(api.getName()).isEqualTo("api-name");
                soft.assertThat(api.getDeployedAt()).isEqualTo(Instant.parse("2020-02-03T20:22:02.00Z").atZone(ZoneOffset.UTC));
                soft.assertThat(api.getCreatedAt()).isEqualTo(Instant.parse("2020-02-01T20:22:02.00Z").atZone(ZoneOffset.UTC));
                soft.assertThat(api.getVisibility()).isEqualTo(io.gravitee.apim.core.api.model.Api.Visibility.PUBLIC);
                soft.assertThat(api.getLifecycleState()).isEqualTo(io.gravitee.apim.core.api.model.Api.LifecycleState.STARTED);
                soft.assertThat(api.getUpdatedAt()).isEqualTo(Instant.parse("2020-02-02T20:22:02.00Z").atZone(ZoneOffset.UTC));
                soft.assertThat(api.getPicture()).isEqualTo("my-picture");
                soft.assertThat(api.isDisableMembershipNotifications()).isTrue();
                soft.assertThat(api.getApiLifecycleState()).isEqualTo(io.gravitee.apim.core.api.model.Api.ApiLifecycleState.PUBLISHED);
                soft.assertThat(api.getBackground()).isEqualTo("my-background");
            });
        }

        @Test
        void should_convert_from_v4_repository_to_core_model_with_null_definition() {
            var repository = apiV4().definition(null).build();

            var api = ApiAdapter.INSTANCE.toCoreModel(repository);

            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(api.getId()).isEqualTo("my-id");
                soft.assertThat(api.getEnvironmentId()).isEqualTo("env-id");
                soft.assertThat(api.getGroups()).containsExactly("group-1");
                soft.assertThat(api.getCategories()).containsExactly("category-1");
                soft.assertThat(api.getLabels()).containsExactly("label-1");
                soft.assertThat(api.getCrossId()).isEqualTo("cross-id");
                soft.assertThat(api.getDescription()).isEqualTo("api-description");
                soft.assertThat(api.getVersion()).isEqualTo("1.0.0");
                soft.assertThat(api.getDefinitionVersion()).isEqualTo(DefinitionVersion.V4);
                soft.assertThat(api.getApiDefinition()).isNull();
                soft.assertThat(api.getApiDefinitionHttpV4()).isNull();
                soft.assertThat(api.getType()).isEqualTo(ApiType.PROXY);
                soft.assertThat(api.getName()).isEqualTo("api-name");
                soft.assertThat(api.getDeployedAt()).isEqualTo(Instant.parse("2020-02-03T20:22:02.00Z").atZone(ZoneOffset.UTC));
                soft.assertThat(api.getCreatedAt()).isEqualTo(Instant.parse("2020-02-01T20:22:02.00Z").atZone(ZoneOffset.UTC));
                soft.assertThat(api.getVisibility()).isEqualTo(io.gravitee.apim.core.api.model.Api.Visibility.PUBLIC);
                soft.assertThat(api.getLifecycleState()).isEqualTo(io.gravitee.apim.core.api.model.Api.LifecycleState.STARTED);
                soft.assertThat(api.getUpdatedAt()).isEqualTo(Instant.parse("2020-02-02T20:22:02.00Z").atZone(ZoneOffset.UTC));
                soft.assertThat(api.getPicture()).isEqualTo("my-picture");
                soft.assertThat(api.isDisableMembershipNotifications()).isTrue();
                soft.assertThat(api.getApiLifecycleState()).isEqualTo(io.gravitee.apim.core.api.model.Api.ApiLifecycleState.PUBLISHED);
                soft.assertThat(api.getBackground()).isEqualTo("my-background");
            });
        }

        @Test
        void should_convert_from_v2_repository_to_core_model() {
            var repository = apiV2().build();

            var api = ApiAdapter.INSTANCE.toCoreModel(repository);

            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(api.getId()).isEqualTo("my-id");
                soft.assertThat(api.getEnvironmentId()).isEqualTo("env-id");
                soft.assertThat(api.getGroups()).containsExactly("group-1");
                soft.assertThat(api.getCategories()).containsExactly("category-1");
                soft.assertThat(api.getLabels()).containsExactly("label-1");
                soft.assertThat(api.getCrossId()).isEqualTo("cross-id");
                soft.assertThat(api.getDescription()).isEqualTo("api-description");
                soft.assertThat(api.getVersion()).isEqualTo("1.0.0");
                soft.assertThat(api.getDefinitionVersion()).isEqualTo(DefinitionVersion.V2);
                soft.assertThat(api.getApiDefinitionHttpV4()).isNull();
                soft
                    .assertThat(api.getApiDefinition())
                    // V2 Api definition is defining a equals/hashcode checking only the id
                    .isEqualTo(io.gravitee.definition.model.Api.builder().id("my-id").name("api-name").version("1.0.0").build());
                soft.assertThat(api.getType()).isEqualTo(ApiType.PROXY);
                soft.assertThat(api.getName()).isEqualTo("api-name");
                soft.assertThat(api.getDeployedAt()).isEqualTo(Instant.parse("2020-02-03T20:22:02.00Z").atZone(ZoneOffset.UTC));
                soft.assertThat(api.getCreatedAt()).isEqualTo(Instant.parse("2020-02-01T20:22:02.00Z").atZone(ZoneOffset.UTC));
                soft.assertThat(api.getVisibility()).isEqualTo(io.gravitee.apim.core.api.model.Api.Visibility.PUBLIC);
                soft.assertThat(api.getLifecycleState()).isEqualTo(io.gravitee.apim.core.api.model.Api.LifecycleState.STARTED);
                soft.assertThat(api.getUpdatedAt()).isEqualTo(Instant.parse("2020-02-02T20:22:02.00Z").atZone(ZoneOffset.UTC));
                soft.assertThat(api.getPicture()).isEqualTo("my-picture");
                soft.assertThat(api.isDisableMembershipNotifications()).isTrue();
                soft.assertThat(api.getApiLifecycleState()).isEqualTo(io.gravitee.apim.core.api.model.Api.ApiLifecycleState.PUBLISHED);
                soft.assertThat(api.getBackground()).isEqualTo("my-background");
            });
        }

        @Test
        void should_convert_from_v2_repository_to_core_model_with_null_definition() {
            var repository = apiV2().definition(null).build();

            var api = ApiAdapter.INSTANCE.toCoreModel(repository);

            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(api.getId()).isEqualTo("my-id");
                soft.assertThat(api.getEnvironmentId()).isEqualTo("env-id");
                soft.assertThat(api.getGroups()).containsExactly("group-1");
                soft.assertThat(api.getCategories()).containsExactly("category-1");
                soft.assertThat(api.getLabels()).containsExactly("label-1");
                soft.assertThat(api.getCrossId()).isEqualTo("cross-id");
                soft.assertThat(api.getDescription()).isEqualTo("api-description");
                soft.assertThat(api.getVersion()).isEqualTo("1.0.0");
                soft.assertThat(api.getDefinitionVersion()).isNull();
                soft.assertThat(api.getApiDefinitionHttpV4()).isNull();
                soft.assertThat(api.getApiDefinition()).isNull();
                soft.assertThat(api.getType()).isEqualTo(ApiType.PROXY);
                soft.assertThat(api.getName()).isEqualTo("api-name");
                soft.assertThat(api.getDeployedAt()).isEqualTo(Instant.parse("2020-02-03T20:22:02.00Z").atZone(ZoneOffset.UTC));
                soft.assertThat(api.getCreatedAt()).isEqualTo(Instant.parse("2020-02-01T20:22:02.00Z").atZone(ZoneOffset.UTC));
                soft.assertThat(api.getVisibility()).isEqualTo(io.gravitee.apim.core.api.model.Api.Visibility.PUBLIC);
                soft.assertThat(api.getLifecycleState()).isEqualTo(io.gravitee.apim.core.api.model.Api.LifecycleState.STARTED);
                soft.assertThat(api.getUpdatedAt()).isEqualTo(Instant.parse("2020-02-02T20:22:02.00Z").atZone(ZoneOffset.UTC));
                soft.assertThat(api.getPicture()).isEqualTo("my-picture");
                soft.assertThat(api.isDisableMembershipNotifications()).isTrue();
                soft.assertThat(api.getApiLifecycleState()).isEqualTo(io.gravitee.apim.core.api.model.Api.ApiLifecycleState.PUBLISHED);
                soft.assertThat(api.getBackground()).isEqualTo("my-background");
            });
        }

        @Test
        void should_convert_origin_context() {
            var apiWithManagementContext = ApiAdapter.INSTANCE.toCoreModel(apiV4().origin("management").build());
            assertThat(apiWithManagementContext).hasOriginContext(new OriginContext.Management());

            var apiWithKubernetesContext = ApiAdapter.INSTANCE.toCoreModel(apiV4().origin("kubernetes").mode("fully_managed").build());
            assertThat(apiWithKubernetesContext)
                .hasOriginContext(new OriginContext.Kubernetes(OriginContext.Kubernetes.Mode.FULLY_MANAGED));
        }
    }

    @Nested
    class ModelToRepository {

        @Test
        void should_convert_v4_api_to_repository() {
            var model = ApiFixtures.aProxyApiV4();
            model
                .getApiDefinitionHttpV4()
                .setFailover(
                    Failover
                        .builder()
                        .enabled(true)
                        .maxRetries(7)
                        .slowCallDuration(500)
                        .openStateDuration(11000)
                        .maxFailures(3)
                        .perSubscription(false)
                        .build()
                );

            var api = ApiAdapter.INSTANCE.toRepository(model);

            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(api.getApiLifecycleState()).isEqualTo(ApiLifecycleState.PUBLISHED);
                soft.assertThat(api.getBackground()).isEqualTo("api-background");
                soft.assertThat(api.getCategories()).containsExactly("category-1");
                soft.assertThat(api.getCreatedAt()).isEqualTo(Date.from(Instant.parse("2020-02-01T20:22:02.00Z")));
                soft.assertThat(api.getCrossId()).isEqualTo("my-api-crossId");
                try {
                    soft
                        .assertThat(api.getDefinition())
                        .isEqualTo(GraviteeJacksonMapper.getInstance().writeValueAsString(model.getApiDefinitionHttpV4()));
                } catch (JsonProcessingException e) {
                    soft.fail(e.getMessage());
                }
                soft.assertThat(api.getDefinitionVersion()).isEqualTo(DefinitionVersion.V4);
                soft.assertThat(api.getDeployedAt()).isEqualTo(Date.from(Instant.parse("2020-02-03T20:22:02.00Z")));
                soft.assertThat(api.getDescription()).isEqualTo("api-description");
                soft.assertThat(api.isDisableMembershipNotifications()).isTrue();
                soft.assertThat(api.getEnvironmentId()).isEqualTo("environment-id");
                soft.assertThat(api.getGroups()).containsExactly("group-1");
                soft.assertThat(api.getId()).isEqualTo("my-api");
                soft.assertThat(api.getLabels()).containsExactly("label-1");
                soft.assertThat(api.getLifecycleState()).isEqualTo(LifecycleState.STARTED);
                soft.assertThat(api.getName()).isEqualTo("My Api");
                soft.assertThat(api.getOrigin()).isEqualTo("management");
                soft.assertThat(api.getPicture()).isEqualTo("api-picture");
                soft.assertThat(api.getType()).isEqualTo(ApiType.PROXY);
                soft.assertThat(api.getUpdatedAt()).isEqualTo(Date.from(Instant.parse("2020-02-02T20:22:02.00Z")));
                soft.assertThat(api.getVisibility()).isEqualTo(Visibility.PUBLIC);
                soft.assertThat(api.getVersion()).isEqualTo("1.0.0");
            });
        }

        @Test
        void should_convert_v2_api_to_repository() {
            var model = ApiFixtures.aProxyApiV2();

            var api = ApiAdapter.INSTANCE.toRepository(model);

            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(api.getApiLifecycleState()).isEqualTo(ApiLifecycleState.PUBLISHED);
                soft.assertThat(api.getBackground()).isEqualTo("api-background");
                soft.assertThat(api.getCategories()).containsExactly("category-1");
                soft.assertThat(api.getCreatedAt()).isEqualTo(Date.from(Instant.parse("2020-02-01T20:22:02.00Z")));
                soft.assertThat(api.getCrossId()).isEqualTo("my-api-crossId");
                soft
                    .assertThat(api.getDefinition())
                    .isEqualTo(
                        "{\"id\":\"my-api\",\"name\":\"api-name\",\"version\":\"1.0.0\",\"gravitee\":\"2.0.0\",\"execution_mode\":\"v3\",\"flow_mode\":\"DEFAULT\",\"proxy\":{\"strip_context_path\":false,\"preserve_host\":false,\"groups\":[{\"name\":\"default-group\",\"endpoints\":[{\"name\":\"default\",\"target\":\"https://api.gravitee.io/echo\",\"weight\":1,\"backup\":false,\"type\":\"http1\"}],\"load_balancing\":{\"type\":\"ROUND_ROBIN\"},\"http\":{\"connectTimeout\":5000,\"idleTimeout\":60000,\"keepAliveTimeout\":30000,\"keepAlive\":true,\"readTimeout\":10000,\"pipelining\":false,\"maxConcurrentConnections\":100,\"useCompression\":true,\"followRedirects\":false}}]},\"properties\":[],\"tags\":[\"tag1\"]}"
                    );
                soft.assertThat(api.getDefinitionVersion()).isEqualTo(DefinitionVersion.V2);
                soft.assertThat(api.getDeployedAt()).isEqualTo(Date.from(Instant.parse("2020-02-03T20:22:02.00Z")));
                soft.assertThat(api.getDescription()).isEqualTo("api-description");
                soft.assertThat(api.isDisableMembershipNotifications()).isTrue();
                soft.assertThat(api.getEnvironmentId()).isEqualTo("environment-id");
                soft.assertThat(api.getGroups()).containsExactly("group-1");
                soft.assertThat(api.getId()).isEqualTo("my-api");
                soft.assertThat(api.getLabels()).containsExactly("label-1");
                soft.assertThat(api.getLifecycleState()).isEqualTo(LifecycleState.STARTED);
                soft.assertThat(api.getName()).isEqualTo("My Api");
                soft.assertThat(api.getOrigin()).isEqualTo("management");
                soft.assertThat(api.getPicture()).isEqualTo("api-picture");
                soft.assertThat(api.getType()).isEqualTo(ApiType.PROXY);
                soft.assertThat(api.getUpdatedAt()).isEqualTo(Date.from(Instant.parse("2020-02-02T20:22:02.00Z")));
                soft.assertThat(api.getVisibility()).isEqualTo(Visibility.PUBLIC);
                soft.assertThat(api.getVersion()).isEqualTo("1.0.0");
            });
        }

        @Test
        void should_convert_origin_context() {
            var apiWithManagementContext = ApiAdapter.INSTANCE.toRepository(
                ApiFixtures.aProxyApiV4().toBuilder().originContext(new OriginContext.Management()).build()
            );
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(apiWithManagementContext.getOrigin()).isEqualTo("management");
                soft.assertThat(apiWithManagementContext.getMode()).isNull();
            });

            var apiWithKubernetesContext = ApiAdapter.INSTANCE.toRepository(
                ApiFixtures
                    .aProxyApiV4()
                    .toBuilder()
                    .originContext(new OriginContext.Kubernetes(OriginContext.Kubernetes.Mode.FULLY_MANAGED))
                    .build()
            );
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(apiWithKubernetesContext.getOrigin()).isEqualTo("kubernetes");
                soft.assertThat(apiWithKubernetesContext.getMode()).isEqualTo("fully_managed");
            });
        }
    }

    @Nested
    class ModelToApiEntity {

        @Test
        void should_convert_v4_api_to_UpdateApiEntity() {
            var model = ApiFixtures
                .aProxyApiV4()
                .toBuilder()
                .apiLifecycleState(io.gravitee.apim.core.api.model.Api.ApiLifecycleState.PUBLISHED)
                .build();

            var updateApiEntity = ApiAdapter.INSTANCE.toUpdateApiEntity(model, model.getApiDefinitionHttpV4());

            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(updateApiEntity.getId()).isEqualTo("my-api");
                soft.assertThat(updateApiEntity.getCrossId()).isEqualTo("my-api-crossId");
                soft.assertThat(updateApiEntity.getName()).isEqualTo("My Api");
                soft.assertThat(updateApiEntity.getDescription()).isEqualTo("api-description");
                soft.assertThat(updateApiEntity.getApiVersion()).isEqualTo("1.0.0");
                soft.assertThat(updateApiEntity.getVisibility().name()).isEqualTo(Visibility.PUBLIC.name());
                soft.assertThat(updateApiEntity.getLifecycleState().name()).isEqualTo(ApiLifecycleState.PUBLISHED.name());
                soft.assertThat(updateApiEntity.getPicture()).isEqualTo("api-picture");
                soft.assertThat(updateApiEntity.getGroups()).containsExactly("group-1");
                soft.assertThat(updateApiEntity.getCategories()).containsExactly("category-1");
                soft.assertThat(updateApiEntity.getTags()).containsExactly("tag1");
                soft.assertThat(updateApiEntity.getLabels()).containsExactly("label-1");
                soft.assertThat(updateApiEntity.isDisableMembershipNotifications()).isTrue();
                soft.assertThat(updateApiEntity.getBackground()).isEqualTo("api-background");
                soft.assertThat(updateApiEntity.getEndpointGroups()).hasSize(1);
                soft
                    .assertThat(updateApiEntity.getEndpointGroups().get(0))
                    .isEqualTo(model.getApiDefinitionHttpV4().getEndpointGroups().get(0));
                soft.assertThat(updateApiEntity.getListeners()).hasSize(1);
                soft.assertThat(updateApiEntity.getListeners().get(0)).isEqualTo(model.getApiDefinitionHttpV4().getListeners().get(0));
            });
        }
    }

    private Api.ApiBuilder apiV4() {
        return Api
            .builder()
            .id("my-id")
            .environmentId("env-id")
            .crossId("cross-id")
            .name("api-name")
            .description("api-description")
            .version("1.0.0")
            .origin("management")
            .mode("fully_managed")
            .definitionVersion(DefinitionVersion.V4)
            .definition(
                """
                {"id": "my-id", "name": "api-name", "type": "proxy", "apiVersion": "1.0.0", "definitionVersion": "4.0.0", "tags": ["tag1"], "listeners": [{"type": "http", "entrypoints": [{ "type": "http-proxy", "qos": "auto", "configuration": {} }], "paths": [{ "path": "/http_proxy" }]}], "endpointGroups": [{"name": "default-group", "type": "http-proxy", "loadBalancer": { "type": "round-robin" }, "sharedConfiguration": {}, "endpoints": [{"name": "default-endpoint", "type": "http-proxy", "secondary": false, "weight": 1, "inheritConfiguration": true, "configuration": { "target": "https://api.gravitee.io/echo" }, "services": {}}], "services": {}}], "analytics": { "enabled": false }, "failover": { "enabled": true, "maxRetries": 7, "slowCallDuration": 500, "openStateDuration": 11000, "maxFailures": 3, "perSubscription": false }, "flowExecution": { "mode": "default", "matchRequired": false }, "flows": []}
                """
            )
            .type(ApiType.PROXY)
            .createdAt(Date.from(Instant.parse("2020-02-01T20:22:02.00Z")))
            .updatedAt(Date.from(Instant.parse("2020-02-02T20:22:02.00Z")))
            .deployedAt(Date.from(Instant.parse("2020-02-03T20:22:02.00Z")))
            .visibility(Visibility.PUBLIC)
            .lifecycleState(LifecycleState.STARTED)
            .picture("my-picture")
            .groups(Set.of("group-1"))
            .categories(Set.of("category-1"))
            .labels(List.of("label-1"))
            .disableMembershipNotifications(true)
            .apiLifecycleState(ApiLifecycleState.PUBLISHED)
            .background("my-background");
    }

    private Api.ApiBuilder apiV2() {
        return Api
            .builder()
            .id("my-id")
            .environmentId("env-id")
            .crossId("cross-id")
            .name("api-name")
            .description("api-description")
            .version("1.0.0")
            .origin("management")
            .mode("fully_managed")
            .definition(
                """
                {"id": "my-id", "name": "api-name", "version": "1.0.0", "gravitee": "2.0.0", "flow_mode": "DEFAULT", "proxy": {"virtual_hosts": [{ "path": "/proxy" }], "strip_context_path": false, "preserve_host": false, "logging": {"mode": "CLIENT_PROXY", "content": "HEADERS_PAYLOADS", "scope": "REQUEST_RESPONSE"}, "groups": [{"name": "default-group", "endpoints": [{"backup": false, "inherit": true, "name": "default", "weight": 1, "type": "http", "target": "https://api.gravitee.io/echo"}], "load_balancing": { "type": "ROUND_ROBIN" }, "http": {"connectTimeout": 5000, "idleTimeout": 60000, "keepAlive": true, "readTimeout": 10000, "pipelining": false, "maxConcurrentConnections": 100, "useCompression": true, "followRedirects": false}}]}}
                """
            )
            .type(ApiType.PROXY)
            .createdAt(Date.from(Instant.parse("2020-02-01T20:22:02.00Z")))
            .updatedAt(Date.from(Instant.parse("2020-02-02T20:22:02.00Z")))
            .deployedAt(Date.from(Instant.parse("2020-02-03T20:22:02.00Z")))
            .visibility(Visibility.PUBLIC)
            .lifecycleState(LifecycleState.STARTED)
            .picture("my-picture")
            .groups(Set.of("group-1"))
            .categories(Set.of("category-1"))
            .labels(List.of("label-1"))
            .disableMembershipNotifications(true)
            .apiLifecycleState(ApiLifecycleState.PUBLISHED)
            .background("my-background");
    }
}
