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
package io.gravitee.rest.api.management.v2.rest.mapper;

import static org.junit.jupiter.api.Assertions.*;

import fixtures.SubscriptionFixtures;
import io.gravitee.rest.api.management.v2.rest.model.BaseSubscription;
import io.gravitee.rest.api.management.v2.rest.model.SubscriptionConsumerConfiguration;
import io.gravitee.rest.api.model.SubscriptionEntity;
import java.util.List;
import java.util.stream.Collectors;
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

    @Test
    void should_map_CreateSubscription_to_NewSubscriptionEntity() {
        final var createSubscription = SubscriptionFixtures.aCreateSubscription();
        final var newSubscriptionEntity = subscriptionMapper.map(createSubscription);

        assertEquals(createSubscription.getPlanId(), newSubscriptionEntity.getPlan());
        assertEquals(createSubscription.getApplicationId(), newSubscriptionEntity.getApplication());
        assertEquals(createSubscription.getMetadata(), newSubscriptionEntity.getMetadata());
        SubscriptionConsumerConfiguration consumerConfig =
            (SubscriptionConsumerConfiguration) createSubscription.getConsumerConfiguration();
        assertEquals(consumerConfig.getEntrypointId(), newSubscriptionEntity.getConfiguration().getEntrypointId());
        assertEquals(consumerConfig.getChannel(), newSubscriptionEntity.getConfiguration().getChannel());
        assertConfigurationEquals(
            consumerConfig.getEntrypointConfiguration(),
            newSubscriptionEntity.getConfiguration().getEntrypointConfiguration()
        );
    }

    @Test
    void should_map_UpdateSubscription_to_UpdateSubscriptionEntity() {
        final var updateSubscription = SubscriptionFixtures.anUpdateSubscription();
        final var updateSubscriptionEntity = subscriptionMapper.map(updateSubscription, "subscriptionId");

        assertEquals("subscriptionId", updateSubscriptionEntity.getId());
        assertEquals(updateSubscription.getMetadata(), updateSubscriptionEntity.getMetadata());
        SubscriptionConsumerConfiguration consumerConfig =
            (SubscriptionConsumerConfiguration) updateSubscription.getConsumerConfiguration();
        assertEquals(consumerConfig.getEntrypointId(), updateSubscriptionEntity.getConfiguration().getEntrypointId());
        assertEquals(consumerConfig.getChannel(), updateSubscriptionEntity.getConfiguration().getChannel());
        assertConfigurationEquals(
            consumerConfig.getEntrypointConfiguration(),
            updateSubscriptionEntity.getConfiguration().getEntrypointConfiguration()
        );
    }

    @Test
    void should_map_RejectSubscription_to_ProcessSubscriptionEntity() {
        final var rejectSubscription = SubscriptionFixtures.aRejectSubscription();
        final var processSubscriptionEntity = subscriptionMapper.map(rejectSubscription, "subscriptionId");

        assertEquals("subscriptionId", processSubscriptionEntity.getId());
        assertEquals(rejectSubscription.getReason(), processSubscriptionEntity.getReason());
        assertNull(processSubscriptionEntity.getCustomApiKey());
        assertNull(processSubscriptionEntity.getStartingAt());
        assertNull(processSubscriptionEntity.getEndingAt());
        assertFalse(processSubscriptionEntity.isAccepted());
    }

    @Test
    void should_map_TransferSubscription_to_TransferSubscriptionEntity() {
        final var transferSubscription = SubscriptionFixtures.aTransferSubscription();
        final var transferSubscriptionEntity = subscriptionMapper.map(transferSubscription, "subscriptionId");

        assertEquals("subscriptionId", transferSubscriptionEntity.getId());
        assertEquals(transferSubscription.getPlanId(), transferSubscriptionEntity.getPlan());
    }

    @Test
    void should_map_core_SubscriptionEntity_to_Subscription() {
        io.gravitee.apim.core.subscription.model.SubscriptionEntity coreEntity =
            io.gravitee.apim.core.subscription.model.SubscriptionEntity.builder()
                .id("sub-1")
                .apiId("api-1")
                .planId("plan-1")
                .applicationId("app-1")
                .status(io.gravitee.apim.core.subscription.model.SubscriptionEntity.Status.ACCEPTED)
                .requestMessage("request")
                .reasonMessage("reason")
                .subscribedBy("user-1")
                .processedBy("admin-1")
                .createdAt(java.time.ZonedDateTime.now())
                .updatedAt(java.time.ZonedDateTime.now())
                .build();

        final var subscription = subscriptionMapper.map(coreEntity);

        assertNotNull(subscription);
        assertEquals("sub-1", subscription.getId());
        assertEquals("api-1", subscription.getApi().getId());
        assertEquals("plan-1", subscription.getPlan().getId());
        assertEquals("app-1", subscription.getApplication().getId());
        assertEquals("request", subscription.getConsumerMessage());
        assertEquals("reason", subscription.getPublisherMessage());
        assertEquals("user-1", subscription.getSubscribedBy().getId());
        assertEquals("admin-1", subscription.getProcessedBy().getId());
    }

    @Test
    void should_map_ApiKeyEntity_to_ApiKey() {
        final var apiKeyEntity = SubscriptionFixtures.anApiKeyEntity();
        final var apiKey = subscriptionMapper.mapToApiKey(apiKeyEntity);

        assertEquals(apiKeyEntity.getId(), apiKey.getId());
        assertEquals(apiKeyEntity.getKey(), apiKey.getKey());
        assertEquals(apiKeyEntity.getApplication().getId(), apiKey.getApplication().getId());
        assertEquals(apiKeyEntity.getApplication().getName(), apiKey.getApplication().getName());
        assertEquals(apiKeyEntity.getApplication().getDescription(), apiKey.getApplication().getDescription());
        assertEquals(apiKeyEntity.getDaysToExpirationOnLastNotification(), apiKey.getDaysToExpirationOnLastNotification());

        assertEquals(apiKeyEntity.isRevoked(), apiKey.getRevoked());
        assertEquals(apiKeyEntity.isExpired(), apiKey.getExpired());
        assertEquals(apiKeyEntity.isPaused(), apiKey.getPaused());

        final List<String> subscriptionIds = apiKey.getSubscriptions().stream().map(BaseSubscription::getId).collect(Collectors.toList());
        assertTrue(apiKeyEntity.getSubscriptions().stream().map(SubscriptionEntity::getId).allMatch(subscriptionIds::contains));

        assertEquals(apiKeyEntity.getCreatedAt().getTime(), apiKey.getCreatedAt().toInstant().toEpochMilli());
        assertEquals(apiKeyEntity.getUpdatedAt().getTime(), apiKey.getUpdatedAt().toInstant().toEpochMilli());
        assertEquals(apiKeyEntity.getExpireAt().getTime(), apiKey.getExpireAt().toInstant().toEpochMilli());
        assertEquals(apiKeyEntity.getRevokedAt().getTime(), apiKey.getRevokedAt().toInstant().toEpochMilli());
    }
}
