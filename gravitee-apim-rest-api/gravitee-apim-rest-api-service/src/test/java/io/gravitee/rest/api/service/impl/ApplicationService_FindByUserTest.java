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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doReturn;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.api.search.ApplicationCriteria;
import io.gravitee.repository.management.api.search.builder.SortableBuilder;
import io.gravitee.repository.management.model.ApiKeyMode;
import io.gravitee.repository.management.model.Application;
import io.gravitee.repository.management.model.ApplicationStatus;
import io.gravitee.repository.management.model.ApplicationType;
import io.gravitee.rest.api.model.MembershipEntity;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.application.ApplicationListItem;
import io.gravitee.rest.api.model.common.SortableImpl;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.impl.ApplicationServiceImpl;
import java.util.*;
import org.assertj.core.util.Lists;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ApplicationService_FindByUserTest {

    private static final String APPLICATION_ID = "id-app";
    private static final String GROUP_APPLICATION_ID = "id-group-app";

    private static final String USERNAME = "Gravitee.io APIM";

    @InjectMocks
    private ApplicationServiceImpl applicationService = new ApplicationServiceImpl();

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private MembershipEntity appMembership;

    @Mock
    private MembershipEntity groupAppMembership;

    @Mock
    private Application application;

    @Mock
    private Application groupApplication;

    @Mock
    private UserService userService;

    @Mock
    private MembershipService membershipService;

    @Mock
    private GroupService groupService;

    @Mock
    private RoleService roleService;

    @Mock
    private RoleEntity primaryOwnerRole;

    @After
    public void tearDown() {
        GraviteeContext.cleanContext();
    }

    @Test
    public void shouldFindByUser() throws Exception {
        GraviteeContext.setCurrentEnvironment("envId");
        when(application.getId()).thenReturn(APPLICATION_ID);
        when(application.getStatus()).thenReturn(ApplicationStatus.ACTIVE);
        when(application.getType()).thenReturn(ApplicationType.SIMPLE);
        when(application.getApiKeyMode()).thenReturn(ApiKeyMode.UNSPECIFIED);

        when(
            membershipService.getReferenceIdsByMemberAndReference(MembershipMemberType.USER, USERNAME, MembershipReferenceType.APPLICATION)
        )
            .thenReturn(Collections.singleton(APPLICATION_ID));

        ApplicationCriteria criteria = ApplicationCriteria
            .builder()
            .restrictedToIds(Set.of(APPLICATION_ID))
            .environmentIds(Set.of(GraviteeContext.getExecutionContext().getEnvironmentId()))
            .status(ApplicationStatus.ACTIVE)
            .build();
        when(applicationRepository.search(criteria, null, null)).thenReturn(new Page<>(List.of(application), 1, 1, 1));

        when(roleService.findPrimaryOwnerRoleByOrganization(any(), any())).thenReturn(mock(RoleEntity.class));

        MembershipEntity po = new MembershipEntity();
        po.setMemberId(USERNAME);
        po.setMemberType(MembershipMemberType.USER);
        po.setReferenceId(APPLICATION_ID);
        po.setReferenceType(MembershipReferenceType.APPLICATION);
        po.setRoleId("APPLICATION_PRIMARY_OWNER");
        when(membershipService.getMembershipsByReferencesAndRole(any(), any(), any())).thenReturn(Collections.singleton(po));

        Set<ApplicationListItem> apps = applicationService.findByUser(GraviteeContext.getExecutionContext(), USERNAME);

        Assert.assertNotNull(apps);
        Assert.assertFalse("should find app", apps.isEmpty());
        Assert.assertEquals(APPLICATION_ID, apps.iterator().next().getId());
    }

    @Test
    public void shouldNotFindByUserBecauseOfArchived() throws Exception {
        Set<ApplicationListItem> apps = applicationService.findByUser(GraviteeContext.getExecutionContext(), USERNAME);

        Assert.assertNotNull(apps);
        Assert.assertTrue("should not find app", apps.isEmpty());
    }

    @Test
    public void shouldFindByUserAndGroup() throws Exception {
        GraviteeContext.setCurrentEnvironment("envId");
        when(groupAppMembership.getReferenceId()).thenReturn(GROUP_APPLICATION_ID);
        when(groupAppMembership.getRoleId()).thenReturn("APPLICATION_PRIMARY_OWNER");

        when(application.getId()).thenReturn(APPLICATION_ID);
        when(application.getStatus()).thenReturn(ApplicationStatus.ACTIVE);
        when(application.getType()).thenReturn(ApplicationType.SIMPLE);
        when(application.getApiKeyMode()).thenReturn(ApiKeyMode.UNSPECIFIED);
        when(groupApplication.getId()).thenReturn(GROUP_APPLICATION_ID);
        when(groupApplication.getStatus()).thenReturn(ApplicationStatus.ACTIVE);
        when(groupApplication.getType()).thenReturn(ApplicationType.SIMPLE);
        when(groupApplication.getApiKeyMode()).thenReturn(ApiKeyMode.UNSPECIFIED);

        when(
            membershipService.getReferenceIdsByMemberAndReference(MembershipMemberType.USER, USERNAME, MembershipReferenceType.APPLICATION)
        )
            .thenReturn(Collections.singleton(APPLICATION_ID));

        when(membershipService.getMembershipsByMemberAndReference(MembershipMemberType.USER, USERNAME, MembershipReferenceType.GROUP))
            .thenReturn(Collections.singleton(groupAppMembership));

        RoleEntity role = mock(RoleEntity.class);
        when(role.getScope()).thenReturn(RoleScope.APPLICATION);
        when(roleService.findPrimaryOwnerRoleByOrganization(any(), any())).thenReturn(role);
        when(roleService.findById(any())).thenReturn(role);

        ApplicationCriteria criteria = ApplicationCriteria
            .builder()
            .restrictedToIds(Set.of(APPLICATION_ID))
            .environmentIds(Set.of(GraviteeContext.getExecutionContext().getEnvironmentId()))
            .status(ApplicationStatus.ACTIVE)
            .build();
        when(applicationRepository.search(criteria, null, null)).thenReturn(new Page<>(List.of(application, groupApplication), 1, 2, 2));

        MembershipEntity poApp = new MembershipEntity();
        poApp.setId("poApp-id");
        poApp.setMemberId(USERNAME);
        poApp.setMemberType(MembershipMemberType.USER);
        poApp.setReferenceId(APPLICATION_ID);
        poApp.setReferenceType(MembershipReferenceType.APPLICATION);
        poApp.setRoleId("APPLICATION_PRIMARY_OWNER");
        MembershipEntity poGroupApp = new MembershipEntity();
        poGroupApp.setId("poGroupApp-id");
        poGroupApp.setMemberId(USERNAME);
        poGroupApp.setMemberType(MembershipMemberType.USER);
        poGroupApp.setReferenceId(GROUP_APPLICATION_ID);
        poGroupApp.setReferenceType(MembershipReferenceType.APPLICATION);
        poGroupApp.setRoleId("APPLICATION_PRIMARY_OWNER");
        Set<MembershipEntity> memberships = new HashSet<>();
        memberships.add(poApp);
        memberships.add(poGroupApp);
        when(membershipService.getMembershipsByReferencesAndRole(any(), any(), any())).thenReturn(memberships);

        Set<ApplicationListItem> apps = applicationService.findByUser(GraviteeContext.getExecutionContext(), USERNAME);

        Assert.assertNotNull(apps);
        Assert.assertFalse("should find apps", apps.isEmpty());
        Assert.assertEquals(2, apps.size());
    }

    @Test
    public void shouldFindByUserWithSortable() throws Exception {
        GraviteeContext.setCurrentEnvironment("envId");
        when(application.getId()).thenReturn(APPLICATION_ID);
        when(application.getStatus()).thenReturn(ApplicationStatus.ACTIVE);
        when(application.getType()).thenReturn(ApplicationType.SIMPLE);
        when(application.getApiKeyMode()).thenReturn(ApiKeyMode.UNSPECIFIED);
        when(
            membershipService.getReferenceIdsByMemberAndReference(MembershipMemberType.USER, USERNAME, MembershipReferenceType.APPLICATION)
        )
            .thenReturn(Collections.singleton(APPLICATION_ID));

        ApplicationCriteria criteria = ApplicationCriteria
            .builder()
            .restrictedToIds(Set.of(APPLICATION_ID))
            .environmentIds(Set.of(GraviteeContext.getExecutionContext().getEnvironmentId()))
            .status(ApplicationStatus.ACTIVE)
            .build();
        when(applicationRepository.search(criteria, null, new SortableBuilder().field("name").setAsc(true).build()))
            .thenReturn(new Page<>(List.of(application), 1, 2, 2));

        when(roleService.findPrimaryOwnerRoleByOrganization(any(), any())).thenReturn(mock(RoleEntity.class));

        MembershipEntity po = new MembershipEntity();
        po.setMemberId(USERNAME);
        po.setMemberType(MembershipMemberType.USER);
        po.setReferenceId(APPLICATION_ID);
        po.setReferenceType(MembershipReferenceType.APPLICATION);
        po.setRoleId("APPLICATION_PRIMARY_OWNER");
        when(membershipService.getMembershipsByReferencesAndRole(any(), any(), any())).thenReturn(Collections.singleton(po));

        Set<ApplicationListItem> apps = applicationService.findByUser(
            GraviteeContext.getExecutionContext(),
            USERNAME,
            new SortableImpl("name", true)
        );

        Assert.assertNotNull(apps);
        Assert.assertFalse("should find app", apps.isEmpty());
        Assert.assertEquals(APPLICATION_ID, apps.iterator().next().getId());
    }

    @Test
    public void shouldFindIdsByUser() throws Exception {
        GraviteeContext.setCurrentEnvironment("envId");

        when(
            membershipService.getReferenceIdsByMemberAndReference(MembershipMemberType.USER, USERNAME, MembershipReferenceType.APPLICATION)
        )
            .thenReturn(Collections.singleton(APPLICATION_ID));

        ApplicationCriteria criteria = ApplicationCriteria
            .builder()
            .restrictedToIds(Set.of(APPLICATION_ID))
            .environmentIds(Set.of(GraviteeContext.getExecutionContext().getEnvironmentId()))
            .status(ApplicationStatus.ACTIVE)
            .build();
        when(applicationRepository.searchIds(criteria, null)).thenReturn(Set.of(APPLICATION_ID));

        MembershipEntity po = new MembershipEntity();
        po.setMemberId(USERNAME);
        po.setMemberType(MembershipMemberType.USER);
        po.setReferenceId(APPLICATION_ID);
        po.setReferenceType(MembershipReferenceType.APPLICATION);
        po.setRoleId("APPLICATION_PRIMARY_OWNER");

        Set<String> apps = applicationService.findIdsByUser(GraviteeContext.getExecutionContext(), USERNAME);

        Assert.assertNotNull(apps);
        Assert.assertFalse("should find app", apps.isEmpty());
        Assert.assertEquals(APPLICATION_ID, apps.iterator().next());
    }

    @Test
    public void shouldFindIdsByUserAndPermission() throws Exception {
        GraviteeContext.setCurrentEnvironment("envId");

        when(
            membershipService.getReferenceIdsByMemberAndReference(MembershipMemberType.USER, USERNAME, MembershipReferenceType.APPLICATION)
        )
            .thenReturn(Collections.singleton(APPLICATION_ID));

        ApplicationCriteria criteria = ApplicationCriteria
            .builder()
            .restrictedToIds(Set.of(APPLICATION_ID))
            .environmentIds(Set.of(GraviteeContext.getExecutionContext().getEnvironmentId()))
            .status(ApplicationStatus.ACTIVE)
            .build();
        when(applicationRepository.searchIds(criteria, null)).thenReturn(Set.of(APPLICATION_ID));

        MembershipEntity po = new MembershipEntity();
        po.setMemberId(USERNAME);
        po.setMemberType(MembershipMemberType.USER);
        po.setReferenceId(APPLICATION_ID);
        po.setReferenceType(MembershipReferenceType.APPLICATION);
        po.setRoleId("APPLICATION_PRIMARY_OWNER");

        Set<String> apps = applicationService.findIdsByUserAndPermission(
            GraviteeContext.getExecutionContext(),
            USERNAME,
            null,
            RolePermission.APPLICATION_SUBSCRIPTION,
            RolePermissionAction.READ
        );

        Assert.assertNotNull(apps);
        Assert.assertFalse("should find app", apps.isEmpty());
        Assert.assertEquals(APPLICATION_ID, apps.iterator().next());
    }
}
