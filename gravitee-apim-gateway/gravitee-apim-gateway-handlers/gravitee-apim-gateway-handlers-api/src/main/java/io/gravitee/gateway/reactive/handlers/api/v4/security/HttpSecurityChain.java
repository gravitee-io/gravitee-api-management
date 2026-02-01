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
import io.gravitee.definition.model.v4.plan.AbstractPlan;
import io.gravitee.gateway.handlers.api.ReactableApiProduct;
import io.gravitee.gateway.handlers.api.registry.ApiProductRegistry;
import io.gravitee.gateway.handlers.api.registry.ProductPlanDefinitionCache;
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

    public HttpSecurityChain(
        @Nonnull final Api api,
        @Nonnull final PolicyManager policyManager,
        @Nonnull final ExecutionPhase executionPhase
    ) {
        this(api, policyManager, executionPhase, null, null, null);
    }

    /**
     * Constructor with API Product support. When apiProductRegistry and productPlanDefinitionCache
     * are provided, product plans are iterated first (product-first validation per PRD).
     */
    public HttpSecurityChain(
        @Nonnull final Api api,
        @Nonnull final PolicyManager policyManager,
        @Nonnull final ExecutionPhase executionPhase,
        @Nullable final String environmentId,
        @Nullable final ApiProductRegistry apiProductRegistry,
        @Nullable final ProductPlanDefinitionCache productPlanDefinitionCache
    ) {
        super(
            Flowable.fromIterable(
                buildSecurityPlans(api, policyManager, executionPhase, environmentId, apiProductRegistry, productPlanDefinitionCache)
            ),
            executionPhase
        );
    }

    private static List<HttpSecurityPlan> buildSecurityPlans(
        Api api,
        PolicyManager policyManager,
        ExecutionPhase executionPhase,
        String environmentId,
        ApiProductRegistry apiProductRegistry,
        ProductPlanDefinitionCache productPlanDefinitionCache
    ) {
        List<HttpSecurityPlan> productPlans = new ArrayList<>();
        List<HttpSecurityPlan> apiPlans = new ArrayList<>();

        addProductPlansFirst(
            productPlans,
            api,
            environmentId,
            apiProductRegistry,
            productPlanDefinitionCache,
            policyManager,
            executionPhase
        );
        addApiPlans(apiPlans, api, policyManager, executionPhase);

        // Product plans must be tried before API plans (PRD product-first).
        // Sort each group by plan order, then concatenate: product plans first, then API plans.
        productPlans.sort(Comparator.comparingInt(HttpSecurityPlan::order));
        apiPlans.sort(Comparator.comparingInt(HttpSecurityPlan::order));

        List<HttpSecurityPlan> result = new ArrayList<>(productPlans.size() + apiPlans.size());
        result.addAll(productPlans);
        result.addAll(apiPlans);
        return result;
    }

    private static void addProductPlansFirst(
        List<HttpSecurityPlan> out,
        Api api,
        String environmentId,
        ApiProductRegistry apiProductRegistry,
        ProductPlanDefinitionCache productPlanDefinitionCache,
        PolicyManager policyManager,
        ExecutionPhase executionPhase
    ) {
        if (environmentId == null || apiProductRegistry == null || productPlanDefinitionCache == null) {
            return;
        }
        Collection<ReactableApiProduct> allProducts = apiProductRegistry.getAll();
        if (allProducts == null) {
            return;
        }
        for (ReactableApiProduct product : allProducts) {
            if (
                product.getEnvironmentId() != null &&
                product.getEnvironmentId().equals(environmentId) &&
                product.getApiIds() != null &&
                product.getApiIds().contains(api.getId())
            ) {
                List<? extends AbstractPlan> plans = productPlanDefinitionCache.getByApiProductId(product.getId());
                if (plans != null) {
                    for (AbstractPlan plan : plans) {
                        HttpSecurityPlan httpSecurityPlan = HttpSecurityPlanFactory.forPlan(
                            api.getId(),
                            plan,
                            policyManager,
                            executionPhase
                        );
                        if (httpSecurityPlan != null) {
                            out.add(httpSecurityPlan);
                        }
                    }
                }
            }
        }
    }

    private static void addApiPlans(List<HttpSecurityPlan> out, Api api, PolicyManager policyManager, ExecutionPhase executionPhase) {
        stream(api.getPlans())
            .map(plan -> HttpSecurityPlanFactory.forPlan(api.getId(), plan, policyManager, executionPhase))
            .filter(Objects::nonNull)
            .forEach(out::add);
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
