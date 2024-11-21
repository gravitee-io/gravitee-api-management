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
package io.gravitee.gateway.reactive.handlers.api.v4.security.plan;

import io.gravitee.definition.model.v4.plan.AbstractPlan;
import io.gravitee.gateway.reactive.api.ExecutionPhase;
import io.gravitee.gateway.reactive.api.policy.http.HttpSecurityPolicy;
import io.gravitee.gateway.reactive.handlers.api.security.plan.HttpSecurityPlan;
import io.gravitee.gateway.reactive.handlers.api.v4.security.policy.SecurityPolicyFactory;
import io.gravitee.gateway.reactive.policy.PolicyManager;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class HttpSecurityPlanFactory {

    private HttpSecurityPlanFactory() {}

    @Nullable
    public static HttpSecurityPlan forPlan(
        @Nonnull final String apiId,
        @Nonnull AbstractPlan plan,
        @Nonnull PolicyManager policyManager,
        @Nonnull ExecutionPhase executionPhase
    ) {
        if (plan.usePushMode()) {
            log.debug("The plan [{}] (api [{}]) is using the {} mode, it has no security policy.", plan.getName(), apiId, plan.getMode());
            return null;
        }

        final HttpSecurityPolicy policy = SecurityPolicyFactory.forPlan(apiId, plan, policyManager, executionPhase);

        if (policy != null) {
            return new HttpSecurityPlan(plan.getId(), policy, plan.getSelectionRule());
        }

        log.warn(
            "No policy has been found for the plan [{}] (api [{}]) with security [{}]. This plan will remain unreachable",
            plan.getName(),
            apiId,
            plan.getSecurity()
        );
        return null;
    }
}
