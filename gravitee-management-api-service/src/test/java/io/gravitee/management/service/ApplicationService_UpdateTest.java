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
import io.gravitee.management.model.UpdateApplicationEntity;
import io.gravitee.management.model.permissions.SystemRole;
import io.gravitee.management.service.exceptions.ApplicationNotFoundException;
import io.gravitee.management.service.exceptions.ClientIdAlreadyExistsException;
import io.gravitee.management.service.exceptions.TechnicalManagementException;
import io.gravitee.management.service.impl.ApplicationServiceImpl;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.repository.management.model.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.internal.util.collections.Sets;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ApplicationService_UpdateTest {

    private static final String APPLICATION_ID = "id-app";
    private static final String APPLICATION_NAME = "myApplication";
    private static final String USER_NAME = "myUser";
    private static final String CLIENT_ID = "myClientId";

    @InjectMocks
    private ApplicationServiceImpl applicationService = new ApplicationServiceImpl();

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private MembershipRepository membershipRepository;

    @Mock
    private UserService userService;

    @Mock
    private UpdateApplicationEntity existingApplication;

    @Mock
    private Application application;

    @Mock
    private AuditService auditService;

    @Mock
    private SubscriptionService subscriptionService;

    @Test
    public void shouldUpdate() throws TechnicalException {
        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(application));
        when(application.getName()).thenReturn(APPLICATION_NAME);
        when(application.getStatus()).thenReturn(ApplicationStatus.ACTIVE);
        when(existingApplication.getName()).thenReturn(APPLICATION_NAME);
        when(existingApplication.getDescription()).thenReturn("My description");
        when(applicationRepository.update(any())).thenReturn(application);
        Membership po = new Membership(USER_NAME, APPLICATION_ID, MembershipReferenceType.APPLICATION);
        po.setRoles(Collections.singletonMap(RoleScope.APPLICATION.getId(), SystemRole.PRIMARY_OWNER.name()));
        when(membershipRepository.findByReferencesAndRole(any(), any(), eq(RoleScope.APPLICATION), any()))
                .thenReturn(Collections.singleton(po));

        final ApplicationEntity applicationEntity = applicationService.update(APPLICATION_ID, existingApplication);

        verify(applicationRepository).update(argThat(application -> APPLICATION_NAME.equals(application.getName()) &&
            application.getUpdatedAt() != null));

        assertNotNull(applicationEntity);
        assertEquals(APPLICATION_NAME, applicationEntity.getName());
    }

    @Test(expected = ApplicationNotFoundException.class)
    public void shouldNotUpdateBecauseNotFound() throws TechnicalException {
        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.empty());

        applicationService.update(APPLICATION_ID, existingApplication);
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotUpdateBecauseTechnicalException() throws TechnicalException {
        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(application));
        when(applicationRepository.update(any())).thenThrow(TechnicalException.class);

        when(existingApplication.getName()).thenReturn(APPLICATION_NAME);
        when(existingApplication.getDescription()).thenReturn("My description");

        applicationService.update(APPLICATION_ID, existingApplication);
    }

    @Test
    public void shouldUpdateBecauseSameApplication() throws TechnicalException {
        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(application));
        when(application.getId()).thenReturn(APPLICATION_ID);
        when(application.getClientId()).thenReturn(CLIENT_ID);
        when(existingApplication.getClientId()).thenReturn(CLIENT_ID);
        when(application.getName()).thenReturn(APPLICATION_NAME);
        when(application.getStatus()).thenReturn(ApplicationStatus.ACTIVE);
        when(existingApplication.getName()).thenReturn(APPLICATION_NAME);
        when(existingApplication.getDescription()).thenReturn("My description");
        when(applicationRepository.update(any())).thenReturn(application);
        Membership po = new Membership(USER_NAME, APPLICATION_ID, MembershipReferenceType.APPLICATION);
        po.setRoles(Collections.singletonMap(RoleScope.APPLICATION.getId(), SystemRole.PRIMARY_OWNER.name()));
        when(membershipRepository.findByReferencesAndRole(any(), any(), eq(RoleScope.APPLICATION), any()))
                .thenReturn(Collections.singleton(po));

        final ApplicationEntity applicationEntity = applicationService.update(APPLICATION_ID, existingApplication);

        verify(applicationRepository).update(argThat(application -> APPLICATION_NAME.equals(application.getName()) &&
                application.getUpdatedAt() != null));

        assertNotNull(applicationEntity);
        assertEquals(APPLICATION_NAME, applicationEntity.getName());
    }

    @Test (expected = ClientIdAlreadyExistsException.class)
    public void shouldNotUpdateBecauseDifferentApplication() throws TechnicalException {
        Application other = mock(Application.class);
        when(other.getId()).thenReturn("other-app");
        when(other.getClientId()).thenReturn(CLIENT_ID);

        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(application));
        when(applicationRepository.findAll(ApplicationStatus.ACTIVE)).thenReturn(Sets.newSet(other));

        when(application.getId()).thenReturn(APPLICATION_ID);
        when(existingApplication.getClientId()).thenReturn(CLIENT_ID);

        applicationService.update(APPLICATION_ID, existingApplication);
    }
}
