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
import org.junit.Test;

import java.util.Collections;
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
    public void shouldNotFindByType() throws TechnicalException {
        Set<Group> groups = groupRepository.findByType(Group.Type.API);

        Assert.assertNotNull(groups);
        Assert.assertEquals(0, groups.size());
    }
}
