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

import io.gravitee.gateway.handlers.api.services.SubscriptionService;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.SubscriptionRepository;
import io.gravitee.repository.management.api.search.SubscriptionCriteria;
import io.gravitee.repository.management.model.Subscription;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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
    private SubscriptionService subscriptionService;

    @Before
    public void setup() {
        subscriptionRefresher.setSubscriptionRepository(subscriptionRepository);
        subscriptionRefresher.setSubscriptionService(subscriptionService);
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

        // all subscriptions have been saved in cache
        verify(subscriptionService, times(1)).save(argThat(sub -> sub.getId().equals("sub-1")));
        verify(subscriptionService, times(1)).save(argThat(sub -> sub.getId().equals("sub-2")));
        verify(subscriptionService, times(1)).save(argThat(sub -> sub.getId().equals("sub-3")));

        verifyNoMoreInteractions(subscriptionService);
    }

    private Subscription buildTestSubscription(String id) {
        Subscription subscription = new Subscription();
        subscription.setId(id);
        return subscription;
    }
}
