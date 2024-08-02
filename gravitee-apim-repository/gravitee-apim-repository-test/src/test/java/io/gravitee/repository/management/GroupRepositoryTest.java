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

import static io.gravitee.repository.utils.DateUtils.compareDate;
import static java.util.Arrays.asList;
import static org.junit.Assert.*;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.model.Group;
import java.util.*;
import java.util.stream.Collectors;
import org.junit.Test;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class GroupRepositoryTest extends AbstractManagementRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/group-tests/";
    }

    @Test
    public void shouldCreateGroup() throws TechnicalException {
        Group group = new Group();
        group.setId("1");
        group.setName("my group");
        group.setEnvironmentId("DEFAULT");
        group.setLockApiRole(true);
        group.setLockApplicationRole(true);
        group.setSystemInvitation(true);
        group.setEmailInvitation(true);
        group.setMaxInvitation(10);
        group.setDisableMembershipNotifications(true);

        Group group1 = groupRepository.create(group);

        assertNotNull(group1);
        assertNotNull(group1.getId());
        assertEquals(group.getId(), group1.getId());
        assertEquals(group.getEnvironmentId(), group1.getEnvironmentId());
        assertEquals(group.getName(), group1.getName());
        assertEquals(group.isLockApiRole(), group1.isLockApiRole());
        assertEquals(group.isLockApplicationRole(), group1.isLockApplicationRole());
        assertEquals(group.isSystemInvitation(), group1.isSystemInvitation());
        assertEquals(group.isEmailInvitation(), group1.isEmailInvitation());
        assertEquals(group.getMaxInvitation(), group1.getMaxInvitation());
        assertEquals(group.isDisableMembershipNotifications(), group1.isDisableMembershipNotifications());
    }

    @Test
    public void shouldFindById() throws TechnicalException {
        Optional<Group> group = groupRepository.findById("group-application-1");

        assertNotNull(group);
        assertTrue(group.isPresent());
        assertEquals("group-application-1", group.get().getId());
        assertEquals("group-application-1 environment-id", group.get().getEnvironmentId());
        assertEquals("group-application-1 name", group.get().getName());
        assertTrue(group.get().isLockApiRole());
        assertTrue(group.get().isLockApplicationRole());
        assertTrue(group.get().isSystemInvitation());
        assertTrue(group.get().isEmailInvitation());
        assertTrue(group.get().isDisableMembershipNotifications());
        assertEquals(99, group.get().getMaxInvitation().intValue());
        assertEquals(2, group.get().getEventRules().size());
        assertEquals("api-primary-owner-id", group.get().getApiPrimaryOwner());
    }

    @Test
    public void shouldNotFindByUnknownId() throws TechnicalException {
        Optional<Group> group = groupRepository.findById("unknown");

        assertNotNull(group);
        assertFalse(group.isPresent());
    }

    @Test
    public void shouldUpdate() throws TechnicalException {
        Group group = new Group();
        group.setId("group-application-1");
        group.setEnvironmentId("new_DEFAULT");
        group.setName("Modified Name");
        group.setUpdatedAt(new Date(1000000000000L));
        group.setLockApiRole(true);
        group.setLockApplicationRole(true);
        group.setSystemInvitation(true);
        group.setEmailInvitation(true);
        group.setDisableMembershipNotifications(false);
        group.setMaxInvitation(99);
        group.setApiPrimaryOwner("new-po-user-id");

        Group update = groupRepository.update(group);

        assertEquals(group.getId(), update.getId());
        assertEquals(group.getEnvironmentId(), update.getEnvironmentId());
        assertEquals(group.getName(), update.getName());
        assertTrue(compareDate(new Date(1000000000000L), update.getUpdatedAt()));
        assertTrue(update.isLockApiRole());
        assertTrue(update.isLockApplicationRole());
        assertTrue(update.isSystemInvitation());
        assertTrue(update.isEmailInvitation());
        assertFalse(update.isDisableMembershipNotifications());
        assertEquals(99, update.getMaxInvitation().intValue());
        assertEquals(group.getApiPrimaryOwner(), update.getApiPrimaryOwner());
    }

    @Test
    public void shouldFindAll() throws TechnicalException {
        Set<Group> groups = groupRepository.findAll();

        assertNotNull(groups);
        assertFalse("not empty", groups.isEmpty());
        assertEquals(7, groups.size());
    }

    @Test
    public void shouldFindAllByEnvironment() throws TechnicalException {
        Set<Group> groups = groupRepository.findAllByEnvironment("DEFAULT");

        assertNotNull(groups);
        assertFalse("not empty", groups.isEmpty());
        assertEquals(1, groups.size());
    }

    @Test
    public void shouldFindAllByOrganization() throws TechnicalException {
        Set<Group> groups = groupRepository.findAllByOrganization("ORGANIZATION_1");

        assertNotNull(groups);
        assertFalse("not empty", groups.isEmpty());
        assertEquals(2, groups.size());
    }

    @Test
    public void shouldDelete() throws TechnicalException {
        groupRepository.delete("group-api-to-delete");
        Optional<Group> group = groupRepository.findById("group-api-to-delete");

        assertNotNull(group);
        assertFalse(group.isPresent());
    }

    @Test
    public void should_delete_by_environment_id() throws TechnicalException {
        Set<Group> beforeDeleted = groupRepository.findAllByEnvironment("ToBeDeleted");

        List<String> deleted = groupRepository.deleteByEnvironmentId("ToBeDeleted");

        Set<Group> afterDeleted = groupRepository.findAllByEnvironment("ToBeDeleted");
        assertEquals(2, beforeDeleted.size());
        assertEquals(2, deleted.size());
        assertEquals(0, afterDeleted.size());
    }

    @Test
    public void shouldFindByIds() throws TechnicalException {
        Set<Group> groups = groupRepository.findByIds(new HashSet<>(asList("group-application-1", "group-api-to-delete", "unknown")));

        assertNotNull(groups);
        assertFalse(groups.isEmpty());
        assertEquals(2, groups.size());
        assertTrue(
            groups.stream().map(Group::getId).collect(Collectors.toList()).containsAll(asList("group-application-1", "group-api-to-delete"))
        );
    }

    @Test
    public void shouldNotFindByEmptyIds() throws TechnicalException {
        Set<Group> groups = groupRepository.findByIds(Collections.emptySet());

        assertNotNull(groups);
        assertTrue(groups.isEmpty());
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotUpdateUnknownGroup() throws Exception {
        Group unknownGroup = new Group();
        unknownGroup.setId("unknown");
        groupRepository.update(unknownGroup);
        fail("An unknown group should not be updated");
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotUpdateNull() throws Exception {
        groupRepository.update(null);
        fail("A null group should not be updated");
    }
}
