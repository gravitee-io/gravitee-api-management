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
package io.gravitee.repository.mock.management;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.collections.Sets.newSet;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.GroupRepository;
import io.gravitee.repository.management.model.Group;
import io.gravitee.repository.management.model.GroupEvent;
import io.gravitee.repository.management.model.GroupEventRule;
import io.gravitee.repository.mock.AbstractRepositoryMock;
import java.util.Date;
import java.util.HashSet;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class GroupRepositoryMock extends AbstractRepositoryMock<GroupRepository> {

    public GroupRepositoryMock() {
        super(GroupRepository.class);
    }

    @Override
    protected void prepare(GroupRepository groupRepository) throws Exception {
        final Group createGroup = new Group();
        createGroup.setId("1");
        createGroup.setEnvironmentId("DEFAULT");
        createGroup.setName("my group");
        createGroup.setLockApiRole(true);
        createGroup.setLockApplicationRole(true);
        createGroup.setSystemInvitation(true);
        createGroup.setEmailInvitation(true);
        createGroup.setMaxInvitation(10);
        createGroup.setDisableMembershipNotifications(true);
        when(groupRepository.create(any())).thenReturn(createGroup);

        final Group group_application_1 = new Group();
        group_application_1.setId("group-application-1");
        group_application_1.setEnvironmentId("group-application-1 environment-id");
        group_application_1.setName("group-application-1 name");
        group_application_1.setLockApiRole(true);
        group_application_1.setLockApplicationRole(true);
        group_application_1.setSystemInvitation(true);
        group_application_1.setEmailInvitation(true);
        group_application_1.setDisableMembershipNotifications(true);
        group_application_1.setMaxInvitation(99);
        GroupEventRule eventRule1 = new GroupEventRule();
        eventRule1.setEvent(GroupEvent.API_CREATE);
        GroupEventRule eventRule2 = new GroupEventRule();
        eventRule2.setEvent(GroupEvent.APPLICATION_CREATE);
        group_application_1.setEventRules(asList(eventRule1, eventRule2));
        group_application_1.setApiPrimaryOwner("api-primary-owner-id");

        final Group group_api_to_delete = new Group();
        group_api_to_delete.setId("group-api-to-delete");
        group_api_to_delete.setName("group-api-to-delete");
        final Group group_updated = new Group();
        group_updated.setId("group-application-1");
        group_updated.setName("Modified Name");
        group_updated.setEnvironmentId("new_DEFAULT");
        group_updated.setUpdatedAt(new Date(1000000000000L));
        group_updated.setLockApiRole(true);
        group_updated.setLockApplicationRole(true);
        group_updated.setSystemInvitation(true);
        group_updated.setEmailInvitation(true);
        group_updated.setDisableMembershipNotifications(false);
        group_updated.setMaxInvitation(99);
        group_updated.setApiPrimaryOwner("new-po-user-id");

        final Group group_env_1_org_1 = new Group();
        group_env_1_org_1.setId("group_env_1_org_1");
        group_env_1_org_1.setName("group env 1 org 1");
        group_env_1_org_1.setEnvironmentId("ENV_ORG_1");

        final Group group_env_2_org_1 = new Group();
        group_env_2_org_1.setId("group_env_2_org_1");
        group_env_2_org_1.setName("group env 2 org 1");
        group_env_2_org_1.setEnvironmentId("SECOND_ENV_ORG_1");

        final Group group_env_org_2 = new Group();
        group_env_org_2.setId("group_env_org_2");
        group_env_org_2.setName("group env org 1");
        group_env_org_2.setEnvironmentId("ENV_ORG_2");

        when(groupRepository.findAll())
            .thenReturn(newSet(group_application_1, group_api_to_delete, group_env_1_org_1, group_env_2_org_1, group_env_org_2));
        when(groupRepository.findAllByEnvironment("DEFAULT")).thenReturn(newSet(createGroup));
        when(groupRepository.findAllByOrganization("ORGANIZATION_1")).thenReturn(newSet(group_env_1_org_1, group_env_2_org_1));
        when(groupRepository.findById("group-application-1")).thenReturn(of(group_application_1));
        when(groupRepository.findById("unknown")).thenReturn(empty());
        when(groupRepository.findById("group-api-to-delete")).thenReturn(empty());
        when(groupRepository.update(argThat(o -> o != null && o.getId().equals("unknown")))).thenThrow(new TechnicalException());

        when(groupRepository.update(argThat(o -> o != null && o.getId().equals("group-application-1")))).thenReturn(group_updated);

        when(groupRepository.findByIds(new HashSet<>(asList("group-application-1", "group-api-to-delete", "unknown"))))
            .thenReturn(new HashSet<>(asList(group_application_1, group_api_to_delete)));
        when(groupRepository.findByIds(emptySet())).thenReturn(emptySet());

        when(groupRepository.update(argThat(o -> o == null || o.getId().equals("unknown")))).thenThrow(new IllegalStateException());
    }
}
