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

import static io.gravitee.repository.management.model.Application.METADATA_CLIENT_ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.model.ApiKeyMode;
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
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.configuration.application.ApplicationTypeService;
import io.gravitee.rest.api.service.configuration.application.ClientRegistrationService;
import io.gravitee.rest.api.service.converter.ApplicationConverter;
import io.gravitee.rest.api.service.exceptions.*;
import io.gravitee.rest.api.service.impl.configuration.application.registration.client.register.ClientRegistrationResponse;
import java.util.*;
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

    private static final ExecutionContext EXECUTION_CONTEXT = new ExecutionContext("DEFAULT", "DEFAULT");

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

    @Mock
    private ApplicationConverter applicationConverter;

    @Mock
    private ClientRegistrationService clientRegistrationService;

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
        when(application.getApiKeyMode()).thenReturn(ApiKeyMode.UNSPECIFIED);
        when(application.getStatus()).thenReturn(ApplicationStatus.ACTIVE);
        when(applicationRepository.create(any())).thenReturn(application);
        when(newApplication.getName()).thenReturn(APPLICATION_NAME);
        when(newApplication.getDescription()).thenReturn("My description");
        when(groupService.findByEvent(eq(GraviteeContext.getCurrentEnvironment()), any())).thenReturn(Collections.emptySet());
        when(userService.findById(eq(GraviteeContext.getExecutionContext()), any())).thenReturn(mock(UserEntity.class));
        when(applicationConverter.toApplication(any(NewApplicationEntity.class))).thenCallRealMethod();

        final ApplicationEntity applicationEntity = applicationService.create(
            GraviteeContext.getExecutionContext(),
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

        when(
            parameterService.findAsBoolean(
                GraviteeContext.getExecutionContext(),
                Key.APPLICATION_REGISTRATION_ENABLED,
                "DEFAULT",
                ParameterReferenceType.ENVIRONMENT
            )
        )
            .thenReturn(Boolean.FALSE);

        applicationService.create(GraviteeContext.getExecutionContext(), newApplication, USER_NAME);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotCreateBecauseAppTypeIsNotAllowed() {
        ApplicationSettings settings = new ApplicationSettings();
        OAuthClientSettings clientSettings = new OAuthClientSettings();
        clientSettings.setApplicationType("web");
        settings.setoAuthClient(clientSettings);
        when(newApplication.getSettings()).thenReturn(settings);
        when(
            parameterService.findAsBoolean(
                GraviteeContext.getExecutionContext(),
                Key.APPLICATION_REGISTRATION_ENABLED,
                "DEFAULT",
                ParameterReferenceType.ENVIRONMENT
            )
        )
            .thenReturn(Boolean.TRUE);
        applicationService.create(GraviteeContext.getExecutionContext(), newApplication, USER_NAME);
    }

    @Test(expected = ApplicationGrantTypesNotFoundException.class)
    public void shouldNotCreateBecauseGrantTypesIsEmpty() {
        ApplicationSettings settings = new ApplicationSettings();
        OAuthClientSettings clientSettings = new OAuthClientSettings();
        clientSettings.setApplicationType("web");
        settings.setoAuthClient(clientSettings);
        when(newApplication.getSettings()).thenReturn(settings);
        when(
            parameterService.findAsBoolean(
                GraviteeContext.getExecutionContext(),
                Key.APPLICATION_REGISTRATION_ENABLED,
                "DEFAULT",
                ParameterReferenceType.ENVIRONMENT
            )
        )
            .thenReturn(Boolean.TRUE);
        when(
            parameterService.findAsBoolean(
                GraviteeContext.getExecutionContext(),
                Key.APPLICATION_TYPE_WEB_ENABLED,
                "DEFAULT",
                ParameterReferenceType.ENVIRONMENT
            )
        )
            .thenReturn(Boolean.TRUE);
        applicationService.create(GraviteeContext.getExecutionContext(), newApplication, USER_NAME);
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
        when(
            parameterService.findAsBoolean(
                GraviteeContext.getExecutionContext(),
                Key.APPLICATION_REGISTRATION_ENABLED,
                "DEFAULT",
                ParameterReferenceType.ENVIRONMENT
            )
        )
            .thenReturn(Boolean.TRUE);
        when(
            parameterService.findAsBoolean(
                GraviteeContext.getExecutionContext(),
                Key.APPLICATION_TYPE_WEB_ENABLED,
                "DEFAULT",
                ParameterReferenceType.ENVIRONMENT
            )
        )
            .thenReturn(Boolean.TRUE);
        applicationService.create(GraviteeContext.getExecutionContext(), newApplication, USER_NAME);
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
        when(
            parameterService.findAsBoolean(
                GraviteeContext.getExecutionContext(),
                Key.APPLICATION_REGISTRATION_ENABLED,
                "DEFAULT",
                ParameterReferenceType.ENVIRONMENT
            )
        )
            .thenReturn(Boolean.TRUE);
        when(
            parameterService.findAsBoolean(
                GraviteeContext.getExecutionContext(),
                Key.APPLICATION_TYPE_WEB_ENABLED,
                "DEFAULT",
                ParameterReferenceType.ENVIRONMENT
            )
        )
            .thenReturn(Boolean.TRUE);
        applicationService.create(GraviteeContext.getExecutionContext(), newApplication, USER_NAME);
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

        applicationService.create(GraviteeContext.getExecutionContext(), newApplication, USER_NAME);
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotCreateForUserBecauseTechnicalException() throws TechnicalException {
        ApplicationSettings settings = new ApplicationSettings();
        SimpleApplicationSettings clientSettings = new SimpleApplicationSettings();
        clientSettings.setClientId(CLIENT_ID);
        settings.setApp(clientSettings);
        when(newApplication.getSettings()).thenReturn(settings);

        when(applicationRepository.findAllByEnvironment("DEFAULT", ApplicationStatus.ACTIVE)).thenThrow(TechnicalException.class);

        applicationService.create(GraviteeContext.getExecutionContext(), newApplication, USER_NAME);
    }

    @Test(expected = InvalidApplicationApiKeyModeException.class)
    public void shouldNotCreateCauseSharedApiKeyModeDisabled() {
        ApplicationSettings settings = new ApplicationSettings();
        SimpleApplicationSettings clientSettings = new SimpleApplicationSettings();
        clientSettings.setClientId(CLIENT_ID);
        settings.setApp(clientSettings);
        when(newApplication.getSettings()).thenReturn(settings);

        when(
            parameterService.findAsBoolean(
                GraviteeContext.getExecutionContext(),
                Key.PLAN_SECURITY_APIKEY_SHARED_ALLOWED,
                ParameterReferenceType.ENVIRONMENT
            )
        )
            .thenReturn(false);

        when(newApplication.getApiKeyMode()).thenReturn(io.gravitee.rest.api.model.ApiKeyMode.SHARED);

        applicationService.create(GraviteeContext.getExecutionContext(), newApplication, USER_NAME);
    }

    @Test
    public void shouldCreateOauthApp() throws TechnicalException {
        when(application.getName()).thenReturn(APPLICATION_NAME);
        when(application.getType()).thenReturn(ApplicationType.SIMPLE);
        when(application.getStatus()).thenReturn(ApplicationStatus.ACTIVE);
        when(applicationRepository.create(any())).thenReturn(application);
        when(newApplication.getName()).thenReturn(APPLICATION_NAME);
        when(newApplication.getDescription()).thenReturn("My description");
        when(groupService.findByEvent(eq(GraviteeContext.getCurrentEnvironment()), any())).thenReturn(Collections.emptySet());
        when(userService.findById(any(), any())).thenReturn(mock(UserEntity.class));

        // client registration is enabled, and browser app type also
        when(parameterService.findAsBoolean(any(), eq(Key.APPLICATION_REGISTRATION_ENABLED), any(), eq(ParameterReferenceType.ENVIRONMENT)))
            .thenReturn(true);
        when(parameterService.findAsBoolean(any(), eq(Key.APPLICATION_TYPE_BROWSER_ENABLED), any(), eq(ParameterReferenceType.ENVIRONMENT)))
            .thenReturn(true);

        // oauth app settings contains everything required
        ApplicationSettings settings = new ApplicationSettings();
        OAuthClientSettings oAuthClientSettings = new OAuthClientSettings();
        oAuthClientSettings.setGrantTypes(List.of("application-grant-type"));
        oAuthClientSettings.setApplicationType("BROWSER");
        settings.setoAuthClient(oAuthClientSettings);
        when(newApplication.getSettings()).thenReturn(settings);
        when(newApplication.getSettings()).thenReturn(settings);

        // mock application type service
        ApplicationTypeEntity applicationTypeEntity = new ApplicationTypeEntity();
        ApplicationGrantTypeEntity applicationGrantTypeEntity = new ApplicationGrantTypeEntity();
        applicationGrantTypeEntity.setType("application-grant-type");
        applicationGrantTypeEntity.setResponse_types(List.of("response-type"));
        applicationTypeEntity.setAllowed_grant_types(List.of(applicationGrantTypeEntity));
        applicationTypeEntity.setRequires_redirect_uris(false);
        when(applicationTypeService.getApplicationType("BROWSER")).thenReturn(applicationTypeEntity);

        // mock response from DCR with a new client ID
        ClientRegistrationResponse clientRegistrationResponse = new ClientRegistrationResponse();
        clientRegistrationResponse.setClientId("client-id-from-clientRegistration");
        when(clientRegistrationService.register(any(), any())).thenReturn(clientRegistrationResponse);
        when(applicationConverter.toApplication(any(NewApplicationEntity.class))).thenCallRealMethod();

        applicationService.create(EXECUTION_CONTEXT, newApplication, USER_NAME);

        // ensure app has been created with client_id from DCR in metadata
        verify(applicationRepository)
            .create(argThat(application -> application.getMetadata().get(METADATA_CLIENT_ID).equals("client-id-from-clientRegistration")));
    }
}
