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

import static io.gravitee.rest.api.service.common.DefaultRoleEntityDefinition.ROLE_INTEGRATION_OWNER;
import static io.gravitee.rest.api.service.common.DefaultRoleEntityDefinition.ROLE_INTEGRATION_USER;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.OrganizationRepository;
import io.gravitee.repository.management.model.Organization;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class IntegrationRolesUpgraderTest {

    @InjectMocks
    IntegrationRolesUpgrader upgrader;

    @Mock
    RoleService roleService;

    @Mock
    OrganizationRepository organizationRepository;

    @Test
    public void upgrade_should_failed_because_of_exception() throws TechnicalException {
        when(organizationRepository.findAll()).thenThrow(new RuntimeException());

        assertFalse(upgrader.upgrade());

        verify(organizationRepository, times(1)).findAll();
        verifyNoMoreInteractions(organizationRepository);
    }

    @Test
    public void shouldInitializeIntegrationRoles() throws TechnicalException {
        final Organization organization = mock(Organization.class);
        when(organization.getId()).thenReturn(GraviteeContext.getDefaultOrganization());
        when(organizationRepository.findAll()).thenReturn(Set.of(organization));

        final ExecutionContext expectedExecutionContext = new ExecutionContext(organization);

        upgrader.upgrade();

        verify(roleService, times(1)).create(expectedExecutionContext, ROLE_INTEGRATION_USER);
        verify(roleService, times(1)).create(expectedExecutionContext, ROLE_INTEGRATION_OWNER);
        verify(roleService, times(1)).createOrUpdateSystemRoles(expectedExecutionContext, organization.getId());
    }

    @Test
    public void test_order() {
        Assert.assertEquals(UpgraderOrder.INTEGRATION_ROLES_UPGRADER, upgrader.getOrder());
    }
}
