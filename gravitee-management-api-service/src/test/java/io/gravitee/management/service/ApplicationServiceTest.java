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

import io.gravitee.management.model.*;
import io.gravitee.management.service.exceptions.ApplicationAlreadyExistsException;
import io.gravitee.management.service.exceptions.ApplicationNotFoundException;
import io.gravitee.management.service.exceptions.TechnicalManagementException;
import io.gravitee.management.service.impl.ApplicationServiceImpl;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.repository.management.model.Application;
import io.gravitee.repository.management.model.Membership;
import io.gravitee.repository.management.model.MembershipReferenceType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;

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
    private MembershipRepository membershipRepository;

    @Mock
    private UserService userService;

    @Mock
    private NewApplicationEntity newApplication;

    @Mock
    private UpdateApplicationEntity existingApplication;

    @Mock
    private Application application;

    @Test
    public void shouldFindById() throws TechnicalException {
        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(application));
        Membership po = new Membership(USER_NAME, APPLICATION_ID, MembershipReferenceType.APPLICATION);
        po.setType(MembershipType.PRIMARY_OWNER.name());
        when(membershipRepository.findByReferenceAndMembershipType(any(), any(), any()))
                .thenReturn(Collections.singleton(po));
        when(userService.findByName(USER_NAME)).thenReturn(new UserEntity());

        final ApplicationEntity applicationEntity = applicationService.findById(APPLICATION_ID);

        assertNotNull(applicationEntity);
    }

    @Test(expected = ApplicationNotFoundException.class)
    public void shouldNotFindByIdBecauseNotExists() throws TechnicalException {
        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.empty());
        applicationService.findById(APPLICATION_ID);
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotFindByIdBecauseTechnicalException() throws TechnicalException {
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
    public void shouldUpdate() throws TechnicalException {
        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(application));
        when(application.getName()).thenReturn(APPLICATION_NAME);
        when(existingApplication.getName()).thenReturn(APPLICATION_NAME);
        when(existingApplication.getDescription()).thenReturn("My description");
        when(applicationRepository.update(any())).thenReturn(application);
        Membership po = new Membership(USER_NAME, APPLICATION_ID, MembershipReferenceType.APPLICATION);
        po.setType(MembershipType.PRIMARY_OWNER.name());
        when(membershipRepository.findByReferencesAndMembershipType(any(), any(), any()))
                .thenReturn(Collections.singleton(po));

        final ApplicationEntity applicationEntity = applicationService.update(APPLICATION_ID, existingApplication);

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
    public void shouldNotFindByNameWhenNull() throws Exception {
        Set<ApplicationEntity> set = applicationService.findByName(null);
        assertNotNull(set);
        assertEquals("result is empty", 0, set.size());
        verify(applicationRepository, never()).findByName(any());
    }

    @Test
    public void shouldNotFindByNameWhenEmpty() throws Exception {
        Set<ApplicationEntity> set = applicationService.findByName(" ");
        assertNotNull(set);
        assertEquals("result is empty", 0, set.size());
        verify(applicationRepository, never()).findByName(any());
    }

    @Test
    public void shouldNotFindByName() throws Exception {
        Set<ApplicationEntity> set = applicationService.findByName("a");
        assertNotNull(set);
        assertEquals("result is empty", 0, set.size());
        verify(applicationRepository, times(1)).findByName("a");
    }

    /*
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
    */
}
