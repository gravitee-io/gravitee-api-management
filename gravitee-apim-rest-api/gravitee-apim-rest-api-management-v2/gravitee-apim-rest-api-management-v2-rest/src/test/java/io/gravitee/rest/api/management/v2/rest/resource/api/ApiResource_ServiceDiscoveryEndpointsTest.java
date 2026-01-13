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
package io.gravitee.rest.api.management.v2.rest.resource.api;

import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import fixtures.definition.ApiDefinitionFixtures;
import io.gravitee.apim.core.api.use_case.GetApiDefinitionUseCase;
import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.definition.model.v4.endpointgroup.service.EndpointGroupServices;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.listener.http.Path;
import io.gravitee.definition.model.v4.service.Service;
import io.gravitee.rest.api.management.v2.rest.model.ServiceDiscoveryEndpointsResponse;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class ApiResource_ServiceDiscoveryEndpointsTest extends ApiResourceTest {

    @Autowired
    GetApiDefinitionUseCase getApiDefinitionUseCase;

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis";
    }

    @Test
    public void should_get_empty_endpoints_when_service_is_missing() {
        Api apiDefinition = Api.builder()
            .name("an-api")
            .apiVersion("1.0.0")
            .type(ApiType.PROXY)
            .listeners(List.of(HttpListener.builder().paths(List.of(new Path())).build()))
            .endpointGroups(
                List.of(
                    EndpointGroup.builder()
                        .name("default-group")
                        .type("http-proxy")
                        .services(
                            EndpointGroupServices.builder()
                                .discovery(Service.builder().type("kubernetes-service-discovery").enabled(true).configuration("{}").build())
                                .build()
                        )
                        .build()
                )
            )
            .build();

        when(getApiDefinitionUseCase.execute(any())).thenReturn(new GetApiDefinitionUseCase.Output(apiDefinition));

        final Response response = rootTarget(API + "/service-discovery/endpoints").request().get();
        assertThat(response.getStatus()).isEqualTo(OK_200);

        ServiceDiscoveryEndpointsResponse body = response.readEntity(ServiceDiscoveryEndpointsResponse.class);
        assertThat(body.getGroups()).hasSize(1);
        assertThat(body.getGroups().get(0).getName()).isEqualTo("default-group");
        assertThat(body.getGroups().get(0).getEndpoints()).isEmpty();
    }

    @Test
    public void should_return_empty_groups_for_v2_apis() {
        when(getApiDefinitionUseCase.execute(any())).thenReturn(new GetApiDefinitionUseCase.Output(ApiDefinitionFixtures.anApiV2()));

        final Response response = rootTarget(API + "/service-discovery/endpoints").request().get();
        assertThat(response.getStatus()).isEqualTo(OK_200);

        ServiceDiscoveryEndpointsResponse body = response.readEntity(ServiceDiscoveryEndpointsResponse.class);
        assertThat(body.getGroups()).isEmpty();
    }
}
