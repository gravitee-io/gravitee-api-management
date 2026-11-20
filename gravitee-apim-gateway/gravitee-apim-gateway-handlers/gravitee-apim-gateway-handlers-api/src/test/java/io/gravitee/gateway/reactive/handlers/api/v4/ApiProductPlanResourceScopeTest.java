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
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.core.component.CustomComponentProvider;
import io.gravitee.gateway.reactive.api.context.http.HttpExecutionContext;
import io.gravitee.gateway.reactive.api.invoker.HttpInvoker;
import io.gravitee.gateway.reactive.api.message.Message;
import io.gravitee.gateway.reactive.api.policy.base.BasePolicy;
import io.gravitee.gateway.reactive.core.context.MutableExecutionContext;
import io.gravitee.gateway.reactive.core.context.MutableRequest;
import io.gravitee.gateway.reactive.core.context.MutableResponse;
import io.gravitee.gateway.reactive.core.context.WrappedExecutionContext;
import io.gravitee.gateway.reactive.handlers.api.v4.product.EmptyProductResourceLifecycleManager;
import io.gravitee.gateway.resource.ResourceLifecycleManager;
import io.gravitee.resource.api.ResourceManager;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.FlowableTransformer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApiProductPlanResourceScopeTest {

    @Test
    void scoped_context_exposes_the_request_context_it_decorates() {
        MutableExecutionContext ctx = mock(MutableExecutionContext.class);
        AtomicReference<MutableExecutionContext> scoped = new AtomicReference<>();

        ApiProductPlanResourceScope.withProductResourceManager(
            ctx,
            providerFor(mock(ResourceLifecycleManager.class)),
            mock(ResourceLifecycleManager.class),
            scopedCtx -> {
                scoped.set(scopedCtx);
                return Completable.complete();
            }
        ).blockingAwait();

        assertThat(scoped.get()).isNotSameAs(ctx).isInstanceOf(WrappedExecutionContext.class);
        assertThat(((WrappedExecutionContext) scoped.get()).unwrap()).isSameAs(ctx);
    }

    @Test
    void scoped_chunks_transformer_resolves_product_manager() {
        MutableExecutionContext ctx = mock(MutableExecutionContext.class);
        MutableResponse response = mock(MutableResponse.class);
        ResourceLifecycleManager apiManager = mock(ResourceLifecycleManager.class);
        ResourceLifecycleManager productManager = mock(ResourceLifecycleManager.class);

        ComponentProvider apiProvider = providerFor(apiManager);
        when(ctx.response()).thenReturn(response);

        ArgumentCaptor<FlowableTransformer<Buffer, Buffer>> transformerCaptor = ArgumentCaptor.forClass(FlowableTransformer.class);
        when(response.onChunks(transformerCaptor.capture())).thenReturn(Completable.complete());

        AtomicReference<ResourceManager> seen = new AtomicReference<>();
        ApiProductPlanResourceScope.withProductResourceManager(ctx, apiProvider, productManager, scopedCtx ->
            scopedCtx.response().onChunks(chunks -> chunks.doOnNext(buffer -> seen.set(scopedCtx.getComponent(ResourceManager.class))))
        ).blockingAwait();

        Flowable.fromPublisher(transformerCaptor.getValue().apply(Flowable.just(Buffer.buffer("x")))).blockingSubscribe();

        assertThat(seen.get()).isSameAs(productManager);
    }

    @Test
    void scoped_chunks_api_neighbours_see_api_manager_around_product_transformer() {
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

        FlowableTransformer<Buffer, Buffer> productTransformer = transformerCaptor.getValue();
        AtomicReference<ResourceManager> apiBefore = new AtomicReference<>();
        AtomicReference<ResourceManager> apiAfter = new AtomicReference<>();

        Flowable.just(Buffer.buffer("x"))
            .compose(upstream -> upstream.doOnSubscribe(s -> apiBefore.set(ctx.getComponent(ResourceManager.class))))
            .compose(productTransformer)
            .compose(upstream -> upstream.doOnNext(buffer -> apiAfter.set(ctx.getComponent(ResourceManager.class))))
            .blockingSubscribe();

        assertThat(activeProvider.get()).isSameAs(apiProvider);
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
        AtomicReference<ComponentProvider> activeProvider = trackProvider(ctx, apiProvider);
        when(ctx.getComponent(eq(ResourceManager.class))).thenAnswer(invocation ->
            activeProvider.get().getComponent(ResourceManager.class)
        );
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

        assertThat(activeProvider.get()).isSameAs(apiProvider);
        assertThat(apiBefore.get()).isSameAs(apiManager);
        assertThat(apiAfter.get()).isSameAs(apiManager);
    }

    @Test
    void invoker_registered_through_the_scoped_context_resolves_the_product_manager_when_invoked() {
        MutableExecutionContext ctx = mock(MutableExecutionContext.class);
        ResourceLifecycleManager apiManager = mock(ResourceLifecycleManager.class);
        ResourceLifecycleManager productManager = mock(ResourceLifecycleManager.class);
        when(ctx.getComponent(eq(ResourceManager.class))).thenReturn(apiManager);
        AtomicReference<Object> stored = new AtomicReference<>();
        doAnswer(invocation -> {
            stored.set(invocation.getArgument(1));
            return null;
        })
            .when(ctx)
            .setInternalAttribute(eq(ATTR_INTERNAL_INVOKER), any());

        HttpInvoker installed = mock(HttpInvoker.class);
        when(installed.getId()).thenReturn("cache-endpoint-invoker");
        AtomicReference<ResourceManager> seen = new AtomicReference<>();
        when(installed.invoke(any())).thenAnswer(invocation -> {
            seen.set(invocation.getArgument(0, HttpExecutionContext.class).getComponent(ResourceManager.class));
            return Completable.complete();
        });

        ApiProductPlanResourceScope.withProductResourceManager(ctx, providerFor(apiManager), productManager, scopedCtx -> {
            scopedCtx.setInternalAttribute(ATTR_INTERNAL_INVOKER, installed);
            return Completable.complete();
        }).blockingAwait();

        assertThat(stored.get()).isInstanceOf(HttpInvoker.class);
        HttpInvoker storedInvoker = (HttpInvoker) stored.get();
        assertThat(storedInvoker.getId()).isEqualTo("cache-endpoint-invoker");
        storedInvoker.invoke(ctx).blockingAwait();

        assertThat(seen.get()).isSameAs(productManager);
        assertThat(ctx.getComponent(ResourceManager.class)).isSameAs(apiManager);
    }

    @Test
    void endpoint_request_action_registered_through_the_scoped_context_resolves_the_product_manager() {
        MutableExecutionContext ctx = mock(MutableExecutionContext.class);
        ResourceLifecycleManager apiManager = mock(ResourceLifecycleManager.class);
        ResourceLifecycleManager productManager = mock(ResourceLifecycleManager.class);
        when(ctx.getComponent(eq(ResourceManager.class))).thenReturn(apiManager);
        AtomicReference<Function<HttpExecutionContext, Completable>> stored = new AtomicReference<>();
        doAnswer(invocation -> {
            stored.set(invocation.getArgument(1));
            return null;
        })
            .when(ctx)
            .addActionOnEndpointRequest(any(), any());
        BasePolicy policy = mock(BasePolicy.class);
        AtomicReference<ResourceManager> seen = new AtomicReference<>();

        ApiProductPlanResourceScope.withProductResourceManager(ctx, providerFor(apiManager), productManager, scopedCtx -> {
            scopedCtx.addActionOnEndpointRequest(policy, actionCtx -> {
                seen.set(actionCtx.getComponent(ResourceManager.class));
                return Completable.complete();
            });
            return Completable.complete();
        }).blockingAwait();

        stored.get().apply(ctx).blockingAwait();

        assertThat(seen.get()).isSameAs(productManager);
        assertThat(ctx.getComponent(ResourceManager.class)).isSameAs(apiManager);
    }

    @Test
    void invoker_registered_with_put_internal_attribute_resolves_the_product_manager_when_invoked() {
        MutableExecutionContext ctx = mock(MutableExecutionContext.class);
        ResourceLifecycleManager apiManager = mock(ResourceLifecycleManager.class);
        ResourceLifecycleManager productManager = mock(ResourceLifecycleManager.class);
        when(ctx.getComponent(eq(ResourceManager.class))).thenReturn(apiManager);
        AtomicReference<Object> stored = new AtomicReference<>();
        doAnswer(invocation -> {
            stored.set(invocation.getArgument(1));
            return null;
        })
            .when(ctx)
            .putInternalAttribute(eq(ATTR_INTERNAL_INVOKER), any());

        HttpInvoker installed = mock(HttpInvoker.class);
        AtomicReference<ResourceManager> seen = new AtomicReference<>();
        when(installed.invoke(any())).thenAnswer(invocation -> {
            seen.set(invocation.getArgument(0, HttpExecutionContext.class).getComponent(ResourceManager.class));
            return Completable.complete();
        });

        ApiProductPlanResourceScope.withProductResourceManager(ctx, providerFor(apiManager), productManager, scopedCtx -> {
            scopedCtx.putInternalAttribute(ATTR_INTERNAL_INVOKER, installed);
            return Completable.complete();
        }).blockingAwait();

        ((HttpInvoker) stored.get()).invoke(ctx).blockingAwait();

        assertThat(seen.get()).isSameAs(productManager);
        assertThat(ctx.getComponent(ResourceManager.class)).isSameAs(apiManager);
    }

    @Test
    void invoker_written_through_internal_attributes_resolves_the_product_manager_when_invoked() {
        MutableExecutionContext ctx = mock(MutableExecutionContext.class);
        ResourceLifecycleManager apiManager = mock(ResourceLifecycleManager.class);
        ResourceLifecycleManager productManager = mock(ResourceLifecycleManager.class);
        Map<String, Object> attributes = new HashMap<>();
        doReturn(attributes).when(ctx).getInternalAttributes();

        HttpInvoker installed = mock(HttpInvoker.class);
        AtomicReference<ResourceManager> seen = new AtomicReference<>();
        when(installed.invoke(any())).thenAnswer(invocation -> {
            seen.set(invocation.getArgument(0, HttpExecutionContext.class).getComponent(ResourceManager.class));
            return Completable.complete();
        });

        ApiProductPlanResourceScope.withProductResourceManager(ctx, providerFor(apiManager), productManager, scopedCtx -> {
            scopedCtx.<Object>getInternalAttributes().put(ATTR_INTERNAL_INVOKER, installed);
            return Completable.complete();
        }).blockingAwait();

        ((HttpInvoker) attributes.get(ATTR_INTERNAL_INVOKER)).invoke(ctx).blockingAwait();

        assertThat(attributes.get(ATTR_INTERNAL_INVOKER)).isNotSameAs(installed);
        assertThat(seen.get()).isSameAs(productManager);
    }

    @Test
    void non_http_invoker_and_a_second_wrap_are_stored_unchanged() {
        MutableExecutionContext ctx = mock(MutableExecutionContext.class);
        ResourceLifecycleManager productManager = mock(ResourceLifecycleManager.class);
        Map<String, Object> stored = new HashMap<>();
        doAnswer(invocation -> {
            stored.put("value", invocation.getArgument(1));
            return null;
        })
            .when(ctx)
            .setInternalAttribute(eq(ATTR_INTERNAL_INVOKER), any());

        Object legacyInvoker = new Object();
        HttpInvoker installed = mock(HttpInvoker.class);

        ApiProductPlanResourceScope.withProductResourceManager(ctx, providerFor(productManager), productManager, scopedCtx -> {
            scopedCtx.setInternalAttribute(ATTR_INTERNAL_INVOKER, legacyInvoker);
            assertThat(stored.get("value")).isSameAs(legacyInvoker);
            scopedCtx.setInternalAttribute(ATTR_INTERNAL_INVOKER, installed);
            Object wrapped = stored.get("value");
            scopedCtx.setInternalAttribute(ATTR_INTERNAL_INVOKER, wrapped);
            assertThat(stored.get("value")).isSameAs(wrapped);
            return Completable.complete();
        }).blockingAwait();
    }

    @Test
    void scoped_invoker_calls_through_when_already_scoped_and_fails_for_a_context_that_cannot_be_scoped() {
        MutableExecutionContext ctx = mock(MutableExecutionContext.class);
        ResourceLifecycleManager productManager = mock(ResourceLifecycleManager.class);
        AtomicReference<Object> stored = new AtomicReference<>();
        doAnswer(invocation -> {
            stored.set(invocation.getArgument(1));
            return null;
        })
            .when(ctx)
            .setInternalAttribute(eq(ATTR_INTERNAL_INVOKER), any());

        HttpInvoker installed = mock(HttpInvoker.class);
        AtomicReference<HttpExecutionContext> invokedWith = new AtomicReference<>();
        when(installed.invoke(any())).thenAnswer(invocation -> {
            invokedWith.set(invocation.getArgument(0));
            return Completable.complete();
        });
        AtomicReference<MutableExecutionContext> scoped = new AtomicReference<>();

        ApiProductPlanResourceScope.withProductResourceManager(ctx, providerFor(productManager), productManager, scopedCtx -> {
            scoped.set(scopedCtx);
            scopedCtx.setInternalAttribute(ATTR_INTERNAL_INVOKER, installed);
            return Completable.complete();
        }).blockingAwait();

        HttpInvoker storedInvoker = (HttpInvoker) stored.get();
        storedInvoker.invoke(scoped.get()).blockingAwait();
        assertThat(invokedWith.get()).isSameAs(scoped.get());

        storedInvoker
            .invoke(mock(HttpExecutionContext.class))
            .test()
            .assertError(IllegalStateException.class)
            .assertError(error -> "Product resource scope cannot be restored for this execution context".equals(error.getMessage()));
    }

    @Test
    void entrypoint_response_action_registered_through_the_scoped_context_resolves_the_product_manager() {
        assertDeferredActionUsesTheProductManager(ActionKind.ENTRYPOINT_RESPONSE);
    }

    @Test
    void terminate_action_registered_through_the_scoped_context_resolves_the_product_manager() {
        assertDeferredActionUsesTheProductManager(ActionKind.TERMINATE);
    }

    private void assertDeferredActionUsesTheProductManager(ActionKind kind) {
        MutableExecutionContext ctx = mock(MutableExecutionContext.class);
        ResourceLifecycleManager apiManager = mock(ResourceLifecycleManager.class);
        ResourceLifecycleManager productManager = mock(ResourceLifecycleManager.class);
        when(ctx.getComponent(eq(ResourceManager.class))).thenReturn(apiManager);
        AtomicReference<Function<HttpExecutionContext, Completable>> stored = new AtomicReference<>();
        if (kind == ActionKind.ENTRYPOINT_RESPONSE) {
            doAnswer(invocation -> {
                stored.set(invocation.getArgument(1));
                return null;
            })
                .when(ctx)
                .addActionOnEntrypointResponse(any(), any());
        } else {
            doAnswer(invocation -> {
                stored.set(invocation.getArgument(1));
                return null;
            })
                .when(ctx)
                .addActionOnTerminate(any(), any());
        }
        BasePolicy policy = mock(BasePolicy.class);
        AtomicReference<ResourceManager> seen = new AtomicReference<>();

        ApiProductPlanResourceScope.withProductResourceManager(ctx, providerFor(apiManager), productManager, scopedCtx -> {
            Function<HttpExecutionContext, Completable> action = actionCtx -> {
                seen.set(actionCtx.getComponent(ResourceManager.class));
                return Completable.complete();
            };
            if (kind == ActionKind.ENTRYPOINT_RESPONSE) {
                scopedCtx.addActionOnEntrypointResponse(policy, action);
            } else {
                scopedCtx.addActionOnTerminate(policy, action);
            }
            return Completable.complete();
        }).blockingAwait();

        stored.get().apply(ctx).blockingAwait();

        assertThat(seen.get()).isSameAs(productManager);
        assertThat(ctx.getComponent(ResourceManager.class)).isSameAs(apiManager);

        stored
            .get()
            .apply(mock(HttpExecutionContext.class))
            .test()
            .assertError(IllegalStateException.class)
            .assertError(error -> "Product resource scope cannot be restored for this execution context".equals(error.getMessage()));
    }

    @Test
    void null_product_manager_resolves_no_resource_and_not_the_api_resource() {
        MutableExecutionContext ctx = mock(MutableExecutionContext.class);
        ResourceLifecycleManager apiManager = mock(ResourceLifecycleManager.class);
        Object apiResource = new Object();
        when(apiManager.getResource("prod-cache")).thenReturn(apiResource);
        when(ctx.getComponent(eq(ResourceManager.class))).thenReturn(apiManager);
        AtomicReference<Object> stored = new AtomicReference<>();
        doAnswer(invocation -> {
            stored.set(invocation.getArgument(1));
            return null;
        })
            .when(ctx)
            .setInternalAttribute(eq(ATTR_INTERNAL_INVOKER), any());

        HttpInvoker installed = mock(HttpInvoker.class);
        AtomicReference<Object> resolved = new AtomicReference<>();
        when(installed.invoke(any())).thenAnswer(invocation -> {
            ResourceManager manager = invocation.getArgument(0, HttpExecutionContext.class).getComponent(ResourceManager.class);
            resolved.set(manager.getResource("prod-cache"));
            return Completable.complete();
        });

        ApiProductPlanResourceScope.withProductResourceManager(ctx, providerFor(apiManager), null, scopedCtx -> {
            assertThat(scopedCtx.getComponent(ResourceManager.class)).isSameAs(EmptyProductResourceLifecycleManager.INSTANCE);
            assertThat(scopedCtx.getComponent(ResourceManager.class).getResource("prod-cache")).isNull();
            scopedCtx.setInternalAttribute(ATTR_INTERNAL_INVOKER, installed);
            return Completable.complete();
        }).blockingAwait();

        ((HttpInvoker) stored.get()).invoke(ctx).blockingAwait();

        assertThat(resolved.get()).isNull();
        assertThat(ctx.getComponent(ResourceManager.class).getResource("prod-cache")).isSameAs(apiResource);

        MutableExecutionContext scoped = ApiProductPlanResourceScope.scope(ctx, null);
        assertThat(scoped.getComponent(ResourceManager.class)).isSameAs(EmptyProductResourceLifecycleManager.INSTANCE);
        assertThat(scoped.getComponent(ResourceManager.class).getResource("prod-cache")).isNull();
    }

    @Test
    void constructor_rejects_a_null_product_manager() {
        MutableExecutionContext ctx = mock(MutableExecutionContext.class);
        assertThatThrownBy(() ->
            new ProductPlanResourceScopedExecutionContext(ctx, providerFor(mock(ResourceLifecycleManager.class)), null)
        )
            .isInstanceOf(NullPointerException.class)
            .hasMessage("Product resource manager is not available");
    }

    @Test
    void delegate_captured_by_a_product_invoker_resolves_the_product_manager() {
        MutableExecutionContext ctx = mock(MutableExecutionContext.class);
        ResourceLifecycleManager apiManager = mock(ResourceLifecycleManager.class);
        ResourceLifecycleManager productManager = mock(ResourceLifecycleManager.class);
        when(ctx.getComponent(eq(ResourceManager.class))).thenReturn(apiManager);
        AtomicReference<Object> stored = new AtomicReference<>();
        doAnswer(invocation -> {
            stored.set(invocation.getArgument(1));
            return null;
        })
            .when(ctx)
            .setInternalAttribute(eq(ATTR_INTERNAL_INVOKER), any());

        HttpInvoker previous = mock(HttpInvoker.class);
        AtomicReference<ResourceManager> seenByPrevious = new AtomicReference<>();
        when(previous.invoke(any())).thenAnswer(invocation -> {
            seenByPrevious.set(invocation.getArgument(0, HttpExecutionContext.class).getComponent(ResourceManager.class));
            return Completable.complete();
        });
        HttpInvoker productInvoker = mock(HttpInvoker.class);
        when(productInvoker.invoke(any())).thenAnswer(invocation -> previous.invoke(invocation.getArgument(0)));

        ApiProductPlanResourceScope.withProductResourceManager(ctx, providerFor(apiManager), productManager, scopedCtx -> {
            scopedCtx.setInternalAttribute(ATTR_INTERNAL_INVOKER, productInvoker);
            return Completable.complete();
        }).blockingAwait();

        ((HttpInvoker) stored.get()).invoke(ctx).blockingAwait();

        assertThat(seenByPrevious.get()).isSameAs(productManager);
        assertThat(ctx.getComponent(ResourceManager.class)).isSameAs(apiManager);
    }

    @Test
    void internal_attribute_map_delegates_reads_and_scopes_set_value() {
        MutableExecutionContext ctx = mock(MutableExecutionContext.class);
        ResourceLifecycleManager apiManager = mock(ResourceLifecycleManager.class);
        ResourceLifecycleManager productManager = mock(ResourceLifecycleManager.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("other", "kept");
        doReturn(attributes).when(ctx).getInternalAttributes();

        HttpInvoker installed = mock(HttpInvoker.class);
        AtomicReference<ResourceManager> seen = new AtomicReference<>();
        when(installed.invoke(any())).thenAnswer(invocation -> {
            seen.set(invocation.getArgument(0, HttpExecutionContext.class).getComponent(ResourceManager.class));
            return Completable.complete();
        });

        ApiProductPlanResourceScope.withProductResourceManager(ctx, providerFor(apiManager), productManager, scopedCtx -> {
            Map<String, Object> map = scopedCtx.getInternalAttributes();
            assertThat(map.get("other")).isEqualTo("kept");
            assertThat(map.containsKey("other")).isTrue();
            assertThat(map.size()).isEqualTo(1);
            assertThat(map.remove("other")).isEqualTo("kept");
            assertThat(map.containsKey("other")).isFalse();
            assertThat(attributes).doesNotContainKey("other");

            attributes.put(ATTR_INTERNAL_INVOKER, "placeholder");
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (ATTR_INTERNAL_INVOKER.equals(entry.getKey())) {
                    entry.setValue(installed);
                }
            }
            return Completable.complete();
        }).blockingAwait();

        assertThat(attributes.get(ATTR_INTERNAL_INVOKER)).isNotSameAs(installed);
        ((HttpInvoker) attributes.get(ATTR_INTERNAL_INVOKER)).invoke(ctx).blockingAwait();
        assertThat(seen.get()).isSameAs(productManager);
    }

    private enum ActionKind {
        ENTRYPOINT_RESPONSE,
        TERMINATE,
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
        lenient()
            .doAnswer(invocation -> {
                activeProvider.set(invocation.getArgument(0));
                return null;
            })
            .when(ctx)
            .componentProvider(any(ComponentProvider.class));
        return activeProvider;
    }
}
