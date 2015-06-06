/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.platforms.jetty.node;

import io.gravitee.common.component.LifecycleComponent;
import io.gravitee.common.utils.ManifestUtils;
import io.gravitee.gateway.api.Node;
import io.gravitee.gateway.core.impl.DefaultReactor;
import io.gravitee.gateway.platforms.jetty.JettyEmbeddedContainer;
import io.gravitee.gateway.platforms.jetty.context.JettyPlatformContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class JettyNode implements Node {

    /**
     * Logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(JettyNode.class);

    @Override
    public void start() {
        LOGGER.info("Gateway [{}] is now starting...", name());

        doStart();
    }

    @Override
    public void stop() {
        LOGGER.info("Gateway [{}] is stopping", name());

        Map<String, LifecycleComponent> components = getLifecycleComponents();
        for (Map.Entry<String, LifecycleComponent> component : components.entrySet()) {
            LOGGER.info("\tStopping component {}", component.getKey());

            try {
                component.getValue().stop();
            } catch (Exception e) {
                LOGGER.error("An error occurs while stopping component {}", component.getKey(), e);
            }
        }

        LOGGER.info("Gateway [{}] stopped", name());
    }

    @Override
    public String name() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "localhost";
        }
    }

    protected void doStart() {
        long startTime = System.currentTimeMillis(); // Get the start Time

        Map<String, LifecycleComponent> components = getLifecycleComponents();
        for (Map.Entry<String, LifecycleComponent> component : components.entrySet()) {
            LOGGER.info("\tStarting component: {}", component.getKey());

            try {
                component.getValue().start();
            } catch (Exception e) {
                LOGGER.error("An error occurs while starting component {}", component.getKey(), e);
            }
        }

        long endTime = System.currentTimeMillis(); // Get the end Time

        LOGGER.info("Gateway [{} - {}] started in {} ms.", new Object[] { name(), ManifestUtils.getVersion(), (endTime - startTime) });
    }

    private Map<String, LifecycleComponent> lifecycleComponents;

    private Map<String, LifecycleComponent> getLifecycleComponents() {
        if (lifecycleComponents == null) {
            lifecycleComponents = new HashMap();

            // Add Jetty Gateway implementation
            lifecycleComponents.put("jetty-node", new JettyEmbeddedContainer(new JettyPlatformContext(new DefaultReactor())));

            // TODO: Add Admin Rest API
            // TODO: Add Admin web console
        }

        return lifecycleComponents;
    }
}
