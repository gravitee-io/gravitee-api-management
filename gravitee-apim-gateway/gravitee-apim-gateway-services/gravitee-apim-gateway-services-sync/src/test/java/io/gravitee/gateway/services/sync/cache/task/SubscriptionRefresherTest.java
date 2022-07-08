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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.SubscriptionRepository;
import io.gravitee.repository.management.api.search.SubscriptionCriteria;
import io.gravitee.repository.management.model.Subscription;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SubscriptionRefresherTest {

    @InjectMocks
    private FullSubscriptionRefresher subscriptionRefresher;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private Map<String, Object> cache;

    @Before
    public void setup() {
        subscriptionRefresher.setSubscriptionRepository(subscriptionRepository);
        subscriptionRefresher.setCache(cache);
    }

    @Test
    public void doRefresh_should_read_subscriptions_from_repository_and_cache_them() throws TechnicalException {
        SubscriptionCriteria subscriptionCriteria = mock(SubscriptionCriteria.class);

        List<Subscription> subscriptionList = List.of(
            buildTestSubscription("sub-1"),
            buildTestSubscription("sub-2"),
            buildTestSubscription("sub-3")
        );

        when(subscriptionRepository.search(subscriptionCriteria)).thenReturn(subscriptionList);

        subscriptionRefresher.doRefresh(subscriptionCriteria);

        ArgumentCaptor<String> subscriptionKeyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> subscriptionCaptor = ArgumentCaptor.forClass(Object.class);

        // all subscriptions have been saved in cache
        verify(cache, times(6)).put(subscriptionKeyCaptor.capture(), subscriptionCaptor.capture());

        assertEquals("apisub-1.clientIdsub-1.plansub-1", subscriptionCaptor.getAllValues().get(0));
        verifySubscription("sub-1", (Subscription) subscriptionCaptor.getAllValues().get(1));
        assertEquals("apisub-2.clientIdsub-2.plansub-2", subscriptionCaptor.getAllValues().get(2));
        verifySubscription("sub-2", (Subscription) subscriptionCaptor.getAllValues().get(3));
        assertEquals("apisub-3.clientIdsub-3.plansub-3", subscriptionCaptor.getAllValues().get(4));
        verifySubscription("sub-3", (Subscription) subscriptionCaptor.getAllValues().get(5));
    }

    private Subscription buildTestSubscription(String id) {
        Subscription subscription = new Subscription();
        subscription.setId(id);
        subscription.setApi("api" + id);
        subscription.setApplication("application" + id);
        subscription.setClientId("clientId" + id);
        subscription.setStartingAt(new Date());
        subscription.setEndingAt(new Date(System.currentTimeMillis() + 3600000));
        subscription.setPlan("plan" + id);
        subscription.setStatus(Subscription.Status.ACCEPTED);
        return subscription;
    }

    private void verifySubscription(String id, Subscription subscription) {
        assertEquals(id, subscription.getId());
        assertEquals("api" + id, subscription.getApi());
        assertEquals("application" + id, subscription.getApplication());
        assertEquals("clientId" + id, subscription.getClientId());
        assertNotNull(subscription.getStartingAt());
        assertNotNull(subscription.getEndingAt());
        assertEquals("plan" + id, subscription.getPlan());
        assertEquals("ACCEPTED", subscription.getStatus().name());
    }
}
