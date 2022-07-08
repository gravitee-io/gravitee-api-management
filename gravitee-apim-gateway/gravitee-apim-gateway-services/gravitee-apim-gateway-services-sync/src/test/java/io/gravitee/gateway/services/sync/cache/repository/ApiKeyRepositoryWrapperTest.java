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
package io.gravitee.gateway.services.sync.cache.repository;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.api.search.ApiKeyCriteria;
import io.gravitee.repository.management.model.ApiKey;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiKeyRepositoryWrapperTest {

    private ApiKeyRepositoryWrapper repository;

    @Mock
    private ApiKeyRepository wrappedRepository;

    @Mock
    private Map<String, ApiKey> cache;

    @Before
    public void setUp() {
        repository = new ApiKeyRepositoryWrapper(wrappedRepository, cache);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldCreateThrowsException() throws TechnicalException {
        repository.create(null);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldFindBySubscriptionThrowsException() throws TechnicalException {
        repository.findBySubscription(null);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldFindByPlanThrowsException() throws TechnicalException {
        repository.findByPlan(null);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldUpdateThrowsException() throws TechnicalException {
        repository.update(null);
    }

    @Test
    public void shouldFindByCriteria() throws TechnicalException {
        ApiKeyCriteria apiKeyCriteria = Mockito.mock(ApiKeyCriteria.class);

        List<ApiKey> mockApiKeys = Arrays.asList(Mockito.mock(ApiKey.class), Mockito.mock(ApiKey.class));
        Mockito.when(wrappedRepository.findByCriteria(apiKeyCriteria)).thenReturn(mockApiKeys);

        List<ApiKey> apiKeys = repository.findByCriteria(apiKeyCriteria);
        Assert.assertEquals(mockApiKeys, apiKeys);
    }

    @Test
    public void shouldFindById_empty() throws TechnicalException {
        String apiKey = "1234-4567-7890";

        Mockito.when(cache.get(apiKey)).thenReturn(null);
        Optional<ApiKey> optApiKey = repository.findById(apiKey);

        Assert.assertNotNull(optApiKey);
        Assert.assertFalse(optApiKey.isPresent());
    }

    @Test
    public void shouldFindById_fromCache() throws TechnicalException {
        String apiKey = "1234-4567-7890";
        ApiKey mockApiKey = Mockito.mock(ApiKey.class);

        Mockito.when(cache.get(apiKey)).thenReturn(mockApiKey);
        Optional<ApiKey> optApiKey = repository.findById(apiKey);

        Assert.assertNotNull(optApiKey);
        Assert.assertTrue(optApiKey.isPresent());
        Assert.assertEquals(mockApiKey, optApiKey.get());
    }
}
