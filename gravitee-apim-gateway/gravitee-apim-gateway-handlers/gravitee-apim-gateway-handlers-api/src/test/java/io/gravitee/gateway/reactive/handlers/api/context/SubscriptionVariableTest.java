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
    void should_give_access_to_subscription() {
        Subscription subscription = new Subscription();
        subscription.setId("id");
        subscription.setType(Subscription.Type.PUSH);
        subscription.setMetadata(Map.of("key", "value"));
        subscription.setApplicationName("applicationName");
        subscription.setClientId("clientId");
        SubscriptionVariable subscriptionVariable = new SubscriptionVariable(subscription);

        assertThat(subscriptionVariable)
            .extracting(
                SubscriptionVariable::getId,
                SubscriptionVariable::getType,
                SubscriptionVariable::getMetadata,
                SubscriptionVariable::getApplicationName,
                SubscriptionVariable::getClientId
            )
            .containsExactly("id", Subscription.Type.PUSH, Map.of("key", "value"), "applicationName", "clientId");
    }
}
