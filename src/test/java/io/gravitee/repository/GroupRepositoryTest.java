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
import io.gravitee.repository.management.api.GroupRepository;
import io.gravitee.repository.management.model.Group;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Collections;
import java.util.Date;
import java.util.Optional;
import java.util.Set;

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
        group.setType(Group.Type.API);

        Group group1 = groupRepository.create(group);

        Assert.assertNotNull(group1);
        Assert.assertNotNull(group1.getId());
        Assert.assertEquals(group.getId(), group1.getId());
        Assert.assertNotNull(group1.getAdministrators());
        Assert.assertTrue(group1.getAdministrators().isEmpty());
    }

    @Test
    public void shouldFindByType() throws TechnicalException {
        Set<Group> groups = groupRepository.findByType(Group.Type.APPLICATION);

        Assert.assertNotNull(groups);
        Assert.assertEquals(1, groups.size());
        Group group = groups.iterator().next();
        Assert.assertEquals("group-application-1", group.getName());

        Assert.assertNotNull(group.getAdministrators());
        Assert.assertEquals(2, group.getAdministrators().size());
    }

    @Test
    public void shouldFindById() throws TechnicalException {
        Optional<Group> group = groupRepository.findById("group-application-1");

        Assert.assertNotNull(group);
        Assert.assertTrue(group.isPresent());
        Assert.assertEquals("group-application-1", group.get().getId());
        Assert.assertEquals(Group.Type.APPLICATION, group.get().getType());
        Assert.assertEquals("group-application-1", group.get().getName());
    }

    @Test
    public void shouldNotFindByUnknownId() throws TechnicalException {
        Optional<Group> group = groupRepository.findById("unknown");

        Assert.assertNotNull(group);
        Assert.assertFalse(group.isPresent());
    }

    @Ignore
    @Test(expected = TechnicalException.class)
    public void shouldNotUpdateUnknownGroup() throws TechnicalException {
        Group group = new Group();
        group.setId("unknown");
        group.setType(Group.Type.APPLICATION);
        group.setName("Unknown");

        groupRepository.update(group);

        Assert.fail("should not update an unknown group");
    }

    @Test
    public void shouldUpdate() throws TechnicalException {
        Group group = new Group();
        group.setId("group-application-1");
        group.setType(Group.Type.APPLICATION);
        group.setName("Modified Name");
        group.setUpdatedAt(new Date(0));

        Group update = groupRepository.update(group);

        Assert.assertEquals(group.getId(), update.getId());
        Assert.assertEquals(group.getType(), update.getType());
        Assert.assertEquals(group.getName(), update.getName());
        Assert.assertEquals(new Date(0), update.getUpdatedAt());
    }

    @Test
    public void shouldFindAll() throws TechnicalException {
        Set<Group> groups = groupRepository.findAll();

        Assert.assertNotNull(groups);
        Assert.assertFalse("not empty", groups.isEmpty());
        Assert.assertEquals(2, groups.size());
    }

    @Test
    public void shouldDelete() throws TechnicalException {
        groupRepository.delete("group-api-to-delete");
        Optional<Group> group = groupRepository.findById("group-api-to-delete");

        Assert.assertNotNull(group);
        Assert.assertFalse(group.isPresent());
    }
}
