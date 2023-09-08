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
package io.gravitee.apim.infra.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.repository.management.model.Subscription;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SubscriptionAdapterTest {

    @Test
    void should_convert_subscription_to_subscription_entity() {
        Subscription subscription = new Subscription();
        subscription.setId("subscription-id");
        subscription.setApi("api-id");
        subscription.setApplication("application-id");
        subscription.setClientId("client-id");
        subscription.setType(Subscription.Type.STANDARD);

        SubscriptionEntity subscriptionEntity = SubscriptionAdapter.INSTANCE.toEntity(subscription);
        assertThat(subscriptionEntity.getId()).isEqualTo("subscription-id");
    }

    @Test
    void should_convert_subscription_entity_to_subscription() {
        SubscriptionEntity subscriptionEntity = new SubscriptionEntity();
        subscriptionEntity.setId("subscription-id");
        subscriptionEntity.setApiId("api-id");
        subscriptionEntity.setApplicationId("application-id");
        subscriptionEntity.setClientId("client-id");

        Subscription subscription = SubscriptionAdapter.INSTANCE.fromEntity(subscriptionEntity);
        assertThat(subscription.getId()).isEqualTo("subscription-id");
    }
}
