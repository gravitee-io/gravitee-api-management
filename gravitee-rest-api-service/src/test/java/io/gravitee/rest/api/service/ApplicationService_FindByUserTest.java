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

import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.model.Application;
import io.gravitee.repository.management.model.ApplicationStatus;
import io.gravitee.repository.management.model.ApplicationType;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.application.ApplicationListItem;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.impl.ApplicationServiceImpl;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
    private static final String GROUP_ID = "group-id";

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

    @Test
    public void shouldFindByUser() throws Exception {
        when(appMembership.getReferenceId()).
                thenReturn(APPLICATION_ID);
        when(application.getId()).
                thenReturn(APPLICATION_ID);
        when(application.getStatus()).
                thenReturn(ApplicationStatus.ACTIVE);
        when(application.getType()).
                thenReturn(ApplicationType.SIMPLE);
        when(membershipService.getMembershipsByMemberAndReference(MembershipMemberType.USER, USERNAME, MembershipReferenceType.APPLICATION)).
                thenReturn(Collections.singleton(appMembership));
        when(applicationRepository.findByIds(Collections.singletonList(APPLICATION_ID))).
                thenReturn(Collections.singleton(application));
        when(roleService.findByScopeAndName(any(), any())).thenReturn(Optional.of(mock(RoleEntity.class)));
        
        MembershipEntity po = new MembershipEntity();
        po.setMemberId(USERNAME);
        po.setMemberType(MembershipMemberType.USER);
        po.setReferenceId(APPLICATION_ID);
        po.setReferenceType(MembershipReferenceType.APPLICATION);
        po.setRoleId("APPLICATION_PRIMARY_OWNER");
        when(membershipService.getMembershipsByReferencesAndRole(any(), any(), any())).thenReturn(Collections.singleton(po));

        Set<ApplicationListItem> apps = applicationService.findByUser(USERNAME);

        Assert.assertNotNull(apps);
        Assert.assertFalse("should find app", apps.isEmpty());
        Assert.assertEquals(APPLICATION_ID, apps.iterator().next().getId());
    }

    @Test
    public void shouldNotFindByUserBecauseOfArchived() throws Exception {
        when(appMembership.getReferenceId()).
                thenReturn(APPLICATION_ID);
        when(application.getStatus()).
                thenReturn(ApplicationStatus.ARCHIVED);
        when(membershipService.getMembershipsByMemberAndReference(MembershipMemberType.USER, USERNAME, MembershipReferenceType.APPLICATION)).
                thenReturn(Collections.singleton(appMembership));
        when(applicationRepository.findByIds(Collections.singletonList(APPLICATION_ID))).
                thenReturn(Collections.singleton(application));

        Set<ApplicationListItem> apps = applicationService.findByUser(USERNAME);

        Assert.assertNotNull(apps);
        Assert.assertTrue("should not find app", apps.isEmpty());
    }

    @Test
    public void shouldFindByUserAndGroup() throws Exception {
        when(appMembership.getReferenceId()).
                thenReturn(APPLICATION_ID);
        when(groupAppMembership.getReferenceId()).
                thenReturn(GROUP_APPLICATION_ID);
        when(groupAppMembership.getRoleId()).
            thenReturn("APPLICATION_PRIMARY_OWNER");

        when(application.getId()).
                thenReturn(APPLICATION_ID);
        when(application.getStatus()).
                thenReturn(ApplicationStatus.ACTIVE);
        when(application.getType()).
                thenReturn(ApplicationType.SIMPLE);
        when(groupApplication.getId()).
                thenReturn(GROUP_APPLICATION_ID);
        when(groupApplication.getStatus()).
                thenReturn(ApplicationStatus.ACTIVE);
        when(groupApplication.getType()).
                thenReturn(ApplicationType.SIMPLE);
        
        when(membershipService.getMembershipsByMemberAndReference(MembershipMemberType.USER, USERNAME, MembershipReferenceType.APPLICATION)).
                thenReturn(Collections.singleton(appMembership));
        
        when(membershipService.getMembershipsByMemberAndReference(MembershipMemberType.USER, USERNAME, MembershipReferenceType.GROUP)).
                thenReturn(Collections.singleton(groupAppMembership));
        
        RoleEntity role = mock(RoleEntity.class);
        when(role.getScope()).thenReturn(RoleScope.APPLICATION);
        when(roleService.findByScopeAndName(any(), any())).thenReturn(Optional.of(role));
        when(roleService.findById(any())).thenReturn(role);

        when(applicationRepository.findByIds(any())).
                thenReturn(new HashSet(Arrays.asList(application, groupApplication)));

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

        Set<ApplicationListItem> apps = applicationService.findByUser(USERNAME);

        Assert.assertNotNull(apps);
        Assert.assertFalse("should find apps", apps.isEmpty());
        Assert.assertEquals(2, apps.size());
    }
}
