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

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.google.common.collect.Sets;
import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.api.search.ApplicationCriteria;
import io.gravitee.repository.management.model.ApiKeyMode;
import io.gravitee.repository.management.model.Application;
import io.gravitee.repository.management.model.ApplicationStatus;
import io.gravitee.rest.api.model.MembershipEntity;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.application.ApplicationListItem;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.configuration.application.ClientRegistrationService;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ApplicationService_FindByIdsTest {

    private static final List<String> APPLICATION_IDS = Arrays.asList("id-app-1", "id-app-2");

    @InjectMocks
    private ApplicationServiceImpl applicationService = new ApplicationServiceImpl();

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private MembershipService membershipService;

    @Mock
    private UserService userService;

    @Mock
    private RoleService roleService;

    @Mock
    private RoleEntity primaryOwnerRole;

    @Mock
    private Set<MembershipEntity> primaryOwners;

    @Mock
    private Application app1;

    @Mock
    private Application app2;

    @Mock
    private ClientRegistrationService clientRegistrationService;

    @Before
    public void setUp() {
        GraviteeContext.setCurrentOrganization("DEFAULT");
        GraviteeContext.setCurrentEnvironment("DEFAULT");
        // One application is active
        when(app1.getStatus()).thenReturn(ApplicationStatus.ACTIVE);
        when(app1.getId()).thenReturn(APPLICATION_IDS.get(0));
        when(app1.getApiKeyMode()).thenReturn(ApiKeyMode.UNSPECIFIED);
        // One application is archived/deleted
        when(app2.getStatus()).thenReturn(ApplicationStatus.ARCHIVED);
        when(app2.getId()).thenReturn(APPLICATION_IDS.get(1));
        when(app2.getApiKeyMode()).thenReturn(ApiKeyMode.UNSPECIFIED);

        doReturn(primaryOwnerRole)
            .when(roleService)
            .findPrimaryOwnerRoleByOrganization(GraviteeContext.getCurrentOrganization(), RoleScope.APPLICATION);

        doReturn("role-id").when(primaryOwnerRole).getId();

        when(membershipService.getMembershipsByReferencesAndRole(any(), any(), any())).thenReturn(primaryOwners);
    }

    @After
    public void tearDown() {
        GraviteeContext.cleanContext();
    }

    @Test
    public void shouldFindByIds() throws TechnicalException {
        ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        ApplicationCriteria criteria = ApplicationCriteria
            .builder()
            .restrictedToIds(Sets.newHashSet(APPLICATION_IDS))
            .environmentIds(Set.of(executionContext.getEnvironmentId()))
            .build();
        // Should return both the applications by Ids irrespective of status
        doReturn(new Page(Arrays.asList(app1, app2), 1, 2, 2)).when(applicationRepository).search(criteria, null);
        doReturn(2).when(primaryOwners).size();

        final Set<ApplicationListItem> applications = applicationService.findByIds(executionContext, APPLICATION_IDS);

        assertNotNull(applications);
        assertEquals(APPLICATION_IDS, applications.stream().map(ApplicationListItem::getId).collect(Collectors.toList()));
    }

    @Test
    public void shouldFindByIdsWithDuplicatedIdsAndStatus() throws TechnicalException {
        ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        ApplicationCriteria criteria = ApplicationCriteria
            .builder()
            .restrictedToIds(Sets.newHashSet(APPLICATION_IDS))
            .environmentIds(Set.of(executionContext.getEnvironmentId()))
            .build();

        doReturn(new Page<>(Arrays.asList(app1, app2), 1, 2, 2)).when(applicationRepository).search(criteria, null);
        doReturn(2).when(primaryOwners).size();

        final Set<ApplicationListItem> applications = applicationService.findByIds(
            executionContext,
            List.of("id-app-1", "id-app-1", "id-app-2")
        );

        assertNotNull(applications);
        assertEquals(APPLICATION_IDS, applications.stream().map(ApplicationListItem::getId).collect(Collectors.toList()));
    }

    @Test
    public void shouldFindByIdsWithNoEnvironmentCriteria() throws TechnicalException {
        ExecutionContext executionContext = new ExecutionContext("DEFAULT", null);
        ApplicationCriteria criteria = ApplicationCriteria.builder().restrictedToIds(Sets.newHashSet(APPLICATION_IDS)).build();
        doReturn(new Page(Arrays.asList(app1, app2), 1, 2, 2)).when(applicationRepository).search(criteria, null);
        doReturn(2).when(primaryOwners).size();

        final Set<ApplicationListItem> applications = applicationService.findByIds(executionContext, APPLICATION_IDS);

        assertNotNull(applications);
        assertEquals(APPLICATION_IDS, applications.stream().map(ApplicationListItem::getId).collect(Collectors.toList()));
    }

    @Test
    public void shouldFindByIdsWithEmptySet() throws TechnicalException {
        ExecutionContext executionContext = GraviteeContext.getExecutionContext();

        final Set<ApplicationListItem> applications = applicationService.findByIds(executionContext, Collections.emptySet());

        assertNotNull(applications);
        assertTrue(applications.isEmpty());
        verify(applicationRepository, times(0)).search(any(), any());
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldThrowsIfNoPrimaryOwner() throws TechnicalException {
        ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        ApplicationCriteria criteria = ApplicationCriteria
            .builder()
            .restrictedToIds(Sets.newHashSet(APPLICATION_IDS))
            .environmentIds(Set.of(executionContext.getEnvironmentId()))
            .build();
        doReturn(new Page(Arrays.asList(app1, app2), 1, 2, 2)).when(applicationRepository).search(criteria, null);

        final Set<ApplicationListItem> applications = applicationService.findByIds(GraviteeContext.getExecutionContext(), APPLICATION_IDS);

        assertNotNull(applications);
        assertEquals(APPLICATION_IDS, applications.stream().map(ApplicationListItem::getId).collect(Collectors.toList()));
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldThrowTechnicalManagementException() throws TechnicalException {
        when(applicationRepository.search(any(), any())).thenThrow(new TechnicalException());
        applicationService.findByIds(GraviteeContext.getExecutionContext(), APPLICATION_IDS);
    }
}
