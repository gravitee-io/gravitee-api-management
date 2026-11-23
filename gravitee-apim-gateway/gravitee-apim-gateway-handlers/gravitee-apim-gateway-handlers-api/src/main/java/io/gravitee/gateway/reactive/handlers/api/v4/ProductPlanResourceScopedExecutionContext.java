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
import io.gravitee.resource.api.ResourceManager;
import io.reactivex.rxjava3.core.Completable;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import lombok.experimental.Delegate;

/**
 * Execution context visible to product plan and product flow policies.
 *
 * <p>Synchronous steps and payload transformers resolve the product resource manager via {@link #getComponent}. An
 * invoker, endpoint-request action, entrypoint-response action or terminate action registered through this context
 * keeps that same manager when it runs later. The invoker a product policy registers is scoped as a whole, so the
 * invoker it delegates to also runs with the product resource manager. The request context's {@code componentProvider}
 * is not swapped, so a later API phase that registers its own invoker or action keeps the member API's resources.
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
        this.delegate = delegate;
        this.productScopedProvider = productScopedProvider;
        this.productResourceManager = Objects.requireNonNull(productResourceManager, "Product resource manager is not available");
    }

    @Override
    public <T> T getComponent(Class<T> type) {
        if (type == ResourceManager.class) {
            return type.cast(productResourceManager);
        }
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
        delegate.setInternalAttribute(name, scopeInvoker(name, value));
    }

    @Override
    public void putInternalAttribute(String name, Object value) {
        delegate.putInternalAttribute(name, scopeInvoker(name, value));
    }

    @Override
    public <T> Map<String, T> getInternalAttributes() {
        return new InvokerScopingAttributeMap<>(delegate.getInternalAttributes());
    }

    @Override
    public void addActionOnEndpointRequest(BasePolicy source, Function<HttpExecutionContext, Completable> onEndpointRequestCallback) {
        delegate.addActionOnEndpointRequest(source, actionCtx -> runWithProductResources(actionCtx, onEndpointRequestCallback));
    }

    @Override
    public void addActionOnEntrypointResponse(BasePolicy source, Function<HttpExecutionContext, Completable> onEntrypointResponseCallback) {
        delegate.addActionOnEntrypointResponse(source, actionCtx -> runWithProductResources(actionCtx, onEntrypointResponseCallback));
    }

    @Override
    public void addActionOnTerminate(BasePolicy source, Function<HttpExecutionContext, Completable> onTerminateCallback) {
        delegate.addActionOnTerminate(source, actionCtx -> runWithProductResources(actionCtx, onTerminateCallback));
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

    /**
     * A v3 {@code io.gravitee.gateway.api.Invoker} is adapted later, on the raw context, and is left unchanged. Only an
     * {@link HttpInvoker} installed by a product policy is scoped.
     */
    private Object scopeInvoker(String name, Object value) {
        if (
            ATTR_INTERNAL_INVOKER.equals(name) &&
            value instanceof HttpInvoker invoker &&
            !(value instanceof ProductResourceScopedHttpInvoker)
        ) {
            return new ProductResourceScopedHttpInvoker(invoker);
        }
        return value;
    }

    private interface ScopedOverrides {
        <T> T getComponent(Class<T> type);

        MutableExecutionContext componentProvider(ComponentProvider componentProvider);

        void setInternalAttribute(String name, Object value);

        void putInternalAttribute(String name, Object value);

        <T> Map<String, T> getInternalAttributes();

        void addActionOnEndpointRequest(BasePolicy source, Function<HttpExecutionContext, Completable> onEndpointRequestCallback);

        void addActionOnEntrypointResponse(BasePolicy source, Function<HttpExecutionContext, Completable> onEntrypointResponseCallback);

        void addActionOnTerminate(BasePolicy source, Function<HttpExecutionContext, Completable> onTerminateCallback);
    }

    private final class InvokerScopingAttributeMap<T> extends AbstractMap<String, T> {

        private final Map<String, T> raw;

        private InvokerScopingAttributeMap(Map<String, T> raw) {
            this.raw = raw;
        }

        @Override
        public T get(Object key) {
            return raw.get(key);
        }

        @Override
        public boolean containsKey(Object key) {
            return raw.containsKey(key);
        }

        @Override
        public T remove(Object key) {
            return raw.remove(key);
        }

        @Override
        public int size() {
            return raw.size();
        }

        @Override
        public T put(String key, T value) {
            return raw.put(key, scopedValue(key, value));
        }

        @Override
        public void putAll(Map<? extends String, ? extends T> values) {
            values.forEach(this::put);
        }

        @Override
        public Set<Entry<String, T>> entrySet() {
            return new AbstractSet<>() {
                @Override
                public Iterator<Entry<String, T>> iterator() {
                    Iterator<Entry<String, T>> rawIterator = raw.entrySet().iterator();
                    return new Iterator<>() {
                        @Override
                        public boolean hasNext() {
                            return rawIterator.hasNext();
                        }

                        @Override
                        public Entry<String, T> next() {
                            return new ScopedEntry(rawIterator.next());
                        }

                        @Override
                        public void remove() {
                            rawIterator.remove();
                        }
                    };
                }

                @Override
                public int size() {
                    return raw.size();
                }
            };
        }

        private final class ScopedEntry implements Entry<String, T> {

            private final Entry<String, T> rawEntry;

            private ScopedEntry(Entry<String, T> rawEntry) {
                this.rawEntry = rawEntry;
            }

            @Override
            public String getKey() {
                return rawEntry.getKey();
            }

            @Override
            public T getValue() {
                return rawEntry.getValue();
            }

            @Override
            public T setValue(T value) {
                return rawEntry.setValue(scopedValue(rawEntry.getKey(), value));
            }
        }

        @SuppressWarnings("unchecked")
        private T scopedValue(String key, T value) {
            return (T) scopeInvoker(key, value);
        }
    }

    private final class ProductResourceScopedHttpInvoker implements HttpInvoker {

        private final HttpInvoker delegate;

        private ProductResourceScopedHttpInvoker(HttpInvoker delegate) {
            this.delegate = delegate;
        }

        @Override
        public String getId() {
            return delegate.getId();
        }

        @Override
        public Completable invoke(HttpExecutionContext ctx) {
            return runWithProductResources(ctx, delegate::invoke);
        }
    }
}
