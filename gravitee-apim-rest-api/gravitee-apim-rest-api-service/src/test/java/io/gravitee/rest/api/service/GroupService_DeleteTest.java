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

import static io.gravitee.repository.management.model.Group.AuditEvent.GROUP_DELETED;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

import io.gravitee.common.event.EventManager;
import io.gravitee.common.util.Maps;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.api.GroupRepository;
import io.gravitee.repository.management.api.PageRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.ApplicationCriteria;
import io.gravitee.repository.management.api.search.PageCriteria;
import io.gravitee.repository.management.model.AccessControl;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Group;
import io.gravitee.repository.management.model.Page;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.alert.ApplicationAlertEventType;
import io.gravitee.rest.api.model.alert.ApplicationAlertMembershipEvent;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.model.permissions.SystemRole;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.GroupNotFoundException;
import io.gravitee.rest.api.service.exceptions.InstallationNotFoundException;
import io.gravitee.rest.api.service.exceptions.StillPrimaryOwnerException;
import io.gravitee.rest.api.service.impl.GroupServiceImpl;
import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class GroupService_DeleteTest {

    private static final String ENVIRONMENT_ID = "my-group-id";
    private static final String GROUP_ID = "my-group-id";

    @InjectMocks
    private final GroupService groupService = new GroupServiceImpl();

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private ApiRepository apiRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private PageRepository pageRepository;

    @Mock
    private MembershipService membershipService;

    @Mock
    private PermissionService permissionService;

    @Mock
    private RoleService roleService;

    @Mock
    private AuditService auditService;

    @Mock
    private EventManager eventManager;

    @Test(expected = GroupNotFoundException.class)
    public void throwGroupNotFoundException() throws Exception {
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.empty());
        groupService.delete(ENVIRONMENT_ID, GROUP_ID);
    }

    @Test(expected = StillPrimaryOwnerException.class)
    public void throwStillPrimaryOwnerException() throws Exception {
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(Mockito.mock(Group.class)));

        RoleEntity role = new RoleEntity();
        role.setId("API_PRIMARY_OWNER_ID");
        when(roleService.findByScopeAndName(RoleScope.API, SystemRole.PRIMARY_OWNER.name())).thenReturn(Optional.of(role));
        when(
            membershipService.getMembershipsByMemberAndReferenceAndRole(
                MembershipMemberType.GROUP,
                GROUP_ID,
                MembershipReferenceType.API,
                "API_PRIMARY_OWNER_ID"
            )
        )
            .thenReturn(Set.of(new MembershipEntity()));

        groupService.delete(ENVIRONMENT_ID, GROUP_ID);
    }

    @Test
    public void shouldDeleteGroup() throws Exception {
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(Mockito.mock(Group.class)));

        RoleEntity role = new RoleEntity();
        role.setId("API_PRIMARY_OWNER_ID");
        when(roleService.findByScopeAndName(RoleScope.API, SystemRole.PRIMARY_OWNER.name())).thenReturn(Optional.of(role));

        when(
            membershipService.getMembershipsByMemberAndReferenceAndRole(
                MembershipMemberType.GROUP,
                GROUP_ID,
                MembershipReferenceType.API,
                "API_PRIMARY_OWNER_ID"
            )
        )
            .thenReturn(Collections.emptySet());

        when(apiRepository.search(new ApiCriteria.Builder().environmentId(ENVIRONMENT_ID).groups(GROUP_ID).build()))
            .thenReturn(Collections.emptyList());

        when(applicationRepository.findByGroups(Collections.singletonList(GROUP_ID))).thenReturn(Collections.emptySet());

        when(pageRepository.search(new PageCriteria.Builder().build())).thenReturn(Collections.emptyList());

        groupService.delete(ENVIRONMENT_ID, GROUP_ID);

        verify(membershipService, times(1))
            .deleteReference(
                eq(GraviteeContext.getCurrentOrganization()),
                eq(GraviteeContext.getCurrentEnvironment()),
                eq(MembershipReferenceType.GROUP),
                eq(GROUP_ID)
            );

        verify(membershipService, times(1))
            .deleteReferenceMember(
                eq(GraviteeContext.getCurrentOrganization()),
                eq(GraviteeContext.getCurrentEnvironment()),
                eq(MembershipReferenceType.API),
                eq(null),
                eq(MembershipMemberType.GROUP),
                eq(GROUP_ID)
            );

        verify(membershipService, times(1))
            .deleteReferenceMember(
                eq(GraviteeContext.getCurrentOrganization()),
                eq(GraviteeContext.getCurrentEnvironment()),
                eq(MembershipReferenceType.APPLICATION),
                eq(null),
                eq(MembershipMemberType.GROUP),
                eq(GROUP_ID)
            );

        verify(eventManager, times(1))
            .publishEvent(eq(ApplicationAlertEventType.APPLICATION_MEMBERSHIP_UPDATE), any(ApplicationAlertMembershipEvent.class));

        verify(groupRepository, times(1)).delete(eq(GROUP_ID));

        verify(auditService, times(1)).createEnvironmentAuditLog(eq(ENVIRONMENT_ID), any(), eq(GROUP_DELETED), any(), any(), eq(null));
    }

    @Test
    public void shouldDeleteGroupWithAccessControl() throws Exception {
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(Mockito.mock(Group.class)));

        RoleEntity role = new RoleEntity();
        role.setId("API_PRIMARY_OWNER_ID");
        when(roleService.findByScopeAndName(RoleScope.API, SystemRole.PRIMARY_OWNER.name())).thenReturn(Optional.of(role));

        when(
            membershipService.getMembershipsByMemberAndReferenceAndRole(
                MembershipMemberType.GROUP,
                GROUP_ID,
                MembershipReferenceType.API,
                "API_PRIMARY_OWNER_ID"
            )
        )
            .thenReturn(Collections.emptySet());

        when(apiRepository.search(new ApiCriteria.Builder().environmentId(ENVIRONMENT_ID).groups(GROUP_ID).build()))
            .thenReturn(Collections.emptyList());

        when(applicationRepository.findByGroups(Collections.singletonList(GROUP_ID))).thenReturn(Collections.emptySet());

        Page page = new Page();
        AccessControl accessControlToRemove = new AccessControl();
        accessControlToRemove.setReferenceType("GROUP");
        accessControlToRemove.setReferenceId(GROUP_ID);
        AccessControl accessControlToKeep = new AccessControl();
        page.setAccessControls(new HashSet<>(Set.of(accessControlToRemove, accessControlToKeep)));
        when(pageRepository.search(new PageCriteria.Builder().build())).thenReturn(List.of(page));

        groupService.delete(ENVIRONMENT_ID, GROUP_ID);

        verify(pageRepository, times(1)).update(argThat(arg -> arg.getAccessControls().size() == 1));
    }
}
