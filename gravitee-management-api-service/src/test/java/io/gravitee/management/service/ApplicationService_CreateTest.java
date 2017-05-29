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
import io.gravitee.management.service.exceptions.ApplicationAlreadyExistsException;
import io.gravitee.management.service.exceptions.TechnicalManagementException;
import io.gravitee.management.service.impl.ApplicationServiceImpl;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.repository.management.model.Application;
import io.gravitee.repository.management.model.ApplicationStatus;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.when;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ApplicationService_CreateTest {

    private static final String APPLICATION_NAME = "myApplication";
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
    private NewApplicationEntity newApplication;

    @Mock
    private Application application;

    @Test
    public void shouldCreateForUser() throws TechnicalException {
        when(application.getName()).thenReturn(APPLICATION_NAME);
        when(application.getStatus()).thenReturn(ApplicationStatus.ACTIVE);
        when(applicationRepository.findById(anyString())).thenReturn(Optional.empty());
        when(applicationRepository.create(any())).thenReturn(application);
        when(newApplication.getName()).thenReturn(APPLICATION_NAME);
        when(newApplication.getDescription()).thenReturn("My description");

        final ApplicationEntity applicationEntity = applicationService.create(newApplication, USER_NAME);

        assertNotNull(applicationEntity);
        assertEquals(APPLICATION_NAME, applicationEntity.getName());
    }

    @Test(expected = ApplicationAlreadyExistsException.class)
    public void shouldNotCreateForUserBecauseExists() throws TechnicalException {
        when(applicationRepository.findById(anyString())).thenReturn(Optional.of(application));
        when(newApplication.getName()).thenReturn(APPLICATION_NAME);
        when(newApplication.getDescription()).thenReturn("My description");

        applicationService.create(newApplication, USER_NAME);
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotCreateForUserBecauseTechnicalException() throws TechnicalException {
        when(applicationRepository.findById(anyString())).thenThrow(TechnicalException.class);
        when(newApplication.getName()).thenReturn(APPLICATION_NAME);
        when(newApplication.getDescription()).thenReturn("My description");

        applicationService.create(newApplication, USER_NAME);
    }
}
