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

import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.reactive.core.context.MutableExecutionContext;
import lombok.experimental.Delegate;

/**
 * Execution context visible to product plan policies. Synchronous steps and payload transformers resolve the product
 * resource manager via {@link #getComponent}; the request context's {@code componentProvider} is not swapped, so later
 * API phases and neighbouring stream transformers are unaffected.
 */
final class ProductPlanResourceScopedExecutionContext implements MutableExecutionContext {

    @Delegate(types = { MutableExecutionContext.class }, excludes = ScopedOverrides.class)
    private final MutableExecutionContext delegate;

    private final ComponentProvider productScopedProvider;

    ProductPlanResourceScopedExecutionContext(MutableExecutionContext delegate, ComponentProvider productScopedProvider) {
        this.delegate = delegate;
        this.productScopedProvider = productScopedProvider;
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

    private interface ScopedOverrides {
        <T> T getComponent(Class<T> type);

        MutableExecutionContext componentProvider(ComponentProvider componentProvider);
    }
}
