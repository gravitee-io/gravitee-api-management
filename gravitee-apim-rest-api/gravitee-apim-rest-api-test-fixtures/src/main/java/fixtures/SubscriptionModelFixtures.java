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
package fixtures;

import io.gravitee.rest.api.model.ApiKeyEntity;
import io.gravitee.rest.api.model.SubscriptionConfigurationEntity;
import io.gravitee.rest.api.model.SubscriptionConsumerStatus;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.SubscriptionStatus;
import java.util.Date;
import java.util.Map;
import java.util.Set;

public class SubscriptionModelFixtures {

    private SubscriptionModelFixtures() {}

    private static final SubscriptionEntity.SubscriptionEntityBuilder BASE_SUBSCRIPTION_ENTITY = SubscriptionEntity.builder()
        .id("my-subscription")
        .api("my-api")
        .referenceId("my-api")
        .referenceType("API")
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
            SubscriptionConfigurationEntity.builder()
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

    protected static final ApiKeyEntity.ApiKeyEntityBuilder BASE_API_KEY_ENTITY = ApiKeyEntity.builder()
        .id("my-api-key")
        .key("custom")
        .application(ApplicationModelFixtures.anApplicationEntity())
        .createdAt(new Date())
        .updatedAt(new Date())
        .expireAt(new Date())
        .revokedAt(new Date())
        .updatedAt(new Date())
        .daysToExpirationOnLastNotification(10)
        .expired(true)
        .paused(true)
        .revoked(true)
        .subscriptions(Set.of(BASE_SUBSCRIPTION_ENTITY.build()))
        .revokedAt(new Date());

    public static SubscriptionEntity aSubscriptionEntity() {
        return BASE_SUBSCRIPTION_ENTITY.build();
    }

    public static ApiKeyEntity anApiKeyEntity() {
        return BASE_API_KEY_ENTITY.build();
    }
}
