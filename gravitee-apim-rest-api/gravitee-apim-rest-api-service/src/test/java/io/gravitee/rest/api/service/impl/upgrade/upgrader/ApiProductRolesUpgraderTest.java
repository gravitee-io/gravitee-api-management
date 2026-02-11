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

import static io.gravitee.rest.api.model.permissions.RoleScope.API_PRODUCT;
import static io.gravitee.rest.api.model.permissions.SystemRole.PRIMARY_OWNER;
import static io.gravitee.rest.api.service.common.DefaultRoleEntityDefinition.ROLE_API_PRODUCT_OWNER;
import static io.gravitee.rest.api.service.common.DefaultRoleEntityDefinition.ROLE_API_PRODUCT_USER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.OrganizationRepository;
import io.gravitee.repository.management.model.Organization;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.permissions.Permission;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.Optional;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ApiProductRolesUpgraderTest {

    @InjectMocks
    ApiProductRolesUpgrader upgrader;

    @Mock
    RoleService roleService;

    @Mock
    OrganizationRepository organizationRepository;

    @Test(expected = UpgraderException.class)
    public void upgrade_should_fail_because_of_exception() throws UpgraderException, TechnicalException {
        when(organizationRepository.findAll()).thenThrow(new RuntimeException());
        upgrader.upgrade();
    }

    @Test
    public void shouldCreateApiProductRolesWhenNotPresent() throws UpgraderException, TechnicalException {
        String organizationId = GraviteeContext.getDefaultOrganization();
        Organization organization = mock(Organization.class);
        when(organization.getId()).thenReturn(organizationId);
        when(organizationRepository.findAll()).thenReturn(Set.of(organization));

        ExecutionContext expectedExecutionContext = new ExecutionContext(organization);
        when(roleService.findByScopeAndName(eq(API_PRODUCT), eq(ROLE_API_PRODUCT_USER.getName()), eq(organizationId))).thenReturn(
            Optional.empty()
        );
        when(roleService.findByScopeAndName(eq(API_PRODUCT), eq(ROLE_API_PRODUCT_OWNER.getName()), eq(organizationId))).thenReturn(
            Optional.empty()
        );

        upgrader.upgrade();

        verify(roleService).create(expectedExecutionContext, ROLE_API_PRODUCT_USER);
        verify(roleService).create(expectedExecutionContext, ROLE_API_PRODUCT_OWNER);
        verify(roleService).createOrUpdateSystemRole(
            eq(expectedExecutionContext),
            eq(PRIMARY_OWNER),
            eq(API_PRODUCT),
            any(Permission[].class),
            eq(organizationId)
        );
    }

    @Test
    public void shouldNotCreateUserAndOwnerWhenAlreadyPresent() throws UpgraderException, TechnicalException {
        String organizationId = GraviteeContext.getDefaultOrganization();
        Organization organization = mock(Organization.class);
        when(organization.getId()).thenReturn(organizationId);
        when(organizationRepository.findAll()).thenReturn(Set.of(organization));

        when(roleService.findByScopeAndName(eq(API_PRODUCT), eq(ROLE_API_PRODUCT_USER.getName()), eq(organizationId))).thenReturn(
            Optional.of(new RoleEntity())
        );
        when(roleService.findByScopeAndName(eq(API_PRODUCT), eq(ROLE_API_PRODUCT_OWNER.getName()), eq(organizationId))).thenReturn(
            Optional.of(new RoleEntity())
        );

        upgrader.upgrade();

        verify(roleService, never()).create(any(), eq(ROLE_API_PRODUCT_USER));
        verify(roleService, never()).create(any(), eq(ROLE_API_PRODUCT_OWNER));
        verify(roleService).createOrUpdateSystemRole(
            any(),
            eq(PRIMARY_OWNER),
            eq(API_PRODUCT),
            any(Permission[].class),
            eq(organizationId)
        );
    }

    @Test
    public void test_order() {
        Assert.assertEquals(UpgraderOrder.API_PRODUCT_ROLES_UPGRADER, upgrader.getOrder());
    }
}
