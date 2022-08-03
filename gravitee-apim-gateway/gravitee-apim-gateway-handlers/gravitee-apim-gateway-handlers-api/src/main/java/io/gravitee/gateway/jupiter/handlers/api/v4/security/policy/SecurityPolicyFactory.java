/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.jupiter.handlers.api.v4.security.policy;

import io.gravitee.definition.model.v4.plan.Plan;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.gateway.jupiter.api.ExecutionPhase;
import io.gravitee.gateway.jupiter.api.policy.Policy;
import io.gravitee.gateway.jupiter.api.policy.SecurityPolicy;
import io.gravitee.gateway.jupiter.policy.PolicyManager;
import io.gravitee.gateway.policy.PolicyMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SecurityPolicyFactory {

    private static final Logger log = LoggerFactory.getLogger(SecurityPolicyFactory.class);

    private SecurityPolicyFactory() {}

    public static SecurityPolicy forPlan(
        final String apiId,
        final Plan plan,
        final PolicyManager policyManager,
        final ExecutionPhase executionPhase
    ) {
        PlanSecurity planSecurity = plan.getSecurity();
        final String security = planSecurity.getType();

        if (security == null) {
            return null;
        }

        String policyName = security.toLowerCase().replaceAll("_", "-");
        final Policy policy = policyManager.create(executionPhase, new PolicyMetadata(policyName, planSecurity.getConfiguration()));

        if (policy instanceof SecurityPolicy) {
            return (SecurityPolicy) policy;
        }

        log.warn("Policy [{}] (plan [{}], api [{}]) is not a security policy.", policyName, plan.getName(), apiId);

        return null;
    }
}
