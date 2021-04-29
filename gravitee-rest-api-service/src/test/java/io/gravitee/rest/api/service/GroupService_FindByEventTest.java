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
package io.gravitee.rest.api.service;

import static io.gravitee.repository.management.model.GroupEvent.API_CREATE;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.gravitee.repository.management.api.GroupRepository;
import io.gravitee.repository.management.model.Group;
import io.gravitee.repository.management.model.GroupEvent;
import io.gravitee.repository.management.model.GroupEventRule;
import io.gravitee.rest.api.model.GroupEntity;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.impl.GroupServiceImpl;
import java.util.*;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class GroupService_FindByEventTest {

    @InjectMocks
    private GroupService groupService = new GroupServiceImpl();

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private MembershipService membershipService;

    @Test
    public void shouldGetGroupsByEvents() throws Exception {
        Group grp1 = new Group();
        grp1.setId("grp1");
        grp1.setName("grp1");
        grp1.setEventRules(Collections.singletonList(new GroupEventRule(GroupEvent.API_CREATE)));
        Group grp2 = new Group();
        grp2.setId("grp2");
        grp2.setName("grp2");
        grp2.setEventRules(Collections.singletonList(new GroupEventRule(GroupEvent.API_CREATE)));
        HashSet<Group> findAll = new HashSet<>();
        findAll.add(grp1);
        findAll.add(grp2);
        when(groupRepository.findAllByEnvironment(Mockito.any())).thenReturn(findAll);

        when(membershipService.getRoles(any(), any(), any(), any())).thenReturn(Collections.emptySet());

        Set<GroupEntity> groupEntities = groupService.findByEvent(API_CREATE);

        assertNotNull(groupEntities);
        assertFalse(groupEntities.isEmpty());
        assertEquals(2, groupEntities.size());
        List<String> groupIds = groupEntities.stream().map(GroupEntity::getId).collect(Collectors.toList());
        assertTrue(groupIds.containsAll(Arrays.asList("grp1", "grp2")));
    }

    @Test
    public void shouldNotGetGroupsByEvents() throws Exception {
        Group grp1 = new Group();
        grp1.setId("grp1");
        grp1.setEventRules(Collections.singletonList(new GroupEventRule(GroupEvent.APPLICATION_CREATE)));
        Group grp2 = new Group();
        grp2.setId("grp2");
        grp2.setEventRules(Collections.singletonList(new GroupEventRule(GroupEvent.APPLICATION_CREATE)));
        HashSet<Group> findAll = new HashSet<>();
        findAll.add(grp1);
        findAll.add(grp2);
        when(groupRepository.findAllByEnvironment(any())).thenReturn(findAll);

        Set<GroupEntity> groupEntities = groupService.findByEvent(API_CREATE);

        assertNotNull(groupEntities);
        assertTrue(groupEntities.isEmpty());
    }
}
