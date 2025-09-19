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
package io.gravitee.apim.infra.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import fixtures.core.model.ApiFixtures;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.rest.api.common.apiservices.DefaultManagementDeploymentContext;
import io.gravitee.apim.rest.api.common.apiservices.ManagementApiService;
import io.gravitee.apim.rest.api.common.apiservices.ManagementApiServiceFactory;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.AbstractApi;
import io.gravitee.plugin.apiservice.ApiServicePluginManager;
import io.reactivex.rxjava3.core.Completable;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class ManagementApiServicesManagerTest {

    private ManagementApiServicesManager cut;

    @Mock
    private ApiServicePluginManager apiServicePluginManager;

    @Mock
    private ApplicationContext applicationContext;

    @BeforeEach
    void setUp() {
        cut = new ManagementApiServicesManager(applicationContext, apiServicePluginManager);
    }

    @Nested
    class DeployServices {

        @ParameterizedTest
        @EnumSource(value = DefinitionVersion.class, names = { "V1", "V2" })
        void should_not_deploy_service_for_non_v4_api(DefinitionVersion definitionVersion) {
            final Api api = ApiFixtures.aProxyApiV2();
            api.setDefinitionVersion(definitionVersion);

            cut.deployServices(api);

            verifyNoInteractions(apiServicePluginManager);
        }

        @Test
        void should_not_deploy_service_for_v4_api_without_service_factories() {
            final Api api = ApiFixtures.aProxyApiV4();

            cut.deployServices(api);
            assertThat(cut.servicesByApi).isEmpty();
        }

        @Test
        void should_not_deploy_service_for_v4_api_without_service_configured() {
            final Api api = ApiFixtures.aProxyApiV4();

            when(apiServicePluginManager.getAllFactories(ManagementApiServiceFactory.class)).thenReturn(
                List.of(new FakeDynamicPropertiesApiServiceFactory(false))
            );

            cut.deployServices(api);
            assertThat(cut.servicesByApi).isEmpty();
        }

        @Test
        void should_deploy_services_for_v4_api() {
            final Api api = ApiFixtures.aProxyApiV4();

            when(apiServicePluginManager.getAllFactories(ManagementApiServiceFactory.class)).thenReturn(
                List.of(new FakeDynamicPropertiesApiServiceFactory(true))
            );

            cut.deployServices(api);
            assertThat(cut.servicesByApi).hasSize(1).containsKey(api.getId());
            assertThat(cut.servicesByApi.get(api.getId()))
                .hasSize(1)
                .first()
                .satisfies(managementApiService -> {
                    assertThat(managementApiService).isInstanceOf(FakeDynamicPropertiesApiService.class);
                    final FakeDynamicPropertiesApiService fakeService = (FakeDynamicPropertiesApiService) managementApiService;
                    assertThat(fakeService.hasBeenStarted).isTrue();
                    assertThat(fakeService.hasBeenStopped).isFalse();
                    assertThat(fakeService.hasBeenRestarted).isFalse();
                });
        }
    }

    @Nested
    class StartDynamicProperties {

        @ParameterizedTest
        @EnumSource(value = DefinitionVersion.class, names = { "V1", "V2" })
        void should_not_start_dp_for_non_v4_api(DefinitionVersion definitionVersion) {
            final Api api = ApiFixtures.aProxyApiV2();
            api.setDefinitionVersion(definitionVersion);

            cut.startDynamicProperties(api);

            verifyNoInteractions(apiServicePluginManager);
        }

        @Test
        void should_not_start_dp_for_v4_api_without_service_factories() {
            final Api api = ApiFixtures.aProxyApiV4();

            cut.startDynamicProperties(api);
            assertThat(cut.servicesByApi).isEmpty();
        }

        @Test
        void should_not_start_dp_for_v4_api_without_service_configured() {
            final Api api = ApiFixtures.aProxyApiV4();

            when(apiServicePluginManager.getAllFactories(ManagementApiServiceFactory.class)).thenReturn(
                List.of(new FakeDynamicPropertiesApiServiceFactory(false), new FakeHttpDynamicPropertiesApiServiceFactory(false))
            );

            cut.startDynamicProperties(api);
            assertThat(cut.servicesByApi).isEmpty();
        }

        @Test
        void should_start_dp_for_v4_api() {
            final Api api = ApiFixtures.aProxyApiV4();

            when(apiServicePluginManager.getAllFactories(ManagementApiServiceFactory.class)).thenReturn(
                List.of(new FakeHttpDynamicPropertiesApiServiceFactory(true))
            );

            cut.startDynamicProperties(api);
            assertThat(cut.servicesByApi).hasSize(1).containsKey(api.getId());
            assertThat(cut.servicesByApi.get(api.getId()))
                .hasSize(1)
                .first()
                .satisfies(managementApiService -> {
                    assertThat(managementApiService).isInstanceOf(FakeHttpDynamicPropertiesApiService.class);
                    final FakeHttpDynamicPropertiesApiService fakeService = (FakeHttpDynamicPropertiesApiService) managementApiService;
                    assertThat(fakeService.hasBeenStarted).isTrue();
                    assertThat(fakeService.hasBeenStopped).isFalse();
                    assertThat(fakeService.hasBeenRestarted).isFalse();
                });
        }
    }

    @Nested
    class UndeployServices {

        @Test
        void should_not_undeploy_services_for_v4_unknown_api() {
            final Api api = ApiFixtures.aProxyApiV4();

            // we do not save the api in the internal map servicesByApi
            cut.undeployServices(api);

            assertThat(cut.servicesByApi).isEmpty();
        }

        @Test
        void should_not_undeploy_services_for_v4_api_without_deployed_ones() {
            final Api api = ApiFixtures.aProxyApiV4();
            cut.servicesByApi.put(api.getId(), List.of());

            cut.undeployServices(api);

            assertThat(cut.servicesByApi).isEmpty();
        }

        @Test
        void should_undeploy_services_for_v4_api() {
            final Api api = ApiFixtures.aProxyApiV4();
            final FakeDynamicPropertiesApiService fakeService = new FakeDynamicPropertiesApiService();
            cut.servicesByApi.put(api.getId(), List.of(fakeService));

            cut.undeployServices(api);

            assertThat(cut.servicesByApi).isEmpty();
            assertThat(fakeService.hasBeenStarted).isFalse();
            assertThat(fakeService.hasBeenStopped).isTrue();
            assertThat(fakeService.hasBeenRestarted).isFalse();
        }
    }

    @Nested
    class RestartServices {

        @Test
        void should_not_restart_services_for_v4_unknown_api_but_start_it() {
            final Api api = ApiFixtures.aProxyApiV4();

            when(apiServicePluginManager.getAllFactories(ManagementApiServiceFactory.class)).thenReturn(
                List.of(new FakeDynamicPropertiesApiServiceFactory(true))
            );

            cut.updateServices(api);
            assertThat(cut.servicesByApi).hasSize(1).containsKey(api.getId());
            assertThat(cut.servicesByApi.get(api.getId()))
                .hasSize(1)
                .first()
                .satisfies(managementApiService -> {
                    assertThat(managementApiService).isInstanceOf(FakeDynamicPropertiesApiService.class);
                    final FakeDynamicPropertiesApiService fakeService = (FakeDynamicPropertiesApiService) managementApiService;
                    assertThat(fakeService.hasBeenStarted).isTrue();
                    assertThat(fakeService.hasBeenStopped).isFalse();
                    assertThat(fakeService.hasBeenRestarted).isFalse();
                });
        }

        @Test
        void should_not_restart_services_for_v4_api_whithout_service_but_start_it() {
            final Api api = ApiFixtures.aProxyApiV4();
            cut.servicesByApi.put(api.getId(), List.of());

            when(apiServicePluginManager.getAllFactories(ManagementApiServiceFactory.class)).thenReturn(
                List.of(new FakeDynamicPropertiesApiServiceFactory(true))
            );

            cut.updateServices(api);
            assertThat(cut.servicesByApi).hasSize(1).containsKey(api.getId());
            assertThat(cut.servicesByApi.get(api.getId()))
                .hasSize(1)
                .first()
                .satisfies(managementApiService -> {
                    assertThat(managementApiService).isInstanceOf(FakeDynamicPropertiesApiService.class);
                    final FakeDynamicPropertiesApiService fakeService = (FakeDynamicPropertiesApiService) managementApiService;
                    assertThat(fakeService.hasBeenStarted).isTrue();
                    assertThat(fakeService.hasBeenStopped).isFalse();
                    assertThat(fakeService.hasBeenRestarted).isFalse();
                });
        }

        @Test
        void should_restart_services_for_v4_api() {
            final Api api = ApiFixtures.aProxyApiV4();
            final FakeDynamicPropertiesApiService fakeService = new FakeDynamicPropertiesApiService();
            cut.servicesByApi.put(api.getId(), List.of(fakeService));

            cut.updateServices(api);
            assertThat(cut.servicesByApi).hasSize(1).containsKey(api.getId());
            assertThat(cut.servicesByApi.get(api.getId()))
                .hasSize(1)
                .first()
                .satisfies(managementApiService -> {
                    assertThat(managementApiService).isInstanceOf(FakeDynamicPropertiesApiService.class);
                    final FakeDynamicPropertiesApiService restartedService = (FakeDynamicPropertiesApiService) managementApiService;
                    assertThat(restartedService).isSameAs(fakeService);
                    assertThat(restartedService.hasBeenStarted).isFalse();
                    assertThat(restartedService.hasBeenStopped).isFalse();
                    assertThat(restartedService.hasBeenRestarted).isTrue();
                });
        }
    }

    @Nested
    class StopDynamicProperties {

        @Test
        void shouldStopDynamicPropertiesServiceSuccessfully() {
            // Arrange
            final Api api = ApiFixtures.aProxyApiV4();
            final FakeDynamicPropertiesApiService fakeService1 = new FakeHttpDynamicPropertiesApiService();
            final FakeDynamicPropertiesApiService fakeService2 = new FakeDynamicPropertiesApiService();
            cut.servicesByApi.put(api.getId(), new ArrayList<>(List.of(fakeService1, fakeService2)));
            // Act
            cut.stopDynamicProperties(api);
            // Assert
            assertThat(fakeService1.hasBeenStopped).isTrue();
            assertThat(fakeService2.hasBeenStopped).isFalse();
            assertThat(cut.servicesByApi.get(api.getId()).contains(fakeService1)).isFalse();
        }

        @Test
        void shouldDoNothingWhenNoServicesForApiFound() {
            // Arrange
            final Api api = ApiFixtures.aProxyApiV4();
            final FakeDynamicPropertiesApiService fakeService1 = new FakeDynamicPropertiesApiService();
            final FakeDynamicPropertiesApiService fakeService2 = new FakeDynamicPropertiesApiService();
            cut.servicesByApi.put(api.getId(), new ArrayList<>(List.of(fakeService1, fakeService2)));
            // Act
            cut.stopDynamicProperties(api);
            // Assert
            assertThat(fakeService1.hasBeenStopped).isFalse();
            assertThat(fakeService2.hasBeenStopped).isFalse();
            assertThat(cut.servicesByApi.get(api.getId()).contains(fakeService1)).isTrue();
            assertThat(cut.servicesByApi.get(api.getId()).contains(fakeService2)).isTrue();
        }
    }

    @Nested
    class StopServicesOnNodeStop {

        @SneakyThrows
        @Test
        void should_stop_services_on_node_stop() {
            final FakeDynamicPropertiesApiService fakeService1 = new FakeDynamicPropertiesApiService();
            final FakeDynamicPropertiesApiService fakeService2 = new FakeDynamicPropertiesApiService();
            cut.servicesByApi.put("api1", List.of(fakeService1));
            cut.servicesByApi.put("api2", List.of(fakeService2));

            cut.doStop();
            assertThat(cut.servicesByApi.get("api1"))
                .hasSize(1)
                .first()
                .satisfies(managementApiService -> {
                    assertThat(managementApiService).isInstanceOf(FakeDynamicPropertiesApiService.class);
                    final FakeDynamicPropertiesApiService stoppedService = (FakeDynamicPropertiesApiService) managementApiService;
                    assertThat(stoppedService).isSameAs(fakeService1);
                    assertThat(stoppedService.hasBeenStarted).isFalse();
                    assertThat(stoppedService.hasBeenStopped).isTrue();
                    assertThat(stoppedService.hasBeenRestarted).isFalse();
                });
            assertThat(cut.servicesByApi.get("api2"))
                .hasSize(1)
                .first()
                .satisfies(managementApiService -> {
                    assertThat(managementApiService).isInstanceOf(FakeDynamicPropertiesApiService.class);
                    final FakeDynamicPropertiesApiService stoppedService = (FakeDynamicPropertiesApiService) managementApiService;
                    assertThat(stoppedService).isSameAs(fakeService2);
                    assertThat(stoppedService.hasBeenStarted).isFalse();
                    assertThat(stoppedService.hasBeenStopped).isTrue();
                    assertThat(stoppedService.hasBeenRestarted).isFalse();
                });
        }
    }

    @AllArgsConstructor
    static class FakeDynamicPropertiesApiServiceFactory implements ManagementApiServiceFactory<FakeDynamicPropertiesApiService> {

        private final boolean returnService;

        @Override
        public FakeDynamicPropertiesApiService createService(DefaultManagementDeploymentContext deploymentContext) {
            return returnService ? new FakeDynamicPropertiesApiService() : null;
        }
    }

    static class FakeHttpDynamicPropertiesApiServiceFactory extends FakeDynamicPropertiesApiServiceFactory {

        private boolean returnService;

        public FakeHttpDynamicPropertiesApiServiceFactory(boolean returnService) {
            super(returnService);
            this.returnService = returnService;
        }

        @Override
        public FakeDynamicPropertiesApiService createService(DefaultManagementDeploymentContext deploymentContext) {
            return returnService ? new FakeHttpDynamicPropertiesApiService() : null;
        }
    }

    @Getter
    static class FakeDynamicPropertiesApiService implements ManagementApiService {

        protected boolean hasBeenStarted;
        protected boolean hasBeenStopped;
        protected boolean hasBeenRestarted;

        @Override
        public String id() {
            return "fake";
        }

        @Override
        public String kind() {
            return "fake";
        }

        @Override
        public Completable start() {
            hasBeenStarted = true;
            return Completable.complete();
        }

        @Override
        public Completable stop() {
            hasBeenStopped = true;
            return Completable.complete();
        }

        @Override
        public Completable update(AbstractApi api) {
            hasBeenRestarted = true;
            return Completable.complete();
        }
    }

    @Getter
    static class FakeHttpDynamicPropertiesApiService extends FakeDynamicPropertiesApiService {

        @Override
        public String id() {
            return "http-dynamic-properties";
        }
    }
}
