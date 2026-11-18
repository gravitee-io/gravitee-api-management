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
package io.gravitee.gateway.reactive.handlers.api.v4;

import io.gravitee.gateway.reactive.api.ExecutionPhase;
import io.gravitee.gateway.reactive.api.context.http.HttpPlainExecutionContext;
import io.gravitee.gateway.reactive.api.policy.SecurityToken;
import io.gravitee.gateway.reactive.api.policy.http.HttpSecurityPolicy;
import io.gravitee.gateway.reactive.core.context.MutableExecutionContext;
import io.gravitee.gateway.reactive.handlers.api.security.plan.HttpSecurityPlan;
import io.gravitee.gateway.reactive.handlers.api.security.plan.SecurityPlanContext;
import io.gravitee.gateway.resource.ResourceLifecycleManager;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;

/**
 * Runs one product security plan against that product's resource manager.
 * The request keeps the API's component provider: the scoped context only overrides {@code ResourceManager}.
 */
public final class ProductResourceScopedHttpSecurityPlan extends HttpSecurityPlan {

    private static final HttpSecurityPolicy UNUSED = new HttpSecurityPolicy() {
        @Override
        public String id() {
            return "product-resource-scope";
        }

        @Override
        public Maybe<SecurityToken> extractSecurityToken(HttpPlainExecutionContext ctx) {
            return Maybe.error(new IllegalStateException("A product security plan resolves resources through its delegate plan"));
        }
    };

    private final HttpSecurityPlan delegate;
    private final ResourceLifecycleManager productResourceManager;

    public ProductResourceScopedHttpSecurityPlan(HttpSecurityPlan delegate, ResourceLifecycleManager productResourceManager) {
        super(new SecurityPlanContext(delegate.id(), delegate.id(), null, delegate.apiProductId()), UNUSED);
        this.delegate = delegate;
        this.productResourceManager = productResourceManager;
    }

    @Override
    public String id() {
        return delegate.id();
    }

    @Override
    public int order() {
        return delegate.order();
    }

    @Override
    public Single<Boolean> canExecute(HttpPlainExecutionContext ctx) {
        return delegate.canExecute(scoped(ctx));
    }

    @Override
    public Completable execute(HttpPlainExecutionContext ctx, ExecutionPhase executionPhase) {
        return delegate.execute(scoped(ctx), executionPhase);
    }

    @Override
    public Single<Boolean> wwwAuthenticate(HttpPlainExecutionContext ctx) {
        return delegate.wwwAuthenticate(scoped(ctx));
    }

    @Override
    public Single<Boolean> onWellKnown(HttpPlainExecutionContext ctx) {
        return delegate.onWellKnown(scoped(ctx));
    }

    private HttpPlainExecutionContext scoped(HttpPlainExecutionContext ctx) {
        if (!(ctx instanceof MutableExecutionContext mutable)) {
            return ctx;
        }
        return ApiProductPlanResourceScope.scope(mutable, productResourceManager);
    }
}
