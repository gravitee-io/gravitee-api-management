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
import static org.mockito.Mockito.when;

import inmemory.ApiQueryServiceInMemory;
import inmemory.EnvironmentCrudServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.MembershipQueryServiceInMemory;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.environment.model.Environment;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.management.v2.rest.model.UserApisResponse;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class UsersResourceApisTest extends AbstractResourceTest {

    private static final String USER_ID = "user-1";

    @Autowired
    MembershipQueryServiceInMemory membershipQueryService;

    @Autowired
    ApiQueryServiceInMemory apiQueryService;

    @Autowired
    EnvironmentCrudServiceInMemory environmentCrudService;

    @Override
    protected String contextPath() {
        return "/organizations/" + ORGANIZATION + "/users/" + USER_ID + "/apis";
    }

    @AfterEach
    public void tearDown() {
        super.tearDown();
        Stream.of(membershipQueryService, apiQueryService, environmentCrudService).forEach(InMemoryAlternative::reset);
    }

    private void givenUserWithApi() {
        givenExistingUsers(List.of(BaseUserEntity.builder().id(USER_ID).organizationId(ORGANIZATION).build()));
        membershipQueryService.initWith(
            List.of(
                Membership.builder()
                    .id("m1")
                    .memberId(USER_ID)
                    .memberType(Membership.Type.USER)
                    .referenceType(Membership.ReferenceType.API)
                    .referenceId("api-1")
                    .build()
            )
        );
        apiQueryService.initWith(
            List.of(
                Api.builder().id("api-1").name("My API").version("1.0").visibility(Api.Visibility.PUBLIC).environmentId("env-1").build()
            )
        );
        environmentCrudService.initWith(
            List.of(Environment.builder().id("env-1").name("Development").organizationId(ORGANIZATION).build())
        );
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
        givenUserWithApi();

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
        givenUserWithApi();

        final Response response = rootTarget().queryParam("environmentId", "env-1").request().get();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        var body = response.readEntity(UserApisResponse.class);
        assertThat(body.getData()).hasSize(1);
        assertThat(body.getData().get(0).getId()).isEqualTo("api-1");
    }

    @Test
    void should_return_empty_list_when_no_results() {
        givenExistingUsers(List.of(BaseUserEntity.builder().id(USER_ID).organizationId(ORGANIZATION).build()));

        final Response response = rootTarget().request().get();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);

        var body = response.readEntity(UserApisResponse.class);
        assertThat(body.getData()).isEmpty();
    }
}
