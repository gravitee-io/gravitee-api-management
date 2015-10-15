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

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

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
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.model.Application;

/**
 * @author Azize Elamrani (azize dot elamrani at gmail dot com)
 */
@RunWith(MockitoJUnitRunner.class)
public class ApplicationServiceTest {

    private static final String APPLICATION_NAME = "myApplication";
    private static final String USER_NAME = "myUser";
    private static final String TEAM_NAME = "myTeam";

    @InjectMocks
    private ApplicationService applicationService = new ApplicationServiceImpl();

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private NewApplicationEntity newApplication;
    @Mock
    private UpdateApplicationEntity existingApplication;
    @Mock
    private Application application;

    @Test
    public void shouldFindByName() throws TechnicalException {
        when(applicationRepository.findByName(APPLICATION_NAME)).thenReturn(Optional.of(application));

        final Optional<ApplicationEntity> applicationEntity = applicationService.findByName(APPLICATION_NAME);

        assertNotNull(applicationEntity);
        assertTrue(applicationEntity.isPresent());
    }

    @Test
    public void shouldNotFindByNameBecauseNotExists() throws TechnicalException {
        when(applicationRepository.findByName(APPLICATION_NAME)).thenReturn(Optional.empty());

        final Optional<ApplicationEntity> applicationEntity = applicationService.findByName(APPLICATION_NAME);

        assertNotNull(applicationEntity);
        assertFalse(applicationEntity.isPresent());
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotFindByNameBecauseTechnicalException() throws TechnicalException {
        when(applicationRepository.findByName(APPLICATION_NAME)).thenThrow(TechnicalException.class);

        applicationService.findByName(APPLICATION_NAME);
    }

    @Test
    public void shouldFindByTeam() throws TechnicalException {
        when(applicationRepository.findByTeam(TEAM_NAME)).thenReturn(new HashSet<>(Arrays.asList(application)));

        final Set<ApplicationEntity> applicationEntity = applicationService.findByTeam(TEAM_NAME);

        assertNotNull(applicationEntity);
        assertEquals(1, applicationEntity.size());
    }

    @Test
    public void shouldNotFindByTeamBecauseNotExists() throws TechnicalException {
        when(applicationRepository.findByTeam(TEAM_NAME)).thenReturn(null);

        final Set<ApplicationEntity> applicationEntity = applicationService.findByTeam(TEAM_NAME);

        assertNotNull(applicationEntity);
        assertTrue(applicationEntity.isEmpty());
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotFindByTeamBecauseTechnicalException() throws TechnicalException {
        when(applicationRepository.findByTeam(TEAM_NAME)).thenThrow(TechnicalException.class);

        applicationService.findByTeam(TEAM_NAME);
    }

    @Test
    public void shouldFindByUser() throws TechnicalException {
        when(applicationRepository.findByTeam(USER_NAME)).thenReturn(new HashSet<>(Arrays.asList(application)));

        final Set<ApplicationEntity> applicationEntity = applicationService.findByTeam(USER_NAME);

        assertNotNull(applicationEntity);
        assertEquals(1, applicationEntity.size());
    }

    @Test
    public void shouldNotFindByUserBecauseNotExists() throws TechnicalException {
        when(applicationRepository.findByTeam(USER_NAME)).thenReturn(null);

        final Set<ApplicationEntity> applicationEntity = applicationService.findByTeam(USER_NAME);

        assertNotNull(applicationEntity);
        assertTrue(applicationEntity.isEmpty());
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotFindByTeamUserTechnicalException() throws TechnicalException {
        when(applicationRepository.findByTeam(USER_NAME)).thenThrow(TechnicalException.class);

        applicationService.findByTeam(USER_NAME);
    }

    @Test
    public void shouldCreateForUser() throws TechnicalException {
        when(application.getName()).thenReturn(APPLICATION_NAME);
        when(applicationRepository.findByName(APPLICATION_NAME)).thenReturn(Optional.empty());
        when(applicationRepository.create(any())).thenReturn(application);
        when(newApplication.getName()).thenReturn(APPLICATION_NAME);

        final ApplicationEntity applicationEntity = applicationService.createForUser(newApplication, USER_NAME);

        assertNotNull(applicationEntity);
        assertEquals(APPLICATION_NAME, applicationEntity.getName());
    }

    @Test(expected = ApplicationAlreadyExistsException.class)
    public void shouldNotCreateForUserBecauseExists() throws TechnicalException {
        when(applicationRepository.findByName(APPLICATION_NAME)).thenReturn(Optional.of(application));
        when(newApplication.getName()).thenReturn(APPLICATION_NAME);

        applicationService.createForUser(newApplication, USER_NAME);
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotCreateForUserBecauseTechnicalException() throws TechnicalException {
        when(applicationRepository.findByName(APPLICATION_NAME)).thenThrow(TechnicalException.class);
        when(newApplication.getName()).thenReturn(APPLICATION_NAME);

        applicationService.createForUser(newApplication, USER_NAME);
    }

    @Test
    public void shouldCreateForTeam() throws TechnicalException {
        when(application.getName()).thenReturn(APPLICATION_NAME);
        when(applicationRepository.findByName(APPLICATION_NAME)).thenReturn(Optional.empty());
        when(applicationRepository.create(any())).thenReturn(application);
        when(newApplication.getName()).thenReturn(APPLICATION_NAME);

        final ApplicationEntity applicationEntity = applicationService.createForTeam(newApplication, TEAM_NAME);

        assertNotNull(applicationEntity);
        assertEquals(APPLICATION_NAME, applicationEntity.getName());
    }

    @Test(expected = ApplicationAlreadyExistsException.class)
    public void shouldNotCreateForTeamBecauseExists() throws TechnicalException {
        when(applicationRepository.findByName(APPLICATION_NAME)).thenReturn(Optional.of(application));
        when(newApplication.getName()).thenReturn(APPLICATION_NAME);

        applicationService.createForTeam(newApplication, TEAM_NAME);
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotCreateForTeamBecauseTechnicalException() throws TechnicalException {
        when(applicationRepository.findByName(APPLICATION_NAME)).thenThrow(TechnicalException.class);
        when(newApplication.getName()).thenReturn(APPLICATION_NAME);

        applicationService.createForTeam(newApplication, TEAM_NAME);
    }

    @Test
    public void shouldUpdate() throws TechnicalException {
        when(applicationRepository.findByName(APPLICATION_NAME)).thenReturn(Optional.of(application));
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
        when(applicationRepository.findByName(APPLICATION_NAME)).thenReturn(Optional.empty());
        when(applicationRepository.update(any())).thenReturn(application);

        applicationService.update(APPLICATION_NAME, existingApplication);
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotUpdateBecauseTechnicalException() throws TechnicalException {
        when(applicationRepository.findByName(APPLICATION_NAME)).thenReturn(Optional.of(application));
        when(applicationRepository.update(any())).thenThrow(TechnicalException.class);

        applicationService.update(APPLICATION_NAME, existingApplication);
    }

    @Test
    public void shouldDelete() throws TechnicalException {
        applicationService.delete(APPLICATION_NAME);

        verify(applicationRepository).delete(APPLICATION_NAME);
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotDeleteBecauseTechnicalException() throws TechnicalException {
        doThrow(TechnicalException.class).when(applicationRepository).delete(APPLICATION_NAME);

        applicationService.delete(APPLICATION_NAME);
    }
}
