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
package io.gravitee.gateway.jupiter.handlers.api.security.handler;

import io.gravitee.definition.model.Plan;
import io.gravitee.gateway.jupiter.api.ExecutionPhase;
import io.gravitee.gateway.jupiter.api.policy.Policy;
import io.gravitee.gateway.jupiter.api.policy.SecurityPolicy;
import io.gravitee.gateway.jupiter.policy.PolicyManager;
import io.gravitee.gateway.policy.PolicyMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SecurityPolicyFactory {

    private static final Logger log = LoggerFactory.getLogger(SecurityPolicyFactory.class);

    public static SecurityPolicy forPlan(Plan plan, PolicyManager policyManager) {
        final String security = plan.getSecurity();

        if (security == null) {
            return null;
        }

        String policyName = security.toLowerCase().replaceAll("_", "-");
        final Policy policy = policyManager.create(ExecutionPhase.REQUEST, new PolicyMetadata(policyName, plan.getSecurityDefinition()));

        if (policy instanceof SecurityPolicy) {
            return (SecurityPolicy) policy;
        }

        log.warn("Policy [{}] (plan [{}], api [{}]) is not a security policy.", policyName, plan.getName(), plan.getApi());

        return null;
    }
}
