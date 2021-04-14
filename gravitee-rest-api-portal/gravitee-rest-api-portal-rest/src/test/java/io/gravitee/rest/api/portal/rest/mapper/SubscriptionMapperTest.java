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
package io.gravitee.rest.api.portal.rest.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.SubscriptionStatus;
import io.gravitee.rest.api.portal.rest.model.Subscription;
import io.gravitee.rest.api.portal.rest.model.Subscription.StatusEnum;
import java.time.Instant;
import java.util.Date;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class SubscriptionMapperTest {

    private static final String SUBSCRIPTION_API = "my-subscription-api";
    private static final String SUBSCRIPTION_APPLICATION = "my-subscription-application";
    private static final String SUBSCRIPTION_PLAN = "my-subscription-plan";
    private static final String SUBSCRIPTION_ID = "my-subscription-id";
    private static final String SUBSCRIPTION_CLIENT_ID = "my-subscription-client-id";
    private static final String SUBSCRIPTION_PROCESSED_BY = "my-subscription-processed-by";
    private static final String SUBSCRIPTION_REASON = "my-subscription-reason";
    private static final String SUBSCRIPTION_REQUEST = "my-subscription-request";
    private static final String SUBSCRIPTION_SUBSCRIBED_BY = "my-subscription-subscribed-by";

    private SubscriptionEntity subscriptionEntity;

    @InjectMocks
    private SubscriptionMapper subscriptionMapper;

    @Test
    public void testConvert() {
        Instant now = Instant.now();
        Date nowDate = Date.from(now);

        //init
        subscriptionEntity = new SubscriptionEntity();

        subscriptionEntity.setApi(SUBSCRIPTION_API);
        subscriptionEntity.setApplication(SUBSCRIPTION_APPLICATION);
        subscriptionEntity.setClientId(SUBSCRIPTION_CLIENT_ID);
        subscriptionEntity.setClosedAt(nowDate);
        subscriptionEntity.setCreatedAt(nowDate);
        subscriptionEntity.setEndingAt(nowDate);
        subscriptionEntity.setId(SUBSCRIPTION_ID);
        subscriptionEntity.setPausedAt(nowDate);
        subscriptionEntity.setPlan(SUBSCRIPTION_PLAN);
        subscriptionEntity.setProcessedAt(nowDate);
        subscriptionEntity.setProcessedBy(SUBSCRIPTION_PROCESSED_BY);
        subscriptionEntity.setReason(SUBSCRIPTION_REASON);
        subscriptionEntity.setRequest(SUBSCRIPTION_REQUEST);
        subscriptionEntity.setStartingAt(nowDate);
        subscriptionEntity.setStatus(SubscriptionStatus.ACCEPTED);
        subscriptionEntity.setSubscribedBy(SUBSCRIPTION_SUBSCRIBED_BY);
        subscriptionEntity.setUpdatedAt(nowDate);

        //Test
        Subscription subscription = subscriptionMapper.convert(subscriptionEntity);
        assertNotNull(subscription);

        assertEquals(SUBSCRIPTION_API, subscription.getApi());
        assertEquals(SUBSCRIPTION_APPLICATION, subscription.getApplication());
        assertEquals(now.toEpochMilli(), subscription.getCreatedAt().toInstant().toEpochMilli());
        assertEquals(now.toEpochMilli(), subscription.getEndAt().toInstant().toEpochMilli());
        assertEquals(SUBSCRIPTION_ID, subscription.getId());
        assertEquals(SUBSCRIPTION_PLAN, subscription.getPlan());
        assertEquals(now.toEpochMilli(), subscription.getProcessedAt().toInstant().toEpochMilli());
        assertEquals(SUBSCRIPTION_REQUEST, subscription.getRequest());
        assertEquals(now.toEpochMilli(), subscription.getStartAt().toInstant().toEpochMilli());
        assertEquals(StatusEnum.ACCEPTED, subscription.getStatus());
    }
}
