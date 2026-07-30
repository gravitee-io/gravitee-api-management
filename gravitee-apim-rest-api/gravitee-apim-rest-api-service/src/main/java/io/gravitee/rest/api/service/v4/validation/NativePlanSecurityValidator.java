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
package io.gravitee.rest.api.service.v4.validation;

import io.gravitee.apim.core.plan.model.NativePlanSecurityCategory;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.repository.management.model.Plan;
import io.gravitee.rest.api.service.exceptions.NativePlanAuthenticationConflictException;
import java.util.Collection;

/**
 * Enforces, on the publish paths, that a Native API never mixes plans of different security
 * categories (see {@link NativePlanSecurityCategory}): keyless, mTLS and authentication plans are
 * mutually exclusive.
 *
 * <p>A plan is considered "active" — and therefore conflicting — as soon as it is {@code PUBLISHED}
 * or {@code DEPRECATED}, matching the gateway's native-Kafka reactor which loads both statuses.
 *
 * <p>The same rule is enforced on the CRD import path in
 * {@code ValidatePlanDomainService#validateNoConflictingNativeSecurity}: keep both in sync.
 */
public final class NativePlanSecurityValidator {

    private NativePlanSecurityValidator() {}

    public static void validateNoConflictingSecurity(Plan planToPublish, Collection<Plan> apiPlans) {
        if (planToPublish.getApiType() != ApiType.NATIVE || planToPublish.getSecurity() == null) {
            return;
        }

        NativePlanSecurityCategory categoryToPublish = categoryOf(planToPublish.getSecurity());

        // No self-exclusion needed: callers reject non-STAGING plans before publishing, so the plan
        // being published never matches the PUBLISHED/DEPRECATED filter below.
        boolean hasConflict = apiPlans
            .stream()
            .filter(existingPlan -> existingPlan.getStatus() == Plan.Status.PUBLISHED || existingPlan.getStatus() == Plan.Status.DEPRECATED)
            .filter(existingPlan -> existingPlan.getSecurity() != null)
            .anyMatch(existingPlan -> categoryOf(existingPlan.getSecurity()) != categoryToPublish);

        if (hasConflict) {
            throw new NativePlanAuthenticationConflictException(planToPublish.getSecurity());
        }
    }

    private static NativePlanSecurityCategory categoryOf(Plan.PlanSecurityType securityType) {
        return switch (securityType) {
            case KEY_LESS -> NativePlanSecurityCategory.KEY_LESS;
            case MTLS -> NativePlanSecurityCategory.MTLS;
            default -> NativePlanSecurityCategory.AUTHENTICATION;
        };
    }
}
