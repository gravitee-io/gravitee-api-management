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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.model.Application;
import io.gravitee.repository.management.model.ApplicationStatus;
import io.gravitee.repository.management.model.ApplicationType;
import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.model.NewApplicationEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.application.ApplicationSettings;
import io.gravitee.rest.api.model.application.OAuthClientSettings;
import io.gravitee.rest.api.model.application.SimpleApplicationSettings;
import io.gravitee.rest.api.model.configuration.application.ApplicationGrantTypeEntity;
import io.gravitee.rest.api.model.configuration.application.ApplicationTypeEntity;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.configuration.application.ApplicationTypeService;
import io.gravitee.rest.api.service.exceptions.*;
import io.gravitee.rest.api.service.impl.ApplicationServiceImpl;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ApplicationService_CreateTest {

    private static final String APPLICATION_NAME = "myApplication";
    private static final String CLIENT_ID = "myClientId";
    private static final String USER_NAME = "myUser";

    @InjectMocks
    private ApplicationServiceImpl applicationService = new ApplicationServiceImpl();

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private MembershipService membershipService;

    @Mock
    private UserService userService;

    @Mock
    private GroupService groupService;

    @Mock
    private NewApplicationEntity newApplication;

    @Mock
    private Application application;

    @Mock
    private AuditService auditService;

    @Mock
    private ParameterService parameterService;

    @Mock
    private ApplicationTypeService applicationTypeService;

    @Before
    public void setup() {
        GraviteeContext.cleanContext();
    }

    @Test
    public void shouldCreateForUser() throws TechnicalException {
        ApplicationSettings settings = new ApplicationSettings();
        SimpleApplicationSettings clientSettings = new SimpleApplicationSettings();
        clientSettings.setClientId(CLIENT_ID);
        settings.setApp(clientSettings);
        when(newApplication.getSettings()).thenReturn(settings);
        when(application.getName()).thenReturn(APPLICATION_NAME);
        when(application.getType()).thenReturn(ApplicationType.SIMPLE);
        when(application.getStatus()).thenReturn(ApplicationStatus.ACTIVE);
        when(applicationRepository.create(any())).thenReturn(application);
        when(newApplication.getName()).thenReturn(APPLICATION_NAME);
        when(newApplication.getDescription()).thenReturn("My description");
        when(groupService.findByEvent(eq(GraviteeContext.getCurrentEnvironment()), any())).thenReturn(Collections.emptySet());
        when(userService.findById(any())).thenReturn(mock(UserEntity.class));

        final ApplicationEntity applicationEntity = applicationService.create(
            GraviteeContext.getCurrentEnvironment(),
            newApplication,
            USER_NAME
        );

        assertNotNull(applicationEntity);
        assertEquals(APPLICATION_NAME, applicationEntity.getName());
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotCreateBecauseClientRegistrationDisable() throws TechnicalException {
        ApplicationSettings settings = new ApplicationSettings();
        OAuthClientSettings clientSettings = new OAuthClientSettings();
        clientSettings.setClientId(CLIENT_ID);
        settings.setoAuthClient(clientSettings);
        when(newApplication.getSettings()).thenReturn(settings);

        when(parameterService.findAsBoolean(Key.APPLICATION_REGISTRATION_ENABLED, "DEFAULT", ParameterReferenceType.ENVIRONMENT))
            .thenReturn(Boolean.FALSE);

        applicationService.create(GraviteeContext.getCurrentEnvironment(), newApplication, USER_NAME);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotCreateBecauseAppTypeIsNotAllowed() {
        ApplicationSettings settings = new ApplicationSettings();
        OAuthClientSettings clientSettings = new OAuthClientSettings();
        clientSettings.setApplicationType("web");
        settings.setoAuthClient(clientSettings);
        when(newApplication.getSettings()).thenReturn(settings);
        when(parameterService.findAsBoolean(Key.APPLICATION_REGISTRATION_ENABLED, "DEFAULT", ParameterReferenceType.ENVIRONMENT))
            .thenReturn(Boolean.TRUE);
        applicationService.create(GraviteeContext.getCurrentEnvironment(), newApplication, USER_NAME);
    }

    @Test(expected = ApplicationGrantTypesNotFoundException.class)
    public void shouldNotCreateBecauseGrantTypesIsEmpty() {
        ApplicationSettings settings = new ApplicationSettings();
        OAuthClientSettings clientSettings = new OAuthClientSettings();
        clientSettings.setApplicationType("web");
        settings.setoAuthClient(clientSettings);
        when(newApplication.getSettings()).thenReturn(settings);
        when(parameterService.findAsBoolean(Key.APPLICATION_REGISTRATION_ENABLED, "DEFAULT", ParameterReferenceType.ENVIRONMENT))
            .thenReturn(Boolean.TRUE);
        when(parameterService.findAsBoolean(Key.APPLICATION_TYPE_WEB_ENABLED, "DEFAULT", ParameterReferenceType.ENVIRONMENT))
            .thenReturn(Boolean.TRUE);
        applicationService.create(GraviteeContext.getCurrentEnvironment(), newApplication, USER_NAME);
    }

    @Test(expected = ApplicationGrantTypesNotAllowedException.class)
    public void shouldNotCreateBecauseGrantTypesIsNotAllowed() {
        ApplicationSettings settings = new ApplicationSettings();
        OAuthClientSettings clientSettings = new OAuthClientSettings();
        clientSettings.setApplicationType("web");
        clientSettings.setGrantTypes(Arrays.asList("foobar"));
        settings.setoAuthClient(clientSettings);
        ApplicationTypeEntity applicationType = mock(ApplicationTypeEntity.class);
        when(applicationTypeService.getApplicationType(any())).thenReturn(applicationType);
        when(newApplication.getSettings()).thenReturn(settings);
        when(parameterService.findAsBoolean(Key.APPLICATION_REGISTRATION_ENABLED, "DEFAULT", ParameterReferenceType.ENVIRONMENT))
            .thenReturn(Boolean.TRUE);
        when(parameterService.findAsBoolean(Key.APPLICATION_TYPE_WEB_ENABLED, "DEFAULT", ParameterReferenceType.ENVIRONMENT))
            .thenReturn(Boolean.TRUE);
        applicationService.create(GraviteeContext.getCurrentEnvironment(), newApplication, USER_NAME);
    }

    @Test(expected = ApplicationRedirectUrisNotFound.class)
    public void shouldNotCreateBecauseRedirectURIsNotFound() {
        ApplicationSettings settings = new ApplicationSettings();
        OAuthClientSettings clientSettings = new OAuthClientSettings();
        clientSettings.setApplicationType("web");
        clientSettings.setGrantTypes(Arrays.asList("foobar"));
        settings.setoAuthClient(clientSettings);
        ApplicationTypeEntity applicationType = mock(ApplicationTypeEntity.class);
        ApplicationGrantTypeEntity foobar = new ApplicationGrantTypeEntity();
        foobar.setType("foobar");
        when(applicationType.getRequires_redirect_uris()).thenReturn(true);
        when(applicationType.getAllowed_grant_types()).thenReturn(Arrays.asList(foobar));
        when(applicationTypeService.getApplicationType(any())).thenReturn(applicationType);
        when(newApplication.getSettings()).thenReturn(settings);
        when(parameterService.findAsBoolean(Key.APPLICATION_REGISTRATION_ENABLED, "DEFAULT", ParameterReferenceType.ENVIRONMENT))
            .thenReturn(Boolean.TRUE);
        when(parameterService.findAsBoolean(Key.APPLICATION_TYPE_WEB_ENABLED, "DEFAULT", ParameterReferenceType.ENVIRONMENT))
            .thenReturn(Boolean.TRUE);
        applicationService.create(GraviteeContext.getCurrentEnvironment(), newApplication, USER_NAME);
    }

    @Test(expected = ClientIdAlreadyExistsException.class)
    public void shouldNotCreateBecauseClientIdExists() throws TechnicalException {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("client_id", CLIENT_ID);
        when(application.getMetadata()).thenReturn(metadata);

        ApplicationSettings settings = new ApplicationSettings();
        SimpleApplicationSettings clientSettings = new SimpleApplicationSettings();
        clientSettings.setClientId(CLIENT_ID);
        settings.setApp(clientSettings);
        when(newApplication.getSettings()).thenReturn(settings);

        when(applicationRepository.findAllByEnvironment("DEFAULT", ApplicationStatus.ACTIVE))
            .thenReturn(Collections.singleton(application));

        applicationService.create(GraviteeContext.getCurrentEnvironment(), newApplication, USER_NAME);
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotCreateForUserBecauseTechnicalException() throws TechnicalException {
        ApplicationSettings settings = new ApplicationSettings();
        SimpleApplicationSettings clientSettings = new SimpleApplicationSettings();
        clientSettings.setClientId(CLIENT_ID);
        settings.setApp(clientSettings);
        when(newApplication.getSettings()).thenReturn(settings);

        when(applicationRepository.findAllByEnvironment("DEFAULT", ApplicationStatus.ACTIVE)).thenThrow(TechnicalException.class);

        applicationService.create(GraviteeContext.getCurrentEnvironment(), newApplication, USER_NAME);
    }
}
