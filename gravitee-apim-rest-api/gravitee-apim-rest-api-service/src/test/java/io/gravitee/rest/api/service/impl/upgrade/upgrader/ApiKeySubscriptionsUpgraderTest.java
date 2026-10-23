/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.service.impl.upgrade.upgrader;

import static org.mockito.Mockito.*;

import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.model.ApiKey;
import io.gravitee.rest.api.service.common.UuidString;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.Assert;
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

    @Test(expected = UpgraderException.class)
    public void upgrade_should_failed_because_of_exception() throws TechnicalException, UpgraderException {
        when(apiKeyRepository.findAll()).thenThrow(new RuntimeException());

        upgrader.upgrade();

        verify(apiKeyRepository, times(1)).findAll();
        verifyNoMoreInteractions(apiKeyRepository);
    }

    @Test
    public void upgrade_should_attach_the_legacy_subscription_to_the_subscriptions_of_the_key() throws Exception {
        ApiKey legacyOnly = apiKeyWithSubscription("subscription-1");
        ApiKey legacyNotMergedYet = apiKeyWithSubscription("subscription-4", new ArrayList<>(List.of("subscription-2", "subscription-3")));
        ApiKey legacyAlreadyMerged = apiKeyWithSubscription("subscription-3", new ArrayList<>(List.of("subscription-2", "subscription-3")));
        ApiKey withoutLegacy = apiKeyWithSubscription(null, new ArrayList<>(List.of("subscription-2")));
        ApiKey empty = apiKeyWithSubscription(null, null);
        when(apiKeyRepository.findAll()).thenReturn(Set.of(legacyOnly, legacyNotMergedYet, legacyAlreadyMerged, withoutLegacy, empty));

        upgrader.upgrade();

        verify(apiKeyRepository).addSubscription(legacyOnly.getId(), "subscription-1");
        verify(apiKeyRepository).addSubscription(legacyNotMergedYet.getId(), "subscription-4");
        verify(apiKeyRepository, never()).addSubscription(eq(legacyAlreadyMerged.getId()), any());
        verify(apiKeyRepository, never()).addSubscription(eq(withoutLegacy.getId()), any());
        verify(apiKeyRepository, never()).addSubscription(eq(empty.getId()), any());
    }

    @Test
    public void test_order() {
        Assert.assertEquals(UpgraderOrder.API_KEY_SUBSCRIPTIONS_UPGRADER, upgrader.getOrder());
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
