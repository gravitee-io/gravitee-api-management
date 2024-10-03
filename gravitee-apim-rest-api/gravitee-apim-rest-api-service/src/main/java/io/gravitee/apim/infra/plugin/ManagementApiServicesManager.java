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
import io.gravitee.plugin.apiservice.ApiServicePluginManager;
import io.reactivex.rxjava3.core.Completable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
@Slf4j
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
        if (!api.getDefinitionVersion().equals(DefinitionVersion.V4)) {
            return;
        }
        List<ManagementApiService> services = apiServicePluginManager
            .<ManagementApiServiceFactory<?>>getAllFactories(ManagementApiServiceFactory.class)
            .stream()
            .map(managementApiServiceFactory ->
                managementApiServiceFactory.createService(
                    // FIXME: Kafka Gateway - Manage properly NativeApi definition
                    new DefaultManagementDeploymentContext(api.getApiDefinitionV4(), applicationContext)
                )
            )
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        Completable
            .concat(services.stream().map(ManagementApiService::start).collect(Collectors.toList()))
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

    @SuppressWarnings("java:S6204")
    public void updateServices(Api api) {
        log.debug("Restarting services for api: {}", api.getId());
        final List<ManagementApiService> managedApi = servicesByApi.get(api.getId());
        if (managedApi != null && !managedApi.isEmpty()) {
            Completable
                .concat(
                    managedApi
                        .stream()
                        .map(managementApiService -> managementApiService.update(api.getApiDefinitionV4()))
                        .collect(Collectors.toList())
                )
                .blockingAwait();
            return;
        }
        deployServices(api);
    }

    private static void stopManagementApiServices(Stream<ManagementApiService> apiServices) {
        Completable.concat(apiServices.map(ManagementApiService::stop).collect(Collectors.toList())).blockingAwait();
    }
}
