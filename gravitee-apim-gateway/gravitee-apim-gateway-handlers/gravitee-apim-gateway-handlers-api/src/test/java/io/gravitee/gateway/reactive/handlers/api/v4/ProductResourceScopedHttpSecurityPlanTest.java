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

import static io.gravitee.gateway.reactive.api.context.InternalContextAttributes.ATTR_INTERNAL_SECURITY_DIAGNOSTIC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.core.component.CustomComponentProvider;
import io.gravitee.gateway.reactive.api.ExecutionPhase;
import io.gravitee.gateway.reactive.api.context.http.HttpPlainExecutionContext;
import io.gravitee.gateway.reactive.api.policy.SecurityToken;
import io.gravitee.gateway.reactive.api.policy.http.HttpSecurityPolicy;
import io.gravitee.gateway.reactive.core.context.MutableExecutionContext;
import io.gravitee.gateway.reactive.handlers.api.security.SecurityChainDiagnostic;
import io.gravitee.gateway.reactive.handlers.api.security.plan.HttpSecurityPlan;
import io.gravitee.gateway.reactive.handlers.api.security.plan.SecurityPlanContext;
import io.gravitee.gateway.reactive.handlers.api.v4.product.EmptyProductResourceLifecycleManager;
import io.gravitee.gateway.resource.ResourceLifecycleManager;
import io.gravitee.resource.api.ResourceManager;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.observers.TestObserver;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.helpers.NOPLogger;

class ProductResourceScopedHttpSecurityPlanTest {

    private final MutableExecutionContext ctx = mock(MutableExecutionContext.class);
    private final HttpSecurityPolicy policy = mock(HttpSecurityPolicy.class);
    private final ResourceLifecycleManager productManager = mock(ResourceLifecycleManager.class);
    private final ResourceLifecycleManager apiManager = mock(ResourceLifecycleManager.class);
    private final AtomicReference<ResourceManager> seen = new AtomicReference<>();
    private final Map<String, Object> attributes = new HashMap<>();
    private AtomicReference<ComponentProvider> activeProvider;
    private ProductResourceScopedHttpSecurityPlan cut;

    @BeforeEach
    void setUp() {
        CustomComponentProvider apiComponents = new CustomComponentProvider();
        apiComponents.add(ResourceManager.class, apiManager);
        ComponentProvider apiProvider = apiComponents::getComponent;
        activeProvider = new AtomicReference<>(apiProvider);
        attributes.put(ATTR_INTERNAL_SECURITY_DIAGNOSTIC, new SecurityChainDiagnostic(false));

        lenient().when(policy.id()).thenReturn("oauth2-plan");
        lenient().when(policy.order()).thenReturn(1);
        lenient().when(policy.requireSubscription(any())).thenReturn(false);
        lenient().when(policy.onRequest(any())).thenReturn(Completable.complete());
        lenient().when(ctx.withLogger(any())).thenReturn(NOPLogger.NOP_LOGGER);
        lenient()
            .when(ctx.getInternalAttribute(anyString()))
            .thenAnswer(invocation -> attributes.get(invocation.<String>getArgument(0)));
        lenient()
            .doAnswer(invocation -> {
                attributes.put(invocation.getArgument(0), invocation.getArgument(1));
                return null;
            })
            .when(ctx)
            .setInternalAttribute(anyString(), any());
        lenient()
            .when(ctx.getComponent(ResourceManager.class))
            .thenAnswer(invocation -> activeProvider.get().getComponent(ResourceManager.class));
        lenient()
            .doAnswer(invocation -> {
                activeProvider.set(invocation.getArgument(0));
                return null;
            })
            .when(ctx)
            .componentProvider(any());
        cut = new ProductResourceScopedHttpSecurityPlan(
            new HttpSecurityPlan(new SecurityPlanContext("plan-1", "OAuth2", null, "product-1"), policy),
            productManager
        );
    }

    @Test
    void should_resolve_the_product_resource_manager_and_keep_the_api_provider() {
        when(policy.extractSecurityToken(any())).thenAnswer(invocation -> {
            seen.set(manager(invocation.getArgument(0)));
            return Maybe.just(SecurityToken.forClientId("client-1"));
        });

        cut.canExecute(ctx).test().assertValue(true);

        assertThat(seen.get()).isSameAs(productManager);
        assertThat(activeProvider.get().getComponent(ResourceManager.class)).isSameAs(apiManager);
    }

    @Test
    void should_keep_the_api_provider_when_token_extraction_fails() {
        when(policy.extractSecurityToken(any())).thenAnswer(invocation -> {
            seen.set(manager(invocation.getArgument(0)));
            return Maybe.error(new IllegalStateException("introspection failed"));
        });

        cut.canExecute(ctx).test().assertError(IllegalStateException.class);

        assertThat(seen.get()).isSameAs(productManager);
        assertThat(activeProvider.get().getComponent(ResourceManager.class)).isSameAs(apiManager);
    }

    @Test
    void should_keep_the_api_provider_when_the_plan_is_cancelled() {
        when(policy.extractSecurityToken(any())).thenAnswer(invocation -> {
            seen.set(manager(invocation.getArgument(0)));
            return Maybe.never();
        });

        TestObserver<Boolean> observer = cut.canExecute(ctx).test();
        observer.assertNotComplete();
        observer.dispose();

        assertThat(seen.get()).isSameAs(productManager);
        assertThat(activeProvider.get().getComponent(ResourceManager.class)).isSameAs(apiManager);
    }

    @Test
    void should_resolve_the_product_resource_manager_while_the_plan_executes() {
        when(policy.extractSecurityToken(any())).thenReturn(Maybe.just(SecurityToken.forClientId("client-1")));
        when(policy.onRequest(any())).thenAnswer(invocation -> {
            seen.set(manager(invocation.getArgument(0)));
            return Completable.complete();
        });

        cut.execute(ctx, ExecutionPhase.REQUEST).test().assertComplete();

        assertThat(seen.get()).isSameAs(productManager);
        assertThat(activeProvider.get().getComponent(ResourceManager.class)).isSameAs(apiManager);
    }

    @Test
    void should_use_an_empty_manager_when_the_product_has_none() {
        cut = new ProductResourceScopedHttpSecurityPlan(
            new HttpSecurityPlan(new SecurityPlanContext("plan-1", "OAuth2", null, "product-1"), policy),
            null
        );
        when(policy.extractSecurityToken(any())).thenAnswer(invocation -> {
            seen.set(manager(invocation.getArgument(0)));
            return Maybe.empty();
        });

        cut.canExecute(ctx).test().assertValue(false);

        assertThat(seen.get()).isSameAs(EmptyProductResourceLifecycleManager.INSTANCE);
        assertThat(activeProvider.get().getComponent(ResourceManager.class)).isSameAs(apiManager);
    }

    private static ResourceManager manager(HttpPlainExecutionContext planCtx) {
        return planCtx.getComponent(ResourceManager.class);
    }
}
