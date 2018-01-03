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
package io.gravitee.management.services.subscriptions;

import io.gravitee.management.model.ApiEntity;
import io.gravitee.management.model.SubscriptionEntity;
import io.gravitee.management.model.SubscriptionStatus;
import io.gravitee.management.model.subscription.SubscriptionQuery;
import io.gravitee.management.service.ApiService;
import io.gravitee.management.service.SubscriptionService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;

import static org.mockito.Mockito.*;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com) 
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ScheduledSubscriptionsServiceTest {

    @InjectMocks
    ScheduledSubscriptionsService service = new ScheduledSubscriptionsService();

    @Mock
    ApiService apiService;

    @Mock
    SubscriptionService subscriptionService;

    @Test
    public void shouldCloseOutdatedSubscriptions() {
        ApiEntity apiEntity = mock(ApiEntity.class);
        when(apiEntity.getId()).thenReturn("API_ID");
        SubscriptionEntity endDateInThePast = createSubscription(
                "end_date_in_the_past",
                SubscriptionStatus.ACCEPTED,
                new Date(0));
        SubscriptionEntity noEndDate = createSubscription(
                "no_end_date",
                SubscriptionStatus.ACCEPTED,
                null);
        SubscriptionEntity endDateInTheFuture = createSubscription(
                "end_date_in_the_future",
                SubscriptionStatus.ACCEPTED,
                new Date(Long.MAX_VALUE));
        when(apiService.findAll()).thenReturn(Collections.singleton(apiEntity));

        SubscriptionQuery query = new SubscriptionQuery();
        query.setApi(apiEntity.getId());
        query.setStatuses(Collections.singleton(SubscriptionStatus.ACCEPTED));

        when(subscriptionService.search(query)).
                thenReturn(new HashSet<>(Arrays.asList(
                        endDateInThePast,
                        noEndDate,
                        endDateInTheFuture)));

        service.run();

        verify(apiService, times(1)).findAll();
        verify(subscriptionService, times(1)).search(query);
        verify(subscriptionService, times(1)).close("end_date_in_the_past");
        verify(subscriptionService, never()).close("no_end_date");
        verify(subscriptionService, never()).close("end_date_in_the_future");
    }

    private SubscriptionEntity createSubscription(String id, SubscriptionStatus status, Date endingDate) {
        SubscriptionEntity subscriptionEntity = mock(SubscriptionEntity.class);
        when(subscriptionEntity.getId()).thenReturn(id);
        when(subscriptionEntity.getStatus()).thenReturn(status);
        when(subscriptionEntity.getEndingAt()).thenReturn(endingDate);
        return subscriptionEntity;
    }
}
