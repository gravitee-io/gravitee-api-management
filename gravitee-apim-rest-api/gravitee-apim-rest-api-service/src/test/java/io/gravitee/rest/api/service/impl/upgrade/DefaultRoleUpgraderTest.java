/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.service.impl.upgrade;

import static io.gravitee.rest.api.model.permissions.RoleScope.API;
import static io.gravitee.rest.api.service.common.DefaultRoleEntityDefinition.ROLE_API_REVIEWER;
import static org.mockito.Mockito.*;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.OrganizationRepository;
import io.gravitee.repository.management.model.Organization;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultRoleUpgraderTest {

    @InjectMocks
    DefaultRolesUpgrader upgrader;

    @Mock
    RoleService roleService;

    @Mock
    OrganizationRepository organizationRepository;

    @Test
    public void shouldInitializeDefaultRoles() throws TechnicalException {
        final Organization organization = mock(Organization.class);
        when(organization.getId()).thenReturn(GraviteeContext.getDefaultOrganization());
        when(organizationRepository.findAll()).thenReturn(Set.of(organization));

        final ExecutionContext expectedExecutionContext = new ExecutionContext(organization);

        when(roleService.findAllByOrganization(expectedExecutionContext.getOrganizationId())).thenReturn(List.of());
        upgrader.processOneShotUpgrade();
        verify(roleService, times(1)).findAllByOrganization(expectedExecutionContext.getOrganizationId());
        verify(roleService, times(1)).initialize(expectedExecutionContext, expectedExecutionContext.getOrganizationId());
        verify(roleService, times(1)).createOrUpdateSystemRoles(expectedExecutionContext, organization.getId());
    }

    @Test
    public void shouldCreateApiReviewerRole() throws TechnicalException {
        final Organization organization = mock(Organization.class);
        when(organization.getId()).thenReturn(GraviteeContext.getDefaultOrganization());
        when(organizationRepository.findAll()).thenReturn(Set.of(organization));

        final ExecutionContext expectedExecutionContext = new ExecutionContext(organization);

        when(roleService.findAllByOrganization(GraviteeContext.getDefaultOrganization())).thenReturn(List.of(new RoleEntity()));
        when(roleService.findByScopeAndName(API, ROLE_API_REVIEWER.getName(), GraviteeContext.getCurrentOrganization()))
            .thenReturn(Optional.empty());

        upgrader.processOneShotUpgrade();

        verify(roleService, times(1)).findAllByOrganization(expectedExecutionContext.getOrganizationId());
        verify(roleService, never()).initialize(expectedExecutionContext, expectedExecutionContext.getOrganizationId());
        verify(roleService).findByScopeAndName(API, ROLE_API_REVIEWER.getName(), expectedExecutionContext.getOrganizationId());
        verify(roleService).create(expectedExecutionContext, ROLE_API_REVIEWER);
        verify(roleService, times(1)).createOrUpdateSystemRoles(expectedExecutionContext, organization.getId());
    }
}
