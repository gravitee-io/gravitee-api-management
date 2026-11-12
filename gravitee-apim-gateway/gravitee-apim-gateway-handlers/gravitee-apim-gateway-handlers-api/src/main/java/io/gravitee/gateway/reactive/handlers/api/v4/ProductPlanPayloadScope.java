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

import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.reactive.core.context.MutableExecutionContext;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.FlowableTransformer;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.MaybeTransformer;

/**
 * Scopes {@code ResourceManager} resolution to a single product-registered payload transformer, without disturbing
 * downstream API transformers on the same stream.
 */
final class ProductPlanPayloadScope {

    private ProductPlanPayloadScope() {}

    static MaybeTransformer<Buffer, Buffer> scopedOnBody(
        MutableExecutionContext ctx,
        ComponentProvider productScopedProvider,
        ComponentProvider apiComponentProvider,
        MaybeTransformer<Buffer, Buffer> onBody
    ) {
        return source ->
            Maybe.using(
                () -> {
                    ctx.componentProvider(productScopedProvider);
                    return apiComponentProvider;
                },
                ignored -> source.compose(onBody),
                previous -> ctx.componentProvider(previous),
                true
            );
    }

    static <T> FlowableTransformer<T, T> scopedOnPublisher(
        MutableExecutionContext ctx,
        ComponentProvider productScopedProvider,
        ComponentProvider apiComponentProvider,
        FlowableTransformer<T, T> onPublisher
    ) {
        return source ->
            Flowable.using(
                () -> {
                    ctx.componentProvider(productScopedProvider);
                    return apiComponentProvider;
                },
                ignored -> Flowable.fromPublisher(onPublisher.apply(source)),
                previous -> ctx.componentProvider(previous),
                true
            );
    }
}
