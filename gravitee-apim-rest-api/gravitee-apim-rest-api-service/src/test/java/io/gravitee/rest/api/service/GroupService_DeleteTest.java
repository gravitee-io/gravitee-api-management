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
package io.gravitee.rest.api.service;

import static io.gravitee.repository.management.model.Group.AuditEvent.GROUP_DELETED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

import io.gravitee.common.event.EventManager;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.api.GroupRepository;
import io.gravitee.repository.management.api.IdentityProviderRepository;
import io.gravitee.repository.management.api.PageRepository;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.ApiFieldFilter;
import io.gravitee.repository.management.api.search.PageCriteria;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.model.AccessControl;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Application;
import io.gravitee.repository.management.model.Group;
import io.gravitee.repository.management.model.Page;
import io.gravitee.repository.management.model.PageReferenceType;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.alert.ApplicationAlertEventType;
import io.gravitee.rest.api.model.alert.ApplicationAlertMembershipEvent;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.model.permissions.SystemRole;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.converter.ApiConverter;
import io.gravitee.rest.api.service.exceptions.GroupNotFoundException;
import io.gravitee.rest.api.service.exceptions.StillPrimaryOwnerException;
import io.gravitee.rest.api.service.impl.GroupServiceImpl;
import io.gravitee.rest.api.service.notification.ApiHook;
import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class GroupService_DeleteTest {

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

    @Mock
    private ApiConverter apiConverter;

    @Mock
    private UserService userService;

    @Mock
    private NotifierService notifierService;

    @Mock
    private PlanRepository planRepository;

    @Mock
    private IdentityProviderRepository identityProviderRepository;

    @Mock
    private PortalNotificationConfigService portalNotificationConfigService;

    @Test(expected = GroupNotFoundException.class)
    public void throwGroupNotFoundException() throws Exception {
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.empty());
        groupService.delete(GraviteeContext.getExecutionContext(), GROUP_ID);
    }

    @Test(expected = GroupNotFoundException.class)
    public void shouldNotDeleteBecauseDoesNotBelongToEnvironment() throws Exception {
        final Group group = new Group();
        group.setId(GROUP_ID);
        group.setEnvironmentId("Another_environment");
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));

        groupService.delete(GraviteeContext.getExecutionContext(), GROUP_ID);
    }

    @Test(expected = StillPrimaryOwnerException.class)
    public void throwStillPrimaryOwnerException() throws Exception {
        final Group group = new Group();
        group.setId(GROUP_ID);
        group.setEnvironmentId(GraviteeContext.getCurrentEnvironment());
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));

        RoleEntity role = new RoleEntity();
        role.setId("API_PRIMARY_OWNER_ID");
        when(
            roleService.findByScopeAndName(
                RoleScope.API,
                SystemRole.PRIMARY_OWNER.name(),
                GraviteeContext.getExecutionContext().getOrganizationId()
            )
        ).thenReturn(Optional.of(role));
        when(
            membershipService.getMembershipsByMemberAndReferenceAndRole(
                MembershipMemberType.GROUP,
                GROUP_ID,
                MembershipReferenceType.API,
                "API_PRIMARY_OWNER_ID"
            )
        ).thenReturn(Set.of(new MembershipEntity()));

        groupService.delete(GraviteeContext.getExecutionContext(), GROUP_ID);
    }

    @Test
    public void shouldDeleteGroup() throws Exception {
        final Group group = new Group();
        group.setId(GROUP_ID);
        group.setEnvironmentId(GraviteeContext.getCurrentEnvironment());
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));

        RoleEntity role = new RoleEntity();
        role.setId("API_PRIMARY_OWNER_ID");
        when(
            roleService.findByScopeAndName(
                RoleScope.API,
                SystemRole.PRIMARY_OWNER.name(),
                GraviteeContext.getExecutionContext().getOrganizationId()
            )
        ).thenReturn(Optional.of(role));

        when(
            membershipService.getMembershipsByMemberAndReferenceAndRole(
                MembershipMemberType.GROUP,
                GROUP_ID,
                MembershipReferenceType.API,
                "API_PRIMARY_OWNER_ID"
            )
        ).thenReturn(Collections.emptySet());

        when(
            apiRepository.search(
                new ApiCriteria.Builder().environmentId(GraviteeContext.getExecutionContext().getEnvironmentId()).groups(GROUP_ID).build(),
                null,
                new PageableBuilder().pageSize(100).pageNumber(0).build(),
                ApiFieldFilter.allFields()
            )
        )
            .thenReturn(
                new io.gravitee.common.data.domain.Page<>(List.of(Api.builder().groups(new HashSet<>(Set.of(GROUP_ID))).build()), 0, 1, 1)
            )
            .thenReturn(new io.gravitee.common.data.domain.Page<>(List.of(), 0, 0, 0));

        when(userService.findById(any(), any())).thenReturn(UserEntity.builder().sourceId("test").build());

        when(applicationRepository.findByGroups(Collections.singletonList(GROUP_ID))).thenReturn(Collections.emptySet());

        when(
            pageRepository.search(
                new PageCriteria.Builder()
                    .referenceId(GraviteeContext.getExecutionContext().getEnvironmentId())
                    .referenceType(PageReferenceType.ENVIRONMENT.name())
                    .build()
            )
        ).thenReturn(Collections.emptyList());

        groupService.delete(GraviteeContext.getExecutionContext(), GROUP_ID);

        verify(membershipService, times(1)).deleteReference(
            eq(GraviteeContext.getExecutionContext()),
            eq(MembershipReferenceType.GROUP),
            eq(GROUP_ID)
        );

        verify(membershipService, times(1)).deleteReferenceMember(
            eq(GraviteeContext.getExecutionContext()),
            eq(MembershipReferenceType.API),
            eq(null),
            eq(MembershipMemberType.GROUP),
            eq(GROUP_ID)
        );

        verify(membershipService, times(1)).deleteReferenceMember(
            eq(GraviteeContext.getExecutionContext()),
            eq(MembershipReferenceType.APPLICATION),
            eq(null),
            eq(MembershipMemberType.GROUP),
            eq(GROUP_ID)
        );

        verify(eventManager, times(1)).publishEvent(
            eq(ApplicationAlertEventType.APPLICATION_MEMBERSHIP_UPDATE),
            any(ApplicationAlertMembershipEvent.class)
        );

        verify(groupRepository, times(1)).delete(eq(GROUP_ID));

        verify(auditService, times(1)).createAuditLog(
            eq(GraviteeContext.getExecutionContext()),
            any(),
            eq(GROUP_DELETED),
            any(),
            any(),
            eq(null)
        );

        verify(portalNotificationConfigService, times(1)).removeGroupIds(any(), eq(Set.of(GROUP_ID)));
    }

    @Test
    public void shouldDeleteGroupWithAccessControl() throws Exception {
        final String ANOTHER_GROUP_ID = "another-group-id";

        final Group group = new Group();
        group.setId(GROUP_ID);
        group.setEnvironmentId(GraviteeContext.getCurrentEnvironment());
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));

        RoleEntity role = new RoleEntity();
        role.setId("API_PRIMARY_OWNER_ID");
        when(
            roleService.findByScopeAndName(
                RoleScope.API,
                SystemRole.PRIMARY_OWNER.name(),
                GraviteeContext.getExecutionContext().getOrganizationId()
            )
        ).thenReturn(Optional.of(role));

        when(
            membershipService.getMembershipsByMemberAndReferenceAndRole(
                MembershipMemberType.GROUP,
                GROUP_ID,
                MembershipReferenceType.API,
                "API_PRIMARY_OWNER_ID"
            )
        ).thenReturn(Collections.emptySet());

        UserEntity mockUser = mock(UserEntity.class);
        when(mockUser.getDisplayName()).thenReturn("Mock User");
        when(userService.findById(any(), any())).thenReturn(mockUser);
        when(planRepository.findByApi(anyString())).thenReturn(Collections.emptySet());
        when(identityProviderRepository.findAll()).thenReturn(Collections.emptySet());

        Api api = new Api();
        api.setId("API_ID");
        api.setGroups(new HashSet<>(Collections.singletonList(GROUP_ID)));
        when(
            apiRepository.search(
                new ApiCriteria.Builder().environmentId(GraviteeContext.getExecutionContext().getEnvironmentId()).groups(GROUP_ID).build(),
                null,
                new PageableBuilder().pageSize(100).pageNumber(0).build(),
                ApiFieldFilter.allFields()
            )
        )
            .thenReturn(new io.gravitee.common.data.domain.Page<>(List.of(api), 1, 1, 1))
            .thenReturn(new io.gravitee.common.data.domain.Page<>(List.of(), 0, 0, 0));

        Application application = new Application();
        application.setId("APP_ID");
        application.setName("Test Application");
        application.setGroups(new HashSet<>(Arrays.asList(GROUP_ID, ANOTHER_GROUP_ID)));

        when(applicationRepository.findByGroups(Collections.singletonList(GROUP_ID))).thenReturn(
            new HashSet<>(Collections.singletonList(application))
        );

        Page page = new Page();
        AccessControl accessControlToRemove = new AccessControl();
        accessControlToRemove.setReferenceType("GROUP");
        accessControlToRemove.setReferenceId(GROUP_ID);
        AccessControl accessControlToKeep = new AccessControl();
        page.setAccessControls(new HashSet<>(Set.of(accessControlToRemove, accessControlToKeep)));
        when(
            pageRepository.search(
                new PageCriteria.Builder()
                    .referenceId(GraviteeContext.getExecutionContext().getEnvironmentId())
                    .referenceType(PageReferenceType.ENVIRONMENT.name())
                    .build()
            )
        ).thenReturn(List.of(page));
        doNothing().when(notifierService).trigger(any(ExecutionContext.class), any(ApiHook.class), anyString(), anyMap());

        groupService.delete(GraviteeContext.getExecutionContext(), GROUP_ID);

        verify(pageRepository, times(1)).update(argThat(arg -> arg.getAccessControls().size() == 1));
        verify(apiRepository, times(1)).update(argThat(apiArg -> !apiArg.getGroups().contains(GROUP_ID)));
        verify(applicationRepository, times(1)).update(argThat(applicationArg -> !applicationArg.getGroups().contains(GROUP_ID)));
        verify(applicationRepository).update(
            argThat(app -> app.getGroups().size() == 1 && !app.getGroups().contains(GROUP_ID) && app.getGroups().contains(ANOTHER_GROUP_ID))
        );
    }
}
