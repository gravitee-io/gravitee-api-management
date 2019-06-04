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

import java.time.Instant;
import java.util.Date;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.SubscriptionStatus;
import io.gravitee.rest.api.portal.rest.model.Subscription;
import io.gravitee.rest.api.portal.rest.model.Subscription.StatusEnum;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class SubscriptionMapperTest {

    private static final String API = "my-api";
    private static final String APPLICATION = "my-application";
    private static final String PLAN = "my-plan";
    private static final String SUBSCRIPTION = "my-subscription";

    private SubscriptionEntity subscriptionEntity;

    @InjectMocks
    private SubscriptionMapper subscriptionMapper;
    
    @Test
    public void testConvert() {
        Instant now = Instant.now();
        Date nowDate = Date.from(now);

        //init
        subscriptionEntity = new SubscriptionEntity();
       
        subscriptionEntity.setApi(API);
        subscriptionEntity.setApplication(APPLICATION);
        subscriptionEntity.setClientId(SUBSCRIPTION);
        subscriptionEntity.setClosedAt(nowDate);
        subscriptionEntity.setCreatedAt(nowDate);
        subscriptionEntity.setEndingAt(nowDate);
        subscriptionEntity.setId(SUBSCRIPTION);
        subscriptionEntity.setPausedAt(nowDate);
        subscriptionEntity.setPlan(PLAN);
        subscriptionEntity.setProcessedAt(nowDate);
        subscriptionEntity.setProcessedBy(SUBSCRIPTION);
        subscriptionEntity.setReason(SUBSCRIPTION);
        subscriptionEntity.setRequest(SUBSCRIPTION);
        subscriptionEntity.setStartingAt(nowDate);
        subscriptionEntity.setStatus(SubscriptionStatus.ACCEPTED);
        subscriptionEntity.setSubscribedBy(SUBSCRIPTION);
        subscriptionEntity.setUpdatedAt(nowDate);
        
        
        //Test
        Subscription subscription = subscriptionMapper.convert(subscriptionEntity);
        assertNotNull(subscription);
        
        assertEquals(API, subscription.getApi());
        assertEquals(APPLICATION, subscription.getApplication());
        assertEquals(now.toEpochMilli(), subscription.getCreatedAt().toInstant().toEpochMilli());
        assertEquals(now.toEpochMilli(), subscription.getEndAt().toInstant().toEpochMilli());
        assertEquals(SUBSCRIPTION, subscription.getId());
        assertEquals(PLAN, subscription.getPlan());
        assertEquals(now.toEpochMilli(), subscription.getProcessedAt().toInstant().toEpochMilli());
        assertEquals(SUBSCRIPTION, subscription.getRequest());
        assertEquals(now.toEpochMilli(), subscription.getStartAt().toInstant().toEpochMilli());
        assertEquals(StatusEnum.ACCEPTED, subscription.getStatus());
        
    }
    
}
