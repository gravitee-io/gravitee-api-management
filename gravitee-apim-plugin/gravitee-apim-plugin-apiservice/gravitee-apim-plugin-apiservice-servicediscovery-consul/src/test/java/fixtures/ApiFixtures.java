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
package fixtures;

import static fixtures.definition.ApiDefinitionFixtures.anApiV4;

import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.definition.model.v4.endpointgroup.service.EndpointGroupServices;
import io.gravitee.definition.model.v4.service.Service;
import io.gravitee.gateway.reactive.handlers.api.v4.Api;
import java.util.Collections;
import java.util.List;

public class ApiFixtures {

    private ApiFixtures() {}

    public static Api anApiWithServiceDiscovery(Service discovery) {
        return new Api(
            anApiV4()
                .toBuilder()
                .endpointGroups(
                    List.of(
                        EndpointGroup.builder()
                            .name("default-group")
                            .services(EndpointGroupServices.builder().discovery(discovery).build())
                            .build()
                    )
                )
                .build()
        );
    }

    public static Api anApiWithDefaultGroup(String type) {
        return new Api(
            anApiV4()
                .toBuilder()
                .endpointGroups(
                    List.of(EndpointGroup.builder().name("default-group").type(type).endpoints(Collections.emptyList()).build())
                )
                .build()
        );
    }
}
