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
package io.gravitee.apim.plugin.apiservice.servicediscovery.kubernetes.helper;

import static io.gravitee.apim.plugin.apiservice.servicediscovery.kubernetes.KubernetesServiceDiscoveryService.KUBERNETES_SERVICE_DISCOVERY_ID;

import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.endpointgroup.service.EndpointGroupServices;
import io.gravitee.definition.model.v4.service.Service;

public class KubernetesServiceDiscoveryChecker {

    private KubernetesServiceDiscoveryChecker() {}

    public static boolean canHandle(final Api api) {
        return api
            .getEndpointGroups()
            .stream()
            .anyMatch(endpointGroup -> isKubernetesEnabled(endpointGroup.getServices()));
    }

    public static boolean isKubernetesEnabled(EndpointGroupServices services) {
        return services != null && isServiceEnabled(services.getDiscovery());
    }

    public static boolean isServiceEnabled(final Service discoveryService) {
        return (
            discoveryService != null && discoveryService.isEnabled() && KUBERNETES_SERVICE_DISCOVERY_ID.equals(discoveryService.getType())
        );
    }
}
