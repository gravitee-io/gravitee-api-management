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
package io.gravitee.gateway.handlers.api.manager.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.common.event.EventListener;
import io.gravitee.common.event.EventManager;
import io.gravitee.common.event.impl.SimpleEvent;
import io.gravitee.common.util.DataEncryptor;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.listener.http.Path;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.handlers.api.event.ApiProductChangedEvent;
import io.gravitee.gateway.handlers.api.event.ApiProductEventType;
import io.gravitee.gateway.handlers.api.registry.ApiProductRegistry;
import io.gravitee.gateway.reactor.ReactorEvent;
import io.gravitee.node.api.license.LicenseManager;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiManagerImplApiProductListenerTest {

    @Mock
    private EventManager eventManager;

    @Mock
    private GatewayConfiguration gatewayConfiguration;

    @Mock
    private LicenseManager licenseManager;

    @Mock
    private DataEncryptor dataEncryptor;

    @Mock
    private ApiProductRegistry apiProductRegistry;

    private ApiManagerImpl apiManager;
    private ArgumentCaptor<EventListener<ApiProductEventType, ApiProductChangedEvent>> listenerCaptor;

    @BeforeEach
    void setUp() {
        lenient().when(gatewayConfiguration.hasMatchingTags(any())).thenReturn(false);
        apiManager = new ApiManagerImpl(eventManager, gatewayConfiguration, licenseManager, dataEncryptor, apiProductRegistry);
        listenerCaptor = ArgumentCaptor.forClass(EventListener.class);
        verify(eventManager).subscribeForEvents(listenerCaptor.capture(), eq(ApiProductEventType.UNDEPLOY));
    }

    @Test
    void should_undeploy_product_only_api_on_product_undeploy_event() {
        var api = buildProductOnlyApi();

        when(apiProductRegistry.getApiProductPlanEntriesForApi(eq("api-test"), eq("env-1"))).thenReturn(
            List.of(
                new ApiProductRegistry.ApiProductPlanEntry(
                    "product-1",
                    io.gravitee.definition.model.v4.plan.Plan.builder()
                        .id("plan-test")
                        .name("TestPlan")
                        .status(PlanStatus.PUBLISHED)
                        .build()
                )
            )
        );
        apiManager.register(api);
        verify(eventManager).publishEvent(ReactorEvent.DEPLOY, api);

        when(apiProductRegistry.getApiProductPlanEntriesForApi(eq("api-test"), eq("env-1"))).thenReturn(List.of());

        listenerCaptor
            .getValue()
            .onEvent(new SimpleEvent<>(ApiProductEventType.UNDEPLOY, new ApiProductChangedEvent("product-1", "env-1", Set.of("api-test"))));

        verify(eventManager).publishEvent(ReactorEvent.UNDEPLOY, api);
        assertThat(apiManager.apis()).isEmpty();
    }

    @Test
    void should_ignore_product_undeploy_event_with_null_api_ids() {
        var api = buildProductOnlyApi();

        when(apiProductRegistry.getApiProductPlanEntriesForApi(eq("api-test"), eq("env-1"))).thenReturn(
            List.of(
                new ApiProductRegistry.ApiProductPlanEntry(
                    "product-1",
                    io.gravitee.definition.model.v4.plan.Plan.builder().id("plan-test").status(PlanStatus.PUBLISHED).build()
                )
            )
        );
        apiManager.register(api);

        listenerCaptor
            .getValue()
            .onEvent(new SimpleEvent<>(ApiProductEventType.UNDEPLOY, new ApiProductChangedEvent("product-1", "env-1", null)));

        assertThat(apiManager.apis()).hasSize(1);
    }

    @Test
    void should_keep_api_deployed_when_product_undeploy_does_not_remove_eligibility() {
        var api = buildProductOnlyApi();

        when(apiProductRegistry.getApiProductPlanEntriesForApi(eq("api-test"), eq("env-1"))).thenReturn(
            List.of(
                new ApiProductRegistry.ApiProductPlanEntry(
                    "product-1",
                    io.gravitee.definition.model.v4.plan.Plan.builder().id("plan-test").status(PlanStatus.PUBLISHED).build()
                )
            )
        );
        apiManager.register(api);

        listenerCaptor
            .getValue()
            .onEvent(new SimpleEvent<>(ApiProductEventType.UNDEPLOY, new ApiProductChangedEvent("product-1", "env-1", Set.of("api-test"))));

        verify(eventManager, never()).publishEvent(ReactorEvent.UNDEPLOY, api);
        assertThat(apiManager.apis()).hasSize(1);
    }

    @Test
    void should_re_evaluate_after_product_change_via_public_api() {
        var api = buildProductOnlyApi();

        when(apiProductRegistry.getApiProductPlanEntriesForApi(eq("api-test"), eq("env-1"))).thenReturn(
            List.of(
                new ApiProductRegistry.ApiProductPlanEntry(
                    "product-1",
                    io.gravitee.definition.model.v4.plan.Plan.builder().id("plan-test").status(PlanStatus.PUBLISHED).build()
                )
            )
        );
        apiManager.register(api);
        verify(eventManager).publishEvent(ReactorEvent.DEPLOY, api);

        when(apiProductRegistry.getApiProductPlanEntriesForApi(eq("api-test"), eq("env-1"))).thenReturn(List.of());

        apiManager.reEvaluateAfterProductChange("product-1", Set.of("api-test"));

        verify(eventManager).publishEvent(ReactorEvent.UNDEPLOY, api);
        assertThat(apiManager.apis()).isEmpty();
    }

    @Test
    void should_serialize_product_undeploy_with_concurrent_register() throws Exception {
        var api = buildProductOnlyApi();

        when(apiProductRegistry.getApiProductPlanEntriesForApi(eq("api-test"), eq("env-1"))).thenReturn(
            List.of(
                new ApiProductRegistry.ApiProductPlanEntry(
                    "product-1",
                    io.gravitee.definition.model.v4.plan.Plan.builder().id("plan-test").status(PlanStatus.PUBLISHED).build()
                )
            )
        );
        apiManager.register(api);

        when(apiProductRegistry.getApiProductPlanEntriesForApi(eq("api-test"), eq("env-1"))).thenReturn(List.of());

        CountDownLatch undeployInsideHandler = new CountDownLatch(1);
        CountDownLatch releaseUndeploy = new CountDownLatch(1);
        CountDownLatch registerCompleted = new CountDownLatch(1);

        doAnswer(invocation -> {
            if (invocation.getArgument(0) == ReactorEvent.UNDEPLOY) {
                undeployInsideHandler.countDown();
                releaseUndeploy.await(5, TimeUnit.SECONDS);
            }
            return null;
        })
            .when(eventManager)
            .publishEvent(any(), any());

        Thread productUndeployThread = new Thread(() ->
            listenerCaptor
                .getValue()
                .onEvent(
                    new SimpleEvent<>(ApiProductEventType.UNDEPLOY, new ApiProductChangedEvent("product-1", "env-1", Set.of("api-test")))
                )
        );
        productUndeployThread.start();

        assertThat(undeployInsideHandler.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(apiManager.apis()).isEmpty();

        when(apiProductRegistry.getApiProductPlanEntriesForApi(eq("api-test"), eq("env-1"))).thenReturn(
            List.of(
                new ApiProductRegistry.ApiProductPlanEntry(
                    "product-1",
                    io.gravitee.definition.model.v4.plan.Plan.builder().id("plan-test").status(PlanStatus.PUBLISHED).build()
                )
            )
        );

        Thread registerThread = new Thread(() -> {
            apiManager.register(buildProductOnlyApi());
            registerCompleted.countDown();
        });
        registerThread.start();

        assertThat(registerCompleted.await(200, TimeUnit.MILLISECONDS)).isFalse();

        releaseUndeploy.countDown();
        productUndeployThread.join(5_000);
        registerThread.join(5_000);

        assertThat(registerCompleted.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(apiManager.apis()).hasSize(1);
    }

    private io.gravitee.gateway.reactive.handlers.api.v4.Api buildProductOnlyApi() {
        HttpListener httpListener = new HttpListener();
        httpListener.setPaths(List.of(mock(Path.class)));

        var definition = new io.gravitee.definition.model.v4.Api();
        definition.setId("api-test");
        definition.setPlans(Collections.emptyList());

        var api = new io.gravitee.gateway.reactive.handlers.api.v4.Api(definition);
        api.setEnabled(true);
        api.setEnvironmentId("env-1");
        api.setDeployedAt(new Date());
        api.setOrganizationId("org-id");
        return api;
    }
}
