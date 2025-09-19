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
package io.gravitee.rest.api.management.v2.rest.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import fixtures.ServiceFixture;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.services.discovery.EndpointDiscoveryService;
import io.gravitee.definition.model.services.dynamicproperty.DynamicPropertyService;
import io.gravitee.definition.model.services.dynamicproperty.http.HttpDynamicPropertyProviderConfiguration;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

public class ServiceMapperTest {

    private final ServiceMapper serviceMapper = Mappers.getMapper(ServiceMapper.class);

    @Test
    void shouldMapApiServicesV2ToServices() throws JsonProcessingException {
        var apiServicesV2 = ServiceFixture.anApiServicesV2();

        var services = serviceMapper.map(apiServicesV2);
        assertThat(services.getDynamicPropertyService().getName()).isEqualTo(DynamicPropertyService.SERVICE_KEY);
        assertThat(services.getDynamicPropertyService().isEnabled()).isEqualTo(apiServicesV2.getDynamicProperty().getEnabled());
        assertThat(services.getDynamicPropertyService().getProvider().name()).isEqualTo(
            apiServicesV2.getDynamicProperty().getProvider().name()
        );
        assertThat(services.getDynamicPropertyService().getSchedule()).isEqualTo(apiServicesV2.getDynamicProperty().getSchedule());
        assertThat(services.getDynamicPropertyService().getConfiguration()).isInstanceOf(HttpDynamicPropertyProviderConfiguration.class);
        var config = (HttpDynamicPropertyProviderConfiguration) services.getDynamicPropertyService().getConfiguration();
        assertThat(config.getUrl()).isEqualTo(
            apiServicesV2.getDynamicProperty().getConfiguration().getHttpDynamicPropertyProviderConfiguration().getUrl()
        );
        assertThat(config.getMethod().name()).isEqualTo(
            apiServicesV2.getDynamicProperty().getConfiguration().getHttpDynamicPropertyProviderConfiguration().getMethod().name()
        );
        assertThat(config.getSpecification()).isEqualTo(
            apiServicesV2.getDynamicProperty().getConfiguration().getHttpDynamicPropertyProviderConfiguration().getSpecification()
        );
        assertThat(config.getBody()).isEqualTo(
            apiServicesV2.getDynamicProperty().getConfiguration().getHttpDynamicPropertyProviderConfiguration().getBody()
        );
        assertThat(services.getDiscoveryService()).isNull();
        assertThat(services.getHealthCheckService()).isNotNull();
    }

    @Test
    void shouldMapEmptyApiServicesV2ToNull() {
        var apiServicesV2 = ServiceFixture.anApiServicesV2().toBuilder().dynamicProperty(null).healthCheck(null).build();

        var services = serviceMapper.map(apiServicesV2);
        assertThat(services).isNotNull();
        assertThat(services.isEmpty()).isTrue();
    }

    @Test
    void shouldMapEndpointGroupServicesV2ToServices() throws JsonProcessingException {
        var endpointGroupServicesV2ServicesV2 = ServiceFixture.anEndpointGroupServicesV2();

        var services = serviceMapper.map(endpointGroupServicesV2ServicesV2);
        assertThat(services.getDiscoveryService().getName()).isEqualTo(EndpointDiscoveryService.SERVICE_KEY);
        assertThat(services.getDiscoveryService().isEnabled()).isEqualTo(endpointGroupServicesV2ServicesV2.getDiscovery().getEnabled());
        assertThat(services.getDiscoveryService().getProvider()).isEqualTo(endpointGroupServicesV2ServicesV2.getDiscovery().getProvider());
        assertThat(services.getDiscoveryService().getConfiguration()).isEqualTo(
            new GraviteeMapper().writeValueAsString(endpointGroupServicesV2ServicesV2.getDiscovery().getConfiguration())
        );
        assertThat(services.getDynamicPropertyService()).isNull();
        assertThat(services.getHealthCheckService()).isNull();
    }

    @Test
    void shouldMapEmptyEndpointGroupServicesV2ToNull() {
        var endpointGroupServicesV2ServicesV2 = ServiceFixture.anEndpointGroupServicesV2().toBuilder().discovery(null).build();

        var services = serviceMapper.map(endpointGroupServicesV2ServicesV2);
        assertThat(services).isNotNull();
        assertThat(services.isEmpty()).isTrue();
    }

    @Test
    void shouldMapToServicesV4() throws JsonProcessingException {
        var serviceV4 = ServiceFixture.aServiceV4();

        var service = serviceMapper.map(serviceV4);
        assertThat(service.getType()).isEqualTo(serviceV4.getType());
        assertThat(service.getConfiguration()).isEqualTo(new GraviteeMapper().writeValueAsString(serviceV4.getConfiguration()));
        assertThat(service.isEnabled()).isEqualTo(serviceV4.getEnabled());
    }
}
