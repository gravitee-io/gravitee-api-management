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
package fixtures.definition;

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.ExecutionMode;
import io.gravitee.definition.model.Proxy;
import io.gravitee.definition.model.VirtualHost;
import io.gravitee.definition.model.federation.FederatedApi;
import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.analytics.Analytics;
import io.gravitee.definition.model.v4.endpointgroup.Endpoint;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.definition.model.v4.flow.execution.FlowExecution;
import io.gravitee.definition.model.v4.listener.entrypoint.Entrypoint;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.listener.http.Path;
import io.gravitee.definition.model.v4.nativeapi.NativeApi;
import java.util.HashMap;
import java.util.List;
import java.util.function.Supplier;

public class ApiDefinitionFixtures {

    private ApiDefinitionFixtures() {}

    private static final Supplier<Api.ApiBuilder<?, ?>> BASE_V4 = () ->
        Api.builder().name("an-api").apiVersion("1.0.0").type(ApiType.PROXY).analytics(Analytics.builder().enabled(false).build());

    private static final Supplier<io.gravitee.definition.model.Api.ApiBuilder> BASE_V2 = () ->
        io.gravitee.definition.model.Api
            .builder()
            .name("an-api")
            .version("1.0.0")
            .executionMode(ExecutionMode.V3)
            .proxy(Proxy.builder().virtualHosts(List.of(new VirtualHost("/"))).build());

    private static final Supplier<NativeApi.NativeApiBuilder<?, ?>> BASE_NATIVE = () ->
        NativeApi.builder().name("an-api").apiVersion("1.0.0").type(ApiType.NATIVE);

    public static Api anApiV4() {
        return aSyncApiV4();
    }

    public static Api aSyncApiV4() {
        var httpListener = HttpListener.builder().paths(List.of(new Path())).build();

        return BASE_V4.get().listeners(List.of(httpListener)).endpointGroups(List.of(EndpointGroup.builder().build())).build();
    }

    public static Api aHttpProxyApiV4(String apiId) {
        return BASE_V4
            .get()
            .id(apiId)
            .analytics(Analytics.builder().enabled(false).build())
            .definitionVersion(DefinitionVersion.V4)
            .type(ApiType.PROXY)
            .listeners(
                List.of(
                    HttpListener
                        .builder()
                        .paths(List.of(Path.builder().path("/http_proxy/").build()))
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
            .build();
    }

    public static NativeApi aNativeApiV4() {
        return BASE_NATIVE.get().build();
    }

    public static NativeApi aNativeApiV4(String apiId) {
        return BASE_NATIVE.get().id(apiId).build();
    }

    public static io.gravitee.definition.model.Api anApiV2() {
        return BASE_V2.get().definitionVersion(DefinitionVersion.V2).plans(new HashMap<>()).build();
    }

    public static io.gravitee.definition.model.Api anApiV1() {
        return BASE_V2.get().definitionVersion(DefinitionVersion.V1).build();
    }

    public static FederatedApi aFederatedApi() {
        return FederatedApi.builder().name("federated-api").providerId("api-provider-id").build();
    }
}
