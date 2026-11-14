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
import io.gravitee.gateway.reactive.api.message.Message;
import io.gravitee.gateway.reactive.core.context.MutableExecutionContext;
import io.gravitee.gateway.reactive.core.context.MutableResponse;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.FlowableTransformer;
import io.reactivex.rxjava3.core.MaybeTransformer;
import lombok.experimental.Delegate;

/**
 * Wraps deferred response payload hooks so product plan policies still resolve the product {@code ResourceManager} when
 * their {@code onBody}/{@code onChunks}/{@code onMessages} transformers run, after the synchronous plan chain ends.
 */
final class ProductPlanPayloadScopedResponse implements MutableResponse {

    @Delegate(types = { MutableResponse.class }, excludes = PayloadHooks.class)
    private final MutableResponse delegate;

    private final MutableExecutionContext ctx;
    private final ComponentProvider productScopedProvider;
    private final ComponentProvider apiComponentProvider;

    ProductPlanPayloadScopedResponse(
        MutableExecutionContext ctx,
        ComponentProvider productScopedProvider,
        ComponentProvider apiComponentProvider
    ) {
        this.ctx = ctx;
        this.delegate = ctx.response();
        this.productScopedProvider = productScopedProvider;
        this.apiComponentProvider = apiComponentProvider;
    }

    @Override
    public Completable onBody(MaybeTransformer<Buffer, Buffer> onBody) {
        return delegate.onBody(ProductPlanPayloadScope.scopedOnBody(ctx, productScopedProvider, apiComponentProvider, onBody));
    }

    @Override
    public Completable onChunks(FlowableTransformer<Buffer, Buffer> onChunks) {
        return delegate.onChunks(ProductPlanPayloadScope.scopedOnPublisher(ctx, productScopedProvider, apiComponentProvider, onChunks));
    }

    @Override
    public Completable onMessages(FlowableTransformer<Message, Message> onMessages) {
        return delegate.onMessages(ProductPlanPayloadScope.scopedOnPublisher(ctx, productScopedProvider, apiComponentProvider, onMessages));
    }

    private interface PayloadHooks {
        Completable onBody(MaybeTransformer<Buffer, Buffer> onBody);

        Completable onChunks(FlowableTransformer<Buffer, Buffer> onChunks);

        Completable onMessages(FlowableTransformer<Message, Message> onMessages);
    }
}
