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

import io.gravitee.apim.core.api.model.NewApi;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.endpointgroup.Endpoint;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.definition.model.v4.listener.entrypoint.Entrypoint;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.listener.http.Path;
import java.util.List;
import java.util.function.Supplier;

public class NewApiFixtures {

    private NewApiFixtures() {}

    public static final String MY_API = "my-api";
    private static final Supplier<NewApi.NewApiBuilder> BASE = () -> NewApi.builder().name("My API").apiVersion("1.0");

    public static NewApi aProxyApiV4() {
        return BASE.get()
            .definitionVersion(DefinitionVersion.V4)
            .type(ApiType.PROXY)
            .listeners(
                List.of(
                    HttpListener.builder()
                        .paths(List.of(Path.builder().path("/http_proxy").build()))
                        .entrypoints(List.of(Entrypoint.builder().type("http-proxy").configuration("{}").build()))
                        .build()
                )
            )
            .endpointGroups(
                List.of(
                    EndpointGroup.builder()
                        .name("default-group")
                        .type("http-proxy")
                        .sharedConfiguration("{}")
                        .endpoints(
                            List.of(
                                Endpoint.builder()
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
            .build();
    }
}
