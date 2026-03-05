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
package io.gravitee.apim.core.application_member.use_case;

import static org.assertj.core.api.Assertions.assertThat;

import inmemory.RoleQueryServiceInMemory;
import io.gravitee.apim.core.membership.model.Role;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetApplicationRolesUseCaseTest {

    private static final String ORGANIZATION_ID = "organization-id";

    private final RoleQueryServiceInMemory roleQueryService = new RoleQueryServiceInMemory();
    private GetApplicationRolesUseCase getApplicationRolesUseCase;

    @BeforeEach
    void setUp() {
        getApplicationRolesUseCase = new GetApplicationRolesUseCase(roleQueryService);
        roleQueryService.reset();
    }

    @Test
    void should_return_application_roles_sorted_by_name_for_organization() {
        roleQueryService.initWith(
            List.of(
                aRole("role-1", "USER", Role.Scope.APPLICATION, ORGANIZATION_ID),
                aRole("role-2", "ADMIN", Role.Scope.APPLICATION, ORGANIZATION_ID),
                aRole("role-3", "PRIMARY_OWNER", Role.Scope.APPLICATION, "another-organization"),
                aRole("role-4", "ADMIN", Role.Scope.API, ORGANIZATION_ID)
            )
        );

        var result = getApplicationRolesUseCase.execute(new GetApplicationRolesUseCase.Input(ORGANIZATION_ID));

        assertThat(result.roles()).map(Role::getName).containsExactly("ADMIN", "USER");
    }

    @Test
    void should_return_empty_list_when_no_application_role_exists() {
        roleQueryService.initWith(List.of(aRole("role-1", "ADMIN", Role.Scope.API, ORGANIZATION_ID)));

        var result = getApplicationRolesUseCase.execute(new GetApplicationRolesUseCase.Input(ORGANIZATION_ID));

        assertThat(result.roles()).isEmpty();
    }

    private static Role aRole(String id, String name, Role.Scope scope, String organizationId) {
        return Role.builder()
            .id(id)
            .name(name)
            .scope(scope)
            .referenceType(Role.ReferenceType.ORGANIZATION)
            .referenceId(organizationId)
            .build();
    }
}
