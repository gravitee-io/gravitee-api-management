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

import io.gravitee.rest.api.management.v2.rest.model.AcceptSubscription;
import io.gravitee.rest.api.management.v2.rest.model.CreateSubscription;
import io.gravitee.rest.api.management.v2.rest.model.RejectSubscription;
import io.gravitee.rest.api.management.v2.rest.model.RenewApiKey;
import io.gravitee.rest.api.management.v2.rest.model.SubscriptionConsumerConfiguration;
import io.gravitee.rest.api.management.v2.rest.model.TransferSubscription;
import io.gravitee.rest.api.management.v2.rest.model.UpdateApiKey;
import io.gravitee.rest.api.management.v2.rest.model.UpdateSubscription;
import io.gravitee.rest.api.management.v2.rest.model.VerifySubscription;
import io.gravitee.rest.api.model.ApiKeyEntity;
import io.gravitee.rest.api.model.SubscriptionEntity;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SubscriptionFixtures {

    private SubscriptionFixtures() {}

    private static final CreateSubscription.CreateSubscriptionBuilder BASE_CREATE_SUBSCRIPTION = CreateSubscription.builder()
        .planId("my-plan")
        .applicationId("my-application")
        .customApiKey("custom")
        .consumerConfiguration(
            SubscriptionConsumerConfiguration.builder()
                .entrypointConfiguration("{\"nice\": \"config\"}")
                .entrypointId("entrypoint-id")
                .channel("channel")
                .build()
        )
        .metadata(Map.of("meta1", "value1", "meta2", "value2"));

    private static final UpdateSubscription.UpdateSubscriptionBuilder BASE_UPDATE_SUBSCRIPTION = UpdateSubscription.builder()
        .startingAt(OffsetDateTime.now())
        .endingAt(OffsetDateTime.now())
        .consumerConfiguration(
            SubscriptionConsumerConfiguration.builder()
                .entrypointConfiguration("{\"nice\": \"config\"}")
                .entrypointId("entrypoint-id")
                .channel("channel")
                .build()
        )
        .metadata(Map.of("meta1", "value1", "meta2", "value2"));

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
        return AcceptSubscription.builder()
            .customApiKey("custom")
            .reason("reason")
            .startingAt(OffsetDateTime.now())
            .endingAt(OffsetDateTime.now())
            .build();
    }

    public static AcceptSubscription anAcceptSubscriptionWithRandomKey() {
        return AcceptSubscription.builder()
            .customApiKey(UUID.randomUUID().toString()) // Generates a random UUID as the custom API key
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

    public static UpdateApiKey anUpdateApiKey() {
        return UpdateApiKey.builder().expireAt(OffsetDateTime.now()).build();
    }

    public static RenewApiKey aRenewApiKey() {
        return RenewApiKey.builder().customApiKey("custom").build();
    }

    public static SubscriptionEntity aSubscriptionEntity() {
        return SubscriptionModelFixtures.aSubscriptionEntity();
    }

    public static ApiKeyEntity anApiKeyEntity() {
        return SubscriptionModelFixtures.anApiKeyEntity();
    }
}
