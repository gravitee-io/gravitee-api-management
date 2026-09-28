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
package io.gravitee.gateway.handlers.sharedpolicygroup.manager.impl;

import io.gravitee.common.event.EventManager;
import io.gravitee.definition.model.Plugin;
import io.gravitee.gateway.handlers.sharedpolicygroup.ReactableSharedPolicyGroup;
import io.gravitee.gateway.handlers.sharedpolicygroup.event.SharedPolicyGroupEvent;
import io.gravitee.gateway.handlers.sharedpolicygroup.manager.SharedPolicyGroupManager;
import io.gravitee.node.api.license.ForbiddenFeatureException;
import io.gravitee.node.api.license.InvalidLicenseException;
import io.gravitee.node.api.license.LicenseManager;
import io.gravitee.secrets.api.discovery.Definition;
import io.gravitee.secrets.api.discovery.DefinitionMetadata;
import io.gravitee.secrets.api.event.SecretDiscoveryEvent;
import io.gravitee.secrets.api.event.SecretDiscoveryEventType;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.CustomLog;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
public class SharedPolicyGroupManagerImpl implements SharedPolicyGroupManager {

    private static final int PARALLELISM = Runtime.getRuntime().availableProcessors() * 2;
    private static final String SHARED_POLICY_GROUP_DEFINITION_KIND = "shared-policy-group";
    private final Map<SharedPolicyGroupKey, ReactableSharedPolicyGroup> sharedPolicyGroups = new ConcurrentHashMap<>();

    private final EventManager eventManager;
    private final LicenseManager licenseManager;

    public SharedPolicyGroupManagerImpl(EventManager eventManager, LicenseManager licenseManager) {
        this.eventManager = eventManager;
        this.licenseManager = licenseManager;

        eventManager.subscribeForEvents(
            event -> {
                if (!(event.content() instanceof SecretDiscoveryEvent secretDiscoveryEvent)) {
                    return;
                }
                if (!(secretDiscoveryEvent.definition() instanceof Definition definition)) {
                    return;
                }
                if (!SHARED_POLICY_GROUP_DEFINITION_KIND.equals(definition.kind())) {
                    return;
                }
                ReactableSharedPolicyGroup spg = sharedPolicyGroups.get(
                    new SharedPolicyGroupKey(definition.id(), secretDiscoveryEvent.envId())
                );
                if (spg == null) {
                    log.trace("Received SecretDiscoveryEvent for Shared Policy Group {}, but not found in manager", definition.id());
                    return;
                }
                log.info("Secret value changed for Shared Policy Group {}, updating it", definition.id());
                eventManager.publishEvent(SharedPolicyGroupEvent.UPDATE, spg);
            },
            SecretDiscoveryEventType.VALUE_CHANGED
        );
    }

    @Override
    public boolean register(ReactableSharedPolicyGroup sharedPolicyGroup) {
        return register(sharedPolicyGroup, false);
    }

    @Override
    public void unregister(String sharedPolicyGroupId, String environmentId) {
        undeploy(sharedPolicyGroupId, environmentId);
    }

    @Override
    public void unregisterAll(String sharedPolicyGroupId) {
        List<SharedPolicyGroupKey> keys = sharedPolicyGroups
            .keySet()
            .stream()
            .filter(key -> Objects.equals(key.sharedPolicyGroupId(), sharedPolicyGroupId))
            .toList();
        if (keys.isEmpty()) {
            log.warn("Shared Policy Group [{}] is not deployed in any environment", sharedPolicyGroupId);
            return;
        }
        keys.forEach(key -> undeploy(key.sharedPolicyGroupId(), key.environmentId()));
    }

    @Override
    public void refresh() {
        if (!sharedPolicyGroups.isEmpty()) {
            final long begin = System.currentTimeMillis();

            log.info("Starting shared policy groups refresh. {} shared policy groups to be refreshed.", sharedPolicyGroups.size());

            // Create an executor to parallelize a refresh for all the shared policy groups.
            final ExecutorService refreshAllExecutor = createExecutor(Math.min(PARALLELISM, sharedPolicyGroups.size()));

            final List<Callable<Boolean>> toInvoke = sharedPolicyGroups
                .values()
                .stream()
                .map(sharedPolicyGroup -> ((Callable<Boolean>) () -> register(sharedPolicyGroup, true)))
                .toList();

            try {
                refreshAllExecutor.invokeAll(toInvoke);
                refreshAllExecutor.shutdown();
                while (!refreshAllExecutor.awaitTermination(100, TimeUnit.MILLISECONDS));
            } catch (InterruptedException e) {
                log.error("Unable to refresh shared policy groups", e);
                Thread.currentThread().interrupt();
            } finally {
                refreshAllExecutor.shutdown();
            }

            log.info("Shared Policy Groups refresh done in {}ms", (System.currentTimeMillis() - begin));
        }
    }

    @Override
    public Collection<ReactableSharedPolicyGroup> sharedPolicyGroups() {
        return sharedPolicyGroups.values();
    }

    @Override
    public ReactableSharedPolicyGroup get(String sharedPolicyGroupId, String environmentId) {
        return sharedPolicyGroups.get(new SharedPolicyGroupKey(sharedPolicyGroupId, environmentId));
    }

    private boolean register(ReactableSharedPolicyGroup sharedPolicyGroup, boolean force) {
        // Get deployed Shared Policy Group. Copies that share a cross id in different environments are independent.
        ReactableSharedPolicyGroup deployedSharedPolicyGroup = get(sharedPolicyGroup.getId(), sharedPolicyGroup.getEnvironmentId());

        List<Plugin> plugins = sharedPolicyGroup.getDefinition().getPlugins();

        try {
            licenseManager.validatePluginFeatures(
                sharedPolicyGroup.getOrganizationId(),
                plugins
                    .stream()
                    .map(p -> new LicenseManager.Plugin(p.type(), p.id()))
                    .collect(Collectors.toSet())
            );
        } catch (InvalidLicenseException | ForbiddenFeatureException e) {
            log.warn(
                "The Shared Policy Group {} could not be deployed because it is not allowed by the current license",
                sharedPolicyGroup.getName(),
                e
            );
            return false;
        }

        boolean sharedPolicyGroupToDeploy = deployedSharedPolicyGroup == null || force;
        boolean sharedPolicyGroupToUpdate =
            !sharedPolicyGroupToDeploy && deployedSharedPolicyGroup.getDeployedAt().before(sharedPolicyGroup.getDeployedAt());

        // if Shared Policy Group will be deployed or updated
        if (sharedPolicyGroupToDeploy || sharedPolicyGroupToUpdate) {
            // Shared Policy Group is not yet deployed, so let's do it
            if (sharedPolicyGroupToDeploy) {
                deploy(sharedPolicyGroup);
                return true;
            }

            // Shared Policy Group has to be updated, so update it
            if (sharedPolicyGroupToUpdate) {
                update(sharedPolicyGroup);
                return true;
            }
        }
        return false;
    }

    private void deploy(ReactableSharedPolicyGroup sharedPolicyGroup) {
        log.debug("Deployment of {}", sharedPolicyGroup);

        eventManager.publishEvent(
            SecretDiscoveryEventType.DISCOVER,
            new SecretDiscoveryEvent(
                sharedPolicyGroup.getEnvironmentId(),
                sharedPolicyGroup.getDefinition(),
                new DefinitionMetadata(sharedPolicyGroup.getDefinition().getVersion())
            )
        );
        sharedPolicyGroups.put(keyOf(sharedPolicyGroup), sharedPolicyGroup);
        eventManager.publishEvent(SharedPolicyGroupEvent.DEPLOY, sharedPolicyGroup);
        log.info(
            "Shared Policy Group [{}] of environment [{}] has been deployed",
            sharedPolicyGroup.getId(),
            sharedPolicyGroup.getEnvironmentId()
        );
    }

    private void update(ReactableSharedPolicyGroup sharedPolicyGroup) {
        log.debug("Updating {}", sharedPolicyGroup);

        eventManager.publishEvent(
            SecretDiscoveryEventType.DISCOVER,
            new SecretDiscoveryEvent(
                sharedPolicyGroup.getEnvironmentId(),
                sharedPolicyGroup.getDefinition(),
                new DefinitionMetadata(sharedPolicyGroup.getDefinition().getVersion())
            )
        );
        ReactableSharedPolicyGroup previousSharedPolicyGroup = sharedPolicyGroups.put(keyOf(sharedPolicyGroup), sharedPolicyGroup);
        eventManager.publishEvent(SharedPolicyGroupEvent.UPDATE, sharedPolicyGroup);
        if (previousSharedPolicyGroup != null) {
            eventManager.publishEvent(
                SecretDiscoveryEventType.REVOKE,
                new SecretDiscoveryEvent(
                    previousSharedPolicyGroup.getEnvironmentId(),
                    previousSharedPolicyGroup.getDefinition(),
                    new DefinitionMetadata(previousSharedPolicyGroup.getDefinition().getVersion())
                )
            );
        }
        log.info(
            "Shared Policy Group [{}] of environment [{}] has been updated",
            sharedPolicyGroup.getId(),
            sharedPolicyGroup.getEnvironmentId()
        );
    }

    private void undeploy(String sharedPolicyGroupId, String environmentId) {
        ReactableSharedPolicyGroup currentSharedPolicyGroup = sharedPolicyGroups.remove(
            new SharedPolicyGroupKey(sharedPolicyGroupId, environmentId)
        );
        if (currentSharedPolicyGroup != null) {
            log.debug(
                "Undeployment of Shared Policy Group [{}] of environment [{}]",
                currentSharedPolicyGroup.getId(),
                currentSharedPolicyGroup.getEnvironmentId()
            );

            eventManager.publishEvent(SharedPolicyGroupEvent.UNDEPLOY, currentSharedPolicyGroup);
            eventManager.publishEvent(
                SecretDiscoveryEventType.REVOKE,
                new SecretDiscoveryEvent(
                    currentSharedPolicyGroup.getEnvironmentId(),
                    currentSharedPolicyGroup.getDefinition(),
                    new DefinitionMetadata(currentSharedPolicyGroup.getDefinition().getVersion())
                )
            );
            log.info(
                "Shared Policy Group [{}] of environment [{}] has been undeployed",
                currentSharedPolicyGroup.getId(),
                currentSharedPolicyGroup.getEnvironmentId()
            );
        } else {
            log.warn("Shared Policy Group [{}] of environment [{}] is not deployed", sharedPolicyGroupId, environmentId);
        }
    }

    private static SharedPolicyGroupKey keyOf(ReactableSharedPolicyGroup sharedPolicyGroup) {
        return new SharedPolicyGroupKey(sharedPolicyGroup.getId(), sharedPolicyGroup.getEnvironmentId());
    }

    /**
     * Gateway identity of one Shared Policy Group deployment. The cross id is shared across environments;
     * the environment keeps each copy, and its Vault bindings, independent.
     */
    private record SharedPolicyGroupKey(String sharedPolicyGroupId, String environmentId) {}

    private ExecutorService createExecutor(int threadCount) {
        return Executors.newFixedThreadPool(
            threadCount,
            new ThreadFactory() {
                private int counter = 0;

                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, "gio.shared-policy-group-manager-" + counter++);
                }
            }
        );
    }
}
