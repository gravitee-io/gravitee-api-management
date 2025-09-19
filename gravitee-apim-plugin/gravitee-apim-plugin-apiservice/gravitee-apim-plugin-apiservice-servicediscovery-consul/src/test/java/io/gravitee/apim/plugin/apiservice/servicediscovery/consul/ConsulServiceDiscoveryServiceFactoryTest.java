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
package io.gravitee.apim.plugin.apiservice.servicediscovery.consul;

import static fixtures.ApiFixtures.anApiWithServiceDiscovery;
import static fixtures.definition.ApiDefinitionFixtures.anApiV4;
import static io.gravitee.apim.plugin.apiservice.servicediscovery.consul.ConsulServiceDiscoveryService.CONSUL_SERVICE_DISCOVERY_ID;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.v4.service.Service;
import io.gravitee.gateway.reactive.api.context.DeploymentContext;
import io.gravitee.gateway.reactive.handlers.api.v4.Api;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConsulServiceDiscoveryServiceFactoryTest {

    @Mock
    private DeploymentContext deploymentContext;

    @BeforeEach
    void setUp() {}

    @Test
    void should_create_the_service_when_discovery_service_is_enabled_in_group() {
        when(deploymentContext.getComponent(Api.class)).thenReturn(
            anApiWithServiceDiscovery(Service.builder().enabled(true).type(CONSUL_SERVICE_DISCOVERY_ID).build())
        );

        var result = new ConsulServiceDiscoveryServiceFactory().createService(deploymentContext);

        Assertions.assertThat(result).isInstanceOf(ConsulServiceDiscoveryService.class);
    }

    @Test
    void should_not_create_the_service_when_discovery_service_is_disabled_in_group() {
        when(deploymentContext.getComponent(Api.class)).thenReturn(
            anApiWithServiceDiscovery(Service.builder().enabled(false).type(CONSUL_SERVICE_DISCOVERY_ID).build())
        );

        var result = new ConsulServiceDiscoveryServiceFactory().createService(deploymentContext);

        Assertions.assertThat(result).isNull();
    }

    @Test
    void should_not_create_the_service_when_discovery_service_not_match() {
        when(deploymentContext.getComponent(Api.class)).thenReturn(
            anApiWithServiceDiscovery(Service.builder().enabled(true).type("other").build())
        );

        var result = new ConsulServiceDiscoveryServiceFactory().createService(deploymentContext);

        Assertions.assertThat(result).isNull();
    }

    @Test
    void should_not_create_the_service_when_no_discovery_service_defined_in_group() {
        when(deploymentContext.getComponent(Api.class)).thenReturn(anApiWithServiceDiscovery(null));

        var result = new ConsulServiceDiscoveryServiceFactory().createService(deploymentContext);

        Assertions.assertThat(result).isNull();
    }

    @Test
    void should_not_create_the_service_when_no_group_defined() {
        when(deploymentContext.getComponent(Api.class)).thenReturn(new Api(anApiV4()));

        var result = new ConsulServiceDiscoveryServiceFactory().createService(deploymentContext);

        Assertions.assertThat(result).isNull();
    }
}
