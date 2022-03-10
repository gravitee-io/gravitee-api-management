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
package io.gravitee.gateway.services.endpoint.discovery.verticle;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gravitee.common.event.Event;
import io.gravitee.common.event.EventListener;
import io.gravitee.common.event.EventManager;
import io.gravitee.definition.model.Endpoint;
import io.gravitee.definition.model.EndpointGroup;
import io.gravitee.definition.model.HttpClientSslOptions;
import io.gravitee.definition.model.services.discovery.EndpointDiscoveryService;
import io.gravitee.discovery.api.ServiceDiscovery;
import io.gravitee.discovery.api.service.Service;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.reactor.Reactable;
import io.gravitee.gateway.reactor.ReactorEvent;
import io.gravitee.gateway.services.endpoint.discovery.factory.ServiceDiscoveryFactory;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.discovery.ServiceDiscoveryPlugin;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import java.io.IOException;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class EndpointDiscoveryVerticle extends AbstractVerticle implements EventListener<ReactorEvent, Reactable> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EndpointDiscoveryVerticle.class);

    @Autowired
    private EventManager eventManager;

    @Autowired
    private ConfigurablePluginManager<ServiceDiscoveryPlugin> serviceDiscoveryPluginManager;

    @Autowired
    private ServiceDiscoveryFactory serviceDiscoveryFactory;

    private final Map<Api, List<ServiceDiscovery>> apiServiceDiscoveries = new HashMap<>();

    @Autowired
    private ObjectMapper mapper;

    @Override
    public void start(final Promise<Void> startPromise) {
        eventManager.subscribeForEvents(this, ReactorEvent.class);
        startPromise.complete();
    }

    @Override
    public void onEvent(Event<ReactorEvent, Reactable> event) {
        switch (event.type()) {
            case DEPLOY:
                lookupForServiceDiscovery((Api) event.content());
                break;
            case UNDEPLOY:
                stopServiceDiscovery((Api) event.content());
                break;
            case UPDATE:
                stopServiceDiscovery((Api) event.content());
                lookupForServiceDiscovery((Api) event.content());
                break;
        }
    }

    private void stopServiceDiscovery(Api api) {
        List<ServiceDiscovery> discoveries = apiServiceDiscoveries.remove(api);
        if (discoveries != null) {
            LOGGER.info("Stop service discovery for API id[{}] name[{}]", api.getId(), api.getName());
            discoveries.forEach(
                serviceDiscovery -> {
                    try {
                        serviceDiscovery.stop();
                    } catch (Exception ex) {
                        LOGGER.error("Unexpected error while stopping service discovery", ex);
                    }
                }
            );
        }
    }

    private void lookupForServiceDiscovery(Api api) {
        if (api.isEnabled()) {
            for (EndpointGroup group : api.getProxy().getGroups()) {
                EndpointDiscoveryService discoveryService = group.getServices().get(EndpointDiscoveryService.class);
                if (discoveryService != null && discoveryService.isEnabled()) {
                    startServiceDiscovery(api, group, discoveryService);
                }
            }
        }
    }

    private void startServiceDiscovery(final Api api, final EndpointGroup group, final EndpointDiscoveryService discoveryService) {
        LOGGER.info(
            "A discovery service is defined for API id[{}] name[{}] group[{}] type[{}]",
            api.getId(),
            api.getName(),
            group.getName(),
            discoveryService.getProvider()
        );
        ServiceDiscoveryPlugin serviceDiscoveryPlugin = serviceDiscoveryPluginManager.get(discoveryService.getProvider());
        if (serviceDiscoveryPlugin != null) {
            ServiceDiscovery serviceDiscovery = serviceDiscoveryFactory.create(serviceDiscoveryPlugin, discoveryService.getConfiguration());

            List<ServiceDiscovery> discoveries = apiServiceDiscoveries.getOrDefault(api, new ArrayList<>());
            discoveries.add(serviceDiscovery);
            apiServiceDiscoveries.put(api, discoveries);

            try {
                final Set<Endpoint> endpoints = group.getEndpoints() == null ? new HashSet<>() : group.getEndpoints();
                if (group.getEndpoints() == null) {
                    group.setEndpoints(endpoints);
                }
                serviceDiscovery.listen(
                    event -> {
                        LOGGER.info("Receiving a service discovery event id[{}] type[{}]", event.service().id(), event.type());
                        Endpoint endpoint = createEndpoint(event.service(), group);
                        switch (event.type()) {
                            case REGISTER:
                                endpoints.add(endpoint);
                                break;
                            case UNREGISTER:
                                endpoints.remove(endpoint);
                                break;
                        }
                    }
                );
            } catch (Exception ex) {
                LOGGER.error("An errors occurs while starting to listen from service discovery provider", ex);
            }
        } else {
            LOGGER.error(
                "No Service Discovery plugin found for type[{}] api[{}] group[{}]",
                discoveryService.getProvider(),
                api.getId(),
                group.getName()
            );
        }
    }

    private Endpoint createEndpoint(final Service service, final EndpointGroup group) {
        final String scheme = (service.scheme() != null) ? service.scheme() : (service.port() == 443 ? "https" : "http");
        final String basePath = (service.basePath() != null) ? service.basePath() : Service.DEFAULT_BASE_PATH;

        // : is forbidden thanks to https://github.com/gravitee-io/issues/issues/1939
        final String serviceName = "sd#" + service.id().replaceAll(":", "#");
        String target = scheme + "://" + service.host() + (service.port() > 0 ? ":" + service.port() : "") + basePath;

        io.gravitee.definition.model.Endpoint endpoint = new Endpoint(serviceName, target);

        endpoint.setConfiguration(getEndpointConfiguration(group, endpoint, scheme));

        return endpoint;
    }

    private String getEndpointConfiguration(EndpointGroup group, Endpoint endpoint, String scheme) {
        ObjectNode endpointNode = (ObjectNode) mapper.valueToTree(endpoint);

        if (Service.HTTPS_SCHEME.equalsIgnoreCase(scheme)) {
            HttpClientSslOptions groupHttpClientOptions = group.getHttpClientSslOptions();
            // If truststore is defined at the group level, let's use the configuration
            if (groupHttpClientOptions != null && !groupHttpClientOptions.isTrustAll() && groupHttpClientOptions.getTrustStore() != null) {
                endpointNode.putPOJO("ssl", groupHttpClientOptions);
            } else {
                // If SSL configuration has been done at the group level, let's use it
                // If not, made a proper configuration for the discovered endpoint
                groupHttpClientOptions = (groupHttpClientOptions != null) ? groupHttpClientOptions : new HttpClientSslOptions();
                groupHttpClientOptions.setTrustAll(true);
                endpointNode.putPOJO("ssl", groupHttpClientOptions);
            }
        }

        endpointNode.putPOJO("http", group.getHttpClientOptions());
        endpointNode.putPOJO("proxy", group.getHttpProxy());
        // FIXME: should be kept or remove ?
        // endpointNode.putPOJO("headers", group.getHeaders());
        return endpointNode.toString();
    }

    public void setMapper(ObjectMapper mapper) {
        this.mapper = mapper;
    }
}
