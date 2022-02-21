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

import static io.gravitee.repository.management.model.ApiKeyMode.SHARED;
import static io.gravitee.rest.api.model.ApiKeyMode.UNSPECIFIED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.model.ApiKeyMode;
import io.gravitee.repository.management.model.Application;
import io.gravitee.repository.management.model.ApplicationStatus;
import io.gravitee.repository.management.model.ApplicationType;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.application.ApplicationSettings;
import io.gravitee.rest.api.model.application.SimpleApplicationSettings;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.configuration.application.ClientRegistrationService;
import io.gravitee.rest.api.service.converter.ApplicationConverter;
import io.gravitee.rest.api.service.exceptions.ApplicationNotFoundException;
import io.gravitee.rest.api.service.exceptions.ClientIdAlreadyExistsException;
import io.gravitee.rest.api.service.exceptions.InvalidApplicationApiKeyModeException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.impl.ApplicationServiceImpl;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.internal.util.collections.Sets;
import org.mockito.junit.MockitoJUnitRunner;

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
    private MembershipService membershipService;

    @Mock
    private RoleService roleService;

    @Mock
    private UserService userService;

    @Mock
    private UpdateApplicationEntity updateApplication;

    @Mock
    private Application existingApplication;

    @Mock
    private AuditService auditService;

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private ParameterService parameterService;

    @Mock
    private ClientRegistrationService clientRegistrationService;

    @Mock
    private ApplicationConverter applicationConverter;

    @Test
    public void shouldUpdate() throws TechnicalException {
        ApplicationSettings settings = new ApplicationSettings();
        SimpleApplicationSettings clientSettings = new SimpleApplicationSettings();
        clientSettings.setClientId(CLIENT_ID);
        settings.setApp(clientSettings);

        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(existingApplication));
        when(existingApplication.getName()).thenReturn(APPLICATION_NAME);
        when(existingApplication.getStatus()).thenReturn(ApplicationStatus.ACTIVE);
        when(existingApplication.getType()).thenReturn(ApplicationType.SIMPLE);
        when(existingApplication.getApiKeyMode()).thenReturn(ApiKeyMode.UNSPECIFIED);

        when(updateApplication.getSettings()).thenReturn(settings);
        when(updateApplication.getName()).thenReturn(APPLICATION_NAME);
        when(updateApplication.getDescription()).thenReturn("My description");
        when(updateApplication.getApiKeyMode()).thenReturn(io.gravitee.rest.api.model.ApiKeyMode.SHARED);

        when(applicationRepository.update(any())).thenReturn(existingApplication);

        when(roleService.findPrimaryOwnerRoleByOrganization(any(), any())).thenReturn(mock(RoleEntity.class));

        MembershipEntity po = new MembershipEntity();
        po.setMemberId(USER_NAME);
        po.setMemberType(MembershipMemberType.USER);
        po.setReferenceId(APPLICATION_ID);
        po.setReferenceType(MembershipReferenceType.APPLICATION);
        po.setRoleId("APPLICATION_PRIMARY_OWNER");
        when(membershipService.getMembershipsByReferencesAndRole(any(), any(), any())).thenReturn(Collections.singleton(po));
        when(applicationConverter.toApplication(any(UpdateApplicationEntity.class))).thenCallRealMethod();

        final ApplicationEntity applicationEntity = applicationService.update(
            GraviteeContext.getCurrentOrganization(),
            GraviteeContext.getCurrentEnvironment(),
            APPLICATION_ID,
            updateApplication
        );

        verify(applicationRepository)
            .update(argThat(application -> APPLICATION_NAME.equals(application.getName()) && application.getUpdatedAt() != null));

        assertNotNull(applicationEntity);
        assertEquals(APPLICATION_NAME, applicationEntity.getName());
    }

    @Test(expected = ApplicationNotFoundException.class)
    public void shouldNotUpdateBecauseNotFound() throws TechnicalException {
        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.empty());

        applicationService.update(
            GraviteeContext.getCurrentOrganization(),
            GraviteeContext.getCurrentEnvironment(),
            APPLICATION_ID,
            updateApplication
        );
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotUpdateBecauseTechnicalException() throws TechnicalException {
        ApplicationSettings settings = new ApplicationSettings();
        SimpleApplicationSettings clientSettings = new SimpleApplicationSettings();
        clientSettings.setClientId(CLIENT_ID);
        settings.setApp(clientSettings);
        when(updateApplication.getSettings()).thenReturn(settings);
        when(existingApplication.getType()).thenReturn(ApplicationType.SIMPLE);
        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(existingApplication));
        when(applicationRepository.update(any())).thenThrow(TechnicalException.class);
        when(applicationConverter.toApplication(any(UpdateApplicationEntity.class))).thenCallRealMethod();

        when(updateApplication.getName()).thenReturn(APPLICATION_NAME);
        when(updateApplication.getDescription()).thenReturn("My description");

        applicationService.update(
            GraviteeContext.getCurrentOrganization(),
            GraviteeContext.getCurrentEnvironment(),
            APPLICATION_ID,
            updateApplication
        );
    }

    @Test
    public void shouldUpdateBecauseSameApplication() throws TechnicalException {
        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(existingApplication));
        when(existingApplication.getId()).thenReturn(APPLICATION_ID);

        Map<String, String> metadata = new HashMap<>();
        metadata.put("client_id", CLIENT_ID);
        when(existingApplication.getMetadata()).thenReturn(metadata);

        ApplicationSettings settings = new ApplicationSettings();
        SimpleApplicationSettings clientSettings = new SimpleApplicationSettings();
        clientSettings.setClientId(CLIENT_ID);
        settings.setApp(clientSettings);
        when(updateApplication.getSettings()).thenReturn(settings);

        when(updateApplication.getName()).thenReturn(APPLICATION_NAME);
        when(existingApplication.getName()).thenReturn(APPLICATION_NAME);
        when(existingApplication.getStatus()).thenReturn(ApplicationStatus.ACTIVE);
        when(existingApplication.getType()).thenReturn(ApplicationType.SIMPLE);
        when(existingApplication.getApiKeyMode()).thenReturn(ApiKeyMode.UNSPECIFIED);
        when(updateApplication.getName()).thenReturn(APPLICATION_NAME);
        when(updateApplication.getDescription()).thenReturn("My description");
        when(applicationRepository.update(any())).thenReturn(existingApplication);
        when(applicationConverter.toApplication(any(UpdateApplicationEntity.class))).thenCallRealMethod();

        when(roleService.findPrimaryOwnerRoleByOrganization(any(), any())).thenReturn(mock(RoleEntity.class));

        MembershipEntity po = new MembershipEntity();
        po.setMemberId(USER_NAME);
        po.setMemberType(MembershipMemberType.USER);
        po.setReferenceId(APPLICATION_ID);
        po.setReferenceType(MembershipReferenceType.APPLICATION);
        po.setRoleId("APPLICATION_PRIMARY_OWNER");
        when(membershipService.getMembershipsByReferencesAndRole(any(), any(), any())).thenReturn(Collections.singleton(po));

        final ApplicationEntity applicationEntity = applicationService.update(
            GraviteeContext.getCurrentOrganization(),
            GraviteeContext.getCurrentEnvironment(),
            APPLICATION_ID,
            updateApplication
        );

        verify(applicationRepository)
            .update(argThat(application -> APPLICATION_NAME.equals(application.getName()) && application.getUpdatedAt() != null));

        assertNotNull(applicationEntity);
        assertEquals(APPLICATION_NAME, applicationEntity.getName());
    }

    @Test(expected = ClientIdAlreadyExistsException.class)
    public void shouldNotUpdateBecauseDifferentApplication() throws TechnicalException {
        Application other = mock(Application.class);
        when(other.getId()).thenReturn("other-app");

        Map<String, String> metadata = new HashMap<>();
        metadata.put("client_id", CLIENT_ID);
        when(other.getMetadata()).thenReturn(metadata);

        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(existingApplication));
        when(existingApplication.getType()).thenReturn(ApplicationType.SIMPLE);
        when(applicationRepository.findAllByEnvironment("DEFAULT", ApplicationStatus.ACTIVE)).thenReturn(Sets.newSet(other));

        when(existingApplication.getId()).thenReturn(APPLICATION_ID);

        ApplicationSettings settings = new ApplicationSettings();
        SimpleApplicationSettings clientSettings = new SimpleApplicationSettings();
        clientSettings.setClientId(CLIENT_ID);
        settings.setApp(clientSettings);

        when(updateApplication.getSettings()).thenReturn(settings);

        applicationService.update(
            GraviteeContext.getCurrentOrganization(),
            GraviteeContext.getCurrentEnvironment(),
            APPLICATION_ID,
            updateApplication
        );
    }

    @Test(expected = InvalidApplicationApiKeyModeException.class)
    public void should_throw_exception_trying_to_update_apiKeyMode_shared() throws TechnicalException {
        // existing application has a SHARED api key mode
        when(existingApplication.getApiKeyMode()).thenReturn(SHARED);
        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(existingApplication));

        // updated application has a UNSPECIFIED api key mode
        when(updateApplication.getApiKeyMode()).thenReturn(UNSPECIFIED);
        when(updateApplication.getSettings()).thenReturn(new ApplicationSettings());
        updateApplication.getSettings().setApp(new SimpleApplicationSettings());

        // this should throw exception cause API key mode update is forbidden
        applicationService.update(
            GraviteeContext.getCurrentOrganization(),
            GraviteeContext.getCurrentEnvironment(),
            APPLICATION_ID,
            updateApplication
        );
    }
}
