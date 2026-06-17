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

import static io.gravitee.gateway.reactive.reactor.v4.secrets.ApiV4DefinitionSecretRefsFinder.API_V4_DEFINITION_KIND;
import static io.gravitee.gateway.reactive.reactor.v4.secrets.NativeApiV4DefinitionSecretRefsFinder.NATIVE_API_V4_DEFINITION_KIND;

import io.gravitee.common.event.EventManager;
import io.gravitee.common.util.DataEncryptor;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.Plugin;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.handlers.api.event.ApiProductChangedEvent;
import io.gravitee.gateway.handlers.api.event.ApiProductEventType;
import io.gravitee.gateway.handlers.api.manager.ActionOnApi;
import io.gravitee.gateway.handlers.api.manager.ApiManager;
import io.gravitee.gateway.handlers.api.manager.Deployer;
import io.gravitee.gateway.handlers.api.manager.deployer.ApiDeployer;
import io.gravitee.gateway.handlers.api.registry.ApiProductRegistry;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.gateway.reactor.ReactorEvent;
import io.gravitee.node.api.license.ForbiddenFeatureException;
import io.gravitee.node.api.license.InvalidLicenseException;
import io.gravitee.node.api.license.LicenseManager;
import io.gravitee.secrets.api.discovery.Definition;
import io.gravitee.secrets.api.discovery.DefinitionMetadata;
import io.gravitee.secrets.api.event.SecretDiscoveryEvent;
import io.gravitee.secrets.api.event.SecretDiscoveryEventType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import lombok.CustomLog;
import org.slf4j.MDC;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
public class ApiManagerImpl implements ApiManager {

    private static final int PARALLELISM = Runtime.getRuntime().availableProcessors() * 2;

    private static final Set<String> SUPPORTED_SECRET_DEFINITION_KINDS = Set.of(API_V4_DEFINITION_KIND, NATIVE_API_V4_DEFINITION_KIND);

    private final EventManager eventManager;
    private final GatewayConfiguration gatewayConfiguration;
    private final LicenseManager licenseManager;
    private final Map<String, ReactableApi<?>> apis = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Lock> apiLocks = new ConcurrentHashMap<>();
    private final Map<Class<? extends ReactableApi<?>>, ? extends Deployer<?>> deployers;
    private final ApiProductRegistry apiProductRegistry;

    public ApiManagerImpl(
        final EventManager eventManager,
        final GatewayConfiguration gatewayConfiguration,
        LicenseManager licenseManager,
        final DataEncryptor dataEncryptor
    ) {
        this(eventManager, gatewayConfiguration, licenseManager, dataEncryptor, null);
    }

    public ApiManagerImpl(
        final EventManager eventManager,
        final GatewayConfiguration gatewayConfiguration,
        LicenseManager licenseManager,
        final DataEncryptor dataEncryptor,
        final ApiProductRegistry apiProductRegistry
    ) {
        this.eventManager = eventManager;
        this.gatewayConfiguration = gatewayConfiguration;
        this.licenseManager = licenseManager;
        this.apiProductRegistry = apiProductRegistry;
        deployers = Map.ofEntries(
            Map.entry(Api.class, new ApiDeployer(gatewayConfiguration, dataEncryptor)),
            Map.entry(
                io.gravitee.gateway.reactive.handlers.api.v4.Api.class,
                new io.gravitee.gateway.reactive.handlers.api.v4.deployer.ApiDeployer(gatewayConfiguration, dataEncryptor)
            ),
            Map.entry(
                io.gravitee.gateway.reactive.handlers.api.v4.NativeApi.class,
                new io.gravitee.gateway.reactive.handlers.api.v4.deployer.NativeApiDeployer(gatewayConfiguration, dataEncryptor)
            ),
            Map.entry(
                io.gravitee.gateway.reactive.handlers.api.v4.EdgeApi.class,
                new io.gravitee.gateway.reactive.handlers.api.v4.deployer.EdgeApiDeployer(gatewayConfiguration, dataEncryptor)
            )
        );

        subscribeToSecretDiscoveryEvents();
        subscribeToApiProductEventsIfNeeded();
    }

    private void subscribeToSecretDiscoveryEvents() {
        eventManager.subscribeForEvents(
            event -> {
                if (event.content() instanceof SecretDiscoveryEvent secretDiscoveryEvent) {
                    onSecretDiscoveryValueChanged(secretDiscoveryEvent);
                }
            },
            SecretDiscoveryEventType.VALUE_CHANGED
        );
    }

    private void onSecretDiscoveryValueChanged(SecretDiscoveryEvent secretDiscoveryEvent) {
        if (!(secretDiscoveryEvent.definition() instanceof Definition definition)) {
            return;
        }
        if (!SUPPORTED_SECRET_DEFINITION_KINDS.contains(definition.kind())) {
            log.debug(
                "Received SecretDiscoveryEvent for definition {} with kind {}, but we only handle API V4 and Native API definitions",
                definition.id(),
                definition.kind()
            );
            return;
        }
        updateApiOnSecretChange(definition);
    }

    private void updateApiOnSecretChange(Definition definition) {
        ReactableApi<?> api = apis.get(definition.id());
        if (api == null) {
            log.trace(
                "Received SecretDiscoveryEvent for API {}, but it is not found in the API manager. Unable to update it",
                definition.id()
            );
            return;
        }
        MDC.put("api", api.getId());
        try {
            log.info("Secret value changed for API {}, updating it", definition.id());
            eventManager.publishEvent(ReactorEvent.UPDATE, api);
            log.info("{} has been updated", api);
        } finally {
            MDC.remove("api");
        }
    }

    private void subscribeToApiProductEventsIfNeeded() {
        if (apiProductRegistry == null) {
            return;
        }
        eventManager.subscribeForEvents(
            event -> {
                if (event.content() instanceof ApiProductChangedEvent productEvent) {
                    onApiProductUndeploy(productEvent);
                }
            },
            ApiProductEventType.UNDEPLOY
        );
    }

    private void onApiProductUndeploy(ApiProductChangedEvent productEvent) {
        if (productEvent.getApiIds() == null) {
            return;
        }
        reEvaluateApisAfterProductChange(productEvent.getProductId(), productEvent.getApiIds());
    }

    private boolean register(ReactableApi<?> api, boolean force) {
        return withPerApiLock(api.getId(), () -> doRegister(api, force));
    }

    private boolean doRegister(ReactableApi<?> api, boolean force) {
        ReactableApi<?> deployedApi = get(api.getId());

        List<Plugin> plugins;
        if (api.getDefinitionVersion() == DefinitionVersion.V4) {
            plugins = ((io.gravitee.definition.model.v4.AbstractApi) api.getDefinition()).getPlugins();
        } else {
            plugins = ((io.gravitee.definition.model.Api) api.getDefinition()).getPlugins();
        }

        try {
            licenseManager.validatePluginFeatures(
                api.getOrganizationId(),
                plugins
                    .stream()
                    .map(p -> new LicenseManager.Plugin(p.type(), p.id()))
                    .collect(Collectors.toSet())
            );
        } catch (InvalidLicenseException | ForbiddenFeatureException e) {
            log.warn("The API {} could not be deployed because it is not allowed by the current license", api.getName(), e);
            return false;
        }

        if (isApiShardEligible(api)) {
            boolean apiToDeploy = deployedApi == null || force;
            boolean apiToUpdate =
                !apiToDeploy &&
                (deployedApi.getDeployedAt().before(api.getDeployedAt()) && !Objects.equals(deployedApi.getRevision(), api.getRevision()));

            if (apiToDeploy || apiToUpdate) {
                Deployer deployer = deployers.get(api.getClass());
                deployer.initialize(api);
            }

            if (apiToDeploy) {
                deploy(api);
                return true;
            } else if (apiToUpdate) {
                update(api);
                return true;
            }
        } else {
            log.debug("The API {} has been ignored because not in configured tags {}", api.getName(), api.getTags());

            if (deployedApi != null) {
                undeploy(api.getId());
            }
        }

        return false;
    }

    @Override
    public ActionOnApi requiredActionFor(final ReactableApi<?> reactableApi) {
        ReactableApi<?> deployedApi = get(reactableApi.getId());
        if (!reactableApi.enabled()) {
            log.debug("requiredActionFor [{}]: UNDEPLOY (disabled)", reactableApi.getId());
            return ActionOnApi.UNDEPLOY;
        }

        if (isApiShardEligible(reactableApi)) {
            boolean apiToDeploy = deployedApi == null;
            boolean apiToUpdate =
                !apiToDeploy &&
                (deployedApi.getDeployedAt().before(reactableApi.getDeployedAt()) &&
                    !Objects.equals(deployedApi.getRevision(), reactableApi.getRevision()));

            if (apiToDeploy || apiToUpdate) {
                log.debug(
                    "requiredActionFor [{}]: DEPLOY (shard-eligible, toDeploy={}, toUpdate={})",
                    reactableApi.getId(),
                    apiToDeploy,
                    apiToUpdate
                );
                return ActionOnApi.DEPLOY;
            }
            log.debug("requiredActionFor [{}]: NONE (shard-eligible but already up-to-date)", reactableApi.getId());
        } else if (deployedApi != null) {
            log.debug("requiredActionFor [{}]: UNDEPLOY (no longer eligible)", reactableApi.getId());
            return ActionOnApi.UNDEPLOY;
        } else {
            log.debug(
                "requiredActionFor [{}]: NONE — not shard-eligible (tags={}, env={})",
                reactableApi.getId(),
                reactableApi.getTags(),
                reactableApi.getEnvironmentId()
            );
        }
        return ActionOnApi.NONE;
    }

    @Override
    public boolean register(ReactableApi api) {
        return register(api, false);
    }

    @Override
    public void unregister(String apiId) {
        withPerApiLock(apiId, () -> undeploy(apiId));
    }

    @Override
    public void refresh() {
        if (apis.isEmpty()) {
            return;
        }

        final long begin = System.currentTimeMillis();

        List<ReactableApi<?>> allApis = new ArrayList<>(apis.values());

        log.info("Starting apis refresh. {} deployed apis to be refreshed.", apis.size());

        if (allApis.isEmpty()) {
            return;
        }

        final ExecutorService refreshAllExecutor = createExecutor(Math.min(PARALLELISM, allApis.size()));

        final List<Callable<Boolean>> toInvoke = allApis
            .stream()
            .map(api -> ((Callable<Boolean>) () -> register(api, true)))
            .collect(Collectors.toList());

        try {
            refreshAllExecutor.invokeAll(toInvoke);
            refreshAllExecutor.shutdown();
            while (!refreshAllExecutor.awaitTermination(100, TimeUnit.MILLISECONDS));
        } catch (InterruptedException e) {
            log.error("Unable to refresh apis", e);
            Thread.currentThread().interrupt();
        } finally {
            refreshAllExecutor.shutdown();
        }

        log.info("Apis refresh done in {}ms", (System.currentTimeMillis() - begin));
    }

    private void deploy(ReactableApi api) {
        MDC.put("api", api.getId());
        log.debug("Deployment of {}", api);

        if (api.enabled()) {
            Deployer deployer = deployers.get(api.getClass());
            List<String> plans = deployer.getPlans(api);
            boolean includedInApiProduct = isIncludedInApiProductWithPlans(api);

            // Deploy the API if it has at least one plan or is exposed via an API Product (no plans required)
            if (!plans.isEmpty() || includedInApiProduct) {
                log.debug("Deploying {} (has plans or included in API Product)", api);
                eventManager.publishEvent(
                    SecretDiscoveryEventType.DISCOVER,
                    new SecretDiscoveryEvent(api.getEnvironmentId(), api.getDefinition(), new DefinitionMetadata(api.getRevision()))
                );
                apis.put(api.getId(), api);
                eventManager.publishEvent(ReactorEvent.DEPLOY, api);
                log.info("{} has been deployed", api);
            } else {
                log.warn("There is no published plan associated to this API, skipping deployment...");
            }
        } else {
            log.debug("{} is not enabled. Skip deployment.", api);
        }

        MDC.remove("api");
    }

    private ExecutorService createExecutor(int threadCount) {
        return Executors.newFixedThreadPool(
            threadCount,
            new ThreadFactory() {
                private int counter = 0;

                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, "gio.api-manager-" + counter++);
                }
            }
        );
    }

    private void update(ReactableApi<?> api) {
        MDC.put("api", api.getId());
        log.debug("Updating {}", api);

        Deployer deployer = deployers.get(api.getClass());
        List<String> plans = deployer.getPlans(api);
        boolean includedInApiProduct = isIncludedInApiProductWithPlans(api);

        if (api.enabled() && (!plans.isEmpty() || includedInApiProduct)) {
            log.debug("Updating {} (has plans or included in API Product)", api);
            eventManager.publishEvent(
                SecretDiscoveryEventType.DISCOVER,
                new SecretDiscoveryEvent(api.getEnvironmentId(), api.getDefinition(), new DefinitionMetadata(api.getRevision()))
            );

            ReactableApi<?> previousApi = apis.get(api.getId());

            apis.put(api.getId(), api);
            eventManager.publishEvent(ReactorEvent.UPDATE, api);

            if (previousApi != null) {
                // Revoke the previous API revision
                eventManager.publishEvent(
                    SecretDiscoveryEventType.REVOKE,
                    new SecretDiscoveryEvent(
                        previousApi.getEnvironmentId(),
                        previousApi.getDefinition(),
                        new DefinitionMetadata(previousApi.getRevision())
                    )
                );
            }
            log.info("{} has been updated", api);
        } else if (plans.isEmpty() && !includedInApiProduct) {
            log.warn("There is no published plan associated to this API, undeploy it...");
            undeploy(api.getId());
        } else {
            log.info("API {} is disabled, undeploy it...", api.getId());
            undeploy(api.getId());
        }

        MDC.remove("api");
    }

    private boolean isApiShardEligible(ReactableApi<?> api) {
        if (gatewayConfiguration.hasMatchingTags(api.getTags())) {
            return true;
        }
        boolean productMatch = isIncludedInApiProductWithPlans(api);
        if (!productMatch) {
            log.debug(
                "API [{}] (tags={}) not shard-eligible: no matching gateway tags and no API Product plan entries found (env={})",
                api.getId(),
                api.getTags(),
                api.getEnvironmentId()
            );
        }
        return productMatch;
    }

    private boolean isIncludedInApiProductWithPlans(ReactableApi<?> api) {
        if (apiProductRegistry == null || api.getEnvironmentId() == null) {
            return false;
        }
        List<ApiProductRegistry.ApiProductPlanEntry> apiProductPlanEntries = apiProductRegistry.getApiProductPlanEntriesForApi(
            api.getId(),
            api.getEnvironmentId()
        );
        return apiProductPlanEntries != null && !apiProductPlanEntries.isEmpty();
    }

    @Override
    public void reEvaluateAfterProductChange(String productId, Set<String> apiIds) {
        reEvaluateApisAfterProductChange(productId, apiIds);
    }

    private void reEvaluateApisAfterProductChange(String productId, Set<String> apiIds) {
        log.debug("Re-evaluating {} API(s) after API Product [{}] change", apiIds.size(), productId);
        for (String apiId : apiIds) {
            withPerApiLock(apiId, () -> {
                ReactableApi<?> deployedApi = apis.get(apiId);
                if (deployedApi != null && !isApiShardEligible(deployedApi)) {
                    log.info("API [{}] no longer eligible after API Product [{}] change — undeploying", apiId, productId);
                    undeploy(apiId);
                }
            });
        }
    }

    private void withPerApiLock(String apiId, Runnable action) {
        withPerApiLock(apiId, () -> {
            action.run();
            return null;
        });
    }

    private <T> T withPerApiLock(String apiId, Callable<T> action) {
        Lock lock = apiLocks.computeIfAbsent(apiId, k -> new ReentrantLock());
        lock.lock();
        try {
            return action.call();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to execute locked action for API [" + apiId + "]", e);
        } finally {
            lock.unlock();
        }
    }

    private void undeploy(String apiId) {
        ReactableApi<?> currentApi = apis.remove(apiId);
        if (currentApi != null) {
            MDC.put("api", apiId);
            log.debug("Undeployment of {}", currentApi);
            eventManager.publishEvent(ReactorEvent.UNDEPLOY, currentApi);
            eventManager.publishEvent(
                SecretDiscoveryEventType.REVOKE,
                new SecretDiscoveryEvent(
                    currentApi.getEnvironmentId(),
                    currentApi.getDefinition(),
                    new DefinitionMetadata(currentApi.getRevision())
                )
            );
            log.info("{} has been undeployed", currentApi);
            MDC.remove("api");
        }
    }

    @Override
    public Collection<ReactableApi<?>> apis() {
        return apis.values();
    }

    @Override
    public ReactableApi<?> get(String name) {
        return apis.get(name);
    }
}
