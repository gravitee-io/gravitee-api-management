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
package fixtures;

import io.gravitee.rest.api.management.v2.rest.model.*;
import io.gravitee.rest.api.model.SubscriptionConfigurationEntity;
import io.gravitee.rest.api.model.SubscriptionConsumerStatus;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.SubscriptionStatus;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.Map;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SubscriptionFixtures {

    private static final SubscriptionEntity.SubscriptionEntityBuilder BASE_SUBSCRIPTION_ENTITY = SubscriptionEntity
        .builder()
        .id("my-subscription")
        .api("my-api")
        .plan("my-plan")
        .application("my-application")
        .createdAt(new Date())
        .updatedAt(new Date())
        .closedAt(new Date())
        .endingAt(new Date())
        .pausedAt(new Date())
        .processedAt(new Date())
        .status(SubscriptionStatus.ACCEPTED)
        .consumerPausedAt(new Date())
        .consumerStatus(SubscriptionConsumerStatus.STOPPED)
        .daysToExpirationOnLastNotification(12)
        .security("api-key")
        .configuration(
            SubscriptionConfigurationEntity
                .builder()
                .entrypointConfiguration("{\"nice\": \"config\"}")
                .entrypointId("entrypoint-id")
                .channel("channel")
                .build()
        )
        .request("request")
        .reason("reason")
        .metadata(Map.of("meta1", "value1", "meta2", "value2"))
        .subscribedBy("subscribed-by")
        .processedBy("processed-by");

    private static final CreateSubscription.CreateSubscriptionBuilder BASE_CREATE_SUBSCRIPTION = CreateSubscription
        .builder()
        .planId("my-plan")
        .applicationId("my-application")
        .customApiKey("custom")
        .consumerConfiguration(
            SubscriptionConsumerConfiguration
                .builder()
                .entrypointConfiguration("{\"nice\": \"config\"}")
                .entrypointId("entrypoint-id")
                .channel("channel")
                .build()
        )
        .metadata(Map.of("meta1", "value1", "meta2", "value2"));

    private static final UpdateSubscription.UpdateSubscriptionBuilder BASE_UPDATE_SUBSCRIPTION = UpdateSubscription
        .builder()
        .startingAt(OffsetDateTime.now())
        .endingAt(OffsetDateTime.now())
        .consumerConfiguration(
            SubscriptionConsumerConfiguration
                .builder()
                .entrypointConfiguration("{\"nice\": \"config\"}")
                .entrypointId("entrypoint-id")
                .channel("channel")
                .build()
        )
        .metadata(Map.of("meta1", "value1", "meta2", "value2"));

    public static SubscriptionEntity aSubscriptionEntity() {
        return BASE_SUBSCRIPTION_ENTITY.build();
    }

    public static CreateSubscription aCreateSubscription() {
        return BASE_CREATE_SUBSCRIPTION.build();
    }

    public static UpdateSubscription anUpdateSubscription() {
        return BASE_UPDATE_SUBSCRIPTION.build();
    }

    public static VerifySubscription aVerifySubscription() {
        return VerifySubscription.builder().applicationId("my-application").apiKey("custom").build();
    }

    public static AcceptSubscription anAcceptSubscription() {
        return AcceptSubscription
            .builder()
            .customApiKey("custom")
            .reason("reason")
            .startingAt(OffsetDateTime.now())
            .endingAt(OffsetDateTime.now())
            .build();
    }

    public static RejectSubscription aRejectSubscription() {
        return RejectSubscription.builder().reason("reason").build();
    }

    public static TransferSubscription aTransferSubscription() {
        return TransferSubscription.builder().planId("other-plan").build();
    }
}
