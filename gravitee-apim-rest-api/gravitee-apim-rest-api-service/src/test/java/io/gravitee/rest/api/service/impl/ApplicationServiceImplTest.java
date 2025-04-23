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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import io.gravitee.repository.management.api.search.ApplicationCriteria;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.application.ApplicationQuery;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ApplicationServiceImplTest {

    @InjectMocks
    private final ApplicationServiceImpl applicationService = new ApplicationServiceImpl();

    @Mock
    private MembershipService membershipService;

    @Test
    public void buildSearchCriteria_userAndIds() {
        ExecutionContext executionContext = new ExecutionContext("org1", "env1");
        ApplicationQuery query = ApplicationQuery.builder().user("user1").ids(Set.of("app1")).build();
        when(membershipService.getReferenceIdsByMemberAndReference(MembershipMemberType.USER, "user1", MembershipReferenceType.APPLICATION))
            .thenReturn(Set.of("app1", "app2"));
        when(membershipService.getMembershipsByMemberAndReference(MembershipMemberType.USER, "user1", MembershipReferenceType.GROUP))
            .thenReturn(Set.of());
        ApplicationCriteria criteria = applicationService.buildSearchCriteria(executionContext, query);
        assertThat(criteria.getEnvironmentIds().size()).isEqualTo(1);
        assertTrue(criteria.getEnvironmentIds().contains("env1"));
        assertThat(criteria.getRestrictedToIds().size()).isEqualTo(1);
        assertTrue(criteria.getRestrictedToIds().contains("app1"));
    }

    @Test
    public void buildSearchCriteria_userAndIds_noAuthorizedApps() {
        ExecutionContext executionContext = new ExecutionContext("org1", "env1");
        ApplicationQuery query = ApplicationQuery.builder().user("user1").ids(Set.of("app3")).build();
        when(membershipService.getReferenceIdsByMemberAndReference(MembershipMemberType.USER, "user1", MembershipReferenceType.APPLICATION))
            .thenReturn(Set.of("app1", "app2"));
        when(membershipService.getMembershipsByMemberAndReference(MembershipMemberType.USER, "user1", MembershipReferenceType.GROUP))
            .thenReturn(Set.of());
        ApplicationCriteria criteria = applicationService.buildSearchCriteria(executionContext, query);
        assertNull(criteria);
    }
}
