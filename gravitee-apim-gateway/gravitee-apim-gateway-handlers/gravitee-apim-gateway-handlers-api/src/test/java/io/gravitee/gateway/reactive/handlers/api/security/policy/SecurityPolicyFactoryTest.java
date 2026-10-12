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
package io.gravitee.gateway.reactive.handlers.api.security.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.Plan;
import io.gravitee.gateway.policy.PolicyMetadata;
import io.gravitee.gateway.reactive.api.ExecutionPhase;
import io.gravitee.gateway.reactive.api.policy.base.BaseSecurityPolicy;
import io.gravitee.gateway.reactive.policy.PolicyInstantiationException;
import io.gravitee.gateway.reactive.policy.PolicyManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class SecurityPolicyFactoryTest {

    @Mock
    private PolicyManager policyManager;

    @Test
    void should_ignore_the_plan_when_its_security_policy_cannot_be_instantiated() {
        Plan plan = new Plan();
        plan.setName("jwt-plan");
        plan.setSecurity("JWT");
        when(policyManager.create(eq(ExecutionPhase.REQUEST), any(PolicyMetadata.class))).thenThrow(
            new PolicyInstantiationException("jwt", ExecutionPhase.REQUEST)
        );

        BaseSecurityPolicy policy = SecurityPolicyFactory.forPlan(plan, policyManager);

        assertThat(policy).isNull();
    }
}
