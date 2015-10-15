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

import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.management.model.ApiEntity;
import io.gravitee.management.model.NewApiEntity;
import io.gravitee.management.model.UpdateApiEntity;
import io.gravitee.management.service.exceptions.ApiAlreadyExistsException;
import io.gravitee.management.service.exceptions.ApiNotFoundException;
import io.gravitee.management.service.exceptions.TechnicalManagementException;
import io.gravitee.management.service.impl.ApiServiceImpl;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.LifecycleState;

/**
 * @author Azize Elamrani (azize dot elamrani at gmail dot com)
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiServiceTest {

    private static final String API_NAME = "myAPI";
    private static final String USER_NAME = "myUser";
    private static final String TEAM_NAME = "myTeam";

    @InjectMocks
    private ApiService apiService = new ApiServiceImpl();

    @Mock
    private ApiRepository apiRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private NewApiEntity newApi;
    @Mock
    private UpdateApiEntity existingApi;
    @Mock
    private Api api;

    @Test
    public void shouldCreateForUser() throws TechnicalException {
        when(api.getName()).thenReturn(API_NAME);
        when(apiRepository.findByName(API_NAME)).thenReturn(Optional.empty());
        when(apiRepository.create(any())).thenReturn(api);
        when(newApi.getName()).thenReturn(API_NAME);

        final ApiEntity apiEntity = apiService.createForUser(newApi, USER_NAME);

        assertNotNull(apiEntity);
        assertEquals(API_NAME, apiEntity.getName());
    }

    @Test(expected = ApiAlreadyExistsException.class)
    public void shouldNotCreateForUserBecauseExists() throws TechnicalException {
        when(apiRepository.findByName(API_NAME)).thenReturn(Optional.of(api));
        when(newApi.getName()).thenReturn(API_NAME);

        apiService.createForUser(newApi, USER_NAME);
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotCreateForUserBecauseTechnicalException() throws TechnicalException {
        when(apiRepository.findByName(API_NAME)).thenThrow(TechnicalException.class);
        when(newApi.getName()).thenReturn(API_NAME);

        apiService.createForUser(newApi, USER_NAME);
    }

    @Test
    public void shouldCreateForTeam() throws TechnicalException {
        when(api.getName()).thenReturn(API_NAME);
        when(apiRepository.findByName(API_NAME)).thenReturn(Optional.empty());
        when(apiRepository.create(any())).thenReturn(api);
        when(newApi.getName()).thenReturn(API_NAME);

        final ApiEntity apiEntity = apiService.createForTeam(newApi, TEAM_NAME, USER_NAME);

        assertNotNull(apiEntity);
        assertEquals(API_NAME, apiEntity.getName());
    }

    @Test(expected = ApiAlreadyExistsException.class)
    public void shouldNotCreateForTeamBecauseExists() throws TechnicalException {
        when(apiRepository.findByName(API_NAME)).thenReturn(Optional.of(api));
        when(newApi.getName()).thenReturn(API_NAME);

        apiService.createForTeam(newApi, TEAM_NAME, USER_NAME);
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotCreateForTeamBecauseTechnicalException() throws TechnicalException {
        when(apiRepository.findByName(API_NAME)).thenThrow(TechnicalException.class);
        when(newApi.getName()).thenReturn(API_NAME);

        apiService.createForTeam(newApi, TEAM_NAME, USER_NAME);
    }

    @Test
    public void shouldFindByName() throws TechnicalException {
        when(apiRepository.findByName(API_NAME)).thenReturn(Optional.of(api));

        final Optional<ApiEntity> apiEntity = apiService.findByName(API_NAME);

        assertNotNull(apiEntity);
        assertTrue(apiEntity.isPresent());
    }

    @Test
    public void shouldNotFindByNameBecauseNotExists() throws TechnicalException {
        when(apiRepository.findByName(API_NAME)).thenReturn(Optional.empty());

        final Optional<ApiEntity> apiEntity = apiService.findByName(API_NAME);

        assertNotNull(apiEntity);
        assertFalse(apiEntity.isPresent());
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotFindByNameBecauseTechnicalException() throws TechnicalException {
        when(apiRepository.findByName(API_NAME)).thenThrow(TechnicalException.class);

        apiService.findByName(API_NAME);
    }

    @Test
    public void shouldFindAll() throws TechnicalException {
        when(apiRepository.findAll()).thenReturn(new HashSet(asList(api)));

        final Set<ApiEntity> apiEntities = apiService.findAll();

        assertNotNull(apiEntities);
        assertEquals(1, apiEntities.size());
    }

    @Test
    public void shouldNotFindAllBecauseNotFound() throws TechnicalException {
        when(apiRepository.findAll()).thenReturn(null);

        final Set<ApiEntity> apiEntities = apiService.findAll();

        assertNotNull(apiEntities);
        assertTrue(apiEntities.isEmpty());
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotFindAllBecauseTechnicalException() throws TechnicalException {
        when(apiRepository.findAll()).thenThrow(TechnicalException.class);

        apiService.findAll();
    }

    @Test
    public void shouldFindByTeam() throws TechnicalException {
        when(apiRepository.findByTeam(TEAM_NAME, true)).thenReturn(new HashSet<>(Arrays.asList(api)));

        final Set<ApiEntity> apiEntities = apiService.findByTeam(TEAM_NAME, true);

        assertNotNull(apiEntities);
        assertEquals(1, apiEntities.size());
    }

    @Test
    public void shouldNotFindByTeamBecauseNotExists() throws TechnicalException {
        when(apiRepository.findByTeam(TEAM_NAME, true)).thenReturn(null);

        final Set<ApiEntity> apiEntities = apiService.findByTeam(TEAM_NAME, true);

        assertNotNull(apiEntities);
        assertTrue(apiEntities.isEmpty());
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotFindByTeamBecauseTechnicalException() throws TechnicalException {
        when(apiRepository.findByTeam(TEAM_NAME, true)).thenThrow(TechnicalException.class);

        apiService.findByTeam(TEAM_NAME, true);
    }

    @Test
    public void shouldFindByUser() throws TechnicalException {
        when(apiRepository.findByUser(USER_NAME, true)).thenReturn(new HashSet<>(Arrays.asList(api)));

        final Set<ApiEntity> apiEntities = apiService.findByUser(USER_NAME, true);

        assertNotNull(apiEntities);
        assertEquals(1, apiEntities.size());
    }

    @Test
    public void shouldNotFindByUserBecauseNotExists() throws TechnicalException {
        when(apiRepository.findByUser(USER_NAME, true)).thenReturn(null);

        final Set<ApiEntity> apiEntities = apiService.findByUser(USER_NAME, true);

        assertNotNull(apiEntities);
        assertTrue(apiEntities.isEmpty());
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotFindByUserBecauseTechnicalException() throws TechnicalException {
        when(apiRepository.findByUser(USER_NAME, true)).thenThrow(TechnicalException.class);

        apiService.findByUser(USER_NAME, true);
    }

    @Test
    public void shouldUpdate() throws TechnicalException {
        when(apiRepository.findByName(API_NAME)).thenReturn(Optional.of(api));
        when(apiRepository.update(any())).thenReturn(api);
        when(api.getName()).thenReturn(API_NAME);

        final ApiEntity apiEntity = apiService.update(API_NAME, existingApi);

        assertNotNull(apiEntity);
        assertEquals(API_NAME, apiEntity.getName());
    }

    @Test(expected = ApiNotFoundException.class)
    public void shouldNotUpdateBecauseNotFound() throws TechnicalException {
        when(apiRepository.findByName(API_NAME)).thenReturn(Optional.empty());
        when(apiRepository.update(any())).thenReturn(api);

        apiService.update(API_NAME, existingApi);
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotUpdateBecauseTechnicalException() throws TechnicalException {
        when(apiRepository.findByName(API_NAME)).thenReturn(Optional.of(api));
        when(apiRepository.update(any())).thenThrow(TechnicalException.class);

        apiService.update(API_NAME, existingApi);
    }

    @Test
    public void shouldDelete() throws TechnicalException {
        apiService.delete(API_NAME);

        verify(apiRepository).delete(API_NAME);
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotDeleteBecauseTechnicalException() throws TechnicalException {
        doThrow(TechnicalException.class).when(apiRepository).delete(API_NAME);

        apiService.delete(API_NAME);
    }

    @Test
    public void shouldStart() throws TechnicalException {
        when(apiRepository.findByName(API_NAME)).thenReturn(Optional.of(api));

        apiService.start(API_NAME);

        verify(api).setUpdatedAt(any());
        verify(api).setLifecycleState(LifecycleState.STARTED);
        verify(apiRepository).update(api);
    }

    @Test
    public void shouldNotStartBecauseNotFound() throws TechnicalException {
        when(apiRepository.findByName(API_NAME)).thenReturn(Optional.empty());

        apiService.start(API_NAME);

        verify(apiRepository, never()).update(api);
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotStartBecauseTechnicalException() throws TechnicalException {
        when(apiRepository.findByName(API_NAME)).thenThrow(TechnicalException.class);

        apiService.start(API_NAME);
    }

    @Test
    public void shouldStop() throws TechnicalException {
        when(apiRepository.findByName(API_NAME)).thenReturn(Optional.of(api));

        apiService.stop(API_NAME);

        verify(api).setUpdatedAt(any());
        verify(api).setLifecycleState(LifecycleState.STOPPED);
        verify(apiRepository).update(api);
    }

    @Test
    public void shouldNotStopBecauseNotFound() throws TechnicalException {
        when(apiRepository.findByName(API_NAME)).thenReturn(Optional.empty());

        apiService.stop(API_NAME);

        verify(apiRepository, never()).update(api);
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotStopBecauseTechnicalException() throws TechnicalException {
        when(apiRepository.findByName(API_NAME)).thenThrow(TechnicalException.class);

        apiService.stop(API_NAME);
    }
}
