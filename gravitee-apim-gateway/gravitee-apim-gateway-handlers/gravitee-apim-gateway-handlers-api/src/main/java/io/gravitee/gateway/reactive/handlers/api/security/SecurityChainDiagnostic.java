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

import io.gravitee.gateway.reactive.api.policy.SecurityToken;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Data;

@Data
public class SecurityChainDiagnostic {

    private static final String MESSAGE_INVALID_TOKEN = "The provided authentication token is invalid";
    private static final String MESSAGE_NOT_AUTHORIZED = "The provided credentials are not authorized";
    private static final String MESSAGE_EXPIRED = "Access has expired for the provided credentials";
    private static final String MESSAGE_NO_PLAN_MATCHED = "No plan matched the request";
    private static final String MESSAGE_NO_TOKEN = "The request did not include an authentication token";
    private static final String MESSAGE_UNAUTHORIZED = "Unauthorized";

    private final boolean verbose401;

    List<String> noTokenPlans;
    List<String> invalidTokenPlans;
    List<String> noMatchingRulePlans;
    List<NoSubscriptionInfo> noSubscriptionPlans;
    List<String> expiredSubscriptionPlans;

    private record NoSubscriptionInfo(String planName, String tokenType, String maskedToken) {}

    public SecurityChainDiagnostic() {
        this(false);
    }

    public SecurityChainDiagnostic(boolean verbose401) {
        this.verbose401 = verbose401;
    }

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

    public void markPlanHasNoSubscription(String planName, String tokenType, String tokenValue) {
        if (noSubscriptionPlans == null) {
            noSubscriptionPlans = new ArrayList<>();
        }
        noSubscriptionPlans.add(new NoSubscriptionInfo(planName, tokenType, maskToken(tokenValue)));
    }

    private String maskToken(String token) {
        if (token == null || token.isEmpty()) {
            return "***";
        }
        if (token.length() <= 4) {
            return token.charAt(0) + "***";
        }
        if (token.length() <= 8) {
            return token.substring(0, 2) + "***" + token.substring(token.length() - 2);
        }
        return token.substring(0, 4) + "***" + token.substring(token.length() - 4);
    }

    public void markPlanHasExpiredSubscription(String planName, String applicationName) {
        if (expiredSubscriptionPlans == null) {
            expiredSubscriptionPlans = new ArrayList<>();
        }
        expiredSubscriptionPlans.add(planName + " (application: " + applicationName + ")");
    }

    public void markPlanHasInvalidToken(String planName) {
        if (invalidTokenPlans == null) {
            invalidTokenPlans = new ArrayList<>();
        }
        invalidTokenPlans.add(planName);
    }

    public String message() {
        if (!verbose401) {
            return MESSAGE_UNAUTHORIZED;
        }

        if (invalidTokenPlans != null) {
            return MESSAGE_INVALID_TOKEN;
        }

        if (noSubscriptionPlans != null) {
            return MESSAGE_NOT_AUTHORIZED;
        }

        if (expiredSubscriptionPlans != null) {
            return MESSAGE_EXPIRED;
        }

        if (noMatchingRulePlans != null) {
            return MESSAGE_NO_PLAN_MATCHED;
        }

        if (noTokenPlans != null) {
            return MESSAGE_NO_TOKEN;
        }

        return MESSAGE_UNAUTHORIZED;
    }

    public Exception cause() {
        if (invalidTokenPlans != null) {
            return new Exception("The provided authentication token is invalid for the following " + formatPlans(invalidTokenPlans));
        }

        if (noSubscriptionPlans != null) {
            return new Exception(formatNoSubscriptionMessage());
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

    private String formatNoSubscriptionMessage() {
        return (
            "No subscription was found for " +
            noSubscriptionPlans
                .stream()
                .collect(
                    Collectors.groupingBy(
                        info -> SecurityToken.TokenType.valueOfOrNone(info.tokenType),
                        LinkedHashMap::new,
                        Collectors.toList()
                    )
                )
                .entrySet()
                .stream()
                .map(entry -> buildNoSubscriptionMessage(entry.getKey(), entry.getValue()))
                .collect(Collectors.joining(" or for "))
        );
    }

    private String buildNoSubscriptionMessage(SecurityToken.TokenType tokenType, List<NoSubscriptionInfo> planDetails) {
        String label = switch (tokenType) {
            case API_KEY, MD5_API_KEY -> "API Key";
            case CLIENT_ID -> "Client ID";
            case CERTIFICATE -> "Certificate";
            default -> "credentials";
        };
        // Token value is the same for all plans of the same type within a request
        String maskedToken = planDetails.getFirst().maskedToken();
        List<String> planNames = planDetails.stream().map(NoSubscriptionInfo::planName).toList();
        return label + ": " + maskedToken + " (" + formatPlans(planNames) + ")";
    }

    private String formatPlans(List<String> plans) {
        String label = plans.size() == 1 ? "plan" : "plans";
        return label + ": " + String.join(", ", plans);
    }
}
