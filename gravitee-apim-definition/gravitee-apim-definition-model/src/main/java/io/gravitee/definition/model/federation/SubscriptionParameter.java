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
package io.gravitee.definition.model.federation;

public sealed interface SubscriptionParameter {
    FederatedPlan plan();

    record ApiKey(FederatedPlan plan) implements SubscriptionParameter {
        public ApiKey {
            if (!plan.isApiKey()) {
                throw new IllegalArgumentException("The plan is not a ApiKey");
            }
        }
    }

    record OAuth(String clientId, FederatedPlan plan) implements SubscriptionParameter {
        public OAuth {
            if (!plan.isOAuth()) {
                throw new IllegalArgumentException("The plan is not a OAuth");
            }
        }
    }

    public static SubscriptionParameter apiKey(FederatedPlan plan) {
        return new ApiKey(plan);
    }

    public static SubscriptionParameter oAuth(String clientId, FederatedPlan plan) {
        return new OAuth(clientId, plan);
    }
}
