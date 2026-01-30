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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public class RepositoryFixtures {

    private RepositoryFixtures() {}

    private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    public static final String MY_API = "my-api";
    public static final String MY_API_NAME = "My Api";
    private static final Supplier<Api.ApiBuilder> BASE = () -> {
        try {
            return Api.builder()
                .id(MY_API)
                .name(MY_API_NAME)
                .environmentId("environment-id")
                .crossId("my-api-crossId")
                .description("api-description")
                .version("1.0.0")
                .createdAt(new RepositoryFixtures().simpleDateFormat.parse("2020-02-01T20:22:02.00Z"))
                .updatedAt(new RepositoryFixtures().simpleDateFormat.parse("2020-02-02T20:22:02.00Z"))
                .deployedAt(new RepositoryFixtures().simpleDateFormat.parse("2020-02-03T20:22:02.00Z"))
                .visibility(Visibility.PUBLIC)
                .lifecycleState(LifecycleState.STARTED)
                .apiLifecycleState(ApiLifecycleState.PUBLISHED)
                .picture("api-picture")
                .groups(Set.of("group-1"))
                .categories(Set.of("category-1"))
                .labels(List.of("label-1"))
                .disableMembershipNotifications(true)
                .origin(Api.ORIGIN_MANAGEMENT)
                .background("api-background");
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    };

    public static Api aProxyApiV4() {
        String type = "http-proxy";
        return BASE.get()
            .type(ApiType.PROXY)
            .definitionVersion(DefinitionVersion.V4)
            .definition(
                io.gravitee.definition.model.v4.Api.builder()
                    .id(MY_API)
                    .name(MY_API_NAME)
                    .apiVersion("1.0.0")
                    .analytics(Analytics.builder().enabled(false).build())
                    .failover(Failover.builder().enabled(false).build())
                    .definitionVersion(DefinitionVersion.V4)
                    .type(ApiType.PROXY)
                    .tags(Set.of("tag1"))
                    .listeners(
                        List.of(
                            HttpListener.builder()
                                .paths(List.of(Path.builder().path("/http_proxy").build()))
                                .entrypoints(List.of(Entrypoint.builder().type(type).configuration("{}").build()))
                                .build()
                        )
                    )
                    .endpointGroups(
                        List.of(
                            EndpointGroup.builder()
                                .name("default-group")
                                .type(type)
                                .sharedConfiguration("{}")
                                .endpoints(
                                    List.of(
                                        Endpoint.builder()
                                            .name("default-endpoint")
                                            .type(type)
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
                    .toString()
            )
            .build();
    }
}
