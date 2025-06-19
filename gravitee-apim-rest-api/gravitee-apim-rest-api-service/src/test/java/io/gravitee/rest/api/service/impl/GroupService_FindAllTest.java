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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.GroupRepository;
import io.gravitee.repository.management.model.Group;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.GroupSimpleEntity;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class GroupService_FindAllTest extends TestCase {

    private static final String ORGANIZATION_ID = "org-id";
    private static final String ENVIRONMENT_ID = "env-id";

    @InjectMocks
    private final GroupService groupService = new GroupServiceImpl();

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private GroupRepository groupRepository;

    @Test
    public void shouldReturnGroupsByEnvironment() throws TechnicalException {
        Set<Group> groups = new HashSet<>();
        Group group1 = new Group();
        group1.setId("group-1");
        group1.setEnvironmentId(ENVIRONMENT_ID);
        Group group2 = new Group();
        group2.setId("group-2");
        group2.setEnvironmentId(ENVIRONMENT_ID);
        groups.add(group1);
        groups.add(group2);
        when(groupRepository.findAllByEnvironment(ENVIRONMENT_ID)).thenReturn(groups);
        Set<Group> result = groupService.findAllByEnvironment(ENVIRONMENT_ID);
        assertThat(result).hasSize(2);
        assertThat(result).extracting(Group::getId).containsExactlyInAnyOrder("group-1", "group-2");
    }

    @Test
    public void shouldReturnEmptySetWhenNoGroupsFoundByEnvironment() throws TechnicalException {
        Set<Group> groups = new HashSet<>();
        when(groupRepository.findAllByEnvironment(ENVIRONMENT_ID)).thenReturn(groups);
        Set<Group> result = groupService.findAllByEnvironment(ENVIRONMENT_ID);
        assertThat(result).isEmpty();
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldThrowTechnicalManagementExceptionWhenRepositoryFails() throws TechnicalException {
        when(groupRepository.findAllByEnvironment(ENVIRONMENT_ID)).thenThrow(new TechnicalException("Database error"));
        groupService.findAllByEnvironment(ENVIRONMENT_ID);
    }

    @Test
    public void shouldReturnGroupsByOrganization() throws TechnicalException {
        Set<Group> groups = new HashSet<>();
        Group group1 = new Group();
        group1.setId("group-1");
        group1.setName("Alpha");
        group1.setEnvironmentId(ENVIRONMENT_ID);
        Group group2 = new Group();
        group2.setId("group-2");
        group2.setName("Beta");
        group2.setEnvironmentId(ENVIRONMENT_ID);
        groups.add(group1);
        groups.add(group2);

        EnvironmentEntity environmentEntity = new EnvironmentEntity();
        environmentEntity.setId(ENVIRONMENT_ID);
        environmentEntity.setName("env-name");

        when(groupRepository.findAllByOrganization(ORGANIZATION_ID)).thenReturn(groups);
        when(environmentService.findByOrgAndIdOrHrid(ORGANIZATION_ID, ENVIRONMENT_ID)).thenReturn(environmentEntity);

        List<GroupSimpleEntity> result = groupService.findAllByOrganization(ORGANIZATION_ID);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(GroupSimpleEntity::getName).containsExactlyInAnyOrder("Alpha", "Beta");
        assertThat(result).extracting(GroupSimpleEntity::getEnvironmentId).containsOnly(ENVIRONMENT_ID);
        assertThat(result).extracting(GroupSimpleEntity::getEnvironmentName).containsOnly("env-name");
    }

    @Test
    public void shouldReturnEmptyListWhenNoGroupsFoundByOrganization() throws TechnicalException {
        when(groupRepository.findAllByOrganization(ORGANIZATION_ID)).thenReturn(Collections.emptySet());

        List<GroupSimpleEntity> result = groupService.findAllByOrganization(ORGANIZATION_ID);

        assertThat(result).isEmpty();
    }
}
