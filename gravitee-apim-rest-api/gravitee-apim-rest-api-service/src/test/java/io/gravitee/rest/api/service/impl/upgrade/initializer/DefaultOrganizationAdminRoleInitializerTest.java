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
package io.gravitee.rest.api.service.impl.upgrade.initializer;

import static org.mockito.Mockito.*;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.OrganizationRepository;
import io.gravitee.repository.management.model.Organization;
import io.gravitee.rest.api.model.permissions.EnvironmentPermission;
import io.gravitee.rest.api.model.permissions.IntegrationPermission;
import io.gravitee.rest.api.model.permissions.OrganizationPermission;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.model.permissions.SystemRole;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultOrganizationAdminRoleInitializerTest {

    @InjectMocks
    DefaultOrganizationAdminRoleInitializer initializer;

    @Mock
    RoleService roleService;

    @Mock
    OrganizationRepository organizationRepository;

    @Test
    public void shouldInitializeDefaultRoles() {
        final Organization organization = mock(Organization.class);
        when(organization.getId()).thenReturn(GraviteeContext.getDefaultOrganization());

        final ExecutionContext executionContext = new ExecutionContext(organization);

        initializer.initializeOrganization(executionContext);
        verify(roleService, times(1))
            .createOrUpdateSystemRole(
                executionContext,
                SystemRole.ADMIN,
                RoleScope.ORGANIZATION,
                OrganizationPermission.values(),
                organization.getId()
            );
        verify(roleService, times(1))
            .createOrUpdateSystemRole(
                executionContext,
                SystemRole.ADMIN,
                RoleScope.ENVIRONMENT,
                EnvironmentPermission.values(),
                executionContext.getOrganizationId()
            );
        verify(roleService, times(1))
            .createOrUpdateSystemRole(
                executionContext,
                SystemRole.PRIMARY_OWNER,
                RoleScope.INTEGRATION,
                IntegrationPermission.values(),
                executionContext.getOrganizationId()
            );
    }

    @Test
    public void testOrder() {
        Assert.assertEquals(InitializerOrder.DEFAULT_ORGANIZATION_ADMIN_ROLE_INITIALIZER, initializer.getOrder());
    }
}
