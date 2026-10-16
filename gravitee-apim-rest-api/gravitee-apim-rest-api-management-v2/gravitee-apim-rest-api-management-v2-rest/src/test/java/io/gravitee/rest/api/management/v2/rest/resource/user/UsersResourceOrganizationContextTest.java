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
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Exercises the real {@code GetUserApisUseCase} (wired to in-memory repositories in the test context)
 * through the HTTP layer to prove that the user-details endpoints are scoped to the organization
 * resolved into the {@code GraviteeContext}, not to the {@code orgId} URL segment. The authenticated
 * context organization is {@link #ORGANIZATION}, while the URL carries a different organization.
 */
class UsersResourceOrganizationContextTest extends AbstractResourceTest {

    private static final String USER_ID = "user-1";
    private static final String URL_ORGANIZATION = "url-org";

    @Autowired
    MembershipQueryServiceInMemory membershipQueryService;

    @Autowired
    ApiQueryServiceInMemory apiQueryService;

    @Autowired
    EnvironmentCrudServiceInMemory environmentCrudService;

    @Override
    protected String contextPath() {
        return "/organizations/" + URL_ORGANIZATION + "/users/" + USER_ID + "/apis";
    }

    @AfterEach
    public void tearDown() {
        super.tearDown();
        Stream.of(membershipQueryService, apiQueryService, environmentCrudService).forEach(InMemoryAlternative::reset);
    }

    @Test
    void should_return_the_users_apis_when_the_user_belongs_to_the_context_organization() {
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
        apiQueryService.initWith(List.of(Api.builder().id("api-1").name("My API").environmentId("env-1").build()));
        environmentCrudService.initWith(
            List.of(Environment.builder().id("env-1").name("Development").organizationId(ORGANIZATION).build())
        );

        final Response response = rootTarget().request().get();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        var body = response.readEntity(UserApisResponse.class);
        assertThat(body.getData()).hasSize(1);
        assertThat(body.getData().get(0).getId()).isEqualTo("api-1");
    }

    @Test
    void should_return_404_when_the_user_belongs_to_the_url_organization_but_not_the_context_organization() {
        givenExistingUsers(List.of(BaseUserEntity.builder().id(USER_ID).organizationId(URL_ORGANIZATION).build()));
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
        apiQueryService.initWith(List.of(Api.builder().id("api-1").name("My API").environmentId("env-1").build()));

        final Response response = rootTarget().request().get();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.NOT_FOUND_404);
    }
}
