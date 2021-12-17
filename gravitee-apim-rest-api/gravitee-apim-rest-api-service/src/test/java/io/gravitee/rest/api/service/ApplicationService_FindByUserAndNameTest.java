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

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.api.search.ApplicationCriteria;
import io.gravitee.rest.api.model.MembershipEntity;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.application.ApplicationListItem;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.impl.ApplicationServiceImpl;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ApplicationService_FindByUserAndNameTest {

    @InjectMocks
    private ApplicationServiceImpl applicationService = new ApplicationServiceImpl();

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private MembershipService membershipService;

    @Test
    public void shouldNotFindByNameWhenNull() throws Exception {
        Set<ApplicationListItem> set = applicationService.findByUserAndNameAndStatus(
            "myUser",
            false,
            null,
            "ACTIVE",
            GraviteeContext.getCurrentEnvironment(),
            GraviteeContext.getCurrentOrganization()
        );
        assertNotNull(set);
        assertEquals("result is empty", 0, set.size());
        verifyNoInteractions(applicationRepository);
    }

    @Test
    public void shouldNotFindByNameWhenEmpty() throws Exception {
        Set<ApplicationListItem> set = applicationService.findByUserAndNameAndStatus(
            "myUser",
            false,
            "",
            "ACTIVE",
            GraviteeContext.getCurrentEnvironment(),
            GraviteeContext.getCurrentOrganization()
        );
        assertNotNull(set);
        assertEquals("result is empty", 0, set.size());
        verifyNoInteractions(applicationRepository);
    }

    @Test
    public void shouldNotFindByName() throws Exception {
        when(applicationRepository.search(any(), any())).thenReturn(new Page<>(Collections.emptyList(), 0, 0, 0));
        Set<ApplicationListItem> set = applicationService.findByUserAndNameAndStatus(
            "myUser",
            true,
            "a",
            "ACTIVE",
            GraviteeContext.getCurrentEnvironment(),
            GraviteeContext.getCurrentOrganization()
        );
        assertNotNull(set);
        assertEquals("result is empty", 0, set.size());
        ArgumentCaptor<ApplicationCriteria> queryCaptor = ArgumentCaptor.forClass(ApplicationCriteria.class);
        Mockito.verify(applicationRepository).search(queryCaptor.capture(), any());
        final ApplicationCriteria query = queryCaptor.getValue();
        assertEquals("a", query.getName());
    }

    @Test
    public void shouldNotCallSearchWhenUserHasNoMemberships() throws TechnicalException {
        // mock applications memberships for this user : nothing found
        when(membershipService.getMembershipsByMemberAndReference(MembershipMemberType.USER, "myUser", MembershipReferenceType.APPLICATION))
            .thenReturn(new HashSet<>());

        // call
        Set<ApplicationListItem> resultSet = applicationService.findByUserAndNameAndStatus(
            "myUser",
            false,
            "random search",
            "ACTIVE",
            GraviteeContext.getCurrentEnvironment(),
            GraviteeContext.getCurrentOrganization()
        );

        // check applicationRepository search has not been called, and so it returns empty
        assertTrue(resultSet.isEmpty());
        verify(applicationRepository, never()).search(any(), any());
    }

    @Test
    public void shouldNotRetrieveMembershipsWhenUserIdAdminAndFindWithoutApplicationId() throws TechnicalException {
        when(applicationRepository.search(any(), any())).thenReturn(new Page<>(Collections.emptyList(), 0, 0, 0));

        // call
        applicationService.findByUserAndNameAndStatus(
            "myUser",
            true,
            "random search",
            "ACTIVE",
            GraviteeContext.getCurrentEnvironment(),
            GraviteeContext.getCurrentOrganization()
        );

        // check membership hasn't been retrieved
        verify(membershipService, never()).getMembershipsByMemberAndReference(any(), any(), any());

        // check applicationRepository search has been called without application
        ArgumentCaptor<ApplicationCriteria> queryCaptor = ArgumentCaptor.forClass(ApplicationCriteria.class);
        Mockito.verify(applicationRepository).search(queryCaptor.capture(), any());
        assertTrue(queryCaptor.getValue().getIds().isEmpty());
    }

    @Test
    public void shouldRetrieveMembershipsAndFindWithApplicationsId() throws TechnicalException {
        when(applicationRepository.search(any(), any())).thenReturn(new Page<>(Collections.emptyList(), 0, 0, 0));

        // mock applications memberships for this user : found applications "myApplicationId1" and "myApplicationId2"
        MembershipEntity membership1 = new MembershipEntity();
        membership1.setId("m1");
        membership1.setReferenceId("myApplicationId1");
        MembershipEntity membership2 = new MembershipEntity();
        membership2.setId("m2");
        membership2.setReferenceId("myApplicationId2");
        Set<MembershipEntity> memberships = new HashSet<>();
        memberships.add(membership1);
        memberships.add(membership2);
        when(membershipService.getMembershipsByMemberAndReference(MembershipMemberType.USER, "myUser", MembershipReferenceType.APPLICATION))
            .thenReturn(memberships);

        // call
        applicationService.findByUserAndNameAndStatus(
            "myUser",
            false,
            "random search",
            "ACTIVE",
            GraviteeContext.getCurrentEnvironment(),
            GraviteeContext.getCurrentOrganization()
        );

        // check applicationRepository search has been called with applications
        ArgumentCaptor<ApplicationCriteria> queryCaptor = ArgumentCaptor.forClass(ApplicationCriteria.class);
        Mockito.verify(applicationRepository).search(queryCaptor.capture(), any());
        assertEquals(2, queryCaptor.getValue().getIds().size());
        assertEquals("myApplicationId1", queryCaptor.getValue().getIds().get(0));
        assertEquals("myApplicationId2", queryCaptor.getValue().getIds().get(1));
    }
}
