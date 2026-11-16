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

import com.fasterxml.jackson.core.type.TypeReference;
import io.gravitee.common.event.EventManager;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.Plugin;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.plan.Plan;
import io.gravitee.definition.model.v4.resource.Resource;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.handlers.api.ReactableApiProduct;
import io.gravitee.gateway.handlers.api.event.ApiProductChangedEvent;
import io.gravitee.gateway.handlers.api.event.ApiProductEventType;
import io.gravitee.gateway.handlers.api.manager.ApiProductChange;
import io.gravitee.gateway.handlers.api.manager.ApiProductManager;
import io.gravitee.gateway.handlers.api.registry.ApiProductRegistry;
import io.gravitee.gateway.handlers.api.secrets.ApiProductSecretDiscovery;
import io.gravitee.gateway.handlers.api.sharding.ApiProductShardingFilter;
import io.gravitee.gateway.reactive.handlers.api.v4.product.ApiProductRuntimeRegistry;
import io.gravitee.gateway.reactive.handlers.api.v4.security.policy.SecurityPolicyFactory;
import io.gravitee.node.api.license.ForbiddenFeatureException;
import io.gravitee.node.api.license.InvalidLicenseException;
import io.gravitee.node.api.license.LicenseManager;
import io.gravitee.secrets.api.discovery.Definition;
import io.gravitee.secrets.api.event.SecretDiscoveryEvent;
import io.gravitee.secrets.api.event.SecretDiscoveryEventType;
import io.reactivex.rxjava3.core.Completable;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.CustomLog;

/**
 * @author Arpit Mishra (arpit.mishra at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
public class ApiProductManagerImpl implements ApiProductManager {

    private static final GraviteeMapper DEFINITION_MAPPER = new GraviteeMapper();
    private static final TypeReference<List<Plan>> PLANS = new TypeReference<>() {};
    private static final TypeReference<List<Flow>> FLOWS = new TypeReference<>() {};
    private static final TypeReference<List<Resource>> RESOURCES = new TypeReference<>() {};

    private final ApiProductRegistry apiProductRegistry;
    private final ApiProductRuntimeRegistry apiProductRuntimeRegistry;
    private final EventManager eventManager;
    private final LicenseManager licenseManager;
    private final GatewayConfiguration gatewayConfiguration;
    private final ApiProductSecretDiscovery secretDiscovery;
    private final Map<String, ReactableApiProduct> apiProducts = new ConcurrentHashMap<>();
    /**
     * The version each runtime was built from, which an index-only update does not change. It stays bound to its
     * secrets until a later runtime is committed, so it can still be put back after a failed update.
     */
    private final Map<String, ReactableApiProduct> runtimeVersions = new ConcurrentHashMap<>();

    public ApiProductManagerImpl(
        ApiProductRegistry apiProductRegistry,
        ApiProductRuntimeRegistry apiProductRuntimeRegistry,
        EventManager eventManager,
        LicenseManager licenseManager,
        GatewayConfiguration gatewayConfiguration,
        ApiProductSecretDiscovery secretDiscovery
    ) {
        this.apiProductRegistry = apiProductRegistry;
        this.apiProductRuntimeRegistry = apiProductRuntimeRegistry;
        this.eventManager = eventManager;
        this.licenseManager = licenseManager;
        this.gatewayConfiguration = gatewayConfiguration;
        this.secretDiscovery = secretDiscovery;
        eventManager.subscribeForEvents(
            event -> {
                if (event.content() instanceof SecretDiscoveryEvent secretDiscoveryEvent) {
                    onSecretValueChanged(secretDiscoveryEvent);
                }
            },
            SecretDiscoveryEventType.VALUE_CHANGED
        );
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

        if (!pluginsAllowedByLicense(apiProduct)) {
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
        // Started before anything is stored, so a product whose runtime cannot start is never served.
        ReactableApiProduct runtimeVersion = startRuntime(apiProduct);
        apiProducts.put(apiProduct.getId(), apiProduct);
        apiProductRegistry.register(apiProduct);

        Completable emit = Completable.fromRunnable(() -> {
            commitRuntimeVersion(runtimeVersion);
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

        // Started before anything is stored, so an update whose runtime cannot start leaves the deployed version serving.
        ReactableApiProduct runtimeVersion = change.affectsMemberApis() ? startRuntime(apiProduct) : null;
        apiProducts.put(apiProduct.getId(), apiProduct);
        apiProductRegistry.register(apiProduct);

        Completable emit = Completable.fromRunnable(() -> {
            if (runtimeVersion != null) {
                commitRuntimeVersion(runtimeVersion);
            }
            publishApiProductUpdateEvent(apiProduct, change);
        });
        return doBeforeEmit
            .apply(change)
            .doOnError(e -> restoreAfterFailedUpdate(apiProduct, previousProduct, change))
            .andThen(emit);
    }

    /**
     * Secret discovery rewrites the references of the version a runtime is built from, so the runtime gets its own
     * copy: the received version stays as sent, and the next one is diffed against it rather than against rewritten
     * references that would always look changed. The copy holds only the plans served here, so this gateway is never
     * granted the secrets of a plan it does not serve. A rewritten reference names the revision it was granted under,
     * so a resource holding a secret never matches the previous runtime's and restarts on every runtime swap.
     */
    private ReactableApiProduct startRuntime(ReactableApiProduct apiProduct) {
        ReactableApiProduct runtimeVersion = copyOfDefinitions(apiProduct);
        apiProductRuntimeRegistry.deploy(runtimeVersion);
        return runtimeVersion;
    }

    private ReactableApiProduct copyOfDefinitions(ReactableApiProduct apiProduct) {
        try {
            return apiProduct
                .toBuilder()
                .plans(copyOf(plansServedHere(apiProduct), PLANS))
                .flows(copyOf(apiProduct.getFlows(), FLOWS))
                .resources(copyOf(apiProduct.getResources(), RESOURCES))
                .build();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to copy the definition of API Product [" + apiProduct.getId() + "] for its runtime", e);
        }
    }

    private static <T> List<T> copyOf(List<T> definitions, TypeReference<List<T>> type) throws IOException {
        return definitions == null ? null : DEFINITION_MAPPER.readValue(DEFINITION_MAPPER.writeValueAsBytes(definitions), type);
    }

    private void commitRuntimeVersion(ReactableApiProduct runtimeVersion) {
        secretDiscovery.bind(runtimeVersion);
        secretDiscovery.release(runtimeVersions.put(runtimeVersion.getId(), runtimeVersion));
    }

    private void onSecretValueChanged(SecretDiscoveryEvent secretDiscoveryEvent) {
        if (
            !(secretDiscoveryEvent.definition() instanceof Definition definition) || !API_PRODUCT_DEFINITION_KIND.equals(definition.kind())
        ) {
            return;
        }
        ReactableApiProduct apiProduct = apiProducts.get(definition.id());
        if (apiProduct == null || !Objects.equals(apiProduct.getEnvironmentId(), secretDiscoveryEvent.envId())) {
            log.trace("Received a secret value change for API Product [{}], which is not deployed here", definition.id());
            return;
        }
        log.info("Secret value changed for API Product [{}], rebuilding its runtime", definition.id());
        try {
            apiProductRuntimeRegistry.redeploy(apiProduct.getId(), apiProduct.getEnvironmentId());
        } catch (RuntimeException e) {
            log.error(
                "Unable to rebuild the runtime of API Product [{}] after a secret value change, its runtime is kept",
                definition.id(),
                e
            );
        }
    }

    /**
     * Checked on each deploy and update only, as for APIs: a product already deployed keeps running when the license
     * changes, and a refused version leaves the deployed one serving.
     */
    private boolean pluginsAllowedByLicense(ReactableApiProduct apiProduct) {
        try {
            licenseManager.validatePluginFeatures(apiProduct.getOrganizationId(), pluginsServedHere(apiProduct));
            return true;
        } catch (InvalidLicenseException | ForbiddenFeatureException e) {
            log.warn(
                "The API Product [{}] ({}) can not be deployed or updated because one of its plugins is not allowed by the current license",
                apiProduct.getName(),
                apiProduct.getId(),
                e
            );
            return false;
        }
    }

    private List<Plan> plansServedHere(ReactableApiProduct apiProduct) {
        return apiProduct.getPlans() == null
            ? null
            : apiProduct
                .getPlans()
                .stream()
                .filter(plan -> ApiProductShardingFilter.servesPlan(gatewayConfiguration, plan))
                .toList();
    }

    private Set<LicenseManager.Plugin> pluginsServedHere(ReactableApiProduct apiProduct) {
        List<Plan> plans = safe(plansServedHere(apiProduct));
        Stream<Plugin> securityPlugins = plans
            .stream()
            .filter(plan -> plan.getSecurity() != null && plan.getSecurity().getType() != null)
            .map(plan -> new Plugin("policy", SecurityPolicyFactory.policyName(plan.getSecurity())));
        Stream<Plugin> planFlowPlugins = plans.stream().flatMap(plan -> plan.getPlugins().stream());
        Stream<Plugin> productFlowPlugins = safe(apiProduct.getFlows())
            .stream()
            .filter(Flow::isEnabled)
            .flatMap(flow -> flow.getPlugins().stream());
        Stream<Plugin> resourcePlugins = safe(apiProduct.getResources())
            .stream()
            .filter(Objects::nonNull)
            .filter(Resource::isEnabled)
            .flatMap(resource -> resource.getPlugins().stream());
        return Stream.of(securityPlugins, planFlowPlugins, productFlowPlugins, resourcePlugins)
            .flatMap(plugins -> plugins)
            .map(plugin -> new LicenseManager.Plugin(plugin.type(), plugin.id()))
            .collect(Collectors.toSet());
    }

    private static <T> List<T> safe(List<T> list) {
        return list == null ? List.of() : list;
    }

    private ApiProductChange changeAgainst(ReactableApiProduct deployed, ReactableApiProduct incoming) {
        return ApiProductChange.between(deployed, incoming, plan -> ApiProductShardingFilter.servesPlan(gatewayConfiguration, plan));
    }

    /**
     * The next version is diffed against the stored one, so a version whose subscriptions failed to load must not stay
     * stored: its plans would be taken as loaded and never loaded again.
     */
    private void forgetFailedDeployment(ReactableApiProduct apiProduct) {
        if (replaceIfStillStored(apiProduct, null)) {
            log.warn("Deployment of API Product [{}] failed, it is not kept as deployed", apiProduct.getId());
            apiProductRegistry.remove(apiProduct.getId(), apiProduct.getEnvironmentId());
            apiProductRuntimeRegistry.remove(apiProduct.getId(), apiProduct.getEnvironmentId());
        }
    }

    private void restoreAfterFailedUpdate(ReactableApiProduct failedProduct, ReactableApiProduct previousProduct, ApiProductChange change) {
        if (replaceIfStillStored(failedProduct, previousProduct)) {
            log.warn("Update of API Product [{}] failed, its previous version is kept as deployed", previousProduct.getId());
            if (change.affectsMemberApis()) {
                putBackRuntime(previousProduct);
            }
            apiProductRegistry.register(previousProduct);
        }
    }

    /** From the version its runtime was built from, whose secrets are still granted until a later version is committed. */
    private void putBackRuntime(ReactableApiProduct previousProduct) {
        try {
            ReactableApiProduct runtimeVersion = runtimeVersions.get(previousProduct.getId());
            apiProductRuntimeRegistry.deploy(runtimeVersion != null ? runtimeVersion : copyOfDefinitions(previousProduct));
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
        ReactableApiProduct currentApiProduct = apiProducts.remove(apiProductId);
        if (currentApiProduct != null) {
            log.debug("Undeploying API Product [{}] from environment [{}]", apiProductId, currentApiProduct.getEnvironmentId());

            apiProductRegistry.remove(apiProductId, currentApiProduct.getEnvironmentId());
            apiProductRuntimeRegistry.remove(apiProductId, currentApiProduct.getEnvironmentId());
            secretDiscovery.release(runtimeVersions.remove(apiProductId));

            emitApiProductChangedEvent(ApiProductEventType.UNDEPLOY, currentApiProduct, Set.of());

            log.info("API Product [{}] has been undeployed", apiProductId);
        } else {
            log.debug("API Product [{}] was not found for undeployment", apiProductId);
        }
    }

    /**
     * Emit the API Product event, which resyncs the member APIs whose eligibility on this gateway may have changed.
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
                "Failed to emit {} event for API Product [{}] — member APIs may not be resynced until the next sync cycle",
                eventType,
                apiProduct.getId(),
                e
            );
        }
    }
}
