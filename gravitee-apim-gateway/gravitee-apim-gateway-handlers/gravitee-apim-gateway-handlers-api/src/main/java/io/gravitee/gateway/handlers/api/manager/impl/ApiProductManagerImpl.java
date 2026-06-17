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
package io.gravitee.gateway.handlers.api.manager.impl;

import io.gravitee.common.event.EventManager;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.handlers.api.ReactableApiProduct;
import io.gravitee.gateway.handlers.api.event.ApiProductChangedEvent;
import io.gravitee.gateway.handlers.api.event.ApiProductEventType;
import io.gravitee.gateway.handlers.api.manager.ApiProductManager;
import io.gravitee.gateway.handlers.api.registry.ApiProductRegistry;
import io.gravitee.gateway.handlers.api.sharding.ApiProductShardingFilter;
import io.gravitee.node.api.license.LicenseManager;
import io.reactivex.rxjava3.core.Completable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.CustomLog;

/**
 * @author Arpit Mishra (arpit.mishra at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
public class ApiProductManagerImpl implements ApiProductManager {

    private final ApiProductRegistry apiProductRegistry;
    private final EventManager eventManager;
    private final LicenseManager licenseManager;
    private final GatewayConfiguration gatewayConfiguration;
    private final Map<String, ReactableApiProduct> apiProducts = new ConcurrentHashMap<>();

    public ApiProductManagerImpl(
        ApiProductRegistry apiProductRegistry,
        EventManager eventManager,
        LicenseManager licenseManager,
        GatewayConfiguration gatewayConfiguration
    ) {
        this.apiProductRegistry = apiProductRegistry;
        this.eventManager = eventManager;
        this.licenseManager = licenseManager;
        this.gatewayConfiguration = gatewayConfiguration;
    }

    @Override
    public void register(ReactableApiProduct apiProduct) {
        register(apiProduct, Completable.complete()).blockingAwait();
    }

    @Override
    public Completable register(ReactableApiProduct apiProduct, Completable doBeforeEmit) {
        return Completable.defer(() -> register(apiProduct, false, doBeforeEmit));
    }

    @Override
    public void unregister(String apiProductId) {
        undeploy(apiProductId);
    }

    @Override
    public ReactableApiProduct get(String apiProductId) {
        return apiProducts.get(apiProductId);
    }

    @Override
    public Collection<ReactableApiProduct> getApiProducts() {
        return apiProducts.values();
    }

    private Completable register(ReactableApiProduct apiProduct, boolean force, Completable doBeforeEmit) {
        // License check: API Products require universe tier
        var license = licenseManager.getOrganizationLicenseOrPlatform(apiProduct.getOrganizationId());
        if (!Objects.equals(license.getTier(), "universe")) {
            log.warn(
                "The API Product [{}] can not be deployed because it is not allowed by the current license (universe tier)",
                apiProduct.getName()
            );
            return Completable.complete();
        }

        if (!ApiProductShardingFilter.matchesProductTags(gatewayConfiguration, apiProduct.getTags())) {
            log.debug(
                "The API Product [{}] has been ignored because not in configured gateway tags {}",
                apiProduct.getName(),
                apiProduct.getTags()
            );
            if (get(apiProduct.getId()) != null) {
                undeploy(apiProduct.getId());
            }
            return Completable.complete();
        }

        // Get currently deployed API Product
        ReactableApiProduct deployedApiProduct = get(apiProduct.getId());

        boolean apiProductToDeploy = deployedApiProduct == null || force;
        boolean apiProductToUpdate =
            !apiProductToDeploy &&
            deployedApiProduct.getDeployedAt() != null &&
            apiProduct.getDeployedAt() != null &&
            deployedApiProduct.getDeployedAt().before(apiProduct.getDeployedAt());

        // Deploy new API Product
        if (apiProductToDeploy) {
            return deploy(apiProduct, doBeforeEmit);
        }

        // Update existing API Product
        if (apiProductToUpdate) {
            return update(apiProduct, doBeforeEmit);
        }

        return Completable.complete();
    }

    private Completable deploy(ReactableApiProduct apiProduct, Completable doBeforeEmit) {
        log.debug("Deploying API Product [{}]", apiProduct.getId());

        apiProducts.put(apiProduct.getId(), apiProduct);
        apiProductRegistry.register(apiProduct);

        Completable emit = Completable.fromRunnable(() -> {
            if (apiProduct.getApiIds() != null && !apiProduct.getApiIds().isEmpty()) {
                emitApiProductChangedEvent(ApiProductEventType.DEPLOY, apiProduct);
            }
            log.info("API Product [{}] has been deployed", apiProduct.getId());
        });
        return (doBeforeEmit != null ? doBeforeEmit : Completable.complete()).andThen(emit);
    }

    private Completable update(ReactableApiProduct apiProduct, Completable doBeforeEmit) {
        log.debug("Updating API Product [{}]", apiProduct.getId());

        ReactableApiProduct previousProduct = apiProducts.get(apiProduct.getId());
        Set<String> previousApiIds = previousProduct != null && previousProduct.getApiIds() != null
            ? previousProduct.getApiIds()
            : Set.of();

        apiProducts.put(apiProduct.getId(), apiProduct);
        apiProductRegistry.register(apiProduct);

        Completable emit = Completable.fromRunnable(() -> publishApiProductUpdateEvent(apiProduct, previousApiIds));
        return (doBeforeEmit != null ? doBeforeEmit : Completable.complete()).andThen(emit);
    }

    private void publishApiProductUpdateEvent(ReactableApiProduct apiProduct, Set<String> previousApiIds) {
        Set<String> allAffectedApiIds = new HashSet<>(previousApiIds);
        if (apiProduct.getApiIds() != null) {
            allAffectedApiIds.addAll(apiProduct.getApiIds());
        }
        if (!allAffectedApiIds.isEmpty()) {
            ApiProductChangedEvent event = new ApiProductChangedEvent(apiProduct.getId(), apiProduct.getEnvironmentId(), allAffectedApiIds);
            eventManager.publishEvent(ApiProductEventType.UPDATE, event);
        }
        log.info("API Product [{}] has been updated", apiProduct.getId());
    }

    private void undeploy(String apiProductId) {
        ReactableApiProduct currentApiProduct = apiProducts.remove(apiProductId);
        if (currentApiProduct != null) {
            log.debug("Undeploying API Product [{}] from environment [{}]", apiProductId, currentApiProduct.getEnvironmentId());

            apiProductRegistry.remove(apiProductId, currentApiProduct.getEnvironmentId());

            emitApiProductChangedEvent(ApiProductEventType.UNDEPLOY, currentApiProduct);

            log.info("API Product [{}] has been undeployed", apiProductId);
        } else {
            log.debug("API Product [{}] was not found for undeployment", apiProductId);
        }
    }

    /**
     * Emit API Product event to trigger security chain refresh in affected reactors.
     */
    private void emitApiProductChangedEvent(ApiProductEventType eventType, ReactableApiProduct apiProduct) {
        try {
            ApiProductChangedEvent event = new ApiProductChangedEvent(
                apiProduct.getId(),
                apiProduct.getEnvironmentId(),
                apiProduct.getApiIds()
            );
            eventManager.publishEvent(eventType, event);
            log.debug("Emitted {} event for API Product [{}]", eventType, apiProduct.getId());
        } catch (Exception e) {
            log.error(
                "Failed to emit {} event for API Product [{}] — security chain may be stale until next sync cycle",
                eventType,
                apiProduct.getId(),
                e
            );
        }
    }
}
