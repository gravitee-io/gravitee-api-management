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
package io.gravitee.apim.core.user.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import inmemory.ApiQueryServiceInMemory;
import inmemory.EnvironmentCrudServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.MembershipQueryServiceInMemory;
import inmemory.UserCrudServiceInMemory;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.environment.domain_service.ValidateEnvironmentInOrganizationDomainService;
import io.gravitee.apim.core.environment.model.Environment;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.user.domain_service.ValidateUserInOrganizationDomainService;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.rest.api.service.exceptions.EnvironmentNotFoundException;
import io.gravitee.rest.api.service.exceptions.UserNotFoundException;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetUserApisUseCaseTest {

    private static final String ORGANIZATION_ID = "org-1";
    private static final String OTHER_ORGANIZATION_ID = "org-2";
    private static final String USER_ID = "user-1";

    MembershipQueryServiceInMemory membershipQueryService = new MembershipQueryServiceInMemory();
    ApiQueryServiceInMemory apiQueryService = new ApiQueryServiceInMemory();
    EnvironmentCrudServiceInMemory environmentCrudService = new EnvironmentCrudServiceInMemory();
    UserCrudServiceInMemory userCrudService = new UserCrudServiceInMemory();
    GetUserApisUseCase useCase;

    @BeforeEach
    void setUp() {
        userCrudService.initWith(List.of(BaseUserEntity.builder().id(USER_ID).organizationId(ORGANIZATION_ID).build()));
        useCase = new GetUserApisUseCase(
            new ValidateUserInOrganizationDomainService(userCrudService),
            new ValidateEnvironmentInOrganizationDomainService(environmentCrudService),
            membershipQueryService,
            apiQueryService,
            environmentCrudService
        );
    }

    @AfterEach
    void tearDown() {
        Stream.of(membershipQueryService, apiQueryService, environmentCrudService, userCrudService).forEach(InMemoryAlternative::reset);
    }

    @Test
    void should_return_user_apis_with_correct_fields() {
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
            List.of(Environment.builder().id("env-1").name("Development").organizationId(ORGANIZATION_ID).build())
        );

        var output = useCase.execute(new GetUserApisUseCase.Input(USER_ID, ORGANIZATION_ID, null, 1, 10));

        assertThat(output.data()).hasSize(1);
        assertThat(output.totalCount()).isEqualTo(1);

        var result = output.data().get(0);
        assertThat(result.getId()).isEqualTo("api-1");
        assertThat(result.getName()).isEqualTo("My API");
        assertThat(result.getVersion()).isEqualTo("1.0");
        assertThat(result.getVisibility()).isEqualTo("PUBLIC");
        assertThat(result.getEnvironmentId()).isEqualTo("env-1");
        assertThat(result.getEnvironmentName()).isEqualTo("Development");
    }

    @Test
    void should_filter_by_environment_id() {
        membershipQueryService.initWith(
            List.of(
                Membership.builder()
                    .id("m1")
                    .memberId(USER_ID)
                    .memberType(Membership.Type.USER)
                    .referenceType(Membership.ReferenceType.API)
                    .referenceId("api-1")
                    .build(),
                Membership.builder()
                    .id("m2")
                    .memberId(USER_ID)
                    .memberType(Membership.Type.USER)
                    .referenceType(Membership.ReferenceType.API)
                    .referenceId("api-2")
                    .build()
            )
        );
        apiQueryService.initWith(
            List.of(
                Api.builder().id("api-1").name("API 1").environmentId("env-1").build(),
                Api.builder().id("api-2").name("API 2").environmentId("env-2").build()
            )
        );
        environmentCrudService.initWith(
            List.of(
                Environment.builder().id("env-1").name("Dev").organizationId(ORGANIZATION_ID).build(),
                Environment.builder().id("env-2").name("Prod").organizationId(ORGANIZATION_ID).build()
            )
        );

        var output = useCase.execute(new GetUserApisUseCase.Input(USER_ID, ORGANIZATION_ID, "env-1", 1, 10));

        assertThat(output.data()).hasSize(1);
        assertThat(output.totalCount()).isEqualTo(1);
        assertThat(output.data().get(0).getId()).isEqualTo("api-1");
    }

    @Test
    void should_return_empty_page_when_user_has_no_memberships() {
        var output = useCase.execute(new GetUserApisUseCase.Input(USER_ID, ORGANIZATION_ID, null, 1, 10));

        assertThat(output.data()).isEmpty();
        assertThat(output.totalCount()).isZero();
    }

    @Test
    void should_paginate_correctly() {
        membershipQueryService.initWith(
            List.of(
                Membership.builder()
                    .id("m1")
                    .memberId(USER_ID)
                    .memberType(Membership.Type.USER)
                    .referenceType(Membership.ReferenceType.API)
                    .referenceId("api-1")
                    .build(),
                Membership.builder()
                    .id("m2")
                    .memberId(USER_ID)
                    .memberType(Membership.Type.USER)
                    .referenceType(Membership.ReferenceType.API)
                    .referenceId("api-2")
                    .build(),
                Membership.builder()
                    .id("m3")
                    .memberId(USER_ID)
                    .memberType(Membership.Type.USER)
                    .referenceType(Membership.ReferenceType.API)
                    .referenceId("api-3")
                    .build()
            )
        );
        apiQueryService.initWith(
            List.of(
                Api.builder().id("api-1").name("API 1").environmentId("env-1").build(),
                Api.builder().id("api-2").name("API 2").environmentId("env-1").build(),
                Api.builder().id("api-3").name("API 3").environmentId("env-1").build()
            )
        );
        environmentCrudService.initWith(List.of(Environment.builder().id("env-1").name("Dev").organizationId(ORGANIZATION_ID).build()));

        var output = useCase.execute(new GetUserApisUseCase.Input(USER_ID, ORGANIZATION_ID, null, 2, 2));

        assertThat(output.data()).hasSize(1);
        assertThat(output.totalCount()).isEqualTo(3);
        assertThat(output.data().get(0).getId()).isEqualTo("api-3");
    }

    @Test
    void should_throw_when_user_belongs_to_another_organization() {
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
        apiQueryService.initWith(List.of(Api.builder().id("api-1").name("API 1").environmentId("env-1").build()));

        assertThatThrownBy(() -> useCase.execute(new GetUserApisUseCase.Input(USER_ID, OTHER_ORGANIZATION_ID, null, 1, 10))).isInstanceOf(
            UserNotFoundException.class
        );
    }

    @Test
    void should_throw_when_environment_belongs_to_another_organization() {
        environmentCrudService.initWith(
            List.of(Environment.builder().id("env-other").name("Other").organizationId(OTHER_ORGANIZATION_ID).build())
        );

        assertThatThrownBy(() -> useCase.execute(new GetUserApisUseCase.Input(USER_ID, ORGANIZATION_ID, "env-other", 1, 10))).isInstanceOf(
            EnvironmentNotFoundException.class
        );
    }
}
