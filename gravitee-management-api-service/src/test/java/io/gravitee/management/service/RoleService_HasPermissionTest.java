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
package io.gravitee.management.service;

import io.gravitee.management.model.permissions.ApiPermission;
import io.gravitee.management.model.permissions.RolePermissionAction;
import io.gravitee.management.service.impl.RoleServiceImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class RoleService_HasPermissionTest {

    @InjectMocks
    private RoleServiceImpl roleService = new RoleServiceImpl();

    @Test
    public void shouldHasPermission() {
        final Map<String, char[]> perms = new HashMap<>();
        perms.put(ApiPermission.DOCUMENTATION.name(),
                new char[]{
                        RolePermissionAction.CREATE.getId(),
                        RolePermissionAction.READ.getId(),
                        RolePermissionAction.UPDATE.getId(),
                        RolePermissionAction.DELETE.getId()
                });

        boolean hasPermission = roleService.hasPermission(
                perms,
                ApiPermission.DOCUMENTATION,
                new RolePermissionAction[]{RolePermissionAction.UPDATE});

        assertTrue(hasPermission);
    }

    @Test
    public void shouldNotHasPermission() {
        final Map<String, char[]> perms = new HashMap<>();
        perms.put(ApiPermission.DOCUMENTATION.name(),
                new char[]{
                        RolePermissionAction.CREATE.getId(),
                        RolePermissionAction.READ.getId(),
                        RolePermissionAction.DELETE.getId()
                });

        boolean hasPermission = roleService.hasPermission(
                perms,
                ApiPermission.DOCUMENTATION,
                new RolePermissionAction[]{RolePermissionAction.UPDATE});

        assertFalse(hasPermission);
    }

    @Test
    public void shouldNotHasPermission2() {
        final Map<String, char[]> perms = new HashMap<>();
        perms.put(ApiPermission.PLAN.name(),
                new char[]{
                        RolePermissionAction.CREATE.getId(),
                        RolePermissionAction.READ.getId(),
                        RolePermissionAction.UPDATE.getId(),
                        RolePermissionAction.DELETE.getId()
                });

        boolean hasPermission = roleService.hasPermission(
                perms,
                ApiPermission.DOCUMENTATION,
                new RolePermissionAction[]{RolePermissionAction.UPDATE});

        assertFalse(hasPermission);
    }
}
