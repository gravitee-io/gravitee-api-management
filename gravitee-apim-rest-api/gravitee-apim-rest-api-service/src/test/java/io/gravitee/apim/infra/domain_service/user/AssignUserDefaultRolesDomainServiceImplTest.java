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
package io.gravitee.apim.infra.domain_service.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.DefaultRoleNotFoundException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class AssignUserDefaultRolesDomainServiceImplTest {

    private static final ExecutionContext EXECUTION_CONTEXT = new ExecutionContext("org-id", "env-id");

    @Mock
    private RoleService roleService;

    @Mock
    private MembershipService membershipService;

    private AssignUserDefaultRolesDomainServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AssignUserDefaultRolesDomainServiceImpl(roleService, membershipService);
    }

    @Test
    void should_assign_default_organization_and_environment_roles_to_the_user() {
        var orgRole = RoleEntity.builder().scope(RoleScope.ORGANIZATION).name("ORG_USER").build();
        var envRole = RoleEntity.builder().scope(RoleScope.ENVIRONMENT).name("ENV_USER").build();
        when(roleService.findDefaultRoleByScopes("org-id", RoleScope.ORGANIZATION, RoleScope.ENVIRONMENT)).thenReturn(
            List.of(orgRole, envRole)
        );

        service.assignDefaultRoles(EXECUTION_CONTEXT, "user-1");

        var referenceCaptor = ArgumentCaptor.forClass(MembershipService.MembershipReference.class);
        verify(membershipService, times(2)).addRoleToMemberOnReference(eq(EXECUTION_CONTEXT), referenceCaptor.capture(), any(), any());
        assertThat(referenceCaptor.getAllValues())
            .extracting(MembershipService.MembershipReference::getType)
            .containsExactlyInAnyOrder(MembershipReferenceType.ORGANIZATION, MembershipReferenceType.ENVIRONMENT);
    }

    @Test
    void should_throw_when_no_default_role_is_found() {
        when(roleService.findDefaultRoleByScopes("org-id", RoleScope.ORGANIZATION, RoleScope.ENVIRONMENT)).thenReturn(List.of());

        assertThatThrownBy(() -> service.assignDefaultRoles(EXECUTION_CONTEXT, "user-1")).isInstanceOf(DefaultRoleNotFoundException.class);
    }
}
