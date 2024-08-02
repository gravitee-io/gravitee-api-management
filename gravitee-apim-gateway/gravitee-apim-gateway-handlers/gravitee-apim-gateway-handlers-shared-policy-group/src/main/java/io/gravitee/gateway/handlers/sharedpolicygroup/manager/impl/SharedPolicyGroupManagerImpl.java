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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class SharedPolicyGroupManagerImpl implements SharedPolicyGroupManager {

    private static final int PARALLELISM = Runtime.getRuntime().availableProcessors() * 2;
    private final Map<String, ReactableSharedPolicyGroup> sharedPolicyGroups = new ConcurrentHashMap<>();

    private final EventManager eventManager;
    private final LicenseManager licenseManager;

    public SharedPolicyGroupManagerImpl(EventManager eventManager, LicenseManager licenseManager) {
        this.eventManager = eventManager;
        this.licenseManager = licenseManager;
    }

    @Override
    public boolean register(ReactableSharedPolicyGroup sharedPolicyGroup) {
        return register(sharedPolicyGroup, false);
    }

    @Override
    public void unregister(String sharedPolicyGroupId) {
        undeploy(sharedPolicyGroupId);
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
    public ReactableSharedPolicyGroup get(String sharedPolicyGroupId) {
        return sharedPolicyGroups.get(sharedPolicyGroupId);
    }

    private boolean register(ReactableSharedPolicyGroup sharedPolicyGroup, boolean force) {
        // Get deployed Shared Policy Group
        ReactableSharedPolicyGroup deployedSharedPolicyGroup = get(sharedPolicyGroup.getId());

        List<Plugin> plugins = sharedPolicyGroup.getDefinition().getPlugins();

        try {
            licenseManager.validatePluginFeatures(
                sharedPolicyGroup.getOrganizationId(),
                plugins.stream().map(p -> new LicenseManager.Plugin(p.type(), p.id())).collect(Collectors.toSet())
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

        sharedPolicyGroups.put(sharedPolicyGroup.getId(), sharedPolicyGroup);
        eventManager.publishEvent(SharedPolicyGroupEvent.DEPLOY, sharedPolicyGroup);
        log.info("Shared Policy Group [{}] has been deployed", sharedPolicyGroup.getId());
    }

    private void update(ReactableSharedPolicyGroup sharedPolicyGroup) {
        log.debug("Updating {}", sharedPolicyGroup);

        sharedPolicyGroups.put(sharedPolicyGroup.getId(), sharedPolicyGroup);
        eventManager.publishEvent(SharedPolicyGroupEvent.UPDATE, sharedPolicyGroup);
        log.info("Shared Policy Group [{}] has been updated", sharedPolicyGroup.getId());
    }

    private void undeploy(String sharedPolicyGroupId) {
        ReactableSharedPolicyGroup currentSharedPolicyGroup = sharedPolicyGroups.remove(sharedPolicyGroupId);
        if (currentSharedPolicyGroup != null) {
            log.debug("Undeployment of Shared Policy Group [{}]", currentSharedPolicyGroup.getEnvironmentId());

            eventManager.publishEvent(SharedPolicyGroupEvent.UNDEPLOY, currentSharedPolicyGroup);
            log.info("[{}] has been undeployed", currentSharedPolicyGroup.getId());
        }
    }

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
