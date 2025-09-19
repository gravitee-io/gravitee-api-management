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
package io.gravitee.apim.plugin.apiservice.servicediscovery.consul.helper;

import static io.gravitee.apim.plugin.apiservice.servicediscovery.consul.ConsulServiceDiscoveryService.CONSUL_SERVICE_DISCOVERY_ID;
import static java.util.Objects.isNull;

import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.endpointgroup.service.EndpointGroupServices;
import io.gravitee.definition.model.v4.service.Service;

public class ConsulServiceDiscoveryChecker {

    private ConsulServiceDiscoveryChecker() {}

    /**
     * Consul Service Discovery service is enabled when it is configured at the group level
     * @param api The api definition
     * @return true if the service needs to be initialized
     */
    public static boolean canHandle(final Api api) {
        return api
            .getEndpointGroups()
            .stream()
            .anyMatch(endpointGroup -> isConsulEnabled(endpointGroup.getServices()));
    }

    public static boolean isConsulEnabled(EndpointGroupServices services) {
        return !isNull(services) && isServiceEnabled(services.getDiscovery());
    }

    public static boolean isServiceEnabled(final Service discoveryService) {
        return discoveryService != null && discoveryService.isEnabled() && CONSUL_SERVICE_DISCOVERY_ID.equals(discoveryService.getType());
    }
}
