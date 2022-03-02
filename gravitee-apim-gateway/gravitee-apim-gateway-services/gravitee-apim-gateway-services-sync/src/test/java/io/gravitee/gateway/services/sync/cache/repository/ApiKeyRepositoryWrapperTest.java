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

import io.gravitee.gateway.handlers.api.definition.ApiKey;
import io.gravitee.gateway.services.sync.cache.ApiKeysCache;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.api.search.ApiKeyCriteria;
import java.util.Arrays;
import java.util.List;
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
    private ApiKeysCache cache;

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

        List<io.gravitee.repository.management.model.ApiKey> mockApiKeys = Arrays.asList(
            Mockito.mock(io.gravitee.repository.management.model.ApiKey.class),
            Mockito.mock(io.gravitee.repository.management.model.ApiKey.class)
        );
        Mockito.when(wrappedRepository.findByCriteria(apiKeyCriteria)).thenReturn(mockApiKeys);

        List<io.gravitee.repository.management.model.ApiKey> apiKeys = repository.findByCriteria(apiKeyCriteria);
        Assert.assertEquals(mockApiKeys, apiKeys);
    }

    @Test
    public void shouldFindByKeyAndApi_empty() throws TechnicalException {
        String apiKey = "1234-4567-7890";
        String apiId = "my-Api-Id";

        Mockito.when(cache.get("my-Api-Id", "1234-4567-7890")).thenReturn(null);
        Optional<io.gravitee.repository.management.model.ApiKey> optApiKey = repository.findByKeyAndApi(apiKey, apiId);

        Assert.assertNotNull(optApiKey);
        Assert.assertFalse(optApiKey.isPresent());
    }

    @Test
    public void shouldFindByKey_fromCache() throws TechnicalException {
        String apiKey = "1234-4567-7890";
        String apiId = "my-Api-Id";
        ApiKey mockApiKey = Mockito.mock(ApiKey.class);

        Mockito.when(cache.get("my-Api-Id", "1234-4567-7890")).thenReturn(mockApiKey);
        Optional<io.gravitee.repository.management.model.ApiKey> optApiKey = repository.findByKeyAndApi(apiKey, apiId);

        Assert.assertNotNull(optApiKey);
        Assert.assertTrue(optApiKey.isPresent());
        Assert.assertEquals(mockApiKey, optApiKey.get());
    }
}
