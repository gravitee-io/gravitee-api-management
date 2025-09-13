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
package io.gravitee.gateway.reactive.handlers.api.security;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class SecurityChainDiagnostic {

    List<String> noTokenPlans;
    List<String> invalidTokenPlans;
    List<String> noMatchingRulePlans;
    List<String> noSubscriptionPlans;
    List<String> expiredSubscriptionPlans;

    public void markPlanHasNoToken(String planName) {
        if (noTokenPlans == null) {
            noTokenPlans = new ArrayList<>();
        }

        noTokenPlans.add(planName);
    }

    public void markPlanHasNoMachingRule(String planName) {
        if (noMatchingRulePlans == null) {
            noMatchingRulePlans = new ArrayList<>();
        }

        noMatchingRulePlans.add(planName);
    }

    public void markPlanHasNoSubscription(String planName) {
        if (noSubscriptionPlans == null) {
            noSubscriptionPlans = new ArrayList<>();
        }
        noSubscriptionPlans.add(planName);
    }

    public void markPlanHasExpiredSubscription(String planName) {
        if (expiredSubscriptionPlans == null) {
            expiredSubscriptionPlans = new ArrayList<>();
        }
        expiredSubscriptionPlans.add(planName);
    }

    public void markPlanHasInvalidToken(String planName) {
        if (invalidTokenPlans == null) {
            invalidTokenPlans = new ArrayList<>();
        }
        invalidTokenPlans.add(planName);
    }

    public Exception cause() {
        if (invalidTokenPlans != null) {
            return new Exception("The provided authentication token is invalid for the following " + formatPlans(invalidTokenPlans));
        }

        if (noSubscriptionPlans != null) {
            return new Exception("No active subscription was found for the following " + formatPlans(noSubscriptionPlans));
        }

        if (expiredSubscriptionPlans != null) {
            return new Exception("The subscription has expired for the following " + formatPlans(expiredSubscriptionPlans));
        }

        if (noMatchingRulePlans != null) {
            return new Exception("None of the selection rules matched for the following " + formatPlans(noMatchingRulePlans));
        }

        if (noTokenPlans != null) {
            return new Exception("The request did not include an authentication token for the following " + formatPlans(noTokenPlans));
        }

        return new Exception("No valid plan was found for this request.");
    }

    private String formatPlans(List<String> plans) {
        String label = plans.size() == 1 ? "plan" : "plans";
        return label + ": " + String.join(", ", plans);
    }
}
