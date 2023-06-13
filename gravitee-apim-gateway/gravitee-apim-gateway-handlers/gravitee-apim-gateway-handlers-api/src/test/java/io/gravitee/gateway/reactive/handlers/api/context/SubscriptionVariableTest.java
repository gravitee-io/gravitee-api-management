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
package io.gravitee.gateway.reactive.handlers.api.context;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.gateway.api.service.Subscription;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
class SubscriptionVariableTest {

    @Test
    void shouldGiveAccessToSubscription() {
        Subscription subscription = new Subscription();
        subscription.setId("id");
        subscription.setType(Subscription.Type.PUSH);
        subscription.setMetadata(Map.of("key", "value"));
        SubscriptionVariable subscriptionVariable = new SubscriptionVariable(subscription);

        assertThat(subscriptionVariable.getId()).isEqualTo("id");
        assertThat(subscriptionVariable.getType()).isEqualTo(Subscription.Type.PUSH);
        assertThat(subscriptionVariable.getMetadata().get("key")).isEqualTo("value");
    }
}
