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
import io.gravitee.gateway.handlers.api.registry.ApiProductRegistry;
import io.gravitee.gateway.reactive.api.ExecutionPhase;
import io.gravitee.gateway.reactive.api.context.http.HttpPlainExecutionContext;
import io.gravitee.gateway.reactive.handlers.api.security.plan.HttpSecurityPlan;
import io.gravitee.gateway.reactive.handlers.api.v4.security.plan.HttpSecurityPlanFactory;
import io.gravitee.gateway.reactive.policy.PolicyManager;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 *
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HttpSecurityChain extends io.gravitee.gateway.reactive.handlers.api.security.HttpSecurityChain {

    // Store constructor parameters for refresh
    private final Api api;
    private final PolicyManager policyManager;
    private final ExecutionPhase executionPhase;
    private final String environmentId;
    private final ApiProductRegistry apiProductRegistry;
    private final PolicyManager apiProductPlanPolicyManager;

    public HttpSecurityChain(
        @Nonnull final Api api,
        @Nonnull final PolicyManager policyManager,
        @Nonnull final ExecutionPhase executionPhase
    ) {
        this(api, policyManager, executionPhase, null, null, null);
    }

    /**
     * Constructor with API Product support. When apiProductRegistry is provided,
     * product plans are iterated first (product-first validation per PRD).
     * When apiProductPlanPolicyManager is provided, it is used for product plan security
     * instead of policyManager (SPG pattern).
     */
    public HttpSecurityChain(
        @Nonnull final Api api,
        @Nonnull final PolicyManager policyManager,
        @Nonnull final ExecutionPhase executionPhase,
        @Nullable final String environmentId,
        @Nullable final ApiProductRegistry apiProductRegistry,
        @Nullable final PolicyManager apiProductPlanPolicyManager
    ) {
        super(
            Flowable.fromIterable(
                buildSecurityPlans(api, policyManager, executionPhase, environmentId, apiProductRegistry, apiProductPlanPolicyManager)
            ),
            executionPhase
        );
        this.api = api;
        this.policyManager = policyManager;
        this.executionPhase = executionPhase;
        this.environmentId = environmentId;
        this.apiProductRegistry = apiProductRegistry;
        this.apiProductPlanPolicyManager = apiProductPlanPolicyManager;
    }

    /**
     * Create a new HttpSecurityChain with current state from ApiProductRegistry.
     * Used to refresh the security chain when API Products are deployed/updated/undeployed.
     *
     * @return new HttpSecurityChain instance built from current registry state
     */
    public HttpSecurityChain refresh() {
        return new HttpSecurityChain(api, policyManager, executionPhase, environmentId, apiProductRegistry, apiProductPlanPolicyManager);
    }

    private static List<HttpSecurityPlan> buildSecurityPlans(
        Api api,
        PolicyManager policyManager,
        ExecutionPhase executionPhase,
        String environmentId,
        ApiProductRegistry apiProductRegistry,
        PolicyManager apiProductPlanPolicyManager
    ) {
        // Sort each group by order individually (stream sorted + collect, like original).
        // Do NOT sort the combined list—that would interleave by order and break product-first.
        List<HttpSecurityPlan> apiProductPlans = getProductPlans(
            api,
            environmentId,
            apiProductRegistry,
            policyManager,
            apiProductPlanPolicyManager,
            executionPhase
        );
        List<HttpSecurityPlan> apiPlans = getApiPlans(api, policyManager, executionPhase);

        // Product plans first, then API plans (each group already sorted by order).
        List<HttpSecurityPlan> result = new ArrayList<>(apiProductPlans.size() + apiPlans.size());
        result.addAll(apiProductPlans);
        result.addAll(apiPlans);
        return result;
    }

    private static List<HttpSecurityPlan> getProductPlans(
        Api api,
        String environmentId,
        ApiProductRegistry apiProductRegistry,
        PolicyManager policyManager,
        PolicyManager apiProductPlanPolicyManager,
        ExecutionPhase executionPhase
    ) {
        if (environmentId == null || apiProductRegistry == null) {
            return List.of();
        }

        PolicyManager productPlanPolicyManager = apiProductPlanPolicyManager != null ? apiProductPlanPolicyManager : policyManager;
        List<ApiProductRegistry.ApiProductPlanEntry> entries = apiProductRegistry.getApiProductPlanEntriesForApi(
            api.getId(),
            environmentId
        );

        return entries
            .stream()
            .map(entry -> HttpSecurityPlanFactory.forPlan(api.getId(), entry.plan(), productPlanPolicyManager, executionPhase))
            .filter(Objects::nonNull)
            .sorted(Comparator.comparingInt(HttpSecurityPlan::order))
            .toList();
    }

    private static List<HttpSecurityPlan> getApiPlans(Api api, PolicyManager policyManager, ExecutionPhase executionPhase) {
        return stream(api.getPlans())
            .map(plan -> HttpSecurityPlanFactory.forPlan(api.getId(), plan, policyManager, executionPhase))
            .filter(Objects::nonNull)
            .sorted(Comparator.comparingInt(HttpSecurityPlan::order))
            .toList();
    }

    @Nonnull
    private static <T> Stream<T> stream(@Nullable Collection<T> collection) {
        return collection != null ? collection.stream() : Stream.empty();
    }

    public Completable execute(HttpPlainExecutionContext ctx) {
        return chain
            .flatMapSingle(httpSecurityPlan -> httpSecurityPlan.onWellKnown(ctx))
            .any(Boolean::booleanValue)
            .flatMapCompletable(onWellKnown -> {
                if (onWellKnown) {
                    return ctx.interrupt();
                }
                return Completable.complete();
            })
            .andThen(super.execute(ctx));
    }
}
