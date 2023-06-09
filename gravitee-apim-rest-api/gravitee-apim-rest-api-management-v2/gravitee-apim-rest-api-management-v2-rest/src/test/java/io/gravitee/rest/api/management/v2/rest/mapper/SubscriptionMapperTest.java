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
package io.gravitee.rest.api.management.v2.rest.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import fixtures.SubscriptionFixtures;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

public class SubscriptionMapperTest extends AbstractMapperTest {

    private final SubscriptionMapper subscriptionMapper = Mappers.getMapper(SubscriptionMapper.class);

    @Test
    void should_map_SubscriptionEntity_to_Subscription() {
        final var subscriptionEntity = SubscriptionFixtures.aSubscriptionEntity();
        final var subscription = subscriptionMapper.map(subscriptionEntity);

        assertEquals(subscriptionEntity.getId(), subscription.getId());
        assertEquals(subscriptionEntity.getApi(), subscription.getApi().getId());
        assertEquals(subscriptionEntity.getApplication(), subscription.getApplication().getId());
        assertEquals(subscriptionEntity.getCreatedAt().getTime(), subscription.getCreatedAt().toInstant().toEpochMilli());
        assertEquals(subscriptionEntity.getUpdatedAt().getTime(), subscription.getUpdatedAt().toInstant().toEpochMilli());
        assertEquals(subscriptionEntity.getClosedAt().getTime(), subscription.getClosedAt().toInstant().toEpochMilli());
        assertEquals(subscriptionEntity.getEndingAt().getTime(), subscription.getEndingAt().toInstant().toEpochMilli());
        assertEquals(subscriptionEntity.getPausedAt().getTime(), subscription.getPausedAt().toInstant().toEpochMilli());
        assertEquals(subscriptionEntity.getProcessedAt().getTime(), subscription.getProcessedAt().toInstant().toEpochMilli());
        assertEquals(subscriptionEntity.getStatus().name(), subscription.getStatus().name());
        assertEquals(subscriptionEntity.getConsumerStatus().name(), subscription.getConsumerStatus().name());
        assertEquals(subscriptionEntity.getConsumerPausedAt().getTime(), subscription.getConsumerPausedAt().toInstant().toEpochMilli());
        assertEquals(subscriptionEntity.getDaysToExpirationOnLastNotification(), subscription.getDaysToExpirationOnLastNotification());
        assertEquals(subscriptionEntity.getSubscribedBy(), subscription.getSubscribedBy().getId());
        assertEquals(subscriptionEntity.getProcessedBy(), subscription.getProcessedBy().getId());
        assertEquals(subscriptionEntity.getRequest(), subscription.getConsumerMessage());
        assertEquals(subscriptionEntity.getReason(), subscription.getPublisherMessage());
        assertEquals(subscriptionEntity.getMetadata(), subscription.getMetadata());

        assertEquals(subscriptionEntity.getConfiguration().getEntrypointId(), subscription.getConsumerConfiguration().getEntrypointId());
        assertEquals(subscriptionEntity.getConfiguration().getChannel(), subscription.getConsumerConfiguration().getChannel());
        assertConfigurationEquals(
            subscriptionEntity.getConfiguration().getEntrypointConfiguration(),
            subscription.getConsumerConfiguration().getEntrypointConfiguration()
        );
    }
}
