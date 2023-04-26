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
package io.gravitee.gateway.services.sync.cache;

import static io.gravitee.repository.management.model.Subscription.Status.ACCEPTED;
import static io.gravitee.repository.management.model.Subscription.Status.CLOSED;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import io.gravitee.gateway.handlers.api.definition.ApiKey;
import io.gravitee.node.api.cache.CacheConfiguration;
import io.gravitee.node.cache.standalone.StandaloneCache;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.api.SubscriptionRepository;
import io.gravitee.repository.management.api.search.SubscriptionCriteria;
import io.gravitee.repository.management.model.Subscription;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junitpioneer.jupiter.RetryingTest;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ApiKeysFetchingCacheTest {

    private ApiKeysFetchingCache apiKeysCache;

    @Mock
    private ApiKeyRepository apiKeyRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private ApiKey cachedApiKey1;

    @Mock
    private ApiKey cachedApiKey2;

    @Mock
    private ApiKey cachedApiKey3;

    @Before
    public void initInternalCache() {
        apiKeysCache =
            new ApiKeysFetchingCache(
                new StandaloneCache<>("ApiKeysCacheTest", new CacheConfiguration()),
                apiKeyRepository,
                subscriptionRepository
            );

        apiKeysCache.cache.put("another-apiId.another-key", Optional.of(cachedApiKey1));
        apiKeysCache.cache.put("api-id.key-id", Optional.of(cachedApiKey2));
        apiKeysCache.cache.put("another-apiId2.another-key", Optional.empty());
    }

    @Test
    public void shouldReturnApiKeyWhenItExists() throws TechnicalException {
        Optional<ApiKey> resultKey = apiKeysCache.get("api-id", "key-id");

        assertTrue(resultKey.isPresent());
        assertSame(cachedApiKey2, resultKey.get());
    }

    @Test
    public void shouldReturnEmptyWhenItExistAsEmpty() throws TechnicalException {
        Optional<ApiKey> resultKey = apiKeysCache.get("another-apiId2", "another-key");

        assertTrue(resultKey.isEmpty());
    }

    @RetryingTest(maxAttempts = 3)
    public void shouldReturnEmptyAndSaveEmptyInCacheWhenItDoesntExist_causeCantFindApiKeyInRepository() throws TechnicalException {
        when(apiKeyRepository.findByKeyAndApi("key-id", "api-id6")).thenReturn(Optional.empty());

        Optional<ApiKey> result = apiKeysCache.get("api-id6", "key-id");

        assertTrue(result.isEmpty());
        assertTrue(apiKeysCache.cache.get("api-id6.key-id").isEmpty());
        assertEquals(4, apiKeysCache.cache.size());
        verifyNoInteractions(subscriptionRepository);
    }

    @RetryingTest(maxAttempts = 3)
    public void shouldReturnEmptyAndSaveEmptyInCacheWhenItDoesntExist_causeCantFindSubscriptionInRepository() throws TechnicalException {
        io.gravitee.repository.management.model.ApiKey apiKey = new io.gravitee.repository.management.model.ApiKey();
        apiKey.setId("my-test-api-key");
        apiKey.setKey("key-id");
        apiKey.setRevoked(false);
        apiKey.setPaused(false);
        apiKey.setSubscriptions(List.of("sub-1", "sub2", "sub-3"));

        when(apiKeyRepository.findByKeyAndApi("key-id", "api-id6")).thenReturn(Optional.of(apiKey));
        ArgumentCaptor<SubscriptionCriteria> criteriaArgumentCaptor = ArgumentCaptor.forClass(SubscriptionCriteria.class);
        when(subscriptionRepository.search(criteriaArgumentCaptor.capture())).thenReturn(List.of());

        Optional<ApiKey> result = apiKeysCache.get("api-id6", "key-id");

        assertTrue(result.isEmpty());
        assertTrue(apiKeysCache.cache.get("api-id6.key-id").isEmpty());
        assertEquals(4, apiKeysCache.cache.size());
        assertEquals(List.of("api-id6"), criteriaArgumentCaptor.getValue().getApis());
        assertEquals(List.of("sub-1", "sub2", "sub-3"), criteriaArgumentCaptor.getValue().getIds());
    }

    @RetryingTest(maxAttempts = 3)
    public void shouldReturnEmptyAndSaveEmptyInCacheWhenItDoesntExist_causeFoundButSubscriptionClosed() throws TechnicalException {
        io.gravitee.repository.management.model.ApiKey apiKey = new io.gravitee.repository.management.model.ApiKey();
        apiKey.setId("my-test-api-key");
        apiKey.setKey("key-id");
        apiKey.setRevoked(false);
        apiKey.setPaused(false);
        apiKey.setSubscriptions(List.of("sub-1", "sub2", "sub-3"));

        Subscription subscription = new Subscription();
        subscription.setApi("api-id6");
        subscription.setStatus(CLOSED);

        when(apiKeyRepository.findByKeyAndApi("key-id", "api-id6")).thenReturn(Optional.of(apiKey));
        ArgumentCaptor<SubscriptionCriteria> criteriaArgumentCaptor = ArgumentCaptor.forClass(SubscriptionCriteria.class);
        when(subscriptionRepository.search(criteriaArgumentCaptor.capture())).thenReturn(List.of(subscription));

        Optional<ApiKey> result = apiKeysCache.get("api-id6", "key-id");

        assertTrue(result.isEmpty());
        assertTrue(apiKeysCache.cache.get("api-id6.key-id").isEmpty());
        assertEquals(4, apiKeysCache.cache.size());
        assertEquals(List.of("api-id6"), criteriaArgumentCaptor.getValue().getApis());
        assertEquals(List.of("sub-1", "sub2", "sub-3"), criteriaArgumentCaptor.getValue().getIds());
    }

    @RetryingTest(maxAttempts = 3)
    public void shouldReturnKeyAndSaveKeyInCacheWhenItDoesntExistButFoundInRepository() throws TechnicalException {
        io.gravitee.repository.management.model.ApiKey apiKey = new io.gravitee.repository.management.model.ApiKey();
        apiKey.setId("my-test-api-key");
        apiKey.setKey("key-id");
        apiKey.setRevoked(false);
        apiKey.setPaused(false);
        apiKey.setSubscriptions(List.of("sub-1", "sub2", "sub-3"));

        Subscription subscription = new Subscription();
        subscription.setApi("api-id6");
        subscription.setStatus(ACCEPTED);

        when(apiKeyRepository.findByKeyAndApi("key-id", "api-id6")).thenReturn(Optional.of(apiKey));
        ArgumentCaptor<SubscriptionCriteria> criteriaArgumentCaptor = ArgumentCaptor.forClass(SubscriptionCriteria.class);
        when(subscriptionRepository.search(criteriaArgumentCaptor.capture())).thenReturn(List.of(subscription));

        Optional<ApiKey> result = apiKeysCache.get("api-id6", "key-id");

        assertTrue(result.isPresent());
        assertEquals("my-test-api-key", result.get().getId());
        assertSame(result.get(), apiKeysCache.cache.get("api-id6.key-id").get());
        assertEquals(4, apiKeysCache.cache.size());
        assertEquals(List.of("api-id6"), criteriaArgumentCaptor.getValue().getApis());
        assertEquals(List.of("sub-1", "sub2", "sub-3"), criteriaArgumentCaptor.getValue().getIds());
    }

    @Test
    public void shouldSearchInRepositoryOnlyOnceWhenParallelCalls() throws TechnicalException {
        io.gravitee.repository.management.model.ApiKey apiKey = new io.gravitee.repository.management.model.ApiKey();
        apiKey.setId("my-test-api-key");
        apiKey.setKey("key-id");
        apiKey.setRevoked(false);
        apiKey.setPaused(false);
        apiKey.setSubscriptions(List.of("sub-1", "sub2", "sub-3"));

        Subscription subscription = new Subscription();
        subscription.setApi("api-id6");
        subscription.setStatus(ACCEPTED);

        when(apiKeyRepository.findByKeyAndApi("key-id", "api-id6"))
            .thenAnswer(invocation -> {
                // api key repository takes 1 second to search api key
                Thread.sleep(1_000);
                return Optional.of(apiKey);
            });

        ArgumentCaptor<SubscriptionCriteria> criteriaArgumentCaptor = ArgumentCaptor.forClass(SubscriptionCriteria.class);
        when(subscriptionRepository.search(criteriaArgumentCaptor.capture())).thenReturn(List.of(subscription));

        // call api cache get 100 times in parrallel
        CompletableFuture<Optional<ApiKey>> futures[] = new CompletableFuture[100];
        for (int i = 0; i < 100; i++) {
            futures[i] =
                CompletableFuture.supplyAsync(() -> {
                    try {
                        return apiKeysCache.get("api-id6", "key-id");
                    } catch (TechnicalException e) {
                        throw new RuntimeException(e);
                    }
                });
        }

        // ensure 100 calls have returned the same API key
        CompletableFuture
            .allOf(futures)
            .thenAccept(v -> {
                for (int i = 1; i < 100; i++) {
                    Optional<ApiKey> result = futures[i].join();
                    assertTrue(result.isPresent());
                    assertEquals("my-test-api-key", result.get().getId());
                    assertSame(result.get(), apiKeysCache.cache.get("api-id6.key-id").get());
                }
            })
            .join();

        // ensure repository have been called only once during 100 calls
        verify(apiKeyRepository, times(1)).findByKeyAndApi(any(), any());
        verify(subscriptionRepository, times(1)).search(any());
    }

    @RetryingTest(maxAttempts = 3)
    public void shouldReturnEmptyIfApiKeyFetchThrowsException() throws TechnicalException {
        when(apiKeyRepository.findByKeyAndApi(any(), any())).thenThrow(TechnicalException.class);

        Optional<ApiKey> result = apiKeysCache.get("api-id6", "key-id");

        assertTrue(result.isEmpty());
        verifyNoInteractions(subscriptionRepository);
    }

    @Test
    public void shouldReturnEmptyIfSubscriptionFetchThrowsException() throws TechnicalException {
        io.gravitee.repository.management.model.ApiKey apiKey = new io.gravitee.repository.management.model.ApiKey();

        when(apiKeyRepository.findByKeyAndApi("key-id", "api-id6")).thenReturn(Optional.of(apiKey));
        when(subscriptionRepository.search(any())).thenThrow(TechnicalException.class);

        Optional<ApiKey> result = apiKeysCache.get("api-id6", "key-id");

        assertTrue(result.isEmpty());
    }
}
