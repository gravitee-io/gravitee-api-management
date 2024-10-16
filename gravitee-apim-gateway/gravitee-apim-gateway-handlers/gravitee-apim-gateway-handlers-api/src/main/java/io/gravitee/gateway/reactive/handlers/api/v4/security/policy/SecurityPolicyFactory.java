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
package io.gravitee.gateway.reactive.handlers.api.v4.security.policy;

import io.gravitee.definition.model.v4.plan.AbstractPlan;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.gateway.policy.PolicyMetadata;
import io.gravitee.gateway.reactive.api.ExecutionPhase;
import io.gravitee.gateway.reactive.api.policy.base.BasePolicy;
import io.gravitee.gateway.reactive.api.policy.base.BaseSecurityPolicy;
import io.gravitee.gateway.reactive.policy.PolicyManager;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class SecurityPolicyFactory {

    private SecurityPolicyFactory() {}

    @SuppressWarnings("unchecked")
    public static <T extends BaseSecurityPolicy> T forPlan(
        final String apiId,
        final AbstractPlan plan,
        final PolicyManager policyManager,
        final ExecutionPhase executionPhase
    ) {
        if (plan.useStandardMode()) {
            PlanSecurity planSecurity = plan.getSecurity();
            if (planSecurity == null || planSecurity.getType() == null) {
                return null;
            }

            String policyName = planSecurity.getType().toLowerCase().replaceAll("_", "-");
            final BasePolicy policy = policyManager.create(executionPhase, new PolicyMetadata(policyName, planSecurity.getConfiguration()));

            if (policy instanceof BaseSecurityPolicy) {
                return (T) policy;
            }

            log.warn("Policy [{}] (plan [{}], api [{}]) is not a security policy.", policyName, plan.getName(), apiId);
        } else {
            log.debug("Plan [{}] is using [{}] mode, no security type for this mode", plan.getName(), plan.getMode());
        }
        return null;
    }
}
