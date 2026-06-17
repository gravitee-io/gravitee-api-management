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

import static io.gravitee.rest.api.model.permissions.RoleScope.ENVIRONMENT;
import static io.gravitee.rest.api.service.common.DefaultRoleEntityDefinition.ROLE_ENVIRONMENT_EDGE_MANAGER;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.OrganizationRepository;
import io.gravitee.repository.management.model.Organization;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class EdgeRolesUpgraderTest {

    @InjectMocks
    EdgeRolesUpgrader upgrader;

    @Mock
    RoleService roleService;

    @Mock
    OrganizationRepository organizationRepository;

    @Test
    public void upgrade_should_fail_because_of_exception() throws TechnicalException {
        assertThrows(UpgraderException.class, () -> {
            when(organizationRepository.findAll()).thenThrow(new RuntimeException());
            upgrader.upgrade();
        });
    }

    @Test
    public void shouldCreateEdgeManagerRoleWhenNotPresent() throws UpgraderException, TechnicalException {
        String organizationId = GraviteeContext.getDefaultOrganization();
        Organization organization = mock(Organization.class);
        when(organization.getId()).thenReturn(organizationId);
        when(organizationRepository.findAll()).thenReturn(Set.of(organization));

        ExecutionContext expectedExecutionContext = new ExecutionContext(organization);
        when(roleService.findByScopeAndName(eq(ENVIRONMENT), eq(ROLE_ENVIRONMENT_EDGE_MANAGER.getName()), eq(organizationId))).thenReturn(
            Optional.empty()
        );

        upgrader.upgrade();

        verify(roleService).create(expectedExecutionContext, ROLE_ENVIRONMENT_EDGE_MANAGER);
        verify(roleService).createOrUpdateSystemRoles(expectedExecutionContext, organizationId);
    }

    @Test
    public void shouldNotCreateEdgeManagerWhenAlreadyPresent() throws UpgraderException, TechnicalException {
        String organizationId = GraviteeContext.getDefaultOrganization();
        Organization organization = mock(Organization.class);
        when(organization.getId()).thenReturn(organizationId);
        when(organizationRepository.findAll()).thenReturn(Set.of(organization));

        when(roleService.findByScopeAndName(eq(ENVIRONMENT), eq(ROLE_ENVIRONMENT_EDGE_MANAGER.getName()), eq(organizationId))).thenReturn(
            Optional.of(new RoleEntity())
        );

        upgrader.upgrade();

        verify(roleService, never()).create(any(), eq(ROLE_ENVIRONMENT_EDGE_MANAGER));
        verify(roleService).createOrUpdateSystemRoles(any(), eq(organizationId));
    }

    @Test
    public void test_order() {
        Assertions.assertEquals(UpgraderOrder.EDGE_ROLES_UPGRADER, upgrader.getOrder());
    }
}
