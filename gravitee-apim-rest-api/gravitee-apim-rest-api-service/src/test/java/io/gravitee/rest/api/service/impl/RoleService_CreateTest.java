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
package io.gravitee.rest.api.service.impl;

import static io.gravitee.rest.api.model.permissions.EnvironmentPermission.DOCUMENTATION;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.RoleRepository;
import io.gravitee.repository.management.model.Role;
import io.gravitee.repository.management.model.RoleScope;
import io.gravitee.rest.api.model.NewRoleEntity;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.exceptions.RoleReservedNameException;
import io.gravitee.rest.api.service.impl.RoleServiceImpl;
import java.util.Collections;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class RoleService_CreateTest {

    @InjectMocks
    private RoleServiceImpl roleService = new RoleServiceImpl();

    @Mock
    private RoleRepository mockRoleRepository;

    @Mock
    private AuditService auditService;

    @Test
    public void shouldCreate() throws TechnicalException {
        NewRoleEntity newRoleEntityMock = mock(NewRoleEntity.class);
        when(newRoleEntityMock.getName()).thenReturn("new mock role");
        when(newRoleEntityMock.getScope()).thenReturn(io.gravitee.rest.api.model.permissions.RoleScope.ENVIRONMENT);
        when(newRoleEntityMock.getPermissions())
            .thenReturn(Collections.singletonMap(DOCUMENTATION.getName(), new char[] { RolePermissionAction.CREATE.getId() }));
        Role roleMock = mock(Role.class);
        when(roleMock.getId()).thenReturn("new_mock_role");
        when(roleMock.getName()).thenReturn("new mock role");
        when(roleMock.getScope()).thenReturn(RoleScope.ENVIRONMENT);
        when(roleMock.getPermissions()).thenReturn(new int[] { 3008 });
        when(mockRoleRepository.create(any())).thenReturn(roleMock);

        RoleEntity entity = roleService.create(newRoleEntityMock);

        assertNotNull("no entoty created", entity);
        assertEquals("invalid id", "new_mock_role", entity.getId());
        assertEquals("invalid name", "new mock role", entity.getName());
        assertEquals("invalid scope", io.gravitee.rest.api.model.permissions.RoleScope.ENVIRONMENT, entity.getScope());
        assertFalse("no permissions found", entity.getPermissions().isEmpty());
        assertTrue("invalid Permission name", entity.getPermissions().containsKey(DOCUMENTATION.getName()));
        char[] perms = entity.getPermissions().get(DOCUMENTATION.getName());
        assertEquals("not enough permissions", 1, perms.length);
        assertEquals("not the good permission", RolePermissionAction.CREATE.getId(), perms[0]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotCreateBecauseOfInvalidPermissionAction() throws TechnicalException {
        NewRoleEntity newRoleEntityMock = mock(NewRoleEntity.class);
        when(newRoleEntityMock.getName()).thenReturn("new mock role");
        when(newRoleEntityMock.getScope()).thenReturn(io.gravitee.rest.api.model.permissions.RoleScope.ENVIRONMENT);
        when(newRoleEntityMock.getPermissions()).thenReturn(Collections.singletonMap(DOCUMENTATION.getName(), new char[] { 'X' }));

        roleService.create(newRoleEntityMock);

        fail("should fail earlier");
    }

    @Test(expected = RoleReservedNameException.class)
    public void shouldNotCreateBecauseOfReservedRoleName() throws TechnicalException {
        NewRoleEntity newRoleEntityMock = mock(NewRoleEntity.class);
        when(newRoleEntityMock.getName()).thenReturn("admin");

        roleService.create(newRoleEntityMock);

        fail("should fail earlier");
    }
}
