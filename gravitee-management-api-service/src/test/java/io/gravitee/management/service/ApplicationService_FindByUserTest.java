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
package io.gravitee.management.service;

import io.gravitee.management.model.ApplicationEntity;
import io.gravitee.management.model.permissions.SystemRole;
import io.gravitee.repository.management.model.RoleScope;
import io.gravitee.management.model.UserEntity;
import io.gravitee.management.service.impl.ApplicationServiceImpl;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.repository.management.model.Application;
import io.gravitee.repository.management.model.ApplicationStatus;
import io.gravitee.repository.management.model.Membership;
import io.gravitee.repository.management.model.MembershipReferenceType;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
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

    @InjectMocks
    private ApplicationServiceImpl applicationService = new ApplicationServiceImpl();

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private MembershipRepository membershipRepository;

    @Mock
    private Membership appMembership;

    @Mock
    private Membership groupAppMembership;

    @Mock
    private Application application;

    @Mock
    private Application groupApplication;

    @Mock
    private UserService userService;

    @Test
    public void shouldFindByUser() throws Exception {
        when(appMembership.getReferenceId()).
                thenReturn(APPLICATION_ID);
        when(application.getId()).
                thenReturn(APPLICATION_ID);
        when(application.getStatus()).
                thenReturn(ApplicationStatus.ACTIVE);
        when(membershipRepository.findByUserAndReferenceType(USERNAME, MembershipReferenceType.APPLICATION)).
                thenReturn(Collections.singleton(appMembership));
        when(applicationRepository.findByIds(Collections.singletonList(APPLICATION_ID))).
                thenReturn(Collections.singleton(application));
        when(membershipRepository.findByUserAndReferenceType(USERNAME, MembershipReferenceType.GROUP)).
                thenReturn(Collections.emptySet());
        when(applicationRepository.findByGroups(Collections.emptyList(), ApplicationStatus.ACTIVE)).
                thenReturn(Collections.emptySet());
        Membership po = new Membership(USERNAME, APPLICATION_ID, MembershipReferenceType.APPLICATION);
        po.setRoles(Collections.singletonMap(RoleScope.APPLICATION.getId(), SystemRole.PRIMARY_OWNER.name()));
        when(membershipRepository.findByReferencesAndRole(any(), any(), eq(RoleScope.APPLICATION), any()))
                .thenReturn(Collections.singleton(po));
//        when(userService.findByUsername(USERNAME, false)).thenReturn(new UserEntity());

        Set<ApplicationEntity> apps = applicationService.findByUser(USERNAME);

        Assert.assertNotNull(apps);
        Assert.assertFalse("should find app", apps.isEmpty());
        Assert.assertEquals(APPLICATION_ID, apps.iterator().next().getId());
    }

    @Test
    public void shouldNotFindByUserBecauseOfArchived() throws Exception {
        when(appMembership.getReferenceId()).
                thenReturn(APPLICATION_ID);
        when(application.getId()).
                thenReturn(APPLICATION_ID);
        when(application.getStatus()).
                thenReturn(ApplicationStatus.ARCHIVED);
        when(membershipRepository.findByUserAndReferenceType(USERNAME, MembershipReferenceType.APPLICATION)).
                thenReturn(Collections.singleton(appMembership));
        when(applicationRepository.findByIds(Collections.singletonList(APPLICATION_ID))).
                thenReturn(Collections.singleton(application));
        when(membershipRepository.findByUserAndReferenceType(USERNAME, MembershipReferenceType.GROUP)).
                thenReturn(Collections.emptySet());
        when(applicationRepository.findByGroups(Collections.emptyList(), ApplicationStatus.ACTIVE)).
                thenReturn(Collections.emptySet());
        Membership po = new Membership(USERNAME, APPLICATION_ID, MembershipReferenceType.APPLICATION);
        po.setRoles(Collections.singletonMap(RoleScope.APPLICATION.getId(), SystemRole.PRIMARY_OWNER.name()));
        when(membershipRepository.findByReferencesAndRole(any(), any(), eq(RoleScope.APPLICATION), any()))
                .thenReturn(Collections.singleton(po));
//        when(userService.findByUsername(USERNAME, false)).thenReturn(new UserEntity());

        Set<ApplicationEntity> apps = applicationService.findByUser(USERNAME);

        Assert.assertNotNull(apps);
        Assert.assertTrue("should not find app", apps.isEmpty());
    }

    @Test
    public void shouldFindByUserAndGroup() throws Exception {
        when(appMembership.getReferenceId()).
                thenReturn(APPLICATION_ID);
        when(groupAppMembership.getReferenceId()).
                thenReturn(GROUP_APPLICATION_ID);
        when(groupAppMembership.getRoles()).
                thenReturn(Collections.singletonMap(RoleScope.APPLICATION.getId(), "USER"));
        when(application.getId()).
                thenReturn(APPLICATION_ID);
        when(application.getStatus()).
                thenReturn(ApplicationStatus.ACTIVE);
        when(groupApplication.getId()).
                thenReturn(GROUP_APPLICATION_ID);
        when(groupApplication.getStatus()).
                thenReturn(ApplicationStatus.ACTIVE);

        when(membershipRepository.findByUserAndReferenceType(USERNAME, MembershipReferenceType.APPLICATION)).
                thenReturn(Collections.singleton(appMembership));
        when(applicationRepository.findByIds(Collections.singletonList(APPLICATION_ID))).
                thenReturn(Collections.singleton(application));
        when(membershipRepository.findByUserAndReferenceType(USERNAME, MembershipReferenceType.GROUP)).
                thenReturn(Collections.singleton(groupAppMembership));
        when(applicationRepository.findByGroups(Collections.singletonList(GROUP_APPLICATION_ID), ApplicationStatus.ACTIVE)).
                thenReturn(Collections.singleton(groupApplication));
        Membership poApp = new Membership(USERNAME, APPLICATION_ID, MembershipReferenceType.APPLICATION);
        poApp.setRoles(Collections.singletonMap(RoleScope.APPLICATION.getId(), SystemRole.PRIMARY_OWNER.name()));
        Membership poGroupApp = new Membership(USERNAME, GROUP_APPLICATION_ID, MembershipReferenceType.APPLICATION);
        poGroupApp.setRoles(Collections.singletonMap(RoleScope.APPLICATION.getId(), SystemRole.PRIMARY_OWNER.name()));
        Set<Membership> memberships = new HashSet<>();
        memberships.add(poApp);
        memberships.add(poGroupApp);
        when(membershipRepository.findByReferencesAndRole(any(), any(), eq(RoleScope.APPLICATION), any()))
                .thenReturn(memberships);

//        when(userService.findByUsername(USERNAME, false)).thenReturn(new UserEntity());

        Set<ApplicationEntity> apps = applicationService.findByUser(USERNAME);

        Assert.assertNotNull(apps);
        Assert.assertFalse("should find apps", apps.isEmpty());
        Assert.assertEquals(2, apps.size());
    }
}
