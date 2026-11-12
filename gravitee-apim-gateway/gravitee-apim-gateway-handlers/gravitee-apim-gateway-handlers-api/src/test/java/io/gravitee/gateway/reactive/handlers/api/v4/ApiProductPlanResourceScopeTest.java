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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.core.component.CustomComponentProvider;
import io.gravitee.gateway.reactive.api.message.Message;
import io.gravitee.gateway.reactive.core.context.MutableExecutionContext;
import io.gravitee.gateway.reactive.core.context.MutableRequest;
import io.gravitee.gateway.reactive.core.context.MutableResponse;
import io.gravitee.gateway.resource.ResourceLifecycleManager;
import io.gravitee.resource.api.ResourceManager;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.FlowableTransformer;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApiProductPlanResourceScopeTest {

    @Test
    void scoped_chunks_transformer_resolves_product_manager() {
        MutableExecutionContext ctx = mock(MutableExecutionContext.class);
        MutableResponse response = mock(MutableResponse.class);
        ResourceLifecycleManager apiManager = mock(ResourceLifecycleManager.class);
        ResourceLifecycleManager productManager = mock(ResourceLifecycleManager.class);

        ComponentProvider apiProvider = providerFor(apiManager);
        AtomicReference<ComponentProvider> activeProvider = trackProvider(ctx, apiProvider);
        when(ctx.getComponent(eq(ResourceManager.class))).thenAnswer(invocation ->
            activeProvider.get().getComponent(ResourceManager.class)
        );
        when(ctx.response()).thenReturn(response);

        ArgumentCaptor<FlowableTransformer<Buffer, Buffer>> transformerCaptor = ArgumentCaptor.forClass(FlowableTransformer.class);
        when(response.onChunks(transformerCaptor.capture())).thenReturn(Completable.complete());

        ApiProductPlanResourceScope.withProductResourceManager(ctx, apiProvider, productManager, scopedCtx ->
            scopedCtx.response().onChunks(chunks -> chunks)
        ).blockingAwait();
        assertThat(activeProvider.get()).isSameAs(apiProvider);

        AtomicReference<ResourceManager> seen = new AtomicReference<>();
        Flowable.fromPublisher(transformerCaptor.getValue().apply(Flowable.just(Buffer.buffer("x"))))
            .doOnNext(buffer -> seen.set(ctx.getComponent(ResourceManager.class)))
            .blockingSubscribe();

        assertThat(seen.get()).isSameAs(productManager);
    }

    @Test
    void scoped_chunks_api_neighbours_see_api_manager_around_product_transformer() {
        MutableExecutionContext ctx = mock(MutableExecutionContext.class);
        MutableResponse response = mock(MutableResponse.class);
        ResourceLifecycleManager apiManager = mock(ResourceLifecycleManager.class);
        ResourceLifecycleManager productManager = mock(ResourceLifecycleManager.class);

        ComponentProvider apiProvider = providerFor(apiManager);
        trackProvider(ctx, apiProvider);
        when(ctx.getComponent(eq(ResourceManager.class))).thenAnswer(invocation -> apiProvider.getComponent(ResourceManager.class));
        when(ctx.response()).thenReturn(response);

        ArgumentCaptor<FlowableTransformer<Buffer, Buffer>> transformerCaptor = ArgumentCaptor.forClass(FlowableTransformer.class);
        when(response.onChunks(transformerCaptor.capture())).thenReturn(Completable.complete());

        ApiProductPlanResourceScope.withProductResourceManager(ctx, apiProvider, productManager, scopedCtx ->
            scopedCtx.response().onChunks(chunks -> chunks)
        ).blockingAwait();

        FlowableTransformer<Buffer, Buffer> productTransformer = transformerCaptor.getValue();
        AtomicReference<ResourceManager> apiBefore = new AtomicReference<>();
        AtomicReference<ResourceManager> apiAfter = new AtomicReference<>();

        Flowable.just(Buffer.buffer("x"))
            .compose(upstream -> upstream.doOnSubscribe(s -> apiBefore.set(ctx.getComponent(ResourceManager.class))))
            .compose(productTransformer)
            .compose(upstream -> upstream.doOnNext(buffer -> apiAfter.set(ctx.getComponent(ResourceManager.class))))
            .blockingSubscribe();

        assertThat(apiBefore.get()).isSameAs(apiManager);
        assertThat(apiAfter.get()).isSameAs(apiManager);
    }

    @Test
    void scoped_messages_api_neighbours_see_api_manager_around_product_transformer() {
        MutableExecutionContext ctx = mock(MutableExecutionContext.class);
        MutableRequest request = mock(MutableRequest.class);
        ResourceLifecycleManager apiManager = mock(ResourceLifecycleManager.class);
        ResourceLifecycleManager productManager = mock(ResourceLifecycleManager.class);

        ComponentProvider apiProvider = providerFor(apiManager);
        trackProvider(ctx, apiProvider);
        when(ctx.getComponent(eq(ResourceManager.class))).thenAnswer(invocation -> apiProvider.getComponent(ResourceManager.class));
        when(ctx.request()).thenReturn(request);

        ArgumentCaptor<FlowableTransformer<Message, Message>> transformerCaptor = ArgumentCaptor.forClass(FlowableTransformer.class);
        when(request.onMessages(transformerCaptor.capture())).thenReturn(Completable.complete());

        ApiProductPlanResourceScope.withProductResourceManager(ctx, apiProvider, productManager, scopedCtx ->
            scopedCtx.request().onMessages(messages -> messages)
        ).blockingAwait();

        FlowableTransformer<Message, Message> productTransformer = transformerCaptor.getValue();
        AtomicReference<ResourceManager> apiBefore = new AtomicReference<>();
        AtomicReference<ResourceManager> apiAfter = new AtomicReference<>();
        Message message = mock(Message.class);

        Flowable.just(message)
            .compose(upstream -> upstream.doOnSubscribe(s -> apiBefore.set(ctx.getComponent(ResourceManager.class))))
            .compose(productTransformer)
            .compose(upstream -> upstream.doOnNext(msg -> apiAfter.set(ctx.getComponent(ResourceManager.class))))
            .blockingSubscribe();

        assertThat(apiBefore.get()).isSameAs(apiManager);
        assertThat(apiAfter.get()).isSameAs(apiManager);
    }

    private static CustomComponentProvider providerComponents(ResourceLifecycleManager manager) {
        CustomComponentProvider components = new CustomComponentProvider();
        components.add(ResourceManager.class, manager);
        return components;
    }

    private static ComponentProvider providerFor(ResourceLifecycleManager manager) {
        return providerComponents(manager)::getComponent;
    }

    private static AtomicReference<ComponentProvider> trackProvider(MutableExecutionContext ctx, ComponentProvider initial) {
        AtomicReference<ComponentProvider> activeProvider = new AtomicReference<>(initial);
        doAnswer(invocation -> {
            activeProvider.set(invocation.getArgument(0));
            return null;
        })
            .when(ctx)
            .componentProvider(any(ComponentProvider.class));
        return activeProvider;
    }
}
