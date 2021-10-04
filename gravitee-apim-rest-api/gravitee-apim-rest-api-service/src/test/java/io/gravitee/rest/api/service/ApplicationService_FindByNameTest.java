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
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.api.search.ApplicationCriteria;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.application.ApplicationListItem;
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
public class ApplicationService_FindByNameTest {

    @InjectMocks
    private ApplicationServiceImpl applicationService = new ApplicationServiceImpl();

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private MembershipService membershipService;

    @Test
    public void shouldNotFindByNameWhenNull() throws Exception {
        Set<ApplicationListItem> set = applicationService.findByName(null, null);
        assertNotNull(set);
        assertEquals("result is empty", 0, set.size());
        verify(applicationRepository, never()).findByName(any());
    }

    @Test
    public void shouldNotFindByNameWhenEmpty() throws Exception {
        Set<ApplicationListItem> set = applicationService.findByName(null, " ");
        assertNotNull(set);
        assertEquals("result is empty", 0, set.size());
        verify(applicationRepository, never()).findByName(any());
    }

    @Test
    public void shouldNotFindByName() throws Exception {
        when(membershipService.getMembershipsByMemberAndReference(any(), any(), any())).thenReturn(Collections.emptySet());
        when(applicationRepository.search(any(), any())).thenReturn(new Page<>(Collections.emptyList(), 0, 0, 0));
        Set<ApplicationListItem> set = applicationService.findByName(null, "a");
        assertNotNull(set);
        assertEquals("result is empty", 0, set.size());
        ArgumentCaptor<ApplicationCriteria> queryCaptor = ArgumentCaptor.forClass(ApplicationCriteria.class);
        Mockito.verify(applicationRepository).search(queryCaptor.capture(), any());
        final ApplicationCriteria query = queryCaptor.getValue();
        assertEquals("a", query.getName());
    }

    @Test
    public void shouldNotCallSearchWhenUserHasNoMemberships() {
        // mock applications memberships for this user : nothing found
        when(membershipService.getMembershipsByMemberAndReference(MembershipMemberType.USER, "myUser", MembershipReferenceType.APPLICATION))
            .thenReturn(new HashSet<>());

        // call
        Set<ApplicationListItem> resultSet = applicationService.findByName("myUser", "random search");

        // check applicationRepository search has not been called, and so it returns empty
        assertTrue(resultSet.isEmpty());
        verify(applicationRepository, never()).search(any(), any());
    }
}
