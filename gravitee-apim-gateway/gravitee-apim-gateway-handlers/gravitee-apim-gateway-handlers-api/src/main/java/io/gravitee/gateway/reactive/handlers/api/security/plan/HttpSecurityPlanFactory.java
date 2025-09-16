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
package io.gravitee.gateway.reactive.handlers.api.security.plan;

import io.gravitee.definition.model.Plan;
import io.gravitee.gateway.reactive.api.policy.http.HttpSecurityPolicy;
import io.gravitee.gateway.reactive.handlers.api.security.policy.SecurityPolicyFactory;
import io.gravitee.gateway.reactive.policy.PolicyManager;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HttpSecurityPlanFactory {

    private static final Logger log = LoggerFactory.getLogger(HttpSecurityPlanFactory.class);

    private HttpSecurityPlanFactory() {}

    @Nullable
    public static HttpSecurityPlan forPlan(@Nonnull Plan plan, @Nonnull PolicyManager policyManager) {
        final HttpSecurityPolicy policy = SecurityPolicyFactory.forPlan(plan, policyManager);

        if (policy != null) {
            return new HttpSecurityPlan(SecurityPlanContext.builder().fromV2(plan).build(), policy);
        }

        log.warn(
            "No policy has been found for the plan [{}] (api [{}]) with security [{}]. This plan will remain unreachable",
            plan.getName(),
            plan.getApi(),
            plan.getSecurity()
        );
        return null;
    }
}
