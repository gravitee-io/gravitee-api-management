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
package io.gravitee.rest.api.service.impl;

import static io.gravitee.rest.api.model.permissions.EnvironmentPermission.DOCUMENTATION;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.RoleRepository;
import io.gravitee.repository.management.model.Role;
import io.gravitee.repository.management.model.RoleScope;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.impl.RoleServiceImpl;
import java.util.Arrays;
import java.util.Optional;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class RoleService_FindByIdTest {

    @InjectMocks
    private RoleServiceImpl roleService = new RoleServiceImpl();

    @Mock
    private RoleRepository mockRoleRepository;

    @BeforeEach
    public void init() {
        GraviteeContext.getCurrentRoles().clear();
    }

    @Test
    public void shouldFindById_C() throws TechnicalException {
        test_int_to_CRUD(3008, RolePermissionAction.CREATE);
    }

    @Test
    public void shouldFindById_R() throws TechnicalException {
        test_int_to_CRUD(3004, RolePermissionAction.READ);
    }

    @Test
    public void shouldFindById_U() throws TechnicalException {
        test_int_to_CRUD(3002, RolePermissionAction.UPDATE);
    }

    @Test
    public void shouldFindById_D() throws TechnicalException {
        test_int_to_CRUD(3001, RolePermissionAction.DELETE);
    }

    @Test
    public void shouldFindById_CRUD() throws TechnicalException {
        test_int_to_CRUD(
            3015,
            RolePermissionAction.CREATE,
            RolePermissionAction.READ,
            RolePermissionAction.UPDATE,
            RolePermissionAction.DELETE
        );
    }

    private void test_int_to_CRUD(int perm, RolePermissionAction... action) throws TechnicalException {
        Role roleMock = mock(Role.class);
        when(roleMock.getScope()).thenReturn(RoleScope.ENVIRONMENT);
        when(roleMock.getId()).thenReturn("id");
        when(roleMock.getName()).thenReturn("name");
        when(roleMock.getPermissions()).thenReturn(new int[] { perm });
        when(mockRoleRepository.findById("id")).thenReturn(Optional.of(roleMock));

        RoleEntity entity = roleService.findById("id");

        assertNotNull(entity, "no entity found");
        assertEquals("id", entity.getId(), "invalid id");
        assertEquals("name", entity.getName(), "invalid name");
        assertEquals(io.gravitee.rest.api.model.permissions.RoleScope.ENVIRONMENT, entity.getScope(), "invalid scope");
        assertFalse(entity.getPermissions().isEmpty(), "no permissions found");
        assertTrue(entity.getPermissions().containsKey(DOCUMENTATION.getName()), "invalid Permission name");
        char[] perms = entity.getPermissions().get(DOCUMENTATION.getName());
        assertEquals(action.length, perms.length, "not enough permissions");
        for (RolePermissionAction rolePermissionAction : action) {
            assertTrue(Arrays.asList(ArrayUtils.toObject(perms)).contains(rolePermissionAction.getId()), "not the good permission");
        }
    }
}
