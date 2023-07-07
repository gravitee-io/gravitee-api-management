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

import static java.util.stream.Collectors.toList;

import io.gravitee.apim.plugin.apiservice.servicediscovery.consul.helper.ConsulServiceDiscoveryChecker;
import io.gravitee.gateway.reactive.api.apiservice.ApiServiceFactory;
import io.gravitee.gateway.reactive.api.context.DeploymentContext;
import io.gravitee.gateway.reactive.handlers.api.v4.Api;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ConsulServiceDiscoveryServiceFactory implements ApiServiceFactory<ConsulServiceDiscoveryService> {

    @Override
    public ConsulServiceDiscoveryService createService(DeploymentContext deploymentContext) {
        final Api api = deploymentContext.getComponent(Api.class);

        var consulEnabledGroups = api
            .getDefinition()
            .getEndpointGroups()
            .stream()
            .filter(group -> ConsulServiceDiscoveryChecker.isConsulEnabled(group.getServices()))
            .collect(toList());

        if (!consulEnabledGroups.isEmpty()) {
            return new ConsulServiceDiscoveryService(deploymentContext, consulEnabledGroups);
        }

        return null;
    }
}
