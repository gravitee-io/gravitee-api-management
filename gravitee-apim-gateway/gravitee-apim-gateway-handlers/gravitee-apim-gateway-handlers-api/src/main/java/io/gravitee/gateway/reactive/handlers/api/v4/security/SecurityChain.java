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
package io.gravitee.gateway.reactive.handlers.api.v4.security;

import io.gravitee.definition.model.v4.Api;
import io.gravitee.gateway.reactive.api.ExecutionPhase;
import io.gravitee.gateway.reactive.handlers.api.security.plan.SecurityPlan;
import io.gravitee.gateway.reactive.handlers.api.v4.security.plan.SecurityPlanFactory;
import io.gravitee.gateway.reactive.policy.PolicyManager;
import io.reactivex.rxjava3.core.Flowable;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SecurityChain extends io.gravitee.gateway.reactive.handlers.api.security.SecurityChain {

    public SecurityChain(@Nonnull final Api api, @Nonnull final PolicyManager policyManager, @Nonnull final ExecutionPhase executionPhase) {
        super(
            Flowable.fromIterable(
                stream(api.getPlans())
                    .map(plan -> SecurityPlanFactory.forPlan(api.getId(), plan, policyManager, executionPhase))
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparingInt(SecurityPlan::order))
                    .collect(Collectors.toList())
            ),
            executionPhase
        );
    }

    @Nonnull
    private static <T> Stream<T> stream(@Nullable Collection<T> collection) {
        return collection != null ? collection.stream() : Stream.empty();
    }
}
