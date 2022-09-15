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

import io.gravitee.node.api.cache.Cache;
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
    private Cache<String, Object> cache;

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
        verify(cache, times(9)).put(subscriptionKeyCaptor.capture(), subscriptionCaptor.capture());

        final List<String> keys = subscriptionKeyCaptor.getAllValues();

        assertEquals("sub-1", keys.get(0));
        assertEquals("apisub-1.clientIdsub-1.plansub-1", keys.get(1));
        assertEquals("apisub-1.clientIdsub-1.null", keys.get(2));
        assertEquals("sub-2", keys.get(3));
        assertEquals("apisub-2.clientIdsub-2.plansub-2", keys.get(4));
        assertEquals("apisub-2.clientIdsub-2.null", keys.get(5));
        assertEquals("sub-3", keys.get(6));
        assertEquals("apisub-3.clientIdsub-3.plansub-3", keys.get(7));
        assertEquals("apisub-3.clientIdsub-3.null", keys.get(8));

        final List<Object> subscriptionsPutInCache = subscriptionCaptor.getAllValues();
        assertEquals("apisub-1.clientIdsub-1.plansub-1", subscriptionsPutInCache.get(0));
        verifySubscription("sub-1", (Subscription) subscriptionsPutInCache.get(1));
        verifySubscription("sub-1", (Subscription) subscriptionsPutInCache.get(2));
        assertEquals("apisub-2.clientIdsub-2.plansub-2", subscriptionsPutInCache.get(3));
        verifySubscription("sub-2", (Subscription) subscriptionsPutInCache.get(4));
        verifySubscription("sub-2", (Subscription) subscriptionsPutInCache.get(5));
        assertEquals("apisub-3.clientIdsub-3.plansub-3", subscriptionsPutInCache.get(6));
        verifySubscription("sub-3", (Subscription) subscriptionsPutInCache.get(7));
        verifySubscription("sub-3", (Subscription) subscriptionsPutInCache.get(8));
    }

    @Test
    public void doRefresh_should_evict_from_cache_when_subscription_closed_or_paused() throws TechnicalException {
        final Subscription subscriptionClosed = buildTestSubscription("sub-1");
        final Subscription subscriptionPaused = buildTestSubscription("sub-2");

        subscriptionClosed.setStatus(Subscription.Status.CLOSED);
        subscriptionPaused.setStatus(Subscription.Status.PAUSED);

        when(cache.get("sub-1")).thenReturn("apisub-1.clientIdsub-1.plansub-1");
        when(cache.get("apisub-1.clientIdsub-1.plansub-1")).thenReturn(subscriptionClosed);
        when(cache.get("sub-2")).thenReturn("apisub-2.clientIdsub-2.plansub-2");
        when(cache.get("apisub-2.clientIdsub-2.plansub-2")).thenReturn(subscriptionPaused);

        when(subscriptionRepository.search(any())).thenReturn(List.of(subscriptionClosed, subscriptionPaused));

        subscriptionRefresher.doRefresh(mock(SubscriptionCriteria.class));

        verify(cache).evict("sub-1");
        verify(cache).evict("apisub-1.clientIdsub-1.plansub-1");
        verify(cache).evict("apisub-1.clientIdsub-1.null");
        verify(cache).evict("sub-2");
        verify(cache).evict("apisub-2.clientIdsub-2.plansub-2");
        verify(cache).evict("apisub-2.clientIdsub-2.null");
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
