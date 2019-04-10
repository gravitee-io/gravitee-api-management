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
import io.gravitee.management.model.NewApplicationEntity;
import io.gravitee.management.model.UserEntity;
import io.gravitee.management.model.application.ApplicationSettings;
import io.gravitee.management.model.application.OAuthClientSettings;
import io.gravitee.management.model.application.SimpleApplicationSettings;
import io.gravitee.management.model.parameters.Key;
import io.gravitee.management.service.configuration.application.ClientRegistrationService;
import io.gravitee.management.service.exceptions.ClientIdAlreadyExistsException;
import io.gravitee.management.service.exceptions.TechnicalManagementException;
import io.gravitee.management.service.impl.ApplicationServiceImpl;
import io.gravitee.management.service.impl.configuration.application.registration.client.register.ClientRegistrationResponse;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.repository.management.model.Application;
import io.gravitee.repository.management.model.ApplicationStatus;
import io.gravitee.repository.management.model.ApplicationType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.internal.util.collections.Sets;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
    private MembershipRepository membershipRepository;

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
    private ClientRegistrationService clientRegistrationService;

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
        when(groupService.findByEvent(any())).thenReturn(Collections.emptySet());
        when(userService.findById(any())).thenReturn(mock(UserEntity.class));

        final ApplicationEntity applicationEntity = applicationService.create(newApplication, USER_NAME);

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

        when(parameterService.findAsBoolean(Key.APPLICATION_REGISTRATION_ENABLED)).thenReturn(Boolean.FALSE);

        applicationService.create(newApplication, USER_NAME);
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

        when(applicationRepository.findAll(ApplicationStatus.ACTIVE)).thenReturn(Collections.singleton(application));

        applicationService.create(newApplication, USER_NAME);
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotCreateForUserBecauseTechnicalException() throws TechnicalException {
        ApplicationSettings settings = new ApplicationSettings();
        SimpleApplicationSettings clientSettings = new SimpleApplicationSettings();
        clientSettings.setClientId(CLIENT_ID);
        settings.setApp(clientSettings);
        when(newApplication.getSettings()).thenReturn(settings);

        when(applicationRepository.findAll(ApplicationStatus.ACTIVE)).thenThrow(TechnicalException.class);
//        when(newApplication.getClientId()).thenReturn(CLIENT_ID);

        applicationService.create(newApplication, USER_NAME);
    }
}
