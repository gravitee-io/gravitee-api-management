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
package io.gravitee.apim.plugin.apiservice.servicediscovery.consul.factory;

import io.gravitee.apim.plugin.apiservice.servicediscovery.consul.factory.configuration.EndpointConfigurationFactory;
import io.gravitee.apim.plugin.apiservice.servicediscovery.consul.factory.configuration.HttpProxyEndpointConfigurationFactory;
import io.gravitee.definition.model.v4.endpointgroup.Endpoint;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.vertx.ext.consul.Service;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class EndpointFactory {

    private static final Map<String, Class<? extends EndpointConfigurationFactory>> configurationFactoryClasses = Map.of(
        HttpProxyEndpointConfigurationFactory.ENDPOINT_TYPE,
        HttpProxyEndpointConfigurationFactory.class
    );
    private static final Map<String, EndpointConfigurationFactory> factories = new HashMap<>();

    private EndpointFactory() {}

    public static Endpoint build(EndpointGroup group, Service service) {
        var type = group.getType();

        if (configurationFactoryClasses.containsKey(type)) {
            var endpoint = new Endpoint();

            endpoint.setName(endpointName(service));
            endpoint.setType(type);
            endpoint.setConfiguration(
                factories
                    .computeIfAbsent(type, t -> {
                        try {
                            return configurationFactoryClasses.get(t).getDeclaredConstructor().newInstance();
                        } catch (Exception e) {
                            throw new IllegalStateException("Unexpected error while instantiating Endpoint Configuration Factory class");
                        }
                    })
                    .buildConfiguration(service)
            );

            if (group.getSharedConfiguration() != null) {
                endpoint.setInheritConfiguration(true);
            }

            if (service.getMeta() != null) {
                Optional.ofNullable(service.getMeta().get(EndpointConfigurationFactory.CONSUL_METADATA_WEIGHT))
                    .map(Integer::parseInt)
                    .ifPresent(endpoint::setWeight);

                Optional.ofNullable(service.getMeta().get(EndpointConfigurationFactory.CONSUL_METADATA_TENANT))
                    .map(v -> Arrays.stream(v.split(",")).map(String::trim).collect(Collectors.toList()))
                    .ifPresent(endpoint::setTenants);
            }

            return endpoint;
        }

        throw new IllegalStateException(String.format("The endpoint type [%s] is not supported", type));
    }

    public static String endpointName(Service service) {
        return "consul#" + service.getId().replace(":", "#");
    }
}
