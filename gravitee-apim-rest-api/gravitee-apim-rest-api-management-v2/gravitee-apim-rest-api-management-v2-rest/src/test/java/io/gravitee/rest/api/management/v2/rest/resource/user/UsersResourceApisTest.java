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
package io.gravitee.rest.api.management.v2.rest.resource.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.user.model.UserApi;
import io.gravitee.apim.core.user.use_case.GetUserApisUseCase;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.management.v2.rest.model.UserApisResponse;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class UsersResourceApisTest extends AbstractResourceTest {

    private static final String USER_ID = "user-1";

    @Override
    protected String contextPath() {
        return "/organizations/" + ORGANIZATION + "/users/" + USER_ID + "/apis";
    }

    @AfterEach
    public void tearDown() {
        super.tearDown();
        reset(getUserApisUseCase);
    }

    @Test
    void should_return_403_when_missing_permission() {
        when(
            permissionService.hasPermission(any(), eq(RolePermission.ORGANIZATION_USERS), eq(ORGANIZATION), eq(RolePermissionAction.READ))
        ).thenReturn(false);

        final Response response = rootTarget().request().get();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.FORBIDDEN_403);
    }

    @Test
    void should_return_200_with_paginated_response() {
        var apis = List.of(
            UserApi.builder()
                .id("api-1")
                .name("My API")
                .version("1.0")
                .visibility("PUBLIC")
                .environmentId("env-1")
                .environmentName("Development")
                .build()
        );

        when(getUserApisUseCase.execute(any())).thenReturn(new GetUserApisUseCase.Output(apis, 1));

        final Response response = rootTarget().request().get();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);

        var body = response.readEntity(UserApisResponse.class);
        assertThat(body.getData()).hasSize(1);
        assertThat(body.getData().get(0).getId()).isEqualTo("api-1");
        assertThat(body.getData().get(0).getName()).isEqualTo("My API");
        assertThat(body.getData().get(0).getVersion()).isEqualTo("1.0");
        assertThat(body.getData().get(0).getVisibility()).isEqualTo("PUBLIC");
        assertThat(body.getData().get(0).getEnvironmentId()).isEqualTo("env-1");
        assertThat(body.getData().get(0).getEnvironmentName()).isEqualTo("Development");
        assertThat(body.getPagination()).isNotNull();
        assertThat(body.getLinks()).isNotNull();
    }

    @Test
    void should_pass_environment_id_query_param() {
        when(getUserApisUseCase.execute(any())).thenReturn(new GetUserApisUseCase.Output(List.of(), 0));

        final Response response = rootTarget().queryParam("environmentId", "env-1").request().get();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
    }

    @Test
    void should_return_empty_list_when_no_results() {
        when(getUserApisUseCase.execute(any())).thenReturn(new GetUserApisUseCase.Output(List.of(), 0));

        final Response response = rootTarget().request().get();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);

        var body = response.readEntity(UserApisResponse.class);
        assertThat(body.getData()).isEmpty();
    }
}
