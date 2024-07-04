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
package fixtures.core.model;

import io.gravitee.apim.core.api.model.Api;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.ExecutionMode;
import io.gravitee.definition.model.FlowMode;
import io.gravitee.definition.model.Proxy;
import io.gravitee.definition.model.federation.FederatedApi;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.analytics.Analytics;
import io.gravitee.definition.model.v4.endpointgroup.Endpoint;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.definition.model.v4.failover.Failover;
import io.gravitee.definition.model.v4.flow.execution.FlowExecution;
import io.gravitee.definition.model.v4.listener.ListenerType;
import io.gravitee.definition.model.v4.listener.entrypoint.Entrypoint;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.listener.http.Path;
import io.gravitee.definition.model.v4.listener.tcp.TcpListener;
import io.gravitee.rest.api.model.context.OriginContext;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public class ApiFixtures {

    private ApiFixtures() {}

    public static final String MY_API = "my-api";
    private static final Supplier<Api.ApiBuilder> BASE = () ->
        Api
            .builder()
            .id(MY_API)
            .name("My Api")
            .environmentId("environment-id")
            .crossId("my-api-crossId")
            .description("api-description")
            .version("1.0.0")
            .createdAt(Instant.parse("2020-02-01T20:22:02.00Z").atZone(ZoneId.systemDefault()))
            .updatedAt(Instant.parse("2020-02-02T20:22:02.00Z").atZone(ZoneId.systemDefault()))
            .deployedAt(Instant.parse("2020-02-03T20:22:02.00Z").atZone(ZoneId.systemDefault()))
            .visibility(Api.Visibility.PUBLIC)
            .lifecycleState(Api.LifecycleState.STARTED)
            .apiLifecycleState(Api.ApiLifecycleState.PUBLISHED)
            .picture("api-picture")
            .groups(Set.of("group-1"))
            .categories(Set.of("category-1"))
            .labels(List.of("label-1"))
            .disableMembershipNotifications(true)
            .originContext(new OriginContext.Management())
            .background("api-background");

    public static Api aProxyApiV4() {
        return BASE
            .get()
            .type(ApiType.PROXY)
            .definitionVersion(DefinitionVersion.V4)
            .apiDefinitionV4(
                io.gravitee.definition.model.v4.Api
                    .builder()
                    .id(MY_API)
                    .name("My Api")
                    .apiVersion("1.0.0")
                    .analytics(Analytics.builder().enabled(false).build())
                    .failover(Failover.builder().enabled(false).build())
                    .definitionVersion(DefinitionVersion.V4)
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
                    .flowExecution(new FlowExecution())
                    .build()
            )
            .build();
    }

    public static Api aProxyApiV2() {
        return BASE
            .get()
            .type(ApiType.PROXY)
            .definitionVersion(DefinitionVersion.V2)
            .apiDefinition(
                io.gravitee.definition.model.Api
                    .builder()
                    .id(MY_API)
                    .name("api-name")
                    .version("1.0.0")
                    .tags(Set.of("tag1"))
                    .definitionVersion(DefinitionVersion.V2)
                    .executionMode(ExecutionMode.V3)
                    .flowMode(FlowMode.DEFAULT)
                    .proxy(
                        Proxy
                            .builder()
                            .groups(
                                Set.of(
                                    io.gravitee.definition.model.EndpointGroup
                                        .builder()
                                        .name("default-group")
                                        .endpoints(
                                            Set.of(
                                                io.gravitee.definition.model.Endpoint
                                                    .builder()
                                                    .name("default")
                                                    .type("http1")
                                                    .target("https://api.gravitee.io/echo")
                                                    .build()
                                            )
                                        )
                                        .build()
                                )
                            )
                            .build()
                    )
                    .plans(new HashMap<>())
                    .build()
            )
            .build();
    }

    public static Api aMessageApiV4() {
        return BASE
            .get()
            .type(ApiType.MESSAGE)
            .definitionVersion(DefinitionVersion.V4)
            .apiDefinitionV4(
                io.gravitee.definition.model.v4.Api
                    .builder()
                    .id("my-api")
                    .name("My message Api")
                    .analytics(Analytics.builder().enabled(false).build())
                    .type(ApiType.MESSAGE)
                    .tags(Set.of("tag1"))
                    .listeners(
                        List.of(
                            HttpListener
                                .builder()
                                .paths(List.of(Path.builder().path("/message").build()))
                                .entrypoints(List.of(Entrypoint.builder().type("sse").configuration("{}").build()))
                                .build()
                        )
                    )
                    .endpointGroups(
                        List.of(
                            EndpointGroup
                                .builder()
                                .name("default-group")
                                .type("mock")
                                .sharedConfiguration("{}")
                                .endpoints(
                                    List.of(
                                        Endpoint
                                            .builder()
                                            .name("default-endpoint")
                                            .type("mock")
                                            .inheritConfiguration(true)
                                            .configuration("{}")
                                            .build()
                                    )
                                )
                                .build()
                        )
                    )
                    .flows(List.of())
                    .build()
            )
            .build();
    }

    public static Api aTcpApiV4() {
        return aTcpApiV4(null);
    }

    public static Api aTcpApiV4(List<String> hosts) {
        return BASE
            .get()
            .type(ApiType.PROXY)
            .definitionVersion(DefinitionVersion.V4)
            .apiDefinitionV4(
                io.gravitee.definition.model.v4.Api
                    .builder()
                    .id(MY_API)
                    .name("My Api")
                    .apiVersion("1.0.0")
                    .analytics(Analytics.builder().enabled(false).build())
                    .definitionVersion(DefinitionVersion.V4)
                    .type(ApiType.PROXY)
                    .tags(Set.of("tag1"))
                    .listeners(
                        List.of(
                            TcpListener
                                .builder()
                                .hosts(null != hosts ? hosts : List.of("foo.example.com", "bar.example.com"))
                                .entrypoints(List.of(Entrypoint.builder().type("tcp-proxy").configuration("{}").build()))
                                .type(ListenerType.TCP)
                                .build()
                        )
                    )
                    .endpointGroups(
                        List.of(
                            EndpointGroup
                                .builder()
                                .name("default-group")
                                .type("tcp-proxy")
                                .sharedConfiguration("{}")
                                .endpoints(
                                    List.of(
                                        Endpoint
                                            .builder()
                                            .name("default-endpoint")
                                            .type("tcp-proxy")
                                            .inheritConfiguration(true)
                                            .configuration("{\"target\":\"https://api.gravitee.io/echo\", \"port\":\"443\"}")
                                            .build()
                                    )
                                )
                                .build()
                        )
                    )
                    .flows(List.of())
                    .flowExecution(new FlowExecution())
                    .build()
            )
            .build();
    }

    public static Api aFederatedApi() {
        return BASE
            .get()
            .crossId(null)
            .lifecycleState(null)
            .apiDefinitionV4(null)
            .apiDefinition(null)
            .originContext(new OriginContext.Integration("integration-id"))
            .federatedApiDefinition(FederatedApi.builder().id(MY_API).providerId("provider-id").name("My Api").apiVersion("1.0.0").build())
            .build();
    }
}
