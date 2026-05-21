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
package io.gravitee.apim.core.subscription.query_service;

import io.gravitee.apim.core.subscription.model.ExpiringSubscription;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.apim.core.subscription.model.SubscriptionReferenceType;
import io.gravitee.rest.api.model.SubscriptionStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface SubscriptionQueryService {
    List<SubscriptionEntity> findExpiredSubscriptions();

    /**
     * Returns subscriptions whose {@code endingAt} falls inside any of the per-bucket windows
     * {@code [now + d*24h, now + d*24h + windowMs)} for each {@code d} in {@code daysBuckets}, restricted
     * to the given statuses. Runs as a single repository query over the outer-union range; callers
     * perform bucket-inference in memory.
     */
    List<ExpiringSubscription> findExpiringSubscriptions(
        Instant now,
        List<Integer> daysBuckets,
        long windowMs,
        List<SubscriptionStatus> statuses
    );

    List<SubscriptionEntity> findSubscriptionsByPlan(String planId);

    List<SubscriptionEntity> findActiveSubscriptionsByPlan(String planId);

    List<SubscriptionEntity> findActiveByApplicationIdAndApiId(String applicationId, String apiId);

    List<SubscriptionEntity> findActiveByApplicationIdsAndApiIds(Set<String> applicationIds, Set<String> apiIds);

    List<SubscriptionEntity> findActiveByApplicationIdAndReferenceIdAndReferenceType(
        String applicationId,
        String referenceId,
        SubscriptionReferenceType referenceType
    );

    List<SubscriptionEntity> findAllByReferenceIdAndReferenceType(String referenceId, SubscriptionReferenceType referenceType);

    Optional<SubscriptionEntity> findByIdAndReferenceIdAndReferenceType(
        String subscriptionId,
        String referenceId,
        SubscriptionReferenceType referenceType
    );

    List<SubscriptionEntity> findActiveByApplicationIdAndPlanSecurityTypes(String applicationId, Collection<String> planSecurityTypes);
}
