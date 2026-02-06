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
package io.gravitee.gateway.services.sync.process.repository.service;

import io.gravitee.gateway.services.sync.process.repository.synchronizer.api.ApiReactorDeployable;
import io.gravitee.repository.management.model.Plan;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
public class PlanService {

    private final Map<String, Set<String>> plansPerApi = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> plansPerApiProduct = new ConcurrentHashMap<>();

    public void register(final ApiReactorDeployable apiReactorDeployable) {
        if (apiReactorDeployable != null && apiReactorDeployable.apiId() != null) {
            plansPerApi.put(apiReactorDeployable.apiId(), apiReactorDeployable.subscribablePlans());
        }
    }

    public void unregister(final ApiReactorDeployable apiReactorDeployable) {
        if (apiReactorDeployable != null && apiReactorDeployable.apiId() != null) {
            plansPerApi.remove(apiReactorDeployable.apiId());
        }
    }

    public void registerForApiProduct(final String apiProductId, final Set<String> planIds) {
        if (apiProductId != null && planIds != null) {
            plansPerApiProduct.put(apiProductId, Set.copyOf(planIds));
        }
    }

    public void unregisterForApiProduct(final String apiProductId) {
        if (apiProductId != null) {
            plansPerApiProduct.remove(apiProductId);
        }
    }

    public boolean isDeployed(final String apiId, final String planId) {
        return isDeployed(apiId, planId, Plan.PlanReferenceType.API);
    }

    /**
     * Check if a plan is deployed for the given reference (API or API Product).
     */
    public boolean isDeployed(final String referenceId, final String planId, final Plan.PlanReferenceType referenceType) {
        if (referenceId == null || planId == null || referenceType == null) {
            return false;
        }
        return switch (referenceType) {
            case API -> Optional.ofNullable(plansPerApi.get(referenceId))
                .map(plans -> plans.contains(planId))
                .orElse(false);
            case API_PRODUCT -> Optional.ofNullable(plansPerApiProduct.get(referenceId))
                .map(plans -> plans.contains(planId))
                .orElse(false);
        };
    }
}
