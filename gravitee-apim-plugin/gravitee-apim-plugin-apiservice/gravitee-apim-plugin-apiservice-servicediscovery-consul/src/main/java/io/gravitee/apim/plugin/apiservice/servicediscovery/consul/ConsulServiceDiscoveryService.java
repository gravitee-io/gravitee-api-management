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

import io.gravitee.apim.plugin.apiservice.servicediscovery.consul.helper.ConsulEventHandler;
import io.gravitee.apim.plugin.apiservice.servicediscovery.consul.helper.ConsulOptionsBuilder;
import io.gravitee.apim.plugin.apiservice.servicediscovery.consul.helper.ConsulServiceDiscoveryChecker;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.gateway.reactive.api.apiservice.ApiService;
import io.gravitee.gateway.reactive.api.context.DeploymentContext;
import io.gravitee.gateway.reactive.api.exception.PluginConfigurationException;
import io.gravitee.gateway.reactive.api.helper.PluginConfigurationHelper;
import io.gravitee.gateway.reactive.core.v4.endpoint.EndpointManager;
import io.gravitee.gateway.reactive.handlers.api.v4.Api;
import io.reactivex.rxjava3.core.Completable;
import io.vertx.ext.consul.ServiceEntryList;
import io.vertx.ext.consul.Watch;
import io.vertx.rxjava3.core.Vertx;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConsulServiceDiscoveryService implements ApiService {

    public static final String CONSUL_SERVICE_DISCOVERY_ID = "consul-service-discovery";

    private final Map<String, Watch<ServiceEntryList>> watchers = new ConcurrentHashMap<>(1);

    private final DeploymentContext deploymentContext;
    private final List<EndpointGroup> consulEnabledGroups;

    private Api api;
    private PluginConfigurationHelper pluginConfigurationHelper;
    private Vertx vertx;
    private EndpointManager endpointManager;

    public ConsulServiceDiscoveryService(DeploymentContext deploymentContext) {
        this.deploymentContext = deploymentContext;
        consulEnabledGroups = new ArrayList<>();
    }

    public ConsulServiceDiscoveryService(DeploymentContext deploymentContext, List<EndpointGroup> consulEnabledGroups) {
        this.deploymentContext = deploymentContext;
        this.consulEnabledGroups = consulEnabledGroups;
    }

    @Override
    public String id() {
        return CONSUL_SERVICE_DISCOVERY_ID;
    }

    @Override
    public String kind() {
        return "service-discovery";
    }

    @Override
    public Completable start() {
        api = deploymentContext.getComponent(Api.class);
        pluginConfigurationHelper = deploymentContext.getComponent(PluginConfigurationHelper.class);
        vertx = deploymentContext.getComponent(Vertx.class);
        endpointManager = deploymentContext.getComponent(EndpointManager.class);

        log.info("Starting service discovery service for api {}.", api.getName());

        consulEnabledGroups.forEach(this::startConsulWatcherFor);

        return Completable.complete();
    }

    @Override
    public Completable stop() {
        log.info("Stopping service discovery service for api {}.", api.getName());
        watchers.values().forEach(Watch::stop);

        return Completable.complete();
    }

    private void startConsulWatcherFor(EndpointGroup group) {
        try {
            var config = pluginConfigurationHelper.readConfiguration(
                ConsulServiceDiscoveryServiceConfiguration.class,
                group.getServices().getDiscovery().getConfiguration()
            );
            watchers.computeIfAbsent(group.getName(), groupName -> initWatcher(group, config));
        } catch (PluginConfigurationException e) {
            log.warn("Unable to start Consul Service Discovery for group [{}] of api [{}]", api.getName(), group.getName(), e);
        }
    }

    private Watch<ServiceEntryList> initWatcher(EndpointGroup group, ConsulServiceDiscoveryServiceConfiguration configuration) {
        var options = ConsulOptionsBuilder.from(configuration);

        Watch<ServiceEntryList> service = Watch.service(configuration.getService(), vertx.getDelegate(), options);
        service.setHandler(event -> new ConsulEventHandler(endpointManager, group, configuration).handle(event));
        return service.start();
    }
}
