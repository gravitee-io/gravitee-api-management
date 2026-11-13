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
package io.gravitee.rest.api.service.impl.upgrade.upgrader;

import static io.gravitee.rest.api.model.permissions.RolePermissionAction.CREATE;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.DELETE;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.READ;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.UPDATE;
import static io.gravitee.rest.api.service.common.DefaultRoleEntityDefinition.ROLE_ENVIRONMENT_API_PUBLISHER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.OrganizationRepository;
import io.gravitee.repository.management.model.Organization;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.UpdateRoleEntity;
import io.gravitee.rest.api.model.permissions.EnvironmentPermission;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CredentialPermissionUpgraderTest {

    private static final String ORGANIZATION_ID = "DEFAULT";
    private static final String CREDENTIAL = EnvironmentPermission.CREDENTIAL.getName();

    @Mock
    RoleService roleService;

    @Mock
    OrganizationRepository organizationRepository;

    @Captor
    ArgumentCaptor<UpdateRoleEntity> roleCaptor;

    CredentialPermissionUpgrader upgrader;

    @BeforeEach
    void setUp() {
        upgrader = new CredentialPermissionUpgrader(roleService, organizationRepository);
    }

    @Test
    void should_refresh_the_system_roles_so_the_admin_gets_the_permission() throws Exception {
        var organization = Organization.builder().id(ORGANIZATION_ID).build();
        when(organizationRepository.findAll()).thenReturn(Set.of(organization));

        assertThat(upgrader.upgrade()).isTrue();

        verify(roleService).createOrUpdateSystemRoles(new ExecutionContext(organization), ORGANIZATION_ID);
    }

    @Test
    void should_grant_the_full_credential_permission_to_the_api_publisher() throws Exception {
        givenThePublisher(aPublisher());

        upgrader.upgrade();

        verify(roleService).update(any(), roleCaptor.capture());
        assertThat(new String(roleCaptor.getValue().getPermissions().get(CREDENTIAL))).isEqualTo(
            new String(new char[] { CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId() })
        );
        assertThat(roleCaptor.getValue().getPermissions()).containsKey(EnvironmentPermission.API.getName());
    }

    @Test
    void should_leave_a_publisher_that_already_holds_the_permission_alone() throws Exception {
        var publisher = aPublisher();
        publisher.getPermissions().put(CREDENTIAL, new char[] { READ.getId() });
        givenThePublisher(publisher);

        upgrader.upgrade();

        verify(roleService, never()).update(any(), any());
    }

    @Test
    void should_not_touch_the_permissions_map_of_the_role_it_reads() throws Exception {
        var publisher = aPublisher();
        var readOnly = Map.copyOf(publisher.getPermissions());
        publisher.setPermissions(readOnly);
        givenThePublisher(publisher);

        upgrader.upgrade();

        verify(roleService).update(any(), roleCaptor.capture());
        assertThat(publisher.getPermissions()).isEqualTo(readOnly).doesNotContainKey(CREDENTIAL);
        assertThat(roleCaptor.getValue().getPermissions()).containsKey(CREDENTIAL);
    }

    @Test
    void should_skip_an_organization_without_the_api_publisher_role() throws Exception {
        when(organizationRepository.findAll()).thenReturn(Set.of(Organization.builder().id(ORGANIZATION_ID).build()));
        when(roleService.findByScopeAndName(RoleScope.ENVIRONMENT, ROLE_ENVIRONMENT_API_PUBLISHER.getName(), ORGANIZATION_ID)).thenReturn(
            Optional.empty()
        );

        assertThat(upgrader.upgrade()).isTrue();

        verify(roleService, never()).update(any(), any());
    }

    @Test
    void should_fail_when_the_organizations_cannot_be_read() throws Exception {
        when(organizationRepository.findAll()).thenThrow(new TechnicalException("db error"));

        assertThrows(UpgraderException.class, () -> upgrader.upgrade());
        verify(roleService, never()).createOrUpdateSystemRoles(any(), any());
    }

    @Test
    void should_run_after_the_secret_permission_upgrader() {
        assertThat(upgrader.getOrder()).isGreaterThan(UpgraderOrder.SECRET_PERMISSION_UPGRADER);
    }

    private void givenThePublisher(RoleEntity publisher) throws TechnicalException {
        when(organizationRepository.findAll()).thenReturn(Set.of(Organization.builder().id(ORGANIZATION_ID).build()));
        when(roleService.findByScopeAndName(eq(RoleScope.ENVIRONMENT), eq(publisher.getName()), eq(ORGANIZATION_ID))).thenReturn(
            Optional.of(publisher)
        );
    }

    private static RoleEntity aPublisher() {
        Map<String, char[]> existing = new HashMap<>();
        existing.put(EnvironmentPermission.API.getName(), new char[] { READ.getId() });
        var name = ROLE_ENVIRONMENT_API_PUBLISHER.getName();
        return RoleEntity.builder().id(name).name(name).scope(RoleScope.ENVIRONMENT).permissions(existing).build();
    }
}
