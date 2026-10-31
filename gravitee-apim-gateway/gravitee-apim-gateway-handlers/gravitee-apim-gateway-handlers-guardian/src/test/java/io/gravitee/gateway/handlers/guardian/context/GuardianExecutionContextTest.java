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
package io.gravitee.gateway.handlers.guardian.context;

import static io.gravitee.gateway.reactive.api.context.InternalContextAttributes.ATTR_INTERNAL_ADAPTED_CONTEXT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.gateway.core.component.CustomComponentProvider;
import io.gravitee.gateway.reactive.api.context.http.HttpExecutionContext;
import io.gravitee.gateway.reactive.api.policy.base.BasePolicy;
import io.gravitee.gateway.reactive.core.context.ComponentScope;
import io.gravitee.gateway.reactive.core.context.DefaultExecutionContext;
import io.gravitee.gateway.reactive.core.context.MutableRequest;
import io.gravitee.gateway.reactive.core.context.MutableResponse;
import io.gravitee.gateway.reactive.policy.adapter.context.ExecutionContextAdapter;
import io.gravitee.resource.api.ResourceManager;
import io.reactivex.rxjava3.core.Completable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GuardianExecutionContextTest {

    private final ResourceManager apiResourceManager = mock(ResourceManager.class);
    private final ResourceManager guardianResourceManager = mock(ResourceManager.class);
    private final BasePolicy guardianPolicy = aPolicy("guardian-policy");
    private final BasePolicy first = aPolicy("first");
    private final BasePolicy second = aPolicy("second");
    private final Object apiComponent = new Object();

    private DefaultExecutionContext apiCtx;
    private GuardianExecutionContext cut;

    @BeforeEach
    void setUp() {
        CustomComponentProvider componentProvider = new CustomComponentProvider();
        componentProvider.add(ResourceManager.class, apiResourceManager);
        componentProvider.add(Object.class, apiComponent);
        apiCtx = new DefaultExecutionContext(mock(MutableRequest.class), mock(MutableResponse.class));
        apiCtx.componentProvider(componentProvider);
        cut = new GuardianExecutionContext(apiCtx, guardianResourceManager, guardianPolicy);
    }

    @Nested
    class Components {

        @Test
        void should_resolve_the_guardian_resources() {
            assertThat(cut.getComponent(ResourceManager.class)).isSameAs(guardianResourceManager);
        }

        @Test
        void should_resolve_the_other_components_from_the_api_context() {
            assertThat(cut.getComponent(Object.class)).isSameAs(apiComponent);
        }
    }

    @Nested
    class Delegation {

        @Test
        void should_act_on_the_api_exchange() {
            assertThat(cut.request()).isSameAs(apiCtx.request());
            assertThat(cut.response()).isSameAs(apiCtx.response());
        }

        @Test
        void should_share_the_attributes_with_the_api_context() {
            cut.setAttribute("from-guardian", "value");
            apiCtx.setInternalAttribute("from-api", "internal");

            assertThat(apiCtx.<String>getAttribute("from-guardian")).isEqualTo("value");
            assertThat(cut.<String>getInternalAttribute("from-api")).isEqualTo("internal");
        }
    }

    @Nested
    class Actions {

        @Test
        void should_give_the_guardian_context_to_the_endpoint_request_actions() {
            List<HttpExecutionContext> received = new ArrayList<>();
            cut.addActionOnEndpointRequest(first, recording(received));

            runAll(apiCtx.drainOnEndpointRequestActions());

            assertThat(received).containsExactly(cut);
        }

        @Test
        void should_give_the_guardian_context_to_the_entrypoint_response_actions() {
            List<HttpExecutionContext> received = new ArrayList<>();
            cut.addActionOnEntrypointResponse(first, recording(received));

            runAll(apiCtx.drainOnEntrypointResponseActions());

            assertThat(received).containsExactly(cut);
        }

        @Test
        void should_give_the_guardian_context_to_the_terminate_actions() {
            List<HttpExecutionContext> received = new ArrayList<>();
            cut.addActionOnTerminate(first, recording(received));

            runAll(apiCtx.drainOnTerminateActions());

            assertThat(received).containsExactly(cut);
        }

        @Test
        void should_register_the_response_actions_under_the_policy_executing_the_guardian() {
            cut.addActionOnResponse(first, ctx -> Completable.complete());
            cut.addActionOnResponse(second, ctx -> Completable.complete());

            assertThat(apiCtx.getOnResponseActions()).containsOnlyKeys(guardianPolicy);
        }

        @Test
        void should_run_the_response_actions_in_reverse_order_with_the_guardian_context_in_the_scope_of_their_policy() {
            List<String> executed = new ArrayList<>();
            cut.addActionOnResponse(first, ctx -> recordScope(ctx, executed));
            cut.addActionOnResponse(second, ctx -> recordScope(ctx, executed));

            apiCtx.getOnResponseAction(guardianPolicy).apply(apiCtx).test().assertComplete();

            assertThat(executed).containsExactly("second", "first");
            assertThat(ComponentScope.peek(apiCtx)).isNull();
        }

        private Completable recordScope(HttpExecutionContext ctx, List<String> executed) {
            return Completable.fromRunnable(() -> {
                assertThat(ctx).isSameAs(cut);
                executed.add(ComponentScope.peek(ctx).name());
            });
        }

        private Function<HttpExecutionContext, Completable> recording(List<HttpExecutionContext> received) {
            return ctx -> Completable.fromRunnable(() -> received.add(ctx));
        }

        private void runAll(Map<BasePolicy, Function<HttpExecutionContext, Completable>> actions) {
            assertThat(actions).hasSize(1);
            actions.values().forEach(action -> action.apply(apiCtx).test().assertComplete());
        }
    }

    @Nested
    class LegacyAdapter {

        @Test
        void should_not_reuse_the_adapter_of_the_api_context() {
            ExecutionContextAdapter apiAdapter = ExecutionContextAdapter.create(apiCtx);

            ExecutionContextAdapter guardianAdapter = ExecutionContextAdapter.create(cut);

            assertThat(guardianAdapter).isNotSameAs(apiAdapter);
            assertThat(guardianAdapter.getComponent(ResourceManager.class)).isSameAs(guardianResourceManager);
        }

        @Test
        void should_not_leak_its_adapter_to_the_api_context() {
            ExecutionContextAdapter guardianAdapter = ExecutionContextAdapter.create(cut);

            assertThat(apiCtx.<Object>getInternalAttribute(ATTR_INTERNAL_ADAPTED_CONTEXT)).isNull();
            assertThat(ExecutionContextAdapter.create(cut)).isSameAs(guardianAdapter);
            assertThat(ExecutionContextAdapter.create(apiCtx).getComponent(ResourceManager.class)).isSameAs(apiResourceManager);
        }
    }

    private static BasePolicy aPolicy(String id) {
        BasePolicy policy = mock(BasePolicy.class);
        when(policy.id()).thenReturn(id);
        return policy;
    }
}
