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

import static io.gravitee.gateway.reactive.api.context.InternalContextAttributes.ATTR_INTERNAL_INVOKER;

import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.reactive.api.context.http.HttpExecutionContext;
import io.gravitee.gateway.reactive.api.invoker.HttpInvoker;
import io.gravitee.gateway.reactive.api.policy.base.BasePolicy;
import io.gravitee.gateway.reactive.core.context.MutableExecutionContext;
import io.gravitee.gateway.reactive.core.context.WrappedExecutionContext;
import io.gravitee.gateway.resource.ResourceLifecycleManager;
import io.reactivex.rxjava3.core.Completable;
import java.util.function.Function;
import lombok.experimental.Delegate;

/**
 * Execution context visible to product plan and product flow policies.
 *
 * <p>Synchronous steps and payload transformers resolve the product resource manager via {@link #getComponent}. An
 * invoker or endpoint-request action registered through this context keeps that same manager when it runs later. The
 * request context's {@code componentProvider} is not swapped, so a later API phase that registers its own invoker or
 * action keeps the member API's resources.
 */
final class ProductPlanResourceScopedExecutionContext implements MutableExecutionContext, WrappedExecutionContext {

    @Delegate(types = { MutableExecutionContext.class }, excludes = ScopedOverrides.class)
    private final MutableExecutionContext delegate;

    private final ComponentProvider productScopedProvider;
    private final ResourceLifecycleManager productResourceManager;

    ProductPlanResourceScopedExecutionContext(
        MutableExecutionContext delegate,
        ComponentProvider productScopedProvider,
        ResourceLifecycleManager productResourceManager
    ) {
        if (productResourceManager == null) {
            throw new IllegalStateException("Product resource manager is not available");
        }
        this.delegate = delegate;
        this.productScopedProvider = productScopedProvider;
        this.productResourceManager = productResourceManager;
    }

    @Override
    public <T> T getComponent(Class<T> type) {
        T scoped = productScopedProvider.getComponent(type);
        if (scoped != null) {
            return scoped;
        }
        return delegate.getComponent(type);
    }

    @Override
    public MutableExecutionContext componentProvider(ComponentProvider componentProvider) {
        return this;
    }

    @Override
    public void setInternalAttribute(String name, Object value) {
        if (
            ATTR_INTERNAL_INVOKER.equals(name) &&
            value instanceof HttpInvoker invoker &&
            !(value instanceof ProductResourceScopedHttpInvoker)
        ) {
            delegate.setInternalAttribute(
                name,
                new ProductResourceScopedHttpInvoker(invoker, productScopedProvider, productResourceManager)
            );
            return;
        }
        delegate.setInternalAttribute(name, value);
    }

    @Override
    public void addActionOnEndpointRequest(BasePolicy source, Function<HttpExecutionContext, Completable> onEndpointRequestCallback) {
        delegate.addActionOnEndpointRequest(source, actionCtx -> runWithProductResources(actionCtx, onEndpointRequestCallback));
    }

    @Override
    public HttpExecutionContext unwrap() {
        return delegate;
    }

    private Completable runWithProductResources(HttpExecutionContext ctx, Function<HttpExecutionContext, Completable> callback) {
        if (ctx instanceof ProductPlanResourceScopedExecutionContext) {
            return callback.apply(ctx);
        }
        if (!(ctx instanceof MutableExecutionContext mutable)) {
            return Completable.error(new IllegalStateException("Product resource scope cannot be restored for this execution context"));
        }
        return ApiProductPlanResourceScope.withProductResourceManager(mutable, productScopedProvider, productResourceManager, scoped ->
            callback.apply(scoped)
        );
    }

    private interface ScopedOverrides {
        <T> T getComponent(Class<T> type);

        MutableExecutionContext componentProvider(ComponentProvider componentProvider);

        void setInternalAttribute(String name, Object value);

        void addActionOnEndpointRequest(BasePolicy source, Function<HttpExecutionContext, Completable> onEndpointRequestCallback);
    }

    private static final class ProductResourceScopedHttpInvoker implements HttpInvoker {

        private final HttpInvoker delegate;
        private final ComponentProvider productScopedProvider;
        private final ResourceLifecycleManager productResourceManager;

        private ProductResourceScopedHttpInvoker(
            HttpInvoker delegate,
            ComponentProvider productScopedProvider,
            ResourceLifecycleManager productResourceManager
        ) {
            this.delegate = delegate;
            this.productScopedProvider = productScopedProvider;
            this.productResourceManager = productResourceManager;
        }

        @Override
        public String getId() {
            return delegate.getId();
        }

        @Override
        public Completable invoke(HttpExecutionContext ctx) {
            if (ctx instanceof ProductPlanResourceScopedExecutionContext) {
                return delegate.invoke(ctx);
            }
            if (!(ctx instanceof MutableExecutionContext mutable)) {
                return Completable.error(new IllegalStateException("Product resource scope cannot be restored for this execution context"));
            }
            return ApiProductPlanResourceScope.withProductResourceManager(mutable, productScopedProvider, productResourceManager, scoped ->
                delegate.invoke(scoped)
            );
        }
    }
}
