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
import static io.gravitee.rest.api.management.v2.rest.resource.api.ApiDefinitionFixtures.httpApi;
import static io.gravitee.rest.api.management.v2.rest.resource.api.ApiDefinitionFixtures.nativeApi;
import static io.gravitee.rest.api.management.v2.rest.resource.api.ApiDefinitionFixtures.v2Api;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.management.v2.rest.model.Api;
import io.gravitee.rest.api.management.v2.rest.model.ApiSearchQuery;
import io.gravitee.rest.api.management.v2.rest.model.ApiV2;
import io.gravitee.rest.api.management.v2.rest.model.ApiV4;
import io.gravitee.rest.api.management.v2.rest.model.ApisResponse;
import io.gravitee.rest.api.management.v2.rest.model.GenericApi;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * The API list and the API search return, API by API, the same content as the single API read: an API whose
 * definition the caller cannot read comes without its gateway definition details.
 */
class ApisResource_DefinitionReadTest extends AbstractApisResourceTest {

    private static final String HTTP_API = "http-api";
    private static final String NATIVE_API = "native-api";
    private static final String V2_API = "v2-api";

    enum ListEndpoint {
        LIST,
        SEARCH,
    }

    @AfterEach
    void resetSharedMocks() {
        // The resource mocks are Spring singletons, shared with the test classes run after this one.
        reset(apiServiceV4, apiSearchServiceV4, apiStateServiceV4);
        GraviteeContext.cleanContext();
    }

    @ParameterizedTest
    @EnumSource(ListEndpoint.class)
    void should_return_http_api_without_definition_details_when_caller_cannot_read_definition(ListEndpoint endpoint) {
        givenApis(httpApi(HTTP_API));
        givenNoDefinitionRead(HTTP_API);

        ApiV4 api = call(endpoint).getFirst().getApiV4();

        assertThat(api.getId()).isEqualTo(HTTP_API);
        assertThat(api.getEndpointGroups()).isNullOrEmpty();
        assertThat(api.getProperties()).isNullOrEmpty();
        assertThat(api.getResources()).isNullOrEmpty();
        var httpListener = api.getListeners().getFirst().getHttpListener();
        assertThat(httpListener.getPaths()).hasSize(1);
        assertThat(httpListener.getPaths().getFirst().getHost()).isNull();
        assertThat(httpListener.getPaths().getFirst().getPath()).isEqualTo("/first");
    }

    @ParameterizedTest
    @EnumSource(ListEndpoint.class)
    void should_return_native_api_without_definition_details_when_caller_cannot_read_definition(ListEndpoint endpoint) {
        givenApis(nativeApi(NATIVE_API));
        givenNoDefinitionRead(NATIVE_API);

        ApiV4 api = call(endpoint).getFirst().getApiV4();

        assertThat(api.getId()).isEqualTo(NATIVE_API);
        assertThat(api.getEndpointGroups()).isNullOrEmpty();
        assertThat(api.getProperties()).isNullOrEmpty();
        assertThat(api.getResources()).isNullOrEmpty();
    }

    @ParameterizedTest
    @EnumSource(ListEndpoint.class)
    void should_return_v2_api_without_definition_details_when_caller_cannot_read_definition(ListEndpoint endpoint) {
        givenApis(v2Api(V2_API));
        givenNoDefinitionRead(V2_API);

        ApiV2 api = call(endpoint).getFirst().getApiV2();

        assertThat(api.getId()).isEqualTo(V2_API);
        assertThat(api.getProxy().getGroups()).isNullOrEmpty();
        assertThat(api.getProxy().getVirtualHosts()).hasSize(1);
        assertThat(api.getProxy().getVirtualHosts().getFirst().getHost()).isNull();
        assertThat(api.getProxy().getVirtualHosts().getFirst().getPath()).isEqualTo("/first");
        assertThat(api.getProperties()).isNullOrEmpty();
    }

    @ParameterizedTest
    @EnumSource(ListEndpoint.class)
    void should_return_v2_api_without_proxy_when_caller_cannot_read_definition(ListEndpoint endpoint) {
        var api = v2Api(V2_API);
        api.setProxy(null);
        givenApis(api);
        givenNoDefinitionRead(V2_API);

        ApiV2 result = call(endpoint).getFirst().getApiV2();

        assertThat(result.getId()).isEqualTo(V2_API);
        assertThat(result.getProperties()).isNullOrEmpty();
    }

    @ParameterizedTest
    @EnumSource(ListEndpoint.class)
    void should_return_v2_api_without_virtual_hosts_when_caller_cannot_read_definition(ListEndpoint endpoint) {
        var api = v2Api(V2_API);
        api.getProxy().setVirtualHosts(new ArrayList<>());
        givenApis(api);
        givenNoDefinitionRead(V2_API);

        ApiV2 result = call(endpoint).getFirst().getApiV2();

        assertThat(result.getId()).isEqualTo(V2_API);
        assertThat(result.getProxy().getVirtualHosts()).isNullOrEmpty();
        assertThat(result.getProxy().getGroups()).isNullOrEmpty();
    }

    @ParameterizedTest
    @EnumSource(ListEndpoint.class)
    void should_return_definition_details_when_caller_can_read_definition(ListEndpoint endpoint) {
        givenApis(httpApi(HTTP_API), nativeApi(NATIVE_API), v2Api(V2_API));

        List<Api> apis = call(endpoint);

        ApiV4 http = apis.get(0).getApiV4();
        assertThat(http.getEndpointGroups()).hasSize(1);
        assertThat(http.getProperties()).hasSize(1);
        assertThat(http.getListeners().getFirst().getHttpListener().getPaths()).hasSize(2);
        assertThat(apis.get(1).getApiV4().getEndpointGroups()).hasSize(1);
        ApiV2 v2 = apis.get(2).getApiV2();
        assertThat(v2.getProxy().getGroups()).hasSize(1);
        assertThat(v2.getProxy().getVirtualHosts()).hasSize(2);
        assertThat(v2.getProperties()).hasSize(1);
    }

    @ParameterizedTest
    @EnumSource(ListEndpoint.class)
    void should_decide_api_by_api(ListEndpoint endpoint) {
        givenApis(httpApi(HTTP_API), v2Api(V2_API));
        givenNoDefinitionRead(HTTP_API);

        List<Api> apis = call(endpoint);

        assertThat(apis.get(0).getApiV4().getEndpointGroups()).isNullOrEmpty();
        assertThat(apis.get(1).getApiV2().getProxy().getGroups()).hasSize(1);
    }

    @ParameterizedTest
    @EnumSource(ListEndpoint.class)
    void should_compute_deployment_state_on_the_complete_api(ListEndpoint endpoint) {
        givenApis(httpApi(HTTP_API));
        givenNoDefinitionRead(HTTP_API);
        // Synchronized only when compared with the complete API, as the deployed definition is
        when(apiStateServiceV4.isSynchronized(any(), any())).thenAnswer(invocation -> {
            ApiEntity api = invocation.getArgument(1);
            return api.getEndpointGroups() != null && !api.getEndpointGroups().isEmpty();
        });

        ApiV4 api = call(endpoint, target -> target.queryParam("expands", "deploymentState"))
            .getFirst()
            .getApiV4();

        assertThat(api.getDeploymentState()).isEqualTo(GenericApi.DeploymentStateEnum.DEPLOYED);
        assertThat(api.getEndpointGroups()).isNullOrEmpty();
    }

    @ParameterizedTest
    @EnumSource(ListEndpoint.class)
    void should_not_compute_deployment_state_when_not_expanded(ListEndpoint endpoint) {
        givenApis(httpApi(HTTP_API));
        givenNoDefinitionRead(HTTP_API);
        when(apiStateServiceV4.isSynchronized(any(), any())).thenReturn(true);

        ApiV4 api = call(endpoint).getFirst().getApiV4();

        assertThat(api.getDeploymentState()).isNull();
        assertThat(api.getEndpointGroups()).isNullOrEmpty();
    }

    private List<Api> call(ListEndpoint endpoint) {
        return call(endpoint, Function.identity());
    }

    private List<Api> call(ListEndpoint endpoint, Function<jakarta.ws.rs.client.WebTarget, jakarta.ws.rs.client.WebTarget> query) {
        Response response = switch (endpoint) {
            case LIST -> query.apply(rootTarget()).request().get();
            case SEARCH -> query.apply(rootTarget("_search")).request().post(Entity.json(new ApiSearchQuery()));
        };
        assertThat(response.getStatus()).isEqualTo(OK_200);
        List<Api> apis = response.readEntity(ApisResponse.class).getData();
        assertThat(apis).isNotEmpty();
        return apis;
    }

    private void givenApis(GenericApiEntity... apis) {
        var content = new ArrayList<GenericApiEntity>(List.of(apis));
        var page = new Page<>(content, 1, content.size(), content.size());
        when(apiServiceV4.findAll(any(), anyString(), anyBoolean(), any(), any(), any())).thenReturn(page);
        when(apiSearchServiceV4.search(any(), anyString(), anyBoolean(), any(), any(), anyBoolean(), anyBoolean(), any())).thenReturn(page);
    }

    private void givenNoDefinitionRead(String apiId) {
        when(
            permissionService.hasPermission(any(), eq(RolePermission.API_DEFINITION), eq(apiId), eq(RolePermissionAction.READ))
        ).thenReturn(false);
    }
}
