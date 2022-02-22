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
import io.gravitee.rest.api.service.common.UuidString;
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

        Set<ApiKey> keys = Set.of(key1, key2, key3);

        when(apiKeyRepository.findAll()).thenReturn(keys);

        upgrader.processOneShotUpgrade();

        verify(apiKeyRepository, times(1)).findAll();
        verify(apiKeyRepository, times(3)).update(argThat(keys::contains));

        assertEquals(expectedApiKey(key1).getSubscriptions(), key1.getSubscriptions());
        assertEquals(expectedApiKey(key2).getSubscriptions(), key2.getSubscriptions());
        assertEquals(expectedApiKey(key3).getSubscriptions(), key3.getSubscriptions());
    }

    @SuppressWarnings("removal")
    private static ApiKey apiKeyWithSubscription(String subscription) {
        ApiKey key = new ApiKey();
        key.setId(UuidString.generateRandom());
        key.setSubscription(subscription);
        return key;
    }

    @SuppressWarnings("removal")
    private static ApiKey expectedApiKey(ApiKey apikey) {
        ApiKey key = new ApiKey();
        key.setId(apikey.getId());
        key.setSubscription(apikey.getSubscription());
        key.setSubscriptions(List.of(apikey.getSubscription()));
        return key;
    }
}
