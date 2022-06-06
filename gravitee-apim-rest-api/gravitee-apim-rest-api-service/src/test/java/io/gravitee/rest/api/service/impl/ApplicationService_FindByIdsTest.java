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
package io.gravitee.rest.api.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApplicationRepository;
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
import io.gravitee.rest.api.service.impl.ApplicationServiceImpl;
import java.util.Arrays;
import java.util.LinkedHashSet;
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
        when(app1.getEnvironmentId()).thenReturn("DEFAULT");
        when(app1.getStatus()).thenReturn(ApplicationStatus.ACTIVE);
        when(app1.getId()).thenReturn(APPLICATION_IDS.get(0));
        when(app1.getApiKeyMode()).thenReturn(ApiKeyMode.UNSPECIFIED);

        when(app2.getEnvironmentId()).thenReturn("DEFAULT");
        when(app2.getStatus()).thenReturn(ApplicationStatus.ACTIVE);
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
        doReturn(new LinkedHashSet<>(Arrays.asList(app1, app2))).when(applicationRepository).findByIds(APPLICATION_IDS);
        doReturn(2).when(primaryOwners).size();

        final Set<ApplicationListItem> applications = applicationService.findByIds(GraviteeContext.getExecutionContext(), APPLICATION_IDS);

        assertNotNull(applications);
        assertEquals(APPLICATION_IDS, applications.stream().map(ApplicationListItem::getId).collect(Collectors.toList()));
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldThrowsIfNoPrimaryOwner() throws TechnicalException {
        doReturn(new LinkedHashSet<>(Arrays.asList(app1, app2))).when(applicationRepository).findByIds(APPLICATION_IDS);

        final Set<ApplicationListItem> applications = applicationService.findByIds(GraviteeContext.getExecutionContext(), APPLICATION_IDS);

        assertNotNull(applications);
        assertEquals(APPLICATION_IDS, applications.stream().map(ApplicationListItem::getId).collect(Collectors.toList()));
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldThrowTechnicalManagementException() throws TechnicalException {
        when(applicationRepository.findByIds(any())).thenThrow(new TechnicalException());
        applicationService.findByIds(GraviteeContext.getExecutionContext(), APPLICATION_IDS);
    }
}
