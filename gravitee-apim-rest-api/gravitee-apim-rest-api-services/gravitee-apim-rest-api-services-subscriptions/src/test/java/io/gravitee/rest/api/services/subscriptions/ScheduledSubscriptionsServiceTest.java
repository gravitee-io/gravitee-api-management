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
package io.gravitee.rest.api.services.subscriptions;

import static org.mockito.Mockito.*;

import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.SubscriptionStatus;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.SubscriptionService;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

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

        SubscriptionEntity endDateInThePast = mock(SubscriptionEntity.class);
        when(endDateInThePast.getId()).thenReturn("end_date_in_the_past");

        when(apiService.findAllLight()).thenReturn(Collections.singleton(apiEntity));

        when(
            subscriptionService.search(
                argThat(
                    subscriptionQuery ->
                        subscriptionQuery.getApis().equals(Collections.singleton("API_ID")) &&
                        subscriptionQuery.getStatuses().equals(Collections.singleton(SubscriptionStatus.ACCEPTED)) &&
                        subscriptionQuery.getEndingAtBefore() > 0
                )
            )
        )
            .thenReturn(new HashSet<>(Collections.singletonList(endDateInThePast)));

        service.run();

        verify(apiService, times(1)).findAllLight();
        verify(subscriptionService, times(1)).close("end_date_in_the_past");
    }
}
