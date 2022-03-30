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
package io.gravitee.rest.api.service.impl.upgrade;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.model.ApiKey;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.UuidString;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiKeySubscriptionsUpgraderTest {

    @InjectMocks
    ApiKeySubscriptionsUpgrader upgrader;

    @Mock
    ApiKeyRepository apiKeyRepository;

    @Test
    public void testUpgrade_shouldCreateListOfSubscriptionsFromSubscription() throws TechnicalException {
        ApiKey key1 = apiKeyWithSubscription("subscription-1");
        ApiKey key2 = apiKeyWithSubscription("subscription-2");
        ApiKey key3 = apiKeyWithSubscription("subscription-3");
        ApiKey key4 = apiKeyWithSubscription(null);
        ApiKey key5 = apiKeyWithSubscription(null, null);
        ApiKey key6 = apiKeyWithSubscription(null, new ArrayList<>(List.of("subscription-2", "subscription-3")));
        ApiKey key7 = apiKeyWithSubscription("subscription-4", new ArrayList<>(List.of("subscription-2", "subscription-3")));
        ApiKey key8 = apiKeyWithSubscription("subscription-3", new ArrayList<>(List.of("subscription-2", "subscription-3")));

        Set<ApiKey> keys = Set.of(key1, key2, key3, key4, key5, key6, key7, key8);

        when(apiKeyRepository.findAll()).thenReturn(keys);

        upgrader.processOneShotUpgrade(GraviteeContext.getExecutionContext());

        verify(apiKeyRepository, times(1)).findAll();
        verify(apiKeyRepository, times(8)).update(argThat(keys::contains));

        assertEquals(List.of("subscription-1"), key1.getSubscriptions());
        assertEquals(List.of("subscription-2"), key2.getSubscriptions());
        assertEquals(List.of("subscription-3"), key3.getSubscriptions());
        assertEquals(List.of(), key4.getSubscriptions());
        assertEquals(List.of(), key5.getSubscriptions());
        assertEquals(List.of("subscription-2", "subscription-3"), key6.getSubscriptions());
        assertEquals(List.of("subscription-2", "subscription-3", "subscription-4"), key7.getSubscriptions());
        assertEquals(List.of("subscription-2", "subscription-3"), key8.getSubscriptions());
    }

    @SuppressWarnings("removal")
    private static ApiKey apiKeyWithSubscription(String subscription) {
        ApiKey key = new ApiKey();
        key.setId(UuidString.generateRandom());
        key.setSubscription(subscription);
        return key;
    }

    private static ApiKey apiKeyWithSubscription(String subscription, List<String> subscriptions) {
        ApiKey key = new ApiKey();
        key.setId(UuidString.generateRandom());
        key.setSubscription(subscription);
        key.setSubscriptions(subscriptions);
        return key;
    }
}
