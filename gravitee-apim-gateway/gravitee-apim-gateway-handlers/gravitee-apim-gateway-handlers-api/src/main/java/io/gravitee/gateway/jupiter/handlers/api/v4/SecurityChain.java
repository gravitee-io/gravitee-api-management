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
package io.gravitee.gateway.jupiter.handlers.api.v4;

import io.gravitee.gateway.jupiter.handlers.api.definition.Api;
import io.gravitee.gateway.jupiter.handlers.api.security.AbstractSecurityChain;
import io.gravitee.gateway.jupiter.handlers.api.security.plan.SecurityPlan;
import io.gravitee.gateway.jupiter.handlers.api.security.plan.SecurityPlanFactory;
import io.gravitee.gateway.jupiter.policy.PolicyManager;
import io.reactivex.Flowable;

import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * {@link SecurityChain} is a special chain dedicated to execute policy associated with plans.
 * The security chain is responsible to create {@link SecurityPlan} for each plan of the api and executed them in order.
 * Only the first {@link SecurityPlan} that can handle the current request is executed.
 * The result of the security chain execution depends on this {@link SecurityPlan} execution.
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SecurityChain extends AbstractSecurityChain {

    public SecurityChain(Api api, PolicyManager policyManager) {
        super(
            Flowable.fromIterable(
                api

                    .getPlans()

                    .stream()
                    .map(plan -> SecurityPlanFactory.forPlan(plan, policyManager))
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparingInt(SecurityPlan::order))
                    .collect(Collectors.toList())
            ));
    }
}
