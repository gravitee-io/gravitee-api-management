package io.gravitee.gateway.reactive.handlers.api.security;

import io.gravitee.gateway.reactive.api.ExecutionWarn;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@Data
public class SecurityChainDiagnostic {

    List<String> allPlans;
    List<String> noTokenPlans;
    List<String> invalidTokenPlans;
    List<String> noMatchingRulePlans;
    List<String> noSubscriptionPlans;
    List<String> expiredSubscriptionPlans;

    public void markPlanHasNoToken(String planId) {
        if (noTokenPlans == null) {
            noTokenPlans = new ArrayList<>();
        }

        noTokenPlans.add(planId);
    }

    public void markPlanHasNoMachingRule(String planId) {
        if (noMatchingRulePlans == null) {
            noMatchingRulePlans = new ArrayList<>();
        }

        noMatchingRulePlans.add(planId);
    }

    public void markPlanHasNoSubscription(String planId) {
        if (noSubscriptionPlans == null) {
            noSubscriptionPlans = new ArrayList<>();
        }
        noSubscriptionPlans.add(planId);
    }

    public void markPlanHasExpiredSubscription(String planId) {
        if (expiredSubscriptionPlans == null) {
            expiredSubscriptionPlans = new ArrayList<>();
        }
        expiredSubscriptionPlans.add(planId);
    }

    public Exception cause() {
        if (invalidTokenPlans != null) {
            return new Exception("The request has an invalid authentication token - INVALID_TOKEN");
        }

        if (noSubscriptionPlans != null) {
            return new Exception(
                "No valid subscription has been found for plans - SUBSCRIPTION_UNKNOWN_PAUSED_OR_EXPIRED"
            );
        }

        if (expiredSubscriptionPlans != null) {
            return new Exception(
                "No valid subscription has been found for plans - SUBSCRIPTION_EXPIRED"
            );
        }

        if (noMatchingRulePlans != null) {
            return new Exception(
                "No matching selection rule for plans - NO_MATCHING_SELECTION_RULE"
            );
        }

        if (noTokenPlans != null) {
            return new Exception("The request didn't provide any authentication token - NO_TOKEN");
        }

        return new Exception("No valid plan not found");
    }

    public void markPlanHasInvalidToken(String planId) {
        if (invalidTokenPlans == null) {
            invalidTokenPlans = new ArrayList<>();
        }
        invalidTokenPlans.add(planId);
    }
}
