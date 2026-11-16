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
package io.gravitee.gateway.reactive.debug.handlers.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.common.event.EventManager;
import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.env.RequestTimeoutConfiguration;
import io.gravitee.gateway.handlers.accesspoint.manager.AccessPointManager;
import io.gravitee.gateway.opentelemetry.TracingContext;
import io.gravitee.gateway.reactive.api.context.DeploymentContext;
import io.gravitee.gateway.reactive.core.v4.endpoint.EndpointManager;
import io.gravitee.gateway.reactive.debug.policy.DebugV4PolicyChainFactory;
import io.gravitee.gateway.reactive.handlers.api.flow.FlowChainFactory;
import io.gravitee.gateway.reactive.handlers.api.v4.Api;
import io.gravitee.gateway.reactive.handlers.api.v4.processor.ApiProcessorChainFactory;
import io.gravitee.gateway.reactive.handlers.api.v4.product.ApiProductFlowChains;
import io.gravitee.gateway.reactive.handlers.api.v4.product.ApiProductRuntime;
import io.gravitee.gateway.reactive.handlers.api.v4.product.ApiProductRuntimeFactory.PolicyChainFactoryCreator;
import io.gravitee.gateway.reactive.handlers.api.v4.product.ApiProductRuntimeRegistry;
import io.gravitee.gateway.reactive.policy.PolicyManager;
import io.gravitee.gateway.reactor.handler.HttpAcceptorFactory;
import io.gravitee.gateway.report.ReporterService;
import io.gravitee.gateway.report.guard.LogGuardService;
import io.gravitee.gateway.resource.ResourceLifecycleManager;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.plugin.apiservice.ApiServicePluginManager;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPluginManager;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class DebugV4ApiReactorTest {

    private final ApiProductRuntimeRegistry apiProductRuntimeRegistry = mock(ApiProductRuntimeRegistry.class);
    private final ApiProductRuntime runtime = mock(ApiProductRuntime.class);
    private final ApiProductFlowChains debugCopy = mock(ApiProductFlowChains.class);
    private DebugV4ApiReactor cut;

    @BeforeEach
    void buildReactor() {
        io.gravitee.definition.model.v4.Api definition = new io.gravitee.definition.model.v4.Api();
        definition.setId("api-id");
        definition.setListeners(List.of());
        Configuration configuration = mock(Configuration.class);
        when(configuration.getProperty(anyString(), any(), any())).thenAnswer(invocation -> invocation.getArgument(2));
        when(apiProductRuntimeRegistry.copyFlowChains(same(runtime), any())).thenReturn(debugCopy);

        cut = new DebugV4ApiReactor(
            new Api(definition),
            mock(DeploymentContext.class),
            mock(ComponentProvider.class),
            new ArrayList<>(),
            mock(PolicyManager.class),
            mock(EntrypointConnectorPluginManager.class),
            mock(ApiServicePluginManager.class),
            mock(EndpointManager.class),
            mock(ResourceLifecycleManager.class),
            mock(ApiProcessorChainFactory.class),
            mock(FlowChainFactory.class),
            mock(io.gravitee.gateway.reactive.handlers.api.v4.flow.FlowChainFactory.class),
            configuration,
            mock(Node.class),
            mock(RequestTimeoutConfiguration.class),
            mock(ReporterService.class),
            mock(AccessPointManager.class),
            mock(EventManager.class),
            new HttpAcceptorFactory(false),
            TracingContext.noop(),
            mock(LogGuardService.class),
            apiProductRuntimeRegistry
        );
    }

    @Test
    void should_run_a_copy_of_the_product_chains_built_with_the_debug_policy_chain_factory() {
        assertThat(cut.apiProductFlowChains(runtime)).isSameAs(debugCopy);

        ArgumentCaptor<PolicyChainFactoryCreator> creator = ArgumentCaptor.forClass(PolicyChainFactoryCreator.class);
        verify(apiProductRuntimeRegistry).copyFlowChains(same(runtime), creator.capture());
        assertThat(creator.getValue().create("api-product-p1", mock(PolicyManager.class), false)).isInstanceOf(
            DebugV4PolicyChainFactory.class
        );
        verify(runtime, never()).flowChains(anyBoolean());
    }

    @Test
    void should_replay_the_same_copy_in_the_response_phase() {
        cut.apiProductFlowChains(runtime);
        cut.apiProductFlowChains(runtime);

        verify(apiProductRuntimeRegistry, times(1)).copyFlowChains(same(runtime), any());
    }
}
