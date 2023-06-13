/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.plugin.apiservice.internal;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.gateway.reactive.api.apiservice.ApiService;
import io.gravitee.gateway.reactive.api.apiservice.ApiServiceFactory;
import io.gravitee.gateway.reactive.api.connector.endpoint.EndpointConnector;
import io.gravitee.gateway.reactive.api.context.DeploymentContext;
import io.gravitee.plugin.apiservice.ApiServicePluginManager;
import io.gravitee.plugin.apiservice.internal.fake.FakeApiService;
import io.gravitee.plugin.apiservice.internal.fake.FakeApiServiceConfiguration;
import io.gravitee.plugin.apiservice.internal.fake.FakeApiServiceFactory;
import io.gravitee.plugin.apiservice.internal.fake.FakeApiServicePlugin;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class DefaultApiServicePluginManagerTest {

    @Mock
    private DeploymentContext deploymentContext;

    private ApiServicePluginManager cut;

    @BeforeEach
    public void beforeEach() {
        cut = new DefaultApiServicePluginManager(new DefaultApiServiceClassLoaderFactory(), null);
    }

    @Test
    public void shouldRegisterNewApiServicePlugin() {
        final DefaultApiServicePlugin<FakeApiServiceFactory, FakeApiServiceConfiguration> apiServicePlugin = new DefaultApiServicePlugin<>(
            new FakeApiServicePlugin(true),
            FakeApiServiceFactory.class,
            null
        );

        cut.register(apiServicePlugin);

        final ApiServiceFactory<FakeApiService> fake = cut.getFactoryById("fake-api-service");
        assertThat(fake).isNotNull();

        final ApiService fakeConnector = fake.createService(deploymentContext);
        assertThat(fakeConnector).isNotNull();
    }

    @Test
    public void shouldNotRetrieveUnRegisterPlugin() {
        final EndpointConnector factoryById = cut.getFactoryById("fake-api-service");
        assertThat(factoryById).isNull();
    }

    @Test
    public void shouldRegisterNotDeployedPlugin() {
        final DefaultApiServicePlugin<FakeApiServiceFactory, FakeApiServiceConfiguration> apiServicePlugin = new DefaultApiServicePlugin<>(
            new FakeApiServicePlugin(false),
            FakeApiServiceFactory.class,
            null
        );

        cut.register(apiServicePlugin);

        final ApiServiceFactory<FakeApiService> fake = cut.getFactoryById("fake-api-service", true);
        assertThat(fake).isNotNull();

        final ApiService fakeConnector = fake.createService(deploymentContext);
        assertThat(fakeConnector).isNotNull();
    }

    @Test
    public void shouldRetrieveAllFactories() {
        final DefaultApiServicePlugin<FakeApiServiceFactory, FakeApiServiceConfiguration> apiServicePlugin = new DefaultApiServicePlugin<>(
            new FakeApiServicePlugin(true),
            FakeApiServiceFactory.class,
            null
        );

        final DefaultApiServicePlugin<FakeApiServiceFactory, FakeApiServiceConfiguration> apiServicePluginNotDeployed =
            new DefaultApiServicePlugin<>(new FakeApiServicePlugin(false), FakeApiServiceFactory.class, null);

        cut.register(apiServicePlugin);
        cut.register(apiServicePluginNotDeployed);

        List<ApiServiceFactory<FakeApiService>> factoryList = cut.getAllFactories(true);

        assertThat(factoryList).isNotNull().hasSize(2);
    }

    @Test
    public void shouldOnlyRetrieveDeployedPluginFactories() {
        final DefaultApiServicePlugin<FakeApiServiceFactory, FakeApiServiceConfiguration> apiServicePlugin = new DefaultApiServicePlugin<>(
            new FakeApiServicePlugin(true),
            FakeApiServiceFactory.class,
            null
        );

        final DefaultApiServicePlugin<FakeApiServiceFactory, FakeApiServiceConfiguration> apiServicePluginNotDeployed =
            new DefaultApiServicePlugin<>(new FakeApiServicePlugin(false), FakeApiServiceFactory.class, null);

        cut.register(apiServicePlugin);
        cut.register(apiServicePluginNotDeployed);

        List<ApiServiceFactory<FakeApiService>> factoryList = cut.getAllFactories();

        assertThat(factoryList).isNotNull().hasSize(1);
    }
}
