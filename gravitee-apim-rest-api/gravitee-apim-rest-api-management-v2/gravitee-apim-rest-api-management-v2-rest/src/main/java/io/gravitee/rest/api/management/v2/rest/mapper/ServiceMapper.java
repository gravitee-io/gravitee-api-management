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

import io.gravitee.definition.model.services.Services;
import io.gravitee.definition.model.services.dynamicproperty.DynamicPropertyProviderConfiguration;
import io.gravitee.definition.model.v4.nativeapi.NativeApiServices;
import io.gravitee.definition.model.v4.service.Service;
import io.gravitee.rest.api.management.v2.rest.model.*;
import java.util.Objects;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mapper(uses = { ConfigurationSerializationMapper.class })
public interface ServiceMapper {
    Logger logger = LoggerFactory.getLogger(io.gravitee.rest.api.management.v2.rest.mapper.ServiceMapper.class);
    ServiceMapper INSTANCE = Mappers.getMapper(ServiceMapper.class);

    @Mapping(target = "configuration", qualifiedByName = "serializeConfiguration")
    Service map(ServiceV4 serviceV4);

    @Mapping(target = "configuration", qualifiedByName = "deserializeConfiguration")
    ServiceV4 mapToV4(Service service);

    @Mapping(target = "dynamicPropertyService", source = "dynamicProperty")
    @Mapping(target = "healthCheckService", source = "healthCheck")
    Services map(ApiServicesV2 apiServicesV2);

    NativeApiServices mapToNativeApiServices(ApiServices service);
    io.gravitee.definition.model.v4.service.ApiServices mapToApiServices(ApiServices service);

    @Mapping(target = "dynamicProperty", source = "dynamicPropertyService")
    @Mapping(target = "healthCheck", source = "healthCheckService")
    ApiServicesV2 map(Services apiServicesV2);

    @Mapping(target = "discoveryService", source = "discovery")
    Services map(EndpointGroupServicesV2 endpointGroupServicesV2);

    @Mapping(target = "discovery", source = "discoveryService")
    EndpointGroupServicesV2 mapToEndpointGroupServices(Services endpointGroupServicesV2);

    @Mapping(target = "configuration", qualifiedByName = "toDynamicPropertyProviderConfiguration")
    @Mapping(target = "name", constant = io.gravitee.definition.model.services.dynamicproperty.DynamicPropertyService.SERVICE_KEY)
    io.gravitee.definition.model.services.dynamicproperty.DynamicPropertyService map(DynamicPropertyService dynamicPropertyService);

    @Mapping(target = "name", constant = io.gravitee.definition.model.services.healthcheck.HealthCheckService.SERVICE_KEY)
    io.gravitee.definition.model.services.healthcheck.HealthCheckService map(HealthCheckService healthCheckService);

    @Mapping(target = "configuration", qualifiedByName = "serializeConfiguration")
    @Mapping(target = "name", constant = io.gravitee.definition.model.services.discovery.EndpointDiscoveryService.SERVICE_KEY)
    io.gravitee.definition.model.services.discovery.EndpointDiscoveryService map(EndpointDiscoveryService endpointDiscoveryService);

    @Mapping(target = "configuration", qualifiedByName = "toDynamicPropertyServiceConfiguration")
    DynamicPropertyService map(io.gravitee.definition.model.services.dynamicproperty.DynamicPropertyService dynamicPropertyService);

    HealthCheckService map(io.gravitee.definition.model.services.healthcheck.HealthCheckService healthCheckService);

    @Mapping(target = "configuration", qualifiedByName = "deserializeConfiguration")
    EndpointDiscoveryService map(io.gravitee.definition.model.services.discovery.EndpointDiscoveryService endpointDiscoveryService);

    io.gravitee.definition.model.services.dynamicproperty.http.HttpDynamicPropertyProviderConfiguration map(
        HttpDynamicPropertyProviderConfiguration httpDynamicPropertyProviderConfiguration
    );

    HttpDynamicPropertyProviderConfiguration map(
        io.gravitee.definition.model.services.dynamicproperty.http.HttpDynamicPropertyProviderConfiguration httpDynamicPropertyProviderConfiguration
    );

    @Named("toDynamicPropertyProviderConfiguration")
    default DynamicPropertyProviderConfiguration mapToDynamicPropertyProviderConfiguration(
        DynamicPropertyServiceConfiguration configuration
    ) {
        if (Objects.isNull(configuration)) {
            return null;
        }
        if (configuration.getActualInstance() instanceof HttpDynamicPropertyProviderConfiguration) {
            return this.map(configuration.getHttpDynamicPropertyProviderConfiguration());
        }
        return null;
    }

    @Named("toDynamicPropertyServiceConfiguration")
    default DynamicPropertyServiceConfiguration mapToDynamicPropertyServiceConfiguration(
        DynamicPropertyProviderConfiguration configuration
    ) {
        if (
            Objects.nonNull(configuration) &&
            configuration instanceof io.gravitee.definition.model.services.dynamicproperty.http.HttpDynamicPropertyProviderConfiguration
        ) {
            var mappedConfiguration = this.map(
                (io.gravitee.definition.model.services.dynamicproperty.http.HttpDynamicPropertyProviderConfiguration) configuration
            );
            return new DynamicPropertyServiceConfiguration(mappedConfiguration);
        }
        return null;
    }
}
