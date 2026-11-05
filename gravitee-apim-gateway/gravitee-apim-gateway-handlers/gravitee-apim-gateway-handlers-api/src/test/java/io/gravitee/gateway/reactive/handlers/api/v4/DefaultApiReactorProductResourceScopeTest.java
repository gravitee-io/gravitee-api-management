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
import static io.gravitee.gateway.reactive.api.context.InternalContextAttributes.ATTR_INTERNAL_INVOKER_SKIP;
import static io.gravitee.gateway.reactive.api.context.InternalContextAttributes.ATTR_INTERNAL_VALIDATE_SUBSCRIPTION;
import static io.gravitee.gateway.reactive.handlers.api.v4.DefaultApiReactor.PENDING_REQUESTS_TIMEOUT_PROPERTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.common.event.EventManager;
import io.gravitee.definition.model.v4.analytics.Analytics;
import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.core.component.CompositeComponentProvider;
import io.gravitee.gateway.core.component.CustomComponentProvider;
import io.gravitee.gateway.env.RequestTimeoutConfiguration;
import io.gravitee.gateway.handlers.accesspoint.manager.AccessPointManager;
import io.gravitee.gateway.handlers.api.manager.ApiProductManager;
import io.gravitee.gateway.handlers.api.registry.ApiProductRegistry;
import io.gravitee.gateway.opentelemetry.TracingContext;
import io.gravitee.gateway.reactive.api.ApiType;
import io.gravitee.gateway.reactive.api.ExecutionPhase;
import io.gravitee.gateway.reactive.api.apiservice.ApiService;
import io.gravitee.gateway.reactive.api.apiservice.ApiServiceFactory;
import io.gravitee.gateway.reactive.api.connector.entrypoint.BaseEntrypointConnector;
import io.gravitee.gateway.reactive.api.context.InternalContextAttributes;
import io.gravitee.gateway.reactive.api.context.http.HttpExecutionContext;
import io.gravitee.gateway.reactive.core.context.DefaultDeploymentContext;
import io.gravitee.gateway.reactive.core.context.MutableExecutionContext;
import io.gravitee.gateway.reactive.core.context.MutableRequest;
import io.gravitee.gateway.reactive.core.context.MutableResponse;
import io.gravitee.gateway.reactive.core.processor.ProcessorChain;
import io.gravitee.gateway.reactive.core.v4.endpoint.EndpointManager;
import io.gravitee.gateway.reactive.core.v4.entrypoint.DefaultEntrypointConnectorResolver;
import io.gravitee.gateway.reactive.core.v4.invoker.HttpEndpointInvoker;
import io.gravitee.gateway.reactive.handlers.api.processor.subscription.SubscriptionProcessor;
import io.gravitee.gateway.reactive.handlers.api.v4.flow.FlowChain;
import io.gravitee.gateway.reactive.handlers.api.v4.flow.FlowChainFactory;
import io.gravitee.gateway.reactive.handlers.api.v4.processor.ApiProcessorChainFactory;
import io.gravitee.gateway.reactive.handlers.api.v4.security.HttpSecurityChain;
import io.gravitee.gateway.reactive.policy.PolicyManager;
import io.gravitee.gateway.reactor.handler.HttpAcceptorFactory;
import io.gravitee.gateway.report.ReporterService;
import io.gravitee.gateway.report.guard.LogGuardService;
import io.gravitee.gateway.resource.ResourceLifecycleManager;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.plugin.apiservice.ApiServicePluginManager;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPluginManager;
import io.gravitee.reporter.api.v4.metric.Metrics;
import io.gravitee.resource.api.ResourceManager;
import io.reactivex.rxjava3.core.Completable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.helpers.NOPLogger;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DefaultApiReactorProductResourceScopeTest {

    private static final String API_ID = "api-id";
    private static final String PRODUCT_ID = "product-id";
    private static final String OTHER_PRODUCT_ID = "other-product-id";
    private static final String RESOURCE_NAME = "shared-cache";
    private static final String CONTEXT_PATH = "/contextPath";

    @Mock
    private ResourceLifecycleManager apiResourceManager;

    @Mock
    private ResourceLifecycleManager productResourceManager;

    @Mock
    private ResourceLifecycleManager emptyProductResourceManager;

    @Mock
    private ResourceLifecycleManager otherProductResourceManager;

    @Mock
    private ApiProductManager apiProductManager;

    @Mock
    private ApiProductRegistry apiProductRegistry;

    @Mock
    private ApiProductPlanPolicyManagerFactory apiProductPlanPolicyManagerFactory;

    @Mock
    private PolicyManager apiProductPlanPolicyManager;

    @Mock
    private FlowChain apiProductPlanFlowChain;

    @Mock
    private FlowChain apiPlanFlowChain;

    @Mock
    private FlowChain apiFlowChain;

    @Mock
    private io.gravitee.gateway.reactive.handlers.api.flow.FlowChain organizationFlowChain;

    @Mock
    private MutableExecutionContext ctx;

    @Mock
    private MutableRequest request;

    @Mock
    private MutableResponse response;

    @Mock
    private BaseEntrypointConnector entrypointConnector;

    @Mock
    private DefaultEntrypointConnectorResolver entrypointConnectorResolver;

    @Mock
    private HttpEndpointInvoker defaultInvoker;

    @Mock
    private HttpSecurityChain securityChain;

    @Mock
    private Api api;

    @Mock
    private io.gravitee.definition.model.v4.Api apiDefinition;

    @Mock
    private Configuration configuration;

    @Mock
    private Node node;

    @Mock
    private PolicyManager policyManager;

    @Mock
    private EntrypointConnectorPluginManager entrypointConnectorPluginManager;

    @Mock
    private ApiServicePluginManager apiServicePluginManager;

    @Mock
    private ApiServiceFactory apiServiceFactory;

    @Mock
    private ApiService apiService;

    @Mock
    private EndpointManager endpointManager;

    @Mock
    private io.gravitee.gateway.reactive.handlers.api.flow.FlowChainFactory flowChainFactory;

    @Mock
    private FlowChainFactory v4FlowChainFactory;

    @Mock
    private ApiProcessorChainFactory apiProcessorChainFactory;

    @Mock
    private ProcessorChain beforeHandleProcessors;

    @Mock
    private ProcessorChain afterHandleProcessors;

    @Mock
    private ProcessorChain beforeSecurityChainProcessors;

    @Mock
    private ProcessorChain beforeApiExecutionProcessors;

    @Mock
    private ProcessorChain afterApiExecutionProcessors;

    @Mock
    private ProcessorChain onErrorProcessors;

    @Mock
    private RequestTimeoutConfiguration requestTimeoutConfiguration;

    @Mock
    private ReporterService reporterService;

    @Mock
    private AccessPointManager accessPointManager;

    @Mock
    private EventManager eventManager;

    @Mock
    private LogGuardService logGuardService;

    private final Object apiResource = new Object();
    private final Object productResource = new Object();
    private CompositeComponentProvider apiComponentProvider;
    private AtomicReference<ResourceManager> resourceManagerSeenByContext;
    private DefaultApiReactor cut;

    @BeforeEach
    void setUp() throws Exception {
        CustomComponentProvider apiComponents = new CustomComponentProvider();
        apiComponents.add(ResourceManager.class, apiResourceManager);
        apiComponentProvider = new CompositeComponentProvider(apiComponents);

        resourceManagerSeenByContext = new AtomicReference<>(apiResourceManager);
        when(apiResourceManager.getResource(RESOURCE_NAME)).thenReturn(apiResource);
        when(productResourceManager.getResource(RESOURCE_NAME)).thenReturn(productResource);
        when(emptyProductResourceManager.getResource(RESOURCE_NAME)).thenReturn(null);
        when(apiProductManager.getResourceManager(PRODUCT_ID)).thenReturn(productResourceManager);
        when(apiProductManager.getResourceManager(OTHER_PRODUCT_ID)).thenReturn(otherProductResourceManager);

        when(ctx.withLogger(any())).thenReturn(NOPLogger.NOP_LOGGER);
        when(ctx.request()).thenReturn(request);
        when(ctx.response()).thenReturn(response);
        when(ctx.metrics()).thenReturn(mock(Metrics.class));
        when(request.contextPath()).thenReturn(CONTEXT_PATH);
        when(request.timestamp()).thenReturn(System.currentTimeMillis());
        when(response.end(any())).thenReturn(Completable.complete());
        when(ctx.componentProvider(any())).thenAnswer(invocation -> {
            ComponentProvider provider = invocation.getArgument(0);
            resourceManagerSeenByContext.set(provider.getComponent(ResourceManager.class));
            return ctx;
        });
        when(ctx.templateVariableProviders(any())).thenReturn(ctx);
        when(ctx.getComponent(ResourceManager.class)).thenAnswer(invocation -> resourceManagerSeenByContext.get());
        when(ctx.getAttribute(SubscriptionProcessor.ATTR_API_PRODUCT)).thenReturn(PRODUCT_ID);

        when(api.getDefinition()).thenReturn(apiDefinition);
        when(api.getId()).thenReturn(API_ID);
        when(api.getDeployedAt()).thenReturn(new Date());
        when(api.getOrganizationId()).thenReturn("org");
        when(api.getEnvironmentId()).thenReturn("env");
        when(apiDefinition.getType()).thenReturn(io.gravitee.definition.model.v4.ApiType.PROXY);
        when(apiDefinition.getAnalytics()).thenReturn(new Analytics());

        stubProcessorChains();
        stubFlowChains();

        when(configuration.getProperty(PENDING_REQUESTS_TIMEOUT_PROPERTY, Long.class, 10_000L)).thenReturn(10_000L);
        when(configuration.getProperty(ATTR_INTERNAL_VALIDATE_SUBSCRIPTION, Boolean.class, true)).thenReturn(true);
        when(requestTimeoutConfiguration.getRequestTimeout()).thenReturn(0L);
        when(requestTimeoutConfiguration.getRequestTimeoutGraceDelay()).thenReturn(10000L);

        when(apiServicePluginManager.getAllFactories()).thenReturn(List.of(apiServiceFactory));
        when(apiServiceFactory.createService(any())).thenReturn(apiService);
        when(apiService.start()).thenReturn(Completable.complete());
        when(apiService.stop()).thenReturn(Completable.complete());

        when(apiProductPlanPolicyManagerFactory.create(api)).thenReturn(apiProductPlanPolicyManager);
        when(v4FlowChainFactory.createProductPlanFlow(any(), any(), any(), any(), any())).thenReturn(apiProductPlanFlowChain);

        cut = buildReactor();
        ReflectionTestUtils.setField(cut, "entrypointConnectorResolver", entrypointConnectorResolver);
        ReflectionTestUtils.setField(cut, "defaultInvoker", defaultInvoker);
        ReflectionTestUtils.setField(cut, "httpSecurityChain", securityChain);
        ReflectionTestUtils.setField(cut, "apiProductPlanFlowChain", apiProductPlanFlowChain);

        when(defaultInvoker.invoke(any(HttpExecutionContext.class))).thenReturn(Completable.complete());
        when(entrypointConnector.handleRequest(any())).thenReturn(Completable.complete());
        when(entrypointConnector.handleResponse(any())).thenReturn(Completable.complete());
        when(entrypointConnectorResolver.resolve(any())).thenReturn(entrypointConnector);
        when(ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_ENTRYPOINT_CONNECTOR)).thenReturn(entrypointConnector);
        when(entrypointConnector.supportedApi()).thenReturn(ApiType.PROXY);
        when(ctx.getInternalAttribute(ATTR_INTERNAL_INVOKER)).thenReturn(defaultInvoker);
        when(ctx.getInternalAttribute(ATTR_INTERNAL_INVOKER_SKIP)).thenReturn(false);
        when(securityChain.execute(any())).thenReturn(Completable.complete());
    }

    @Nested
    class ProductFlowUsesProductResourceManager {

        @Test
        void should_expose_product_resource_manager_during_product_plan_flow() {
            AtomicReference<ResourceManager> managerDuringProductFlow = new AtomicReference<>();
            AtomicReference<Object> resourceDuringProductFlow = new AtomicReference<>();
            when(apiProductPlanFlowChain.execute(ctx, ExecutionPhase.REQUEST)).thenReturn(
                Completable.fromAction(() -> {
                    managerDuringProductFlow.set(ctx.getComponent(ResourceManager.class));
                    resourceDuringProductFlow.set(ctx.getComponent(ResourceManager.class).getResource(RESOURCE_NAME));
                })
            );

            cut.handle(ctx).test().assertComplete();

            assertThat(managerDuringProductFlow.get()).isSameAs(productResourceManager);
            assertThat(resourceDuringProductFlow.get()).isSameAs(productResource);
        }
    }

    @Nested
    class ApiFlowsUseApiResourceManager {

        @Test
        void should_restore_api_resource_manager_for_api_plan_and_api_flows() {
            AtomicReference<ResourceManager> managerDuringApiPlanFlow = new AtomicReference<>();
            AtomicReference<ResourceManager> managerDuringApiFlow = new AtomicReference<>();

            when(apiPlanFlowChain.execute(ctx, ExecutionPhase.REQUEST)).thenReturn(
                Completable.fromAction(() -> managerDuringApiPlanFlow.set(ctx.getComponent(ResourceManager.class)))
            );
            when(apiFlowChain.execute(ctx, ExecutionPhase.REQUEST)).thenReturn(
                Completable.fromAction(() -> managerDuringApiFlow.set(ctx.getComponent(ResourceManager.class)))
            );

            cut.handle(ctx).test().assertComplete();

            assertThat(managerDuringApiPlanFlow.get()).isSameAs(apiResourceManager);
            assertThat(managerDuringApiFlow.get()).isSameAs(apiResourceManager);
            assertThat(resourceManagerSeenByContext.get()).isSameAs(apiResourceManager);
        }
    }

    @Nested
    class RestoreOnFailure {

        @Test
        void should_restore_api_resource_manager_when_product_flow_fails() {
            when(apiProductPlanFlowChain.execute(ctx, ExecutionPhase.REQUEST)).thenReturn(
                Completable.error(new IllegalStateException("product flow failed"))
            );

            cut.handle(ctx).test().assertComplete();

            assertThat(resourceManagerSeenByContext.get()).isSameAs(apiResourceManager);
            assertThat(ctx.getComponent(ResourceManager.class)).isSameAs(apiResourceManager);
        }
    }

    @Nested
    class SameNameIsolation {

        @Test
        void should_keep_same_named_product_and_api_resources_separate() {
            AtomicReference<Object> duringProduct = new AtomicReference<>();
            AtomicReference<Object> duringApi = new AtomicReference<>();

            when(apiProductPlanFlowChain.execute(ctx, ExecutionPhase.REQUEST)).thenReturn(
                Completable.fromAction(() -> duringProduct.set(ctx.getComponent(ResourceManager.class).getResource(RESOURCE_NAME)))
            );
            when(apiPlanFlowChain.execute(ctx, ExecutionPhase.REQUEST)).thenReturn(
                Completable.fromAction(() -> duringApi.set(ctx.getComponent(ResourceManager.class).getResource(RESOURCE_NAME)))
            );

            cut.handle(ctx).test().assertComplete();

            assertThat(duringProduct.get()).isSameAs(productResource);
            assertThat(duringApi.get()).isSameAs(apiResource);
            assertThat(duringProduct.get()).isNotSameAs(duringApi.get());
        }
    }

    @Nested
    class EmptyProductResources {

        @Test
        void should_not_fall_back_to_api_resource_when_product_resource_list_is_empty() {
            when(apiProductManager.getResourceManager(PRODUCT_ID)).thenReturn(emptyProductResourceManager);

            AtomicReference<Object> duringProduct = new AtomicReference<>();
            when(apiProductPlanFlowChain.execute(ctx, ExecutionPhase.REQUEST)).thenReturn(
                Completable.fromAction(() -> duringProduct.set(ctx.getComponent(ResourceManager.class).getResource(RESOURCE_NAME)))
            );

            cut.handle(ctx).test().assertComplete();

            assertThat(duringProduct.get()).isNull();
            assertThat(ctx.getComponent(ResourceManager.class).getResource(RESOURCE_NAME)).isSameAs(apiResource);
        }
    }

    @Nested
    class ConcurrentProducts {

        @Test
        void should_not_leak_resource_managers_across_sequential_product_requests() {
            AtomicReference<ResourceManager> firstProductManager = new AtomicReference<>();
            AtomicReference<ResourceManager> secondProductManager = new AtomicReference<>();

            MutableExecutionContext otherCtx = mock(MutableExecutionContext.class);
            AtomicReference<ResourceManager> otherSeen = new AtomicReference<>(apiResourceManager);
            stubSecondContext(otherCtx, otherSeen);

            when(apiProductPlanFlowChain.execute(ctx, ExecutionPhase.REQUEST)).thenReturn(
                Completable.fromAction(() -> firstProductManager.set(ctx.getComponent(ResourceManager.class)))
            );
            when(apiProductPlanFlowChain.execute(otherCtx, ExecutionPhase.REQUEST)).thenReturn(
                Completable.fromAction(() -> secondProductManager.set(otherCtx.getComponent(ResourceManager.class)))
            );

            cut.handle(ctx).test().assertComplete();
            assertThat(firstProductManager.get()).isSameAs(productResourceManager);
            assertThat(resourceManagerSeenByContext.get()).isSameAs(apiResourceManager);

            cut.handle(otherCtx).test().assertComplete();
            assertThat(secondProductManager.get()).isSameAs(otherProductResourceManager);
            assertThat(otherSeen.get()).isSameAs(apiResourceManager);
            assertThat(firstProductManager.get()).isNotSameAs(secondProductManager.get());
        }
    }

    private void stubSecondContext(MutableExecutionContext otherCtx, AtomicReference<ResourceManager> otherSeen) {
        when(otherCtx.withLogger(any())).thenReturn(NOPLogger.NOP_LOGGER);
        when(otherCtx.request()).thenReturn(request);
        when(otherCtx.response()).thenReturn(response);
        when(otherCtx.metrics()).thenReturn(mock(Metrics.class));
        when(otherCtx.componentProvider(any())).thenAnswer(invocation -> {
            ComponentProvider provider = invocation.getArgument(0);
            otherSeen.set(provider.getComponent(ResourceManager.class));
            return otherCtx;
        });
        when(otherCtx.templateVariableProviders(any())).thenReturn(otherCtx);
        when(otherCtx.getComponent(ResourceManager.class)).thenAnswer(invocation -> otherSeen.get());
        when(otherCtx.getAttribute(SubscriptionProcessor.ATTR_API_PRODUCT)).thenReturn(OTHER_PRODUCT_ID);
        when(otherCtx.getInternalAttribute(ATTR_INTERNAL_INVOKER)).thenReturn(defaultInvoker);
        when(otherCtx.getInternalAttribute(ATTR_INTERNAL_INVOKER_SKIP)).thenReturn(false);
        when(otherCtx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_ENTRYPOINT_CONNECTOR)).thenReturn(entrypointConnector);
        when(organizationFlowChain.execute(otherCtx, ExecutionPhase.REQUEST)).thenReturn(Completable.complete());
        when(organizationFlowChain.execute(otherCtx, ExecutionPhase.RESPONSE)).thenReturn(Completable.complete());
        when(beforeHandleProcessors.execute(otherCtx, ExecutionPhase.REQUEST)).thenReturn(Completable.complete());
        when(afterHandleProcessors.execute(otherCtx, ExecutionPhase.RESPONSE)).thenReturn(Completable.complete());
        when(beforeSecurityChainProcessors.execute(otherCtx, ExecutionPhase.REQUEST)).thenReturn(Completable.complete());
        when(beforeApiExecutionProcessors.execute(otherCtx, ExecutionPhase.REQUEST)).thenReturn(Completable.complete());
        when(afterApiExecutionProcessors.execute(otherCtx, ExecutionPhase.RESPONSE)).thenReturn(Completable.complete());
        when(apiPlanFlowChain.execute(otherCtx, ExecutionPhase.REQUEST)).thenReturn(Completable.complete());
        when(apiPlanFlowChain.execute(otherCtx, ExecutionPhase.RESPONSE)).thenReturn(Completable.complete());
        when(apiFlowChain.execute(otherCtx, ExecutionPhase.REQUEST)).thenReturn(Completable.complete());
        when(apiFlowChain.execute(otherCtx, ExecutionPhase.RESPONSE)).thenReturn(Completable.complete());
        when(apiProductPlanFlowChain.execute(otherCtx, ExecutionPhase.RESPONSE)).thenReturn(Completable.complete());
        when(securityChain.execute(otherCtx)).thenReturn(Completable.complete());
        when(defaultInvoker.invoke(otherCtx)).thenReturn(Completable.complete());
        when(response.end(otherCtx)).thenReturn(Completable.complete());
    }

    private void stubProcessorChains() {
        when(apiProcessorChainFactory.beforeHandle(api)).thenReturn(beforeHandleProcessors);
        when(apiProcessorChainFactory.afterHandle(api)).thenReturn(afterHandleProcessors);
        when(apiProcessorChainFactory.beforeSecurityChain(api)).thenReturn(beforeSecurityChainProcessors);
        when(apiProcessorChainFactory.beforeApiExecution(api)).thenReturn(beforeApiExecutionProcessors);
        when(apiProcessorChainFactory.afterApiExecution(api)).thenReturn(afterApiExecutionProcessors);
        when(apiProcessorChainFactory.onError(api)).thenReturn(onErrorProcessors);
        when(beforeHandleProcessors.execute(any(), any())).thenReturn(Completable.complete());
        when(afterHandleProcessors.execute(any(), any())).thenReturn(Completable.complete());
        when(beforeSecurityChainProcessors.execute(any(), any())).thenReturn(Completable.complete());
        when(beforeApiExecutionProcessors.execute(any(), any())).thenReturn(Completable.complete());
        when(afterApiExecutionProcessors.execute(any(), any())).thenReturn(Completable.complete());
        when(onErrorProcessors.execute(any(), any())).thenReturn(Completable.complete());
        when(beforeHandleProcessors.getId()).thenReturn("beforeHandle");
        when(afterHandleProcessors.getId()).thenReturn("afterHandle");
        when(beforeApiExecutionProcessors.getId()).thenReturn("beforeApi");
        when(afterApiExecutionProcessors.getId()).thenReturn("afterApi");
        when(onErrorProcessors.getId()).thenReturn("onError");
    }

    private void stubFlowChains() {
        when(flowChainFactory.createOrganizationFlow(any(), any())).thenReturn(organizationFlowChain);
        when(organizationFlowChain.execute(any(), any())).thenReturn(Completable.complete());
        when(v4FlowChainFactory.createPlanFlow(any(), any())).thenReturn(apiPlanFlowChain);
        when(v4FlowChainFactory.createApiFlow(any(), any())).thenReturn(apiFlowChain);
        when(apiPlanFlowChain.execute(any(), any())).thenReturn(Completable.complete());
        when(apiFlowChain.execute(any(), any())).thenReturn(Completable.complete());
        when(apiProductPlanFlowChain.execute(any(), any())).thenReturn(Completable.complete());
    }

    private DefaultApiReactor buildReactor() {
        try {
            DefaultApiReactor reactor = new DefaultApiReactor(
                api,
                new DefaultDeploymentContext(),
                apiComponentProvider,
                new ArrayList<>(),
                policyManager,
                entrypointConnectorPluginManager,
                apiServicePluginManager,
                endpointManager,
                apiResourceManager,
                apiProcessorChainFactory,
                flowChainFactory,
                v4FlowChainFactory,
                configuration,
                node,
                requestTimeoutConfiguration,
                reporterService,
                accessPointManager,
                eventManager,
                new HttpAcceptorFactory(false),
                TracingContext.noop(),
                logGuardService,
                apiProductRegistry,
                apiProductPlanPolicyManagerFactory,
                apiProductManager
            );
            reactor.doStart();
            return reactor;
        } catch (Exception e) {
            fail(e);
            return null;
        }
    }
}
