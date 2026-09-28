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
package io.gravitee.apim.infra.plugin;

import com.google.common.annotations.VisibleForTesting;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.rest.api.common.apiservices.DefaultManagementDeploymentContext;
import io.gravitee.apim.rest.api.common.apiservices.ManagementApiService;
import io.gravitee.apim.rest.api.common.apiservices.ManagementApiServiceFactory;
import io.gravitee.common.service.AbstractService;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.AbstractApi;
import io.gravitee.definition.model.v4.nativeapi.NativeApi;
import io.gravitee.plugin.apiservice.ApiServicePluginManager;
import io.reactivex.rxjava3.core.Completable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.CustomLog;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
@CustomLog
public class ManagementApiServicesManager extends AbstractService {

    private final ApiServicePluginManager apiServicePluginManager;

    @VisibleForTesting
    final Map<String, List<ManagementApiService>> servicesByApi = new ConcurrentHashMap<>();

    public ManagementApiServicesManager(ApplicationContext applicationContext, ApiServicePluginManager apiServicePluginManager) {
        this.apiServicePluginManager = apiServicePluginManager;
        this.applicationContext = applicationContext;
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
        log.info("Gracefully stopping ManagementApiServices");
        stopManagementApiServices(servicesByApi.values().stream().flatMap(Collection::stream));
    }

    @SuppressWarnings("java:S6204")
    public void deployServices(Api api) {
        log.debug("Deploying services for api: {}", api.getId());
        final DefaultManagementDeploymentContext deploymentContext = deploymentContext(api);
        List<ManagementApiService> services = deploymentContext == null
            ? List.of()
            : apiServicePluginManager
                .<ManagementApiServiceFactory<?>>getAllFactories(ManagementApiServiceFactory.class)
                .stream()
                .map(managementApiServiceFactory -> managementApiServiceFactory.createService(deploymentContext))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        Completable.concat(services.stream().map(ManagementApiService::start).collect(Collectors.toList()))
            .doOnError(throwable -> log.error("Unable to start management-api-service: {}", throwable.getMessage(), throwable))
            .blockingAwait();
        if (!services.isEmpty()) {
            servicesByApi.put(api.getId(), services);
        }
    }

    @SuppressWarnings("java:S6204")
    public void undeployServices(Api api) {
        log.debug("Undeploying services for api: {}", api.getId());
        final List<ManagementApiService> apiServices = servicesByApi.get(api.getId());
        if (apiServices != null && !apiServices.isEmpty()) {
            stopManagementApiServices(apiServices.stream());
        }

        servicesByApi.remove(api.getId());
    }

    public void startDynamicProperties(Api api) {
        if (!api.getDefinitionVersion().equals(DefinitionVersion.V4)) {
            return;
        }
        final DefaultManagementDeploymentContext deploymentContext = deploymentContext(api);
        if (deploymentContext == null) {
            return;
        }
        List<ManagementApiService> services = apiServicePluginManager
            .<ManagementApiServiceFactory<?>>getAllFactories(ManagementApiServiceFactory.class)
            .stream()
            .map(managementApiServiceFactory -> managementApiServiceFactory.createService(deploymentContext))
            .filter(Objects::nonNull)
            .filter(service -> "http-dynamic-properties".equals(service.id()))
            .collect(Collectors.toList());

        Completable.concat(services.stream().map(ManagementApiService::start).collect(Collectors.toList()))
            .doOnError(throwable -> log.error("Unable to start dynamic-api-service for api {}", api.getId(), throwable))
            .blockingAwait();
        if (!services.isEmpty()) {
            servicesByApi.computeIfAbsent(api.getId(), k -> new ArrayList<>()).addAll(services);
        }
    }

    @SuppressWarnings("java:S6204")
    public void updateServices(Api api) {
        log.debug("Restarting services for api: {}", api.getId());
        final AbstractApi updatedDefinition = v4Definition(api);
        final List<ManagementApiService> managedApi = servicesByApi.get(api.getId());
        if (updatedDefinition != null && managedApi != null && !managedApi.isEmpty()) {
            Completable.concat(
                managedApi
                    .stream()
                    .map(managementApiService -> managementApiService.update(updatedDefinition))
                    .collect(Collectors.toList())
            ).blockingAwait();
            return;
        }
        deployServices(api);
    }

    /**
     * Build a deployment context holding the V4 API definition, whatever the V4 API type is (HTTP or Native).
     *
     * @param api to build the context for
     * @return the deployment context, or null when the API is not a V4 one
     */
    private DefaultManagementDeploymentContext deploymentContext(Api api) {
        return switch (api.getApiDefinitionValue()) {
            case io.gravitee.definition.model.v4.Api v4Api -> new DefaultManagementDeploymentContext(v4Api, applicationContext);
            case NativeApi v4NativeApi -> new DefaultManagementDeploymentContext(v4NativeApi, applicationContext);
            case null, default -> null;
        };
    }

    /**
     * Get the V4 API definition whatever the V4 API type is (HTTP or Native), so that management API services are handed
     * the definition they are able to handle instead of a null one.
     *
     * @param api to extract the definition from
     * @return the V4 definition, or null when the API is not a V4 one
     */
    private static AbstractApi v4Definition(Api api) {
        return api.getApiDefinitionValue() instanceof AbstractApi v4Definition ? v4Definition : null;
    }

    private static void stopManagementApiServices(Stream<ManagementApiService> apiServices) {
        Completable.concat(apiServices.map(ManagementApiService::stop).collect(Collectors.toList())).blockingAwait();
    }

    public void stopDynamicProperties(Api api) {
        Optional.ofNullable(servicesByApi.get(api.getId()))
            .flatMap(services ->
                services
                    .stream()
                    .filter(service -> "http-dynamic-properties".equals(service.id()))
                    .findFirst()
            )
            .ifPresent(service -> service.stop().blockingAwait());
        if (servicesByApi.get(api.getId()) != null) {
            servicesByApi.get(api.getId()).removeIf(service -> "http-dynamic-properties".equals(service.id()));
        }
    }
}
