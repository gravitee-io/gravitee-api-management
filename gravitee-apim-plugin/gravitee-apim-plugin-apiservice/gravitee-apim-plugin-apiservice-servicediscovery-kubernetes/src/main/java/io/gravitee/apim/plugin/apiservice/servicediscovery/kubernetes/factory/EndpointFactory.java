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
package io.gravitee.apim.plugin.apiservice.servicediscovery.kubernetes.factory;

import io.gravitee.apim.plugin.apiservice.servicediscovery.kubernetes.KubernetesServiceDiscoveryServiceConfiguration;
import io.gravitee.definition.model.v4.endpointgroup.Endpoint;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.kubernetes.client.model.v1.EndpointAddress;
import java.util.HashMap;
import java.util.Map;

public class EndpointFactory {

    private static final String ENDPOINT_TYPE_HTTP_PROXY = "http-proxy";
    private static final Map<String, EndpointConfigurationFactory> factories = new HashMap<>();

    private EndpointFactory() {}

    public static Endpoint build(
        EndpointGroup group,
        EndpointAddress address,
        int port,
        KubernetesServiceDiscoveryServiceConfiguration configuration
    ) {
        var type = group.getType();
        if (!ENDPOINT_TYPE_HTTP_PROXY.equals(type)) {
            throw new IllegalStateException(String.format("The endpoint type [%s] is not supported", type));
        }

        var endpoint = new Endpoint();
        endpoint.setName(endpointName(address, port));
        endpoint.setType(type);
        endpoint.setConfiguration(getFactory(type).buildConfiguration(address, port, configuration));

        if (group.getSharedConfiguration() != null) {
            endpoint.setInheritConfiguration(true);
        }

        return endpoint;
    }

    private static EndpointConfigurationFactory getFactory(String type) {
        return factories.computeIfAbsent(type, t -> new HttpProxyEndpointConfigurationFactory());
    }

    public static String endpointName(EndpointAddress address, int port) {
        return "kubernetes#" + address.getIp().replace(":", "#") + "#" + port;
    }
}
