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

import static org.mockito.Mockito.*;

import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.api.search.ApplicationCriteria;
import io.gravitee.repository.management.model.Application;
import io.gravitee.repository.management.model.ApplicationStatus;
import io.gravitee.rest.api.model.MembershipEntity;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.Collections;
import java.util.Set;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ApplicationService_FindIdsByUserTest {

    private static final String APPLICATION_ID = "id-app";
    private static final String USERNAME = "user-name";

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
    public void should_find_ids_with_null_user() throws Exception {
        GraviteeContext.setCurrentEnvironment("envId");

        ApplicationCriteria criteria = ApplicationCriteria.builder()
            .environmentIds(Set.of(GraviteeContext.getExecutionContext().getEnvironmentId()))
            .status(ApplicationStatus.ACTIVE)
            .build();
        when(applicationRepository.searchIds(criteria, null)).thenReturn(Set.of(APPLICATION_ID));

        Set<String> apps = applicationService.findIdsByUser(GraviteeContext.getExecutionContext(), null);

        Assert.assertNotNull(apps);
        Assert.assertFalse("should find app", apps.isEmpty());
        Assert.assertEquals(APPLICATION_ID, apps.iterator().next());
    }

    @Test
    public void should_find_ids_with_non_null_user() throws Exception {
        GraviteeContext.setCurrentEnvironment("envId");

        when(
            membershipService.getReferenceIdsByMemberAndReference(MembershipMemberType.USER, USERNAME, MembershipReferenceType.APPLICATION)
        ).thenReturn(Collections.singleton(APPLICATION_ID));

        ApplicationCriteria criteria = ApplicationCriteria.builder()
            .restrictedToIds(Set.of(APPLICATION_ID))
            .environmentIds(Set.of(GraviteeContext.getExecutionContext().getEnvironmentId()))
            .status(ApplicationStatus.ACTIVE)
            .build();

        when(applicationRepository.searchIds(criteria, null)).thenReturn(Set.of(APPLICATION_ID));

        Set<String> apps = applicationService.findIdsByUser(GraviteeContext.getExecutionContext(), USERNAME);

        Assert.assertNotNull(apps);
        Assert.assertFalse("should find app", apps.isEmpty());
        Assert.assertEquals(APPLICATION_ID, apps.iterator().next());
    }
}
