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
package io.gravitee.repository.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.model.Role;
import io.gravitee.repository.management.model.RoleReferenceType;
import io.gravitee.repository.management.model.RoleScope;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

public class RoleRepositoryTest extends AbstractManagementRepositoryTest {

    private static final RoleReferenceType REFERENCE_TYPE = RoleReferenceType.ORGANIZATION;
    private static final String REFERENCE_ID = "DEFAULT";

    @Override
    protected String getTestCasesPath() {
        return "/data/role-tests/";
    }

    @Test
    public void shouldFindAll() throws Exception {
        final Set<Role> roles = roleRepository.findAll();

        assertNotNull(roles);
        assertEquals(6, roles.size());
    }

    @Test
    public void shouldFindAllByReferenceIdAndReferenceType() throws Exception {
        final Set<Role> roles = roleRepository.findAllByReferenceIdAndReferenceType(REFERENCE_ID, REFERENCE_TYPE);

        assertNotNull(roles);
        assertEquals(3, roles.size());
    }

    @Test
    public void shouldFindAllByIdIn() throws Exception {
        final Set<Role> roles = roleRepository.findAllByIdIn(Set.of("an_api_organisation_role", "API_find_by_scope_2"));

        assertNotNull(roles);
        assertEquals(2, roles.size());
    }

    @Test
    public void shouldReturnEmptyForFindAllByIdIn() throws Exception {
        final Set<Role> roles = roleRepository.findAllByIdIn(Set.of("id_does_not_exist"));

        assertNotNull(roles);
        assertEquals(0, roles.size());
    }

    @Test
    public void shouldCreate() throws Exception {
        final Role role = new Role();
        role.setId("API_to_create");
        role.setName("to create");
        role.setScope(RoleScope.API);
        role.setReferenceId(REFERENCE_ID);
        role.setReferenceType(REFERENCE_TYPE);
        role.setPermissions(new int[] { 3 });

        boolean presentBefore = roleRepository.findById("API_to_create").isPresent();
        Role newRole = roleRepository.create(role);
        boolean presentAfter = roleRepository.findById("API_to_create").isPresent();

        assertFalse(presentBefore, "must not exists before creation");
        assertTrue(presentAfter, "must exists after creation");
        assertEquals(role.getName(), newRole.getName(), "Invalid name");
        assertEquals(role.getReferenceId(), newRole.getReferenceId(), "Invalid reference id");
        assertEquals(role.getReferenceType(), newRole.getReferenceType(), "Invalid reference type");
        assertEquals(role.getScope(), newRole.getScope(), "Invalid scope");
        assertEquals(role.getPermissions()[0], newRole.getPermissions()[0], "Invalid permissions");
    }

    @Test
    public void shouldUpdate() throws Exception {
        final Role role = new Role();
        role.setId("ENVIRONMENT_to_update");
        role.setScope(RoleScope.ENVIRONMENT);
        role.setName("to update");
        role.setReferenceId(REFERENCE_ID);
        role.setReferenceType(REFERENCE_TYPE);
        role.setDescription("new description");
        role.setPermissions(new int[] { 4, 5 });
        role.setDefaultRole(true);

        Role update = roleRepository.update(role);

        assertNotNull(update);
        assertEquals(role.getName(), update.getName(), "invalid name");
        assertEquals(role.getScope(), update.getScope(), "invalid scope");
        assertEquals(role.getReferenceId(), update.getReferenceId(), "invalid reference id");
        assertEquals(role.getReferenceType(), update.getReferenceType(), "invalid reference type");
        assertEquals(role.getDescription(), update.getDescription(), "invalid description");
        assertEquals(role.isDefaultRole(), update.isDefaultRole(), "invalid default role");
        assertEquals(role.isSystem(), update.isSystem(), "invalid system attribute");
        List<Integer> updatePermissions = IntStream.of(update.getPermissions()).boxed().collect(Collectors.toList());
        assertTrue(updatePermissions.contains(4), "invalid permission");
        assertTrue(updatePermissions.contains(5), "invalid permission");
    }

    @Test
    public void shouldDelete() throws Exception {
        boolean presentBefore = roleRepository.findById("ORGANIZATION_to_delete").isPresent();
        roleRepository.delete("ORGANIZATION_to_delete");
        boolean presentAfter = roleRepository.findById("ORGANIZATION_to_delete").isPresent();

        assertTrue(presentBefore, "must exists before creation");
        assertFalse(presentAfter, "must not exists after creation");
    }

    @Test
    public void shouldFindByScopeAndName() throws Exception {
        Optional<Role> role = roleRepository.findByScopeAndNameAndReferenceIdAndReferenceType(
            RoleScope.API,
            "find by scope 1",
            REFERENCE_ID,
            REFERENCE_TYPE
        );
        assertNotNull(role);
        assertTrue(role.isPresent(), "No roles found");
        assertTrue("find by scope 1".equals(role.get().getName()), "not contains scope1");
    }

    @Test
    public void shouldFindByScopeAndRef() throws Exception {
        Set<Role> roles = roleRepository.findByScopeAndReferenceIdAndReferenceType(RoleScope.API, REFERENCE_ID, REFERENCE_TYPE);
        assertNotNull(roles);
        assertFalse(roles.isEmpty(), "No roles found");
        assertEquals(1, roles.size(), "invalid roles count");
        List<String> names = roles.stream().map(Role::getName).collect(Collectors.toList());
        assertTrue(names.contains("find by scope 1"), "not contains scope1");
    }

    @Test
    public void shouldFindById() throws Exception {
        Optional<Role> role = roleRepository.findById("an_api_organisation_role");
        assertTrue(role.isPresent(), "role not found");
        assertEquals("find by scope 1", role.get().getName(), "invalid name");
        assertEquals("role description", role.get().getDescription(), "invalid description");
        assertEquals(RoleScope.API, role.get().getScope(), "invalid scope");
        assertTrue(role.get().isDefaultRole(), "invalid defaultRole");
        assertTrue(role.get().isSystem(), "invalid system attribute");
        assertEquals(1, role.get().getPermissions().length, "invalid permissions");
        assertEquals(1, role.get().getPermissions()[0], "invalid permissions");
    }

    @Test
    public void shouldNotUpdateUnknownRole() throws Exception {
        assertThrows(IllegalStateException.class, () -> {
            Role unknownRole = new Role();
            unknownRole.setId("unknown");
            unknownRole.setName("unknown");
            unknownRole.setReferenceId("unknown");
            unknownRole.setReferenceType(REFERENCE_TYPE);
            roleRepository.update(unknownRole);
            fail("An unknown role should not be updated");
        });
    }

    @Test
    public void shouldNotUpdateNull() throws Exception {
        assertThrows(IllegalStateException.class, () -> {
            roleRepository.update(null);
            fail("A null role should not be updated");
        });
    }

    @Test
    public void shouldFindByIdAndOrganisationId() throws TechnicalException {
        var id = "an_api_organisation_role";
        Optional<Role> result = roleRepository.findByIdAndReferenceIdAndReferenceType(id, REFERENCE_ID, REFERENCE_TYPE);
        SoftAssertions.assertSoftly(soft -> {
            // check that role is present
            soft.assertThat(result).isPresent();
            var role = result.get();
            soft.assertThat(role).isNotNull();
            soft.assertThat(role.getId()).isEqualTo(id);
            soft.assertThat(role.getReferenceId()).isEqualTo(REFERENCE_ID);
            soft.assertThat(role.getReferenceType()).isEqualTo(REFERENCE_TYPE);
        });
    }

    @Test
    public void shouldNotFindByIdAndOrganisationIdWhenOrganisationIdIsWrong() throws TechnicalException {
        assertThat(roleRepository.findByIdAndReferenceIdAndReferenceType("an_api_organisation_role", "dummy", REFERENCE_TYPE)).isEmpty();
    }

    @Test
    public void shouldNotFindByIdAndOrganisationIdWhenRoleIdIsWrong() throws TechnicalException {
        assertThat(roleRepository.findByIdAndReferenceIdAndReferenceType("dummy", REFERENCE_ID, REFERENCE_TYPE)).isEmpty();
    }

    @Test
    public void shouldNotFindByIdAndOrganisationIdWhenReferenceTypeIsWrong() throws TechnicalException {
        assertThat(
            roleRepository.findByIdAndReferenceIdAndReferenceType("API_find_by_id_and_org_id", REFERENCE_ID, RoleReferenceType.ENVIRONMENT)
        ).isEmpty();
    }

    @Test
    public void should_delete_by_reference_id_and_reference_type() throws Exception {
        assertThat(roleRepository.findAllByReferenceIdAndReferenceType("ToBeDeleted", RoleReferenceType.ORGANIZATION)).isNotEmpty();

        List<String> deleted = roleRepository.deleteByReferenceIdAndReferenceType("ToBeDeleted", RoleReferenceType.ORGANIZATION);

        assertEquals(2, deleted.size());
        assertThat(roleRepository.findAllByReferenceIdAndReferenceType("ToBeDeleted", RoleReferenceType.ORGANIZATION)).isEmpty();
    }
}
