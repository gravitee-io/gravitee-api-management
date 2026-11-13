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

import static io.gravitee.gateway.reactive.handlers.api.security.plan.AbstractSecurityPlan.ATTR_INTERNAL_PLAN_API_PRODUCT;
import static io.gravitee.gateway.reactive.handlers.api.v4.product.ApiProductRuntime.ATTR_INTERNAL_API_PRODUCT_RUNTIME;

import io.gravitee.definition.model.v4.Api;
import io.gravitee.gateway.reactive.api.ExecutionPhase;
import io.gravitee.gateway.reactive.api.context.http.HttpPlainExecutionContext;
import io.gravitee.gateway.reactive.handlers.api.security.plan.HttpSecurityPlan;
import io.gravitee.gateway.reactive.handlers.api.v4.product.ApiProductRuntime;
import io.gravitee.gateway.reactive.handlers.api.v4.product.ApiProductRuntimeRegistry;
import io.gravitee.gateway.reactive.handlers.api.v4.product.ApiProductRuntimeRegistry.MemberApiRuntimes;
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
 * Tries, on each request, the security plans of the products listing the API first, sorted by order across products,
 * then the plans of the API, sorted by order. The two groups are never interleaved.
 *
 * <p>The product plans come from the runtimes published when the request starts, which stay pinned while the chain
 * runs. The runtime of the product whose plan accepted the request stays pinned for the rest of the request, so its
 * plan flows run on the same runtime as its security; the others are released.</p>
 *
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HttpSecurityChain extends io.gravitee.gateway.reactive.handlers.api.security.HttpSecurityChain {

    private final List<HttpSecurityPlan> apiPlans;
    private final String apiId;
    private final String environmentId;
    private final ApiProductRuntimeRegistry apiProductRuntimeRegistry;

    public HttpSecurityChain(
        @Nonnull final Api api,
        @Nonnull final PolicyManager policyManager,
        @Nonnull final ExecutionPhase executionPhase
    ) {
        this(api, policyManager, executionPhase, null, null);
    }

    /**
     * @param apiProductRuntimeRegistry the runtimes of the products, null when API Product support is not loaded
     */
    public HttpSecurityChain(
        @Nonnull final Api api,
        @Nonnull final PolicyManager policyManager,
        @Nonnull final ExecutionPhase executionPhase,
        @Nullable final ApiProductRuntimeRegistry apiProductRuntimeRegistry,
        @Nullable final String environmentId
    ) {
        this(getApiPlans(api, policyManager, executionPhase), executionPhase, api.getId(), environmentId, apiProductRuntimeRegistry);
    }

    private HttpSecurityChain(
        List<HttpSecurityPlan> apiPlans,
        ExecutionPhase executionPhase,
        String apiId,
        String environmentId,
        ApiProductRuntimeRegistry apiProductRuntimeRegistry
    ) {
        super(Flowable.fromIterable(apiPlans), executionPhase);
        this.apiPlans = apiPlans;
        this.apiId = apiId;
        this.environmentId = environmentId;
        this.apiProductRuntimeRegistry = apiProductRuntimeRegistry;
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

    @Override
    public Completable execute(HttpPlainExecutionContext ctx) {
        if (apiProductRuntimeRegistry == null || environmentId == null) {
            return execute(ctx, chain);
        }
        // using() settles the pins exactly once, whether the chain terminates or is disposed, even after terminating.
        return Completable.using(
            () -> apiProductRuntimeRegistry.acquireForApi(apiId, environmentId),
            productRuntimes -> execute(ctx, plansWith(productRuntimes)),
            productRuntimes -> keepRuntimeOfAcceptingProduct(ctx, productRuntimes),
            true
        );
    }

    private Flowable<HttpSecurityPlan> plansWith(MemberApiRuntimes productRuntimes) {
        if (productRuntimes.runtimes().isEmpty()) {
            return chain;
        }
        List<HttpSecurityPlan> plans = new ArrayList<>(productRuntimes.securityPlans().size() + apiPlans.size());
        plans.addAll(productRuntimes.securityPlans());
        plans.addAll(apiPlans);
        return Flowable.fromIterable(plans);
    }

    @Override
    protected Completable execute(HttpPlainExecutionContext ctx, Flowable<HttpSecurityPlan> securityPlans) {
        return securityPlans
            .flatMapSingle(httpSecurityPlan -> httpSecurityPlan.onWellKnown(ctx))
            .any(Boolean::booleanValue)
            .flatMapCompletable(onWellKnown -> {
                if (onWellKnown) {
                    return ctx.interrupt();
                }
                return Completable.complete();
            })
            .andThen(super.execute(ctx, securityPlans));
    }

    private static void keepRuntimeOfAcceptingProduct(HttpPlainExecutionContext ctx, MemberApiRuntimes productRuntimes) {
        String acceptingProductId = ctx.getInternalAttribute(ATTR_INTERNAL_PLAN_API_PRODUCT);
        for (ApiProductRuntime runtime : productRuntimes.runtimes()) {
            if (acceptingProductId != null && acceptingProductId.equals(runtime.product().getId())) {
                ctx.setInternalAttribute(ATTR_INTERNAL_API_PRODUCT_RUNTIME, runtime);
                acceptingProductId = null;
            } else {
                runtime.release();
            }
        }
    }
}
