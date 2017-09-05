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
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.model.Group;
import org.junit.Ignore;
import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com) 
 * @author GraviteeSource Team
 */
public class GroupRepositoryTest extends AbstractRepositoryTest {


    @Override
    protected String getTestCasesPath() {
        return "/data/group-tests/";
    }

    @Test
    public void shouldCreateGroup() throws TechnicalException {
        Group group = new Group();
        group.setId("1");
        group.setName("my group");

        Group group1 = groupRepository.create(group);

        assertNotNull(group1);
        assertNotNull(group1.getId());
        assertEquals(group.getId(), group1.getId());
        assertNotNull(group1.getAdministrators());
        assertTrue(group1.getAdministrators().isEmpty());
    }

    @Test
    public void shouldFindById() throws TechnicalException {
        Optional<Group> group = groupRepository.findById("group-application-1");

        assertNotNull(group);
        assertTrue(group.isPresent());
        assertEquals("group-application-1", group.get().getId());
        assertEquals("group-application-1", group.get().getName());
        assertEquals(2, group.get().getAdministrators().size());
        assertEquals(2, group.get().getEventRules().size());
    }

    @Test
    public void shouldNotFindByUnknownId() throws TechnicalException {
        Optional<Group> group = groupRepository.findById("unknown");

        assertNotNull(group);
        assertFalse(group.isPresent());
    }

    @Ignore
    @Test(expected = TechnicalException.class)
    public void shouldNotUpdateUnknownGroup() throws TechnicalException {
        Group group = new Group();
        group.setId("unknown");
        group.setName("Unknown");

        groupRepository.update(group);

        fail("should not update an unknown group");
    }

    @Test
    public void shouldUpdate() throws TechnicalException {
        Group group = new Group();
        group.setId("group-application-1");
        group.setName("Modified Name");
        group.setUpdatedAt(new Date(0));

        Group update = groupRepository.update(group);

        assertEquals(group.getId(), update.getId());
        assertEquals(group.getName(), update.getName());
        assertEquals(new Date(0), update.getUpdatedAt());
    }

    @Test
    public void shouldFindAll() throws TechnicalException {
        Set<Group> groups = groupRepository.findAll();

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
    public void shouldFindByIds() throws TechnicalException {
        Set<Group> groups = groupRepository.findByIds(new HashSet<>(Arrays.asList("group-application-1", "group-api-to-delete", "unknown")));

        assertNotNull(groups);
        assertFalse(groups.isEmpty());
        assertEquals(2, groups.size());
        assertTrue(groups.
                stream().
                map(Group::getId).
                collect(Collectors.toList()).
                containsAll(Arrays.asList("group-application-1", "group-api-to-delete")));
    }
}
