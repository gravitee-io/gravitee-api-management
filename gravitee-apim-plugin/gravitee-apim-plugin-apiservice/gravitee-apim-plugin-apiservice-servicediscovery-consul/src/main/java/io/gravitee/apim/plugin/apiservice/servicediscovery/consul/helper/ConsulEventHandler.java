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

import io.gravitee.apim.plugin.apiservice.servicediscovery.consul.ConsulServiceDiscoveryServiceConfiguration;
import io.gravitee.apim.plugin.apiservice.servicediscovery.consul.factory.EndpointFactory;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.gateway.reactive.core.v4.endpoint.EndpointManager;
import io.vertx.ext.consul.ServiceEntry;
import io.vertx.ext.consul.ServiceEntryList;
import io.vertx.ext.consul.WatchResult;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConsulEventHandler {

    private final EndpointManager endpointManager;
    private final EndpointGroup group;
    private final ConsulServiceDiscoveryServiceConfiguration configuration;

    public ConsulEventHandler(
        EndpointManager endpointManager,
        EndpointGroup group,
        ConsulServiceDiscoveryServiceConfiguration configuration
    ) {
        this.endpointManager = endpointManager;
        this.group = group;
        this.configuration = configuration;
    }

    public void handle(WatchResult<ServiceEntryList> event) {
        if (event.succeeded()) {
            log.debug("Receiving a Consul.io watch event for service: name[{}]", configuration.getService());

            handleUnRegisteredServices(event.nextResult(), event.prevResult());
            handleNewOrUpdatedServices(event.nextResult().getList());
        } else {
            log.error("Unexpected error while watching services catalog", event.cause());
        }
    }

    private void handleNewOrUpdatedServices(List<ServiceEntry> entries) {
        entries.forEach(entry -> {
            var service = entry.getService();
            service.setNodeAddress(entry.getNode().getAddress());

            var endpoint = EndpointFactory.build(group, service);
            endpointManager.addOrUpdateEndpoint(group.getName(), endpoint);
        });
    }

    private void handleUnRegisteredServices(ServiceEntryList newEntries, ServiceEntryList previousEntries) {
        if (previousEntries != null) {
            var old = previousEntries.getList();
            if (old.size() > newEntries.getList().size()) {
                old.removeAll(newEntries.getList());
                old.forEach(oldEntry -> endpointManager.removeEndpoint(EndpointFactory.endpointName(oldEntry.getService())));
            }
        }
    }
}
