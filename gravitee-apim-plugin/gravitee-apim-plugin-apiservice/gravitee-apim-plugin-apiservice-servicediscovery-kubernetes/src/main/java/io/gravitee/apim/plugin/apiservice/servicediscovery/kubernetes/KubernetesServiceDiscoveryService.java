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
package io.gravitee.apim.plugin.apiservice.servicediscovery.kubernetes;

import io.gravitee.apim.plugin.apiservice.servicediscovery.kubernetes.helper.KubernetesEventHandler;
import io.gravitee.apim.plugin.apiservice.servicediscovery.kubernetes.helper.KubernetesServiceDiscoveryServiceHelper;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.gateway.reactive.api.apiservice.ApiService;
import io.gravitee.gateway.reactive.api.context.DeploymentContext;
import io.gravitee.gateway.reactive.api.exception.PluginConfigurationException;
import io.gravitee.gateway.reactive.api.helper.PluginConfigurationHelper;
import io.gravitee.gateway.reactive.core.v4.endpoint.EndpointManager;
import io.gravitee.gateway.reactive.handlers.api.v4.Api;
import io.gravitee.kubernetes.client.KubernetesClient;
import io.gravitee.kubernetes.client.api.ResourceQuery;
import io.gravitee.kubernetes.client.api.WatchQuery;
import io.gravitee.kubernetes.client.exception.ResourceVersionNotFoundException;
import io.gravitee.kubernetes.client.impl.KubernetesClientV1Impl;
import io.gravitee.kubernetes.client.model.v1.Endpoints;
import io.gravitee.kubernetes.client.model.v1.Event;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.disposables.Disposable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.CustomLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

@CustomLog
public class KubernetesServiceDiscoveryService implements ApiService {

    private static final Logger log = LoggerFactory.getLogger(KubernetesServiceDiscoveryService.class);

    public static final String KUBERNETES_SERVICE_DISCOVERY_ID = "kubernetes-service-discovery";
    public static final String PENDING_REQUESTS_TIMEOUT_PROPERTY = "api.pending_requests_timeout";
    private static final String SERVICE_DISCOVERY_KIND = "service-discovery";

    private final Map<String, Disposable> watchers = new ConcurrentHashMap<>(1);
    private final Map<String, java.util.Set<String>> discoveredEndpoints = new ConcurrentHashMap<>(1);

    private final DeploymentContext deploymentContext;
    private final List<EndpointGroup> kubernetesEnabledGroups;

    private Api api;
    private PluginConfigurationHelper pluginConfigurationHelper;
    private EndpointManager endpointManager;
    private Environment environment;
    private KubernetesClient kubernetesClient;
    private long pendingRequestsTimeout;

    public KubernetesServiceDiscoveryService(DeploymentContext deploymentContext) {
        this.deploymentContext = deploymentContext;
        kubernetesEnabledGroups = new ArrayList<>();
    }

    public KubernetesServiceDiscoveryService(DeploymentContext deploymentContext, List<EndpointGroup> kubernetesEnabledGroups) {
        this.deploymentContext = deploymentContext;
        this.kubernetesEnabledGroups = kubernetesEnabledGroups;
    }

    @Override
    public String id() {
        return KUBERNETES_SERVICE_DISCOVERY_ID;
    }

    @Override
    public String kind() {
        return SERVICE_DISCOVERY_KIND;
    }

    @Override
    public Completable start() {
        api = deploymentContext.getComponent(Api.class);
        pluginConfigurationHelper = deploymentContext.getComponent(PluginConfigurationHelper.class);
        endpointManager = deploymentContext.getComponent(EndpointManager.class);
        environment = deploymentContext.getComponent(Environment.class);
        pendingRequestsTimeout = environment.getProperty(PENDING_REQUESTS_TIMEOUT_PROPERTY, Long.class, 10_000L);

        kubernetesClient = resolveKubernetesClient();

        log.info("Starting service discovery service for api {}.", api.getName());
        kubernetesEnabledGroups.forEach(this::startKubernetesWatcherFor);

        return Completable.complete();
    }

    @Override
    public Completable stop() {
        log.info("Stopping service discovery service for api {}.", api.getName());
        watchers.values().forEach(Disposable::dispose);
        discoveredEndpoints.clear();

        return Completable.complete();
    }

    private void startKubernetesWatcherFor(EndpointGroup group) {
        try {
            var config = readConfiguration(group);
            if (config == null || config.getService() == null || config.getService().isBlank()) {
                log.warn("Missing Kubernetes service name for group [{}] of api [{}]", group.getName(), api.getName());
                return;
            }

            watchers.computeIfAbsent(group.getName(), groupName -> initWatcher(group, config));
        } catch (PluginConfigurationException e) {
            log.warn("Unable to start Kubernetes Service Discovery for group [{}] of api [{}]", api.getName(), group.getName(), e);
        }
    }

    private KubernetesServiceDiscoveryServiceConfiguration readConfiguration(EndpointGroup group) throws PluginConfigurationException {
        return pluginConfigurationHelper.readConfiguration(
            KubernetesServiceDiscoveryServiceConfiguration.class,
            group.getServices().getDiscovery().getConfiguration()
        );
    }

    private KubernetesClient resolveKubernetesClient() {
        KubernetesClient client = deploymentContext.getComponent(KubernetesClient.class);
        return client != null ? client : new KubernetesClientV1Impl();
    }

    private Disposable initWatcher(EndpointGroup group, KubernetesServiceDiscoveryServiceConfiguration configuration) {
        String namespace = KubernetesServiceDiscoveryServiceHelper.resolveNamespace(configuration);
        KubernetesEventHandler handler = new KubernetesEventHandler(
            endpointManager,
            group,
            configuration,
            pendingRequestsTimeout,
            discoveredEndpoints
        );

        @SuppressWarnings("unchecked")
        ResourceQuery<Endpoints> listQuery = (ResourceQuery<Endpoints>) (ResourceQuery<?>) ResourceQuery.endpoints(
            namespace,
            configuration.getService()
        ).build();

        kubernetesClient
            .get(listQuery)
            .subscribe(
                endpoints -> handler.handleSnapshot(endpoints == null ? List.of() : List.of(endpoints)),
                throwable ->
                    log.warn(
                        "Unable to list Kubernetes endpoints for service [{}] in namespace [{}]",
                        configuration.getService(),
                        namespace,
                        throwable
                    )
            );

        WatchQuery<Event<Endpoints>> watchQuery = WatchQuery.endpoints()
            .namespace(namespace)
            .resource(configuration.getService())
            .allowWatchBookmarks(true)
            .build();

        return kubernetesClient
            .watch(watchQuery)
            .subscribe(handler::handle, throwable -> {
                if (throwable instanceof ResourceVersionNotFoundException) {
                    log.warn(
                        "Kubernetes resource version expired for service [{}] in namespace [{}]",
                        configuration.getService(),
                        namespace
                    );
                } else {
                    log.error(
                        "Error while watching Kubernetes endpoints for service [{}] in namespace [{}]",
                        configuration.getService(),
                        namespace,
                        throwable
                    );
                }
            });
    }
}
