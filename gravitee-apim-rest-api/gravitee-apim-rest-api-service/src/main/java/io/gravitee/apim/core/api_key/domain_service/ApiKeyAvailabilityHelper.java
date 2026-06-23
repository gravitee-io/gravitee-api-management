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
package io.gravitee.apim.core.api_key.domain_service;

import io.gravitee.apim.core.api_key.model.ApiKeyEntity;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.apim.core.subscription.model.SubscriptionReferenceType;

public final class ApiKeyAvailabilityHelper {

    private ApiKeyAvailabilityHelper() {}

    public static boolean isActiveForSubscription(ApiKeyEntity apiKey, SubscriptionEntity subscription) {
        return (
            isActiveKeyState(apiKey.isRevoked(), apiKey.isPaused(), apiKey.isExpired()) &&
            isActiveSubscriptionStatus(subscription.getStatus())
        );
    }

    public static boolean isActiveKeyState(boolean revoked, boolean paused, boolean expired) {
        return !revoked && !paused && !expired;
    }

    public static boolean matchesReference(SubscriptionEntity subscription, String referenceId, String referenceType) {
        return matchesReference(
            subscription.getReferenceId(),
            subscription.getReferenceType() == null ? null : subscription.getReferenceType().name(),
            subscription.getApiId(),
            referenceId,
            referenceType
        );
    }

    public static boolean matchesReference(
        String subscriptionReferenceId,
        String subscriptionReferenceType,
        String subscriptionApiId,
        String referenceId,
        String referenceType
    ) {
        if (subscriptionReferenceId != null && subscriptionReferenceType != null) {
            return referenceId.equals(subscriptionReferenceId) && referenceType.equals(subscriptionReferenceType);
        }
        return SubscriptionReferenceType.API.name().equals(referenceType) && referenceId.equals(subscriptionApiId);
    }

    public static boolean isActiveSubscriptionStatus(SubscriptionEntity.Status status) {
        return status == SubscriptionEntity.Status.ACCEPTED || status == SubscriptionEntity.Status.PAUSED;
    }

    public static boolean isActiveSubscriptionStatusName(String status) {
        if (status == null) {
            return false;
        }
        try {
            return isActiveSubscriptionStatus(SubscriptionEntity.Status.valueOf(status));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
