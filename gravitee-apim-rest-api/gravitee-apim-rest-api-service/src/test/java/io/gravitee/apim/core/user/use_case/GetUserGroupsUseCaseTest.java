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

import inmemory.EnvironmentCrudServiceInMemory;
import inmemory.GroupQueryServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.MembershipQueryServiceInMemory;
import inmemory.RoleQueryServiceInMemory;
import io.gravitee.apim.core.environment.model.Environment;
import io.gravitee.apim.core.group.model.Group;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.membership.model.Role;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetUserGroupsUseCaseTest {

    private static final String USER_ID = "user-1";

    MembershipQueryServiceInMemory membershipQueryService = new MembershipQueryServiceInMemory();
    GroupQueryServiceInMemory groupQueryService = new GroupQueryServiceInMemory();
    RoleQueryServiceInMemory roleQueryService = new RoleQueryServiceInMemory();
    EnvironmentCrudServiceInMemory environmentCrudService = new EnvironmentCrudServiceInMemory();
    GetUserGroupsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetUserGroupsUseCase(membershipQueryService, groupQueryService, roleQueryService, environmentCrudService);
    }

    @AfterEach
    void tearDown() {
        Stream.of(membershipQueryService, groupQueryService, roleQueryService, environmentCrudService).forEach(InMemoryAlternative::reset);
    }

    @Test
    void should_return_user_groups_with_correct_fields() {
        membershipQueryService.initWith(
            List.of(
                Membership.builder()
                    .id("m1")
                    .memberId(USER_ID)
                    .memberType(Membership.Type.USER)
                    .referenceType(Membership.ReferenceType.GROUP)
                    .referenceId("group-1")
                    .roleId("role-api-user")
                    .build(),
                Membership.builder()
                    .id("m2")
                    .memberId(USER_ID)
                    .memberType(Membership.Type.USER)
                    .referenceType(Membership.ReferenceType.GROUP)
                    .referenceId("group-1")
                    .roleId("role-app-admin")
                    .build()
            )
        );
        groupQueryService.initWith(List.of(Group.builder().id("group-1").name("My Group").environmentId("env-1").build()));
        roleQueryService.initWith(
            List.of(
                Role.builder().id("role-api-user").name("USER").scope(Role.Scope.API).build(),
                Role.builder().id("role-app-admin").name("ADMIN").scope(Role.Scope.APPLICATION).build()
            )
        );
        environmentCrudService.initWith(List.of(Environment.builder().id("env-1").name("Development").build()));

        var output = useCase.execute(new GetUserGroupsUseCase.Input(USER_ID, null, 1, 10));

        assertThat(output.data()).hasSize(1);
        assertThat(output.totalCount()).isEqualTo(1);

        var result = output.data().get(0);
        assertThat(result.getId()).isEqualTo("group-1");
        assertThat(result.getName()).isEqualTo("My Group");
        assertThat(result.getEnvironmentId()).isEqualTo("env-1");
        assertThat(result.getEnvironmentName()).isEqualTo("Development");
        assertThat(result.getRoles()).containsEntry("API", "USER").containsEntry("APPLICATION", "ADMIN");
    }

    @Test
    void should_filter_by_environment_id() {
        membershipQueryService.initWith(
            List.of(
                Membership.builder()
                    .id("m1")
                    .memberId(USER_ID)
                    .memberType(Membership.Type.USER)
                    .referenceType(Membership.ReferenceType.GROUP)
                    .referenceId("group-1")
                    .roleId("role-1")
                    .build(),
                Membership.builder()
                    .id("m2")
                    .memberId(USER_ID)
                    .memberType(Membership.Type.USER)
                    .referenceType(Membership.ReferenceType.GROUP)
                    .referenceId("group-2")
                    .roleId("role-1")
                    .build()
            )
        );
        groupQueryService.initWith(
            List.of(
                Group.builder().id("group-1").name("Group 1").environmentId("env-1").build(),
                Group.builder().id("group-2").name("Group 2").environmentId("env-2").build()
            )
        );
        roleQueryService.initWith(List.of(Role.builder().id("role-1").name("USER").scope(Role.Scope.API).build()));
        environmentCrudService.initWith(
            List.of(Environment.builder().id("env-1").name("Dev").build(), Environment.builder().id("env-2").name("Prod").build())
        );

        var output = useCase.execute(new GetUserGroupsUseCase.Input(USER_ID, "env-1", 1, 10));

        assertThat(output.data()).hasSize(1);
        assertThat(output.totalCount()).isEqualTo(1);
        assertThat(output.data().get(0).getId()).isEqualTo("group-1");
    }

    @Test
    void should_return_empty_page_when_user_has_no_memberships() {
        var output = useCase.execute(new GetUserGroupsUseCase.Input(USER_ID, null, 1, 10));

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
                    .referenceType(Membership.ReferenceType.GROUP)
                    .referenceId("group-1")
                    .roleId("role-1")
                    .build(),
                Membership.builder()
                    .id("m2")
                    .memberId(USER_ID)
                    .memberType(Membership.Type.USER)
                    .referenceType(Membership.ReferenceType.GROUP)
                    .referenceId("group-2")
                    .roleId("role-1")
                    .build(),
                Membership.builder()
                    .id("m3")
                    .memberId(USER_ID)
                    .memberType(Membership.Type.USER)
                    .referenceType(Membership.ReferenceType.GROUP)
                    .referenceId("group-3")
                    .roleId("role-1")
                    .build()
            )
        );
        groupQueryService.initWith(
            List.of(
                Group.builder().id("group-1").name("Group 1").environmentId("env-1").build(),
                Group.builder().id("group-2").name("Group 2").environmentId("env-1").build(),
                Group.builder().id("group-3").name("Group 3").environmentId("env-1").build()
            )
        );
        roleQueryService.initWith(List.of(Role.builder().id("role-1").name("USER").scope(Role.Scope.API).build()));
        environmentCrudService.initWith(List.of(Environment.builder().id("env-1").name("Dev").build()));

        var output = useCase.execute(new GetUserGroupsUseCase.Input(USER_ID, null, 2, 2));

        assertThat(output.data()).hasSize(1);
        assertThat(output.totalCount()).isEqualTo(3);
    }
}
