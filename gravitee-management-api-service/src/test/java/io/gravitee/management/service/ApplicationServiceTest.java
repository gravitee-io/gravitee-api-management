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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;

import java.util.Optional;

import io.gravitee.common.utils.UUID;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import io.gravitee.management.model.ApplicationEntity;
import io.gravitee.management.model.NewApplicationEntity;
import io.gravitee.management.model.UpdateApplicationEntity;
import io.gravitee.management.service.exceptions.ApplicationAlreadyExistsException;
import io.gravitee.management.service.exceptions.ApplicationNotFoundException;
import io.gravitee.management.service.exceptions.TechnicalManagementException;
import io.gravitee.management.service.impl.ApplicationServiceImpl;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.model.Application;

/**
 * @author Azize Elamrani (azize dot elamrani at gmail dot com)
 */
@RunWith(MockitoJUnitRunner.class)
public class ApplicationServiceTest {

    private static final String APPLICATION_ID = "id-app";
    private static final String APPLICATION_NAME = "myApplication";
    private static final String USER_NAME = "myUser";

    @InjectMocks
    private ApplicationServiceImpl applicationService = new ApplicationServiceImpl();

    @Mock
    private ApplicationRepository applicationRepository;
    
    @Mock
    private ApiKeyRepository apiKeyRepository;

    @Mock
    private NewApplicationEntity newApplication;
    @Mock
    private UpdateApplicationEntity existingApplication;
    @Mock
    private Application application;

    @Test
    public void shouldFindByName() throws TechnicalException {
        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(application));

        final ApplicationEntity applicationEntity = applicationService.findById(APPLICATION_ID);

        assertNotNull(applicationEntity);
    }

    @Test(expected = ApplicationNotFoundException.class)
    public void shouldNotFindByNameBecauseNotExists() throws TechnicalException {
        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.empty());
        applicationService.findById(APPLICATION_ID);
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotFindByNameBecauseTechnicalException() throws TechnicalException {
        when(applicationRepository.findById(APPLICATION_ID)).thenThrow(TechnicalException.class);

        applicationService.findById(APPLICATION_ID);
    }

    @Test
    public void shouldCreateForUser() throws TechnicalException {
        when(application.getName()).thenReturn(APPLICATION_NAME);
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

    @Test
    @Ignore
    public void shouldUpdate() throws TechnicalException {
        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(application));
        when(application.getName()).thenReturn(APPLICATION_NAME);
        when(applicationRepository.update(any())).thenReturn(application);

        final ApplicationEntity applicationEntity = applicationService.update(APPLICATION_NAME, existingApplication);

        verify(applicationRepository).update(argThat(new ArgumentMatcher<Application>() {
            public boolean matches(Object argument) {
                final Application application = (Application) argument;
                return APPLICATION_NAME.equals(application.getName()) &&
                    application.getUpdatedAt() != null;
            }
        }));

        assertNotNull(applicationEntity);
        assertEquals(APPLICATION_NAME, applicationEntity.getName());
    }

    @Test(expected = ApplicationNotFoundException.class)
    public void shouldNotUpdateBecauseNotFound() throws TechnicalException {
        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.empty());
        when(applicationRepository.update(any())).thenReturn(application);

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
    public void shouldDelete() throws TechnicalException {
        applicationService.delete(APPLICATION_ID);

        verify(applicationRepository).delete(APPLICATION_ID);
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotDeleteBecauseTechnicalException() throws TechnicalException {
        doThrow(TechnicalException.class).when(applicationRepository).delete(APPLICATION_ID);

        applicationService.delete(APPLICATION_ID);
    }
}
