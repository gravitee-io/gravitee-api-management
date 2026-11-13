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

import static io.gravitee.gateway.handlers.api.secrets.ApiProductDefinitionSecretRefsFinder.API_PRODUCT_DEFINITION_KIND;

import io.gravitee.common.event.EventManager;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.handlers.api.ReactableApiProduct;
import io.gravitee.gateway.handlers.api.event.ApiProductChangedEvent;
import io.gravitee.gateway.handlers.api.event.ApiProductEventType;
import io.gravitee.gateway.handlers.api.manager.ApiProductChange;
import io.gravitee.gateway.handlers.api.manager.ApiProductManager;
import io.gravitee.gateway.handlers.api.registry.ApiProductRegistry;
import io.gravitee.gateway.handlers.api.sharding.ApiProductShardingFilter;
import io.gravitee.gateway.reactive.handlers.api.v4.product.ApiProductRuntimeRegistry;
import io.gravitee.node.api.license.LicenseManager;
import io.gravitee.secrets.api.discovery.Definition;
import io.gravitee.secrets.api.discovery.DefinitionMetadata;
import io.gravitee.secrets.api.event.SecretDiscoveryEvent;
import io.gravitee.secrets.api.event.SecretDiscoveryEventType;
import io.reactivex.rxjava3.core.Completable;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import lombok.CustomLog;

/**
 * @author Arpit Mishra (arpit.mishra at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
public class ApiProductManagerImpl implements ApiProductManager {

    private final ApiProductRegistry apiProductRegistry;
    private final ApiProductRuntimeRegistry apiProductRuntimeRegistry;
    private final EventManager eventManager;
    private final LicenseManager licenseManager;
    private final GatewayConfiguration gatewayConfiguration;
    private final Map<String, ReactableApiProduct> apiProducts = new ConcurrentHashMap<>();
    private final Object productLifecycleMonitor = new Object();

    public ApiProductManagerImpl(
        ApiProductRegistry apiProductRegistry,
        ApiProductRuntimeRegistry apiProductRuntimeRegistry,
        EventManager eventManager,
        LicenseManager licenseManager,
        GatewayConfiguration gatewayConfiguration
    ) {
        this.apiProductRegistry = apiProductRegistry;
        this.apiProductRuntimeRegistry = apiProductRuntimeRegistry;
        this.eventManager = eventManager;
        this.licenseManager = licenseManager;
        this.gatewayConfiguration = gatewayConfiguration;
        subscribeToSecretValueChanges();
    }

    private void subscribeToSecretValueChanges() {
        eventManager.subscribeForEvents(
            event -> {
                if (!(event.content() instanceof SecretDiscoveryEvent secretDiscoveryEvent)) {
                    return;
                }
                if (!(secretDiscoveryEvent.definition() instanceof Definition definition)) {
                    return;
                }
                if (!API_PRODUCT_DEFINITION_KIND.equals(definition.kind())) {
                    return;
                }
                redeployOnSecretValueChange(definition.id(), secretDiscoveryEvent.envId());
            },
            SecretDiscoveryEventType.VALUE_CHANGED
        );
    }

    private void redeployOnSecretValueChange(String productId, String environmentId) {
        synchronized (productLifecycleMonitor) {
            ReactableApiProduct product = get(productId);
            if (product == null) {
                log.trace("Received SecretDiscoveryEvent for API Product {}, but not found in manager", productId);
                return;
            }
            if (!Objects.equals(environmentId, product.getEnvironmentId())) {
                return;
            }
            log.info("Secret value changed for API Product [{}], refreshing its runtime", productId);
            try {
                apiProductRuntimeRegistry.deploy(product, false);
            } catch (RuntimeException e) {
                log.error("Unable to refresh the runtime of API Product [{}] after a secret value change", productId, e);
            }
        }
    }

    @Override
    public void register(ReactableApiProduct apiProduct) {
        register(apiProduct, change -> Completable.complete()).blockingAwait();
    }

    @Override
    public Completable register(ReactableApiProduct apiProduct, Function<ApiProductChange, Completable> doBeforeEmit) {
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

    private Completable register(ReactableApiProduct apiProduct, boolean force, Function<ApiProductChange, Completable> doBeforeEmit) {
        // License check: API Products require apim-api-products feature
        var license = licenseManager.getOrganizationLicenseOrPlatform(apiProduct.getOrganizationId());
        if (license == null || !license.isFeatureEnabled("apim-api-products")) {
            log.warn(
                "The API Product [{}] can not be deployed because it is not allowed by the current license (apim-api-products feature required)",
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

    private Completable deploy(ReactableApiProduct apiProduct, Function<ApiProductChange, Completable> doBeforeEmit) {
        log.debug("Deploying API Product [{}]", apiProduct.getId());

        ApiProductChange change = changeAgainst(null, apiProduct);
        synchronized (productLifecycleMonitor) {
            discoverSecrets(apiProduct);
            try {
                // Started before anything is stored, so a product whose runtime cannot start is never served.
                apiProductRuntimeRegistry.deploy(apiProduct);
            } catch (RuntimeException e) {
                revokeSecrets(apiProduct);
                throw e;
            }
            apiProducts.put(apiProduct.getId(), apiProduct);
            apiProductRegistry.register(apiProduct);
        }

        Completable emit = Completable.fromRunnable(() -> {
            if (apiProduct.getApiIds() != null && !apiProduct.getApiIds().isEmpty()) {
                emitApiProductChangedEvent(ApiProductEventType.DEPLOY, apiProduct, change.apiIdsToResync());
            }
            log.info("API Product [{}] has been deployed", apiProduct.getId());
        });
        return doBeforeEmit
            .apply(change)
            .doOnError(e -> forgetFailedDeployment(apiProduct))
            .andThen(emit);
    }

    private Completable update(ReactableApiProduct apiProduct, Function<ApiProductChange, Completable> doBeforeEmit) {
        log.debug("Updating API Product [{}]", apiProduct.getId());

        ReactableApiProduct previousProduct = apiProducts.get(apiProduct.getId());
        ApiProductChange change = changeAgainst(previousProduct, apiProduct);

        boolean rotatesSecrets = change.affectsMemberApis();
        synchronized (productLifecycleMonitor) {
            // Started before anything is stored, so an update whose runtime cannot start leaves the deployed version serving.
            if (rotatesSecrets) {
                discoverSecrets(apiProduct);
            }
            try {
                if (shouldRedeployRuntime(previousProduct, apiProduct, change)) {
                    Runnable revokePrevious = rotatesSecrets && previousProduct != null ? () -> revokeSecrets(previousProduct) : null;
                    apiProductRuntimeRegistry.deploy(apiProduct, true, revokePrevious);
                }
            } catch (RuntimeException e) {
                if (rotatesSecrets) {
                    revokeSecrets(apiProduct);
                }
                throw e;
            }
            apiProducts.put(apiProduct.getId(), apiProduct);
            apiProductRegistry.register(apiProduct);
        }

        Completable emit = Completable.fromRunnable(() -> publishApiProductUpdateEvent(apiProduct, change));
        return doBeforeEmit
            .apply(change)
            .doOnError(e -> restoreAfterFailedUpdate(apiProduct, previousProduct, change))
            .andThen(emit);
    }

    private ApiProductChange changeAgainst(ReactableApiProduct deployed, ReactableApiProduct incoming) {
        return ApiProductChange.between(deployed, incoming, plan -> ApiProductShardingFilter.servesPlan(gatewayConfiguration, plan));
    }

    /**
     * Member APIs rebuild their security chain when plans or API membership change. The product runtime must also
     * refresh when only index metadata changes but a new revision is published, so plan flows and product resources
     * do not keep resolving secrets from the previous runtime.
     */
    private static boolean shouldRedeployRuntime(
        ReactableApiProduct previousProduct,
        ReactableApiProduct apiProduct,
        ApiProductChange change
    ) {
        if (change.affectsMemberApis()) {
            return true;
        }
        return previousProduct != null && !Objects.equals(revisionOf(previousProduct), revisionOf(apiProduct));
    }

    /**
     * The next version is diffed against the stored one, so a version whose subscriptions failed to load must not stay
     * stored: its plans would be taken as loaded and never loaded again.
     */
    private void forgetFailedDeployment(ReactableApiProduct apiProduct) {
        if (replaceIfStillStored(apiProduct, null)) {
            log.warn("Deployment of API Product [{}] failed, it is not kept as deployed", apiProduct.getId());
            revokeSecrets(apiProduct);
            apiProductRegistry.remove(apiProduct.getId(), apiProduct.getEnvironmentId());
            apiProductRuntimeRegistry.remove(apiProduct.getId(), apiProduct.getEnvironmentId());
        }
    }

    private void restoreAfterFailedUpdate(ReactableApiProduct failedProduct, ReactableApiProduct previousProduct, ApiProductChange change) {
        if (replaceIfStillStored(failedProduct, previousProduct)) {
            log.warn("Update of API Product [{}] failed, its previous version is kept as deployed", previousProduct.getId());
            if (change.affectsMemberApis()) {
                revokeSecrets(failedProduct);
                putBackRuntime(previousProduct);
            }
            apiProductRegistry.register(previousProduct);
        }
    }

    private void putBackRuntime(ReactableApiProduct previousProduct) {
        try {
            apiProductRuntimeRegistry.deploy(previousProduct);
        } catch (RuntimeException e) {
            log.error(
                "Unable to put back the runtime of the deployed version of API Product [{}]: its plan flows follow the failed update until the next one",
                previousProduct.getId(),
                e
            );
        }
    }

    /** A version stored by a later register while this one was loading wins: it is left untouched. */
    private boolean replaceIfStillStored(ReactableApiProduct stored, ReactableApiProduct replacement) {
        AtomicBoolean replaced = new AtomicBoolean();
        apiProducts.computeIfPresent(stored.getId(), (id, current) -> {
            if (current != stored) {
                return current;
            }
            replaced.set(true);
            return replacement;
        });
        return replaced.get();
    }

    private void publishApiProductUpdateEvent(ReactableApiProduct apiProduct, ApiProductChange change) {
        if (!change.affectsMemberApis()) {
            log.info("API Product [{}] has been updated, no member API affected", apiProduct.getId());
            return;
        }
        Set<String> memberApiIds = change.memberApiIds();
        if (!memberApiIds.isEmpty()) {
            ApiProductChangedEvent event = new ApiProductChangedEvent(
                apiProduct.getId(),
                apiProduct.getEnvironmentId(),
                memberApiIds,
                change.apiIdsToResync()
            );
            eventManager.publishEvent(ApiProductEventType.UPDATE, event);
        }
        log.info("API Product [{}] has been updated", apiProduct.getId());
    }

    private void undeploy(String apiProductId) {
        ReactableApiProduct currentApiProduct;
        synchronized (productLifecycleMonitor) {
            currentApiProduct = apiProducts.remove(apiProductId);
            if (currentApiProduct != null) {
                log.debug("Undeploying API Product [{}] from environment [{}]", apiProductId, currentApiProduct.getEnvironmentId());

                revokeSecrets(currentApiProduct);
                apiProductRegistry.remove(apiProductId, currentApiProduct.getEnvironmentId());
                apiProductRuntimeRegistry.remove(apiProductId, currentApiProduct.getEnvironmentId());
            } else {
                log.debug("API Product [{}] was not found for undeployment", apiProductId);
                return;
            }
        }

        emitApiProductChangedEvent(ApiProductEventType.UNDEPLOY, currentApiProduct, Set.of());
        log.info("API Product [{}] has been undeployed", apiProductId);
    }

    /**
     * Emit API Product event to trigger security chain refresh in affected reactors.
     */
    private void emitApiProductChangedEvent(ApiProductEventType eventType, ReactableApiProduct apiProduct, Set<String> apiIdsToResync) {
        try {
            ApiProductChangedEvent event = new ApiProductChangedEvent(
                apiProduct.getId(),
                apiProduct.getEnvironmentId(),
                apiProduct.getApiIds(),
                apiIdsToResync
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

    private void discoverSecrets(ReactableApiProduct apiProduct) {
        eventManager.publishEvent(SecretDiscoveryEventType.DISCOVER, secretDiscoveryEventOf(apiProduct));
    }

    private void revokeSecrets(ReactableApiProduct apiProduct) {
        eventManager.publishEvent(SecretDiscoveryEventType.REVOKE, secretDiscoveryEventOf(apiProduct));
    }

    private static SecretDiscoveryEvent secretDiscoveryEventOf(ReactableApiProduct apiProduct) {
        return new SecretDiscoveryEvent(apiProduct.getEnvironmentId(), apiProduct, new DefinitionMetadata(revisionOf(apiProduct)));
    }

    private static String revisionOf(ReactableApiProduct apiProduct) {
        if (apiProduct.getDeployedAt() != null) {
            return String.valueOf(apiProduct.getDeployedAt().getTime());
        }
        return apiProduct.getVersion();
    }
}
