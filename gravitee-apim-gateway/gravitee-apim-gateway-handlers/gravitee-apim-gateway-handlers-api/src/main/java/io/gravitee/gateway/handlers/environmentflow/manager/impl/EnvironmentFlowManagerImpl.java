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
package io.gravitee.gateway.handlers.environmentflow.manager.impl;

import io.gravitee.common.event.EventManager;
import io.gravitee.definition.model.Plugin;
import io.gravitee.gateway.handlers.environmentflow.manager.EnvironmentFlowManager;
import io.gravitee.gateway.reactive.reactor.environmentflow.ReactableEnvironmentFlow;
import io.gravitee.gateway.reactor.ReactorEvent;
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
public class EnvironmentFlowManagerImpl implements EnvironmentFlowManager {

    private static final int PARALLELISM = Runtime.getRuntime().availableProcessors() * 2;
    private final Map<String, ReactableEnvironmentFlow> environmentFlows = new ConcurrentHashMap<>();

    private final EventManager eventManager;
    private final LicenseManager licenseManager;

    public EnvironmentFlowManagerImpl(EventManager eventManager, LicenseManager licenseManager) {
        this.eventManager = eventManager;
        this.licenseManager = licenseManager;
    }

    @Override
    public boolean register(ReactableEnvironmentFlow environmentFlow) {
        return register(environmentFlow, false);
    }

    @Override
    public void unregister(String environmentFlowId) {
        undeploy(environmentFlowId);
    }

    @Override
    public void refresh() {
        if (environmentFlows != null && !environmentFlows.isEmpty()) {
            final long begin = System.currentTimeMillis();

            log.info("Starting environment flows refresh. {} environment flows to be refreshed.", environmentFlows.size());

            // Create an executor to parallelize a refresh for all the apis.
            final ExecutorService refreshAllExecutor = createExecutor(Math.min(PARALLELISM, environmentFlows.size()));

            final List<Callable<Boolean>> toInvoke = environmentFlows
                .values()
                .stream()
                .map(environmentFlow -> ((Callable<Boolean>) () -> register(environmentFlow, true)))
                .collect(Collectors.toList());

            try {
                refreshAllExecutor.invokeAll(toInvoke);
                refreshAllExecutor.shutdown();
                while (!refreshAllExecutor.awaitTermination(100, TimeUnit.MILLISECONDS));
            } catch (InterruptedException e) {
                log.error("Unable to refresh environment flows", e);
                Thread.currentThread().interrupt();
            } finally {
                refreshAllExecutor.shutdown();
            }

            log.info("Environment Flows refresh done in {}ms", (System.currentTimeMillis() - begin));
        }
    }

    @Override
    public Collection<ReactableEnvironmentFlow> environmentFlows() {
        return environmentFlows.values();
    }

    @Override
    public ReactableEnvironmentFlow get(String environmentFlowId) {
        return environmentFlows.get(environmentFlowId);
    }

    private boolean register(ReactableEnvironmentFlow environmentFlow, boolean force) {
        // Get deployed API
        ReactableEnvironmentFlow deployedEnvironmentFlow = get(environmentFlow.getId());

        List<Plugin> plugins = environmentFlow.getDefinition().getPlugins();

        try {
            licenseManager.validatePluginFeatures(
                environmentFlow.getOrganizationId(),
                plugins.stream().map(p -> new LicenseManager.Plugin(p.type(), p.id())).collect(Collectors.toSet())
            );
        } catch (InvalidLicenseException | ForbiddenFeatureException e) {
            log.warn(
                "The Environment Flow {} could not be deployed because it is not allowed by the current license",
                environmentFlow.getName(),
                e
            );
            return false;
        }

        boolean environmentFlowToDeploy = deployedEnvironmentFlow == null || force;
        boolean environmentFlowToUpdate =
            !environmentFlowToDeploy && deployedEnvironmentFlow.getDeployedAt().before(environmentFlow.getDeployedAt());

        // if API will be deployed or updated
        if (environmentFlowToDeploy || environmentFlowToUpdate) {
            // Environment Flow is not yet deployed, so let's do it
            if (environmentFlowToDeploy) {
                deploy(environmentFlow);
                return true;
            }

            // Environment Flow has to be updated, so update it
            if (environmentFlowToUpdate) {
                update(environmentFlow);
                return true;
            }
        }
        return false;
    }

    private void deploy(ReactableEnvironmentFlow environmentFlow) {
        log.debug("Deployment of {}", environmentFlow);

        environmentFlows.put(environmentFlow.getId(), environmentFlow);
        eventManager.publishEvent(ReactorEvent.DEPLOY, environmentFlow);
        log.info("Environment Flow [{}] has been deployed", environmentFlow.getId());
    }

    private void update(ReactableEnvironmentFlow environmentFlow) {
        log.debug("Updating {}", environmentFlow);

        environmentFlows.put(environmentFlow.getId(), environmentFlow);
        eventManager.publishEvent(ReactorEvent.UPDATE, environmentFlow);
        log.info("Environment Flow [{}] has been updated", environmentFlow.getId());
    }

    private void undeploy(String environmentFlowId) {
        ReactableEnvironmentFlow currentEnvironmentFlow = environmentFlows.remove(environmentFlowId);
        if (currentEnvironmentFlow != null) {
            log.debug("Undeployment of Environment Flow [{}]", currentEnvironmentFlow.getEnvironmentId());

            eventManager.publishEvent(ReactorEvent.UNDEPLOY, currentEnvironmentFlow);
            log.info("[{}] has been undeployed", currentEnvironmentFlow.getId());
        }
    }

    private ExecutorService createExecutor(int threadCount) {
        return Executors.newFixedThreadPool(
            threadCount,
            new ThreadFactory() {
                private int counter = 0;

                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, "gio.environment-flow-manager-" + counter++);
                }
            }
        );
    }
}
