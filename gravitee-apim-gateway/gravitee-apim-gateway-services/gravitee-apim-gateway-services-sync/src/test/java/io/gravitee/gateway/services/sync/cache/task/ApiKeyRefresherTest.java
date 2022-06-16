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
package io.gravitee.gateway.services.sync.cache.task;

import static org.mockito.Mockito.*;

import io.gravitee.gateway.handlers.api.services.ApiKeyService;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.api.SubscriptionRepository;
import io.gravitee.repository.management.api.search.ApiKeyCriteria;
import io.gravitee.repository.management.model.ApiKey;
import io.gravitee.repository.management.model.Subscription;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ApiKeyRefresherTest {

    @InjectMocks
    private FullApiKeyRefresher apiKeyRefresher;

    @Mock
    private ApiKeyRepository apiKeyRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private ApiKeyService apiKeyService;

    @Before
    public void setup() {
        apiKeyRefresher.setApiKeyRepository(apiKeyRepository);
        apiKeyRefresher.setSubscriptionRepository(subscriptionRepository);
        apiKeyRefresher.setApiKeyService(apiKeyService);
    }

    @Test
    public void doRefresh_should_read_apikeys_from_repository_and_cache_them() throws TechnicalException {
        ApiKeyCriteria apiKeyCriteria = mock(ApiKeyCriteria.class);

        List<ApiKey> apiKeysList = List.of(
            buildTestApiKey("key-1", List.of("sub-1", "sub-2", "sub-3")),
            buildTestApiKey("key-2", List.of()),
            buildTestApiKey("key-3", List.of("sub-1", "sub-4")),
            buildTestApiKey("key-4", List.of("sub-unknown"))
        );

        when(apiKeyRepository.findByCriteria(apiKeyCriteria)).thenReturn(apiKeysList);

        when(subscriptionRepository.findByIdIn(Set.of("sub-1", "sub-2", "sub-3", "sub-4", "sub-unknown")))
            .thenReturn(
                List.of(
                    buildTestSubscription("sub-1"),
                    buildTestSubscription("sub-2"),
                    buildTestSubscription("sub-3"),
                    buildTestSubscription("sub-4"),
                    buildTestSubscription("sub-5")
                )
            );

        apiKeyRefresher.doRefresh(apiKeyCriteria);

        // all API keys have been saved in cache
        verify(apiKeyService, times(1)).save(argThat(apiKey -> apiKey.getId().equals("key-1") && apiKey.getSubscription().equals("sub-1")));
        verify(apiKeyService, times(1)).save(argThat(apiKey -> apiKey.getId().equals("key-1") && apiKey.getSubscription().equals("sub-2")));
        verify(apiKeyService, times(1)).save(argThat(apiKey -> apiKey.getId().equals("key-1") && apiKey.getSubscription().equals("sub-3")));
        verify(apiKeyService, times(1)).save(argThat(apiKey -> apiKey.getId().equals("key-3") && apiKey.getSubscription().equals("sub-1")));
        verify(apiKeyService, times(1)).save(argThat(apiKey -> apiKey.getId().equals("key-3") && apiKey.getSubscription().equals("sub-4")));

        verifyNoMoreInteractions(apiKeyService);
    }

    private ApiKey buildTestApiKey(String id, List<String> subscriptions) {
        ApiKey apiKey = new ApiKey();
        apiKey.setId(id);
        apiKey.setSubscriptions(subscriptions);
        return apiKey;
    }

    private Subscription buildTestSubscription(String id) {
        Subscription subscription = new Subscription();
        subscription.setId(id);
        return subscription;
    }
}
