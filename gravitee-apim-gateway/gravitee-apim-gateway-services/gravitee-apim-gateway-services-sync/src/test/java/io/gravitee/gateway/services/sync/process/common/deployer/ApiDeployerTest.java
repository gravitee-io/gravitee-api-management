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
package io.gravitee.gateway.services.sync.process.common.deployer;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.gateway.handlers.api.manager.ApiManager;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.gateway.services.sync.process.common.model.SyncException;
import io.gravitee.gateway.services.sync.process.distributed.service.NoopDistributedSyncService;
import io.gravitee.gateway.services.sync.process.repository.service.AuthzRegistry;
import io.gravitee.gateway.services.sync.process.repository.service.PlanService;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.api.ApiReactorDeployable;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.authz.AuthzEnginePort;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.authz.AuthzEntityIdExtractor;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiDeployerTest {

    @Mock
    private ApiManager apiManager;

    @Mock
    private AuthzEnginePort authzEnginePort;

    private ApiDeployer cut;
    private PlanService planService;
    private AuthzRegistry authzRegistry;

    @BeforeEach
    public void beforeEach() {
        planService = new PlanService();
        authzRegistry = new AuthzRegistry(null);
        org.mockito.Mockito.lenient()
            .when(authzEnginePort.addOrUpdateEntity(any(), any(), any()))
            .thenReturn(io.reactivex.rxjava3.core.Completable.complete());
        org.mockito.Mockito.lenient()
            .when(authzEnginePort.removeEntity(any()))
            .thenReturn(io.reactivex.rxjava3.core.Completable.complete());
        org.mockito.Mockito.lenient().when(authzEnginePort.commit()).thenReturn(io.reactivex.rxjava3.core.Completable.complete());
        cut = new ApiDeployer(
            apiManager,
            planService,
            new NoopDistributedSyncService(),
            authzRegistry,
            AuthzEntityIdExtractor.INSTANCE,
            authzEnginePort
        );
    }

    @Nested
    class DeployTest {

        @Test
        void should_deploy_api() {
            ReactableApi reactableApi = mock(ReactableApi.class);
            ApiReactorDeployable apiReactorDeployable = ApiReactorDeployable.builder().apiId("apiId").reactableApi(reactableApi).build();
            cut.deploy(apiReactorDeployable).test().assertComplete();
            verify(apiManager).register(reactableApi);
        }

        @Test
        void should_return_error_when_api_manager_throw_exception() {
            ReactableApi reactableApi = mock(ReactableApi.class);
            ApiReactorDeployable apiReactorDeployable = ApiReactorDeployable.builder().apiId("apiId").reactableApi(reactableApi).build();
            when(apiManager.register(reactableApi)).thenThrow(new SyncException("error"));
            cut.deploy(apiReactorDeployable).test().assertFailure(SyncException.class);
            verify(apiManager).register(reactableApi);
        }

        @Test
        void should_do_post_action() {
            ReactableApi reactableApi = mock(ReactableApi.class);
            ApiReactorDeployable apiReactorDeployable = ApiReactorDeployable.builder()
                .apiId("apiId")
                .subscribablePlans(Set.of("plan"))
                .reactableApi(reactableApi)
                .build();
            cut.doAfterDeployment(apiReactorDeployable).test().assertComplete();
            assertThat(planService.isDeployed("apiId", "plan")).isTrue();
        }

        @Test
        void should_stage_authz_entities_and_commit_BEFORE_apiManager_register_on_deploy() {
            // Traffic flows to an API as soon as apiManager.register completes; if authz
            // entities aren't already in the engine, policies referring to them evaluate
            // against a partial state. Entities must be committed before register.
            ReactableApi<?> reactableApi = httpProxyApi("api-1");
            ApiReactorDeployable deployable = ApiReactorDeployable.builder().apiId("api-1").reactableApi(reactableApi).build();

            cut.deploy(deployable).test().assertComplete();

            InOrder inOrder = inOrder(authzEnginePort, apiManager);
            inOrder.verify(authzEnginePort).addOrUpdateEntity(eq("Resource::\"api.api-1\""), any(), any());
            inOrder.verify(authzEnginePort).commit();
            inOrder.verify(apiManager).register(reactableApi);
        }
    }

    @Nested
    class UndeployTest {

        @Test
        void should_undeploy_api() {
            ApiReactorDeployable apiReactorDeployable = ApiReactorDeployable.builder().apiId("apiId").build();
            cut.undeploy(apiReactorDeployable).test().assertComplete();
            verify(apiManager).unregister("apiId");
        }

        @Test
        void should_complete_on_error_when_api_manager_throw_exception() {
            ApiReactorDeployable apiReactorDeployable = ApiReactorDeployable.builder().apiId("apiId").build();
            doThrow(new SyncException("error")).when(apiManager).unregister("apiId");
            cut.undeploy(apiReactorDeployable).test().assertFailure(SyncException.class);
            verify(apiManager).unregister("apiId");
        }
    }

    @Nested
    class AuthzRegistryHookTest {

        @Test
        void should_register_entityIds_on_after_deployment_for_v4_proxy_api() {
            ReactableApi<?> reactableApi = httpProxyApi("api-1");
            ApiReactorDeployable deployable = ApiReactorDeployable.builder().apiId("api-1").reactableApi(reactableApi).build();

            cut.doAfterDeployment(deployable).test().assertComplete();

            assertThat(authzRegistry.isResourceDeployed("api.api-1")).isTrue();
        }

        @Test
        void should_register_api_and_mcp_tool_entityIds_for_mcp_proxy_api() {
            ReactableApi<?> reactableApi = mcpProxyWithTools("bookings", List.of("get-booking", "list-bookings"));
            ApiReactorDeployable deployable = ApiReactorDeployable.builder().apiId("bookings").reactableApi(reactableApi).build();

            cut.doAfterDeployment(deployable).test().assertComplete();

            assertThat(authzRegistry.isResourceDeployed("api.bookings")).isTrue();
            assertThat(authzRegistry.isResourceDeployed("mcp.bookings.get-booking")).isTrue();
            assertThat(authzRegistry.isResourceDeployed("mcp.bookings.list-bookings")).isTrue();
        }

        @Test
        void should_skip_registration_when_reactableApi_is_null() {
            ApiReactorDeployable deployable = ApiReactorDeployable.builder().apiId("api-1").build();

            cut.doAfterDeployment(deployable).test().assertComplete();

            assertThat(authzRegistry.isResourceDeployed("api.api-1")).isFalse();
        }

        @Test
        void should_unregister_entityIds_on_undeploy_for_v4_proxy_api() {
            ReactableApi<?> reactableApi = httpProxyApi("api-1");
            ApiReactorDeployable deployable = ApiReactorDeployable.builder().apiId("api-1").reactableApi(reactableApi).build();
            cut.doAfterDeployment(deployable).test().assertComplete();
            assertThat(authzRegistry.isResourceDeployed("api.api-1")).isTrue();

            cut.undeploy(deployable).test().assertComplete();

            assertThat(authzRegistry.isResourceDeployed("api.api-1")).isFalse();
        }

        @Test
        void should_unregister_api_and_mcp_tool_entityIds_for_mcp_proxy_api() {
            ReactableApi<?> reactableApi = mcpProxyWithTools("bookings", List.of("get-booking"));
            ApiReactorDeployable deployable = ApiReactorDeployable.builder().apiId("bookings").reactableApi(reactableApi).build();
            cut.doAfterDeployment(deployable).test().assertComplete();
            assertThat(authzRegistry.isResourceDeployed("mcp.bookings.get-booking")).isTrue();

            cut.undeploy(deployable).test().assertComplete();

            assertThat(authzRegistry.isResourceDeployed("api.bookings")).isFalse();
            assertThat(authzRegistry.isResourceDeployed("mcp.bookings.get-booking")).isFalse();
        }

        @Test
        void should_unregister_authz_before_apiManager_unregister_on_undeploy() {
            ReactableApi<?> reactableApi = httpProxyApi("api-1");
            ApiReactorDeployable deployable = ApiReactorDeployable.builder().apiId("api-1").reactableApi(reactableApi).build();

            AuthzRegistry spyRegistry = spy(new AuthzRegistry(null));
            spyRegistry.registerForApi("api-1", Set.of("api.api-1"));
            ApiDeployer deployerWithSpy = new ApiDeployer(
                apiManager,
                planService,
                new NoopDistributedSyncService(),
                spyRegistry,
                AuthzEntityIdExtractor.INSTANCE,
                authzEnginePort
            );

            deployerWithSpy.undeploy(deployable).test().assertComplete();

            InOrder inOrder = inOrder(spyRegistry, apiManager);
            inOrder.verify(spyRegistry).unregisterApi("api-1");
            inOrder.verify(apiManager).unregister("api-1");
        }

        @Test
        void should_skip_unregistration_when_reactableApi_is_null_on_undeploy() {
            ApiReactorDeployable deployable = ApiReactorDeployable.builder().apiId("api-1").build();

            cut.undeploy(deployable).test().assertComplete();

            verify(apiManager).unregister("api-1");
        }
    }

    @Nested
    class AutoDerivedEntityStagingTest {

        @Test
        void should_stage_every_auto_derived_entityId_on_engine_for_proxy_api_and_commit() {
            ReactableApi<?> reactableApi = httpProxyApi("api-1");
            ApiReactorDeployable deployable = ApiReactorDeployable.builder().apiId("api-1").reactableApi(reactableApi).build();

            cut.deploy(deployable).test().assertComplete();

            verify(authzEnginePort).addOrUpdateEntity(eq("Resource::\"api.api-1\""), any(), any());
            verify(authzEnginePort, atLeastOnce()).commit();
        }

        @Test
        void should_stage_api_root_and_each_mcp_tool_alias_on_engine_for_mcp_proxy_api() {
            ReactableApi<?> reactableApi = mcpProxyWithTools("bookings", List.of("get-booking", "list-bookings"));
            ApiReactorDeployable deployable = ApiReactorDeployable.builder().apiId("bookings").reactableApi(reactableApi).build();

            cut.deploy(deployable).test().assertComplete();

            verify(authzEnginePort).addOrUpdateEntity(eq("Resource::\"api.bookings\""), any(), any());
            verify(authzEnginePort).addOrUpdateEntity(eq("Resource::\"mcp.bookings.get-booking\""), any(), any());
            verify(authzEnginePort).addOrUpdateEntity(eq("Resource::\"mcp.bookings.list-bookings\""), any(), any());
            verify(authzEnginePort, atLeastOnce()).commit();
        }

        @Test
        void should_skip_engine_staging_when_reactableApi_is_null() {
            ApiReactorDeployable deployable = ApiReactorDeployable.builder().apiId("api-1").build();

            cut.deploy(deployable).test().assertComplete();

            verifyNoInteractions(authzEnginePort);
        }

        @Test
        void should_remove_every_auto_derived_entityId_from_engine_on_undeploy_and_commit() {
            ReactableApi<?> reactableApi = mcpProxyWithTools("bookings", List.of("get-booking"));
            ApiReactorDeployable deployable = ApiReactorDeployable.builder().apiId("bookings").reactableApi(reactableApi).build();

            cut.doAfterDeployment(deployable).test().assertComplete();
            cut.undeploy(deployable).test().assertComplete();

            verify(authzEnginePort).removeEntity(eq("Resource::\"api.bookings\""));
            verify(authzEnginePort).removeEntity(eq("Resource::\"mcp.bookings.get-booking\""));
            verify(authzEnginePort, atLeastOnce()).commit();
        }

        @Test
        void should_swallow_engine_failure_on_deploy_so_api_chain_continues() {
            when(authzEnginePort.addOrUpdateEntity(any(), any(), any())).thenReturn(
                io.reactivex.rxjava3.core.Completable.error(new RuntimeException("engine down"))
            );

            ReactableApi<?> reactableApi = httpProxyApi("api-1");
            ApiReactorDeployable deployable = ApiReactorDeployable.builder().apiId("api-1").reactableApi(reactableApi).build();

            cut.deploy(deployable).test().assertComplete();
            cut.doAfterDeployment(deployable).test().assertComplete();

            verify(apiManager).register(reactableApi);
            assertThat(authzRegistry.isResourceDeployed("api.api-1")).isTrue();
        }

        @Test
        void should_swallow_engine_failure_on_undeploy_so_api_chain_continues() {
            when(authzEnginePort.removeEntity(any())).thenReturn(
                io.reactivex.rxjava3.core.Completable.error(new RuntimeException("engine down"))
            );

            ReactableApi<?> reactableApi = httpProxyApi("api-1");
            ApiReactorDeployable deployable = ApiReactorDeployable.builder().apiId("api-1").reactableApi(reactableApi).build();

            cut.doAfterDeployment(deployable).test().assertComplete();
            cut.undeploy(deployable).test().assertComplete();

            verify(apiManager).unregister("api-1");
        }

        @Test
        void should_still_commit_when_a_single_entity_addOrUpdate_fails() {
            when(authzEnginePort.addOrUpdateEntity(eq("Resource::\"mcp.bookings.get-booking\""), any(), any())).thenReturn(
                io.reactivex.rxjava3.core.Completable.error(new RuntimeException("engine wobble"))
            );

            ReactableApi<?> reactableApi = mcpProxyWithTools("bookings", List.of("get-booking", "list-bookings"));
            ApiReactorDeployable deployable = ApiReactorDeployable.builder().apiId("bookings").reactableApi(reactableApi).build();

            cut.deploy(deployable).test().assertComplete();

            verify(authzEnginePort).addOrUpdateEntity(eq("Resource::\"api.bookings\""), any(), any());
            verify(authzEnginePort).addOrUpdateEntity(eq("Resource::\"mcp.bookings.get-booking\""), any(), any());
            verify(authzEnginePort).addOrUpdateEntity(eq("Resource::\"mcp.bookings.list-bookings\""), any(), any());
            verify(authzEnginePort, atLeastOnce()).commit();
        }

        @Test
        void should_complete_when_engine_commit_itself_fails() {
            when(authzEnginePort.commit()).thenReturn(io.reactivex.rxjava3.core.Completable.error(new RuntimeException("commit failed")));

            ReactableApi<?> reactableApi = httpProxyApi("api-1");
            ApiReactorDeployable deployable = ApiReactorDeployable.builder().apiId("api-1").reactableApi(reactableApi).build();

            cut.deploy(deployable).test().assertComplete();
        }

        @Test
        void should_pass_api_attributes_to_engine_for_root_api_entity() {
            ReactableApi<?> reactableApi = httpProxyApi("api-1");
            ApiReactorDeployable deployable = ApiReactorDeployable.builder().apiId("api-1").reactableApi(reactableApi).build();

            cut.deploy(deployable).test().assertComplete();

            verify(authzEnginePort).addOrUpdateEntity(
                eq("Resource::\"api.api-1\""),
                eq(java.util.Map.of("apiId", "api-1", "apiName", "test", "apiVersion", "1")),
                eq(java.util.List.of())
            );
        }

        @Test
        void should_invoke_apiManager_unregister_BEFORE_engine_removeEntity_on_undeploy() {
            ReactableApi<?> reactableApi = httpProxyApi("api-1");
            ApiReactorDeployable deployable = ApiReactorDeployable.builder().apiId("api-1").reactableApi(reactableApi).build();

            cut.doAfterDeployment(deployable).test().assertComplete();
            cut.undeploy(deployable).test().assertComplete();

            InOrder inOrder = inOrder(apiManager, authzEnginePort);
            inOrder.verify(apiManager).unregister("api-1");
            inOrder.verify(authzEnginePort).removeEntity(eq("Resource::\"api.api-1\""));
        }
    }

    private static ReactableApi<?> httpProxyApi(String id) {
        io.gravitee.definition.model.v4.Api definition = new io.gravitee.definition.model.v4.Api();
        definition.setId(id);
        definition.setName("test");
        definition.setApiVersion("1");
        definition.setDefinitionVersion(DefinitionVersion.V4);
        definition.setType(ApiType.PROXY);
        definition.setFlows(List.of());
        return new io.gravitee.gateway.reactive.handlers.api.v4.Api(definition);
    }

    private static ReactableApi<?> mcpProxyWithTools(String id, List<String> toolNames) {
        io.gravitee.definition.model.v4.Api definition = new io.gravitee.definition.model.v4.Api();
        definition.setId(id);
        definition.setName("test");
        definition.setApiVersion("1");
        definition.setDefinitionVersion(DefinitionVersion.V4);
        definition.setType(ApiType.MCP_PROXY);
        definition.setFlows(
            toolNames
                .stream()
                .map(name -> {
                    io.gravitee.definition.model.v4.flow.selector.McpSelector selector =
                        new io.gravitee.definition.model.v4.flow.selector.McpSelector();
                    selector.setMethods(Set.of("tools/call"));
                    io.gravitee.definition.model.v4.flow.Flow flow = new io.gravitee.definition.model.v4.flow.Flow();
                    flow.setName(name);
                    flow.setSelectors(List.of(selector));
                    return flow;
                })
                .toList()
        );
        return new io.gravitee.gateway.reactive.handlers.api.v4.Api(definition);
    }
}
