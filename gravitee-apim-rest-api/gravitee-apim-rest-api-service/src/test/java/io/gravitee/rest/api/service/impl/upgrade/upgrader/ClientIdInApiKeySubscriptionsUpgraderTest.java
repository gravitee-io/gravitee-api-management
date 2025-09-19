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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.SubscriptionRepository;
import io.gravitee.repository.management.model.Subscription;
import io.gravitee.rest.api.service.common.UuidString;
import java.util.List;
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
public class ClientIdInApiKeySubscriptionsUpgraderTest {

    @InjectMocks
    ClientIdInApiKeySubscriptionsUpgrader upgrader;

    @Mock
    SubscriptionRepository subscriptionRepository;

    @Test(expected = UpgraderException.class)
    public void upgrade_should_failed_because_of_exception() throws TechnicalException, UpgraderException {
        when(subscriptionRepository.search(any())).thenThrow(new RuntimeException());

        upgrader.upgrade();

        verify(subscriptionRepository, times(1)).search(any());
        verifyNoMoreInteractions(subscriptionRepository);
    }

    @Test
    public void testUpgrade_shouldRemoveClientIdFromApiKeySubscriptionsOnly() throws TechnicalException, UpgraderException {
        Subscription apiKeySubscriptionWithoutClientId = apiKeySubscription(null);
        Subscription apiKeySubscriptionWithClientId = apiKeySubscription("client-id");

        when(subscriptionRepository.search(any())).thenReturn(List.of(apiKeySubscriptionWithoutClientId, apiKeySubscriptionWithClientId));

        upgrader.upgrade();

        verify(subscriptionRepository, times(1)).update(
            argThat(
                subscription -> subscription.getId().equals(apiKeySubscriptionWithClientId.getId()) && subscription.getClientId() == null
            )
        );
    }

    @Test
    public void test_order() {
        Assert.assertEquals(UpgraderOrder.CLIENT_ID_IN_API_KEY_SUBSCRIPTIONS_UPGRADER, upgrader.getOrder());
    }

    private static Subscription apiKeySubscription(String clientId) {
        Subscription subscription = new Subscription();
        subscription.setId(UuidString.generateRandom());
        subscription.setClientId(clientId);
        return subscription;
    }
}
