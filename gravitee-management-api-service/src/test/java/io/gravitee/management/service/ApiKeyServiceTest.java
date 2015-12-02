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
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import io.gravitee.management.model.ApiKeyEntity;
import io.gravitee.management.service.exceptions.ApiKeyNotFoundException;
import io.gravitee.management.service.exceptions.TechnicalManagementException;
import io.gravitee.management.service.impl.ApiKeyServiceImpl;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.model.ApiKey;

/**
 * @author Azize Elamrani (azize dot elamrani at gmail dot com)
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiKeyServiceTest {

    private static final String API_NAME = "myAPI";
    private static final String APPLICATION_NAME = "myApplication";
    private static final String API_KEY = "ef02ecd0-71bb-11e5-9d70-feff819cdc9f";

    @InjectMocks
    private ApiKeyService apiKeyService = new ApiKeyServiceImpl();

    @Mock
    private ApiKeyRepository apiKeyRepository;

    @Mock
    private ApiKeyGenerator apiKeyGenerator;

    @Mock
    private ApiKey apiKey;
    @Mock
    private Date date;

    @Test
    public void shouldGenerate() throws TechnicalException {
        when(apiKeyGenerator.generate()).thenReturn(API_KEY);
        when(apiKeyRepository.findByApplicationAndApi(APPLICATION_NAME, API_NAME)).thenReturn(new HashSet<>(asList(apiKey)));
        when(apiKeyRepository.create(eq(APPLICATION_NAME), eq(API_NAME), any(ApiKey.class))).thenReturn(apiKey);
        when(apiKey.getKey()).thenReturn(API_KEY);
        when(apiKey.getCreatedAt()).thenReturn(date);
        when(apiKey.isRevoked()).thenReturn(false);
        when(apiKey.getExpiration()).thenReturn(date);

        final ApiKeyEntity apiKey = apiKeyService.generateOrRenew(APPLICATION_NAME, API_NAME);

        verify(this.apiKey, never()).setExpiration(any());
        verify(apiKeyRepository, never()).update(any());

        assertEquals(API_KEY, apiKey.getKey());
        assertEquals(date, apiKey.getCreatedAt());
        assertEquals(date, apiKey.getExpireOn());
        assertEquals(false, apiKey.isRevoked());
    }

    @Test
    public void shouldGenerateAndInvalidOldKeys() throws TechnicalException {
        when(apiKeyGenerator.generate()).thenReturn(API_KEY);
        when(apiKeyRepository.findByApplicationAndApi(APPLICATION_NAME, API_NAME)).thenReturn(new HashSet<>(asList(apiKey)));
        when(apiKeyRepository.create(eq(APPLICATION_NAME), eq(API_NAME), any(ApiKey.class))).thenReturn(apiKey);
        when(apiKey.getKey()).thenReturn(API_KEY);
        when(apiKey.getCreatedAt()).thenReturn(date);
        when(apiKey.isRevoked()).thenReturn(false);

        final ApiKeyEntity apiKeyEntity = apiKeyService.generateOrRenew(APPLICATION_NAME, API_NAME);

        verify(apiKey).setExpiration(any());
        verify(apiKeyRepository).update(apiKey);

        assertEquals(API_KEY, apiKeyEntity.getKey());
        assertEquals(date, apiKeyEntity.getCreatedAt());
        assertEquals(false, apiKeyEntity.isRevoked());
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotGenerateBecauseTechnicalException() throws TechnicalException {
        when(apiKeyGenerator.generate()).thenReturn(API_KEY);
        when(apiKeyRepository.findByApplicationAndApi(APPLICATION_NAME, API_NAME))
            .thenThrow(TechnicalException.class);

        apiKeyService.generateOrRenew(APPLICATION_NAME, API_NAME);
    }

    @Test
    public void shouldRevoke() throws TechnicalException {
        when(apiKeyRepository.retrieve(API_KEY)).thenReturn(Optional.of(apiKey));
        when(apiKey.isRevoked()).thenReturn(false);

        apiKeyService.revoke(API_KEY);

        verify(apiKeyRepository).update(apiKey);
    }

    @Test
    public void shouldNotRevokeBecauseAlreadyRevoked() throws TechnicalException {
        when(apiKeyRepository.retrieve(API_KEY)).thenReturn(Optional.of(apiKey));
        when(apiKey.isRevoked()).thenReturn(true);

        apiKeyService.revoke(API_KEY);

        verify(apiKeyRepository, never()).update(apiKey);
    }

    @Test(expected = ApiKeyNotFoundException.class)
    public void shouldNotRevokeBecauseNotFound() throws TechnicalException {
        when(apiKeyRepository.retrieve(API_KEY)).thenReturn(Optional.empty());

        apiKeyService.revoke(API_KEY);
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotRevokeBecauseTechnicalException() throws TechnicalException {
        when(apiKeyRepository.retrieve(API_KEY)).thenThrow(TechnicalException.class);

        apiKeyService.revoke(API_KEY);
    }

    @Test
    public void shouldGetCurrent() throws TechnicalException {
        final ApiKey maxApiKey = mock(ApiKey.class);

        final Date value = new Date();
        when(apiKey.getCreatedAt()).thenReturn(value);
        when(apiKey.getKey()).thenReturn(API_KEY);
        when(maxApiKey.getCreatedAt()).thenReturn(Date.from(value.toInstant().plus(1, ChronoUnit.DAYS)));
        when(maxApiKey.getKey()).thenReturn("KEY");

        when(apiKeyRepository.findByApplicationAndApi(APPLICATION_NAME, API_KEY)).thenReturn(new HashSet(asList(apiKey, maxApiKey)));

        final Optional<ApiKeyEntity> currentApiKey = apiKeyService.getCurrent(APPLICATION_NAME, API_KEY);

        assertNotNull(currentApiKey);
        assertTrue(currentApiKey.isPresent());
        assertEquals("KEY", currentApiKey.get().getKey());
    }

    @Test
    public void shouldNotGetCurrentBecauseAllRevoked() throws TechnicalException {
        final ApiKey maxApiKey = mock(ApiKey.class);

        final Date value = new Date();
        when(apiKey.getCreatedAt()).thenReturn(value);
        when(apiKey.getKey()).thenReturn(API_KEY);
        when(apiKey.isRevoked()).thenReturn(true);
        when(maxApiKey.getCreatedAt()).thenReturn(Date.from(value.toInstant().plus(1, ChronoUnit.DAYS)));
        when(maxApiKey.getKey()).thenReturn("KEY");
        when(maxApiKey.isRevoked()).thenReturn(true);

        when(apiKeyRepository.findByApplicationAndApi(APPLICATION_NAME, API_KEY)).thenReturn(new HashSet(asList(apiKey, maxApiKey)));

        final Optional<ApiKeyEntity> currentApiKey = apiKeyService.getCurrent(APPLICATION_NAME, API_KEY);

        assertNotNull(currentApiKey);
        assertFalse(currentApiKey.isPresent());
    }

    @Test
    public void shouldNotGetCurrentBecauseNotFound() throws TechnicalException {
        when(apiKeyRepository.findByApplicationAndApi(APPLICATION_NAME, API_KEY)).thenReturn(null);

        final Optional<ApiKeyEntity> currentApiKey = apiKeyService.getCurrent(APPLICATION_NAME, API_KEY);

        assertNotNull(currentApiKey);
        assertFalse(currentApiKey.isPresent());
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotGetCurrentBecauseTechnicalException() throws TechnicalException {
        when(apiKeyRepository.findByApplicationAndApi(APPLICATION_NAME, API_KEY)).thenThrow(TechnicalException.class);

        apiKeyService.getCurrent(APPLICATION_NAME, API_KEY);
    }

    @Test
    public void shouldFindAll() throws TechnicalException {
        final ApiKey maxApiKey = mock(ApiKey.class);

        final Date value = new Date();
        when(apiKey.getCreatedAt()).thenReturn(value);
        when(apiKey.getKey()).thenReturn(API_KEY);
        when(maxApiKey.getCreatedAt()).thenReturn(Date.from(value.toInstant().plus(1, ChronoUnit.DAYS)));
        when(maxApiKey.getKey()).thenReturn("KEY");

        when(apiKeyRepository.findByApplicationAndApi(APPLICATION_NAME, API_KEY)).thenReturn(new HashSet(asList(apiKey, maxApiKey)));

        final Set<ApiKeyEntity> apiKeyEntities = apiKeyService.findAll(APPLICATION_NAME, API_KEY);

        assertNotNull(apiKeyEntities);
        assertEquals(2, apiKeyEntities.size());
    }

    @Test
    public void shouldNotFindAllBecauseNotFound() throws TechnicalException {
        when(apiKeyRepository.findByApplicationAndApi(APPLICATION_NAME, API_KEY)).thenReturn(null);

        final Set<ApiKeyEntity> apiKeyEntities = apiKeyService.findAll(APPLICATION_NAME, API_KEY);

        assertNotNull(apiKeyEntities);
        assertTrue(apiKeyEntities.isEmpty());
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotFindAllBecauseTechnicalException() throws TechnicalException {
        when(apiKeyRepository.findByApplicationAndApi(APPLICATION_NAME, API_KEY)).thenThrow(TechnicalException.class);

        apiKeyService.findAll(APPLICATION_NAME, API_KEY);
    }
}
