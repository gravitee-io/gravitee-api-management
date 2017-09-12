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
package io.gravitee.repository;

import io.gravitee.repository.config.AbstractRepositoryTest;
import io.gravitee.repository.management.model.Role;
import io.gravitee.repository.management.model.RoleScope;
import org.junit.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.*;

public class RoleRepositoryTest extends AbstractRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/role-tests/";
    }

    @Test
    public void shouldFindAll() throws Exception {
        final Set<Role> roles = roleRepository.findAll();

        assertNotNull(roles);
        assertEquals(4, roles.size());
    }

    @Test
    public void shouldCreate() throws Exception {
        final Role role = new Role();
        role.setName("to create");
        role.setScope(RoleScope.API);
        role.setPermissions(new int[]{3});

        boolean presentBefore = roleRepository.findById(role.getScope(), role.getName()).isPresent();
        Role newRole = roleRepository.create(role);
        boolean presentAfter = roleRepository.findById(role.getScope(), role.getName()).isPresent();

        assertFalse("must not exists before creation", presentBefore);
        assertTrue("must exists after creation", presentAfter);
        assertEquals("Invalid name", role.getName(), newRole.getName());
        assertEquals("Invalid scope", role.getScope(), newRole.getScope());
        assertEquals("Invalid permissions", role.getPermissions()[0], newRole.getPermissions()[0]);
    }

    @Test
    public void shouldUpdate() throws Exception {
        final Role role = new Role();
        role.setScope(RoleScope.MANAGEMENT);
        role.setName("to update");
        role.setDescription("new description");
        role.setPermissions(new int[]{4, 5});
        role.setDefaultRole(true);

        Role update = roleRepository.update(role);

        assertNotNull(update);
        assertEquals("invalid name", role.getName(), update.getName());
        assertEquals("invalid scope", role.getScope(), update.getScope());
        assertEquals("invalid description", role.getDescription(), update.getDescription());
        assertEquals("invalid default role", role.isDefaultRole(), update.isDefaultRole());
        assertEquals("invalid system attribute", role.isSystem(), update.isSystem());
        List<Integer> updatePermissions = IntStream.of(update.getPermissions()).boxed().collect(Collectors.toList());
        assertTrue("invalid permission", updatePermissions.contains(4));
        assertTrue("invalid permission", updatePermissions.contains(5));
    }

    @Test
    public void shouldDelete() throws Exception {
        boolean presentBefore = roleRepository.findById(RoleScope.MANAGEMENT, "to delete").isPresent();
        roleRepository.delete(RoleScope.MANAGEMENT, "to delete");
        boolean presentAfter = roleRepository.findById(RoleScope.MANAGEMENT, "to delete").isPresent();

        assertTrue("must exists before creation", presentBefore);
        assertFalse("must not exists after creation", presentAfter);
    }

    @Test
    public void shouldFindByScope() throws Exception {
        Set<Role> roles = roleRepository.findByScope(RoleScope.PORTAL);
        assertNotNull(roles);
        assertFalse("No roles found", roles.isEmpty());
        assertEquals("invalid roles count", 2, roles.size());
        List<String> names = roles.stream().map(Role::getName).collect(Collectors.toList());
        assertTrue("not contains scope1", names.contains("find by scope 1"));
        assertTrue("not contains scope2", names.contains("find by scope 2"));
    }

    @Test
    public void shouldFindById() throws Exception {
        Optional<Role> role = roleRepository.findById(RoleScope.PORTAL, "find by scope 1");
        assertTrue("role not found", role.isPresent());
        assertEquals("invalid name", "find by scope 1", role.get().getName());
        assertEquals("invalid description", "role description", role.get().getDescription());
        assertEquals("invalid scope", RoleScope.PORTAL, role.get().getScope());
        assertTrue("invalid defaultRole", role.get().isDefaultRole());
        assertTrue("invalid system attribute", role.get().isSystem());
        assertEquals("invalid permissions", 1, role.get().getPermissions().length);
        assertEquals("invalid permissions", 1, role.get().getPermissions()[0]);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotUpdateUnknownRole() throws Exception {
        Role unknownRole = new Role();
        unknownRole.setName("unknown");
        roleRepository.update(unknownRole);
        fail("An unknown role should not be updated");
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotUpdateNull() throws Exception {
        roleRepository.update(null);
        fail("A null role should not be updated");
    }
}
