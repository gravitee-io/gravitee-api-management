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
package io.gravitee.management.standalone.node;

import io.gravitee.common.component.LifecycleComponent;
import io.gravitee.common.node.Node;
import io.gravitee.common.utils.ManifestUtils;
import io.gravitee.management.standalone.jetty.JettyEmbeddedContainer;
import io.gravitee.plugin.api.PluginRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class ManagementNode implements Node, ApplicationContextAware {

    /**
     * Logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ManagementNode.class);

    private ApplicationContext applicationContext;

    @Override
    public void start() {
        LOGGER.info("Gravitee Management [{}] is now starting...", name());

        doStart();
    }

    @Override
    public void stop() {
        LOGGER.info("Gravitee Management [{}] is stopping", name());

        List<Class<? extends LifecycleComponent>> components = getLifecycleComponents();
        for(Class<? extends LifecycleComponent> componentClass: components) {
            LOGGER.info("\tStopping component: {}", componentClass.getSimpleName());

            try {
                LifecycleComponent lifecyclecomponent = applicationContext.getBean(componentClass);
                lifecyclecomponent.stop();
            } catch (Exception e) {
                LOGGER.error("An error occurs while stopping component {}", componentClass.getSimpleName(), e);
            }
        }

        LOGGER.info("Gravitee Management [{}] stopped", name());
    }

    @Override
    public String name() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (final UnknownHostException e) {
            LOGGER.info("The local host name could not be resolved into an address", e);
            return "localhost";
        }
    }

    protected void doStart() {
        long startTime = System.currentTimeMillis(); // Get the start Time

        List<Class<? extends LifecycleComponent>> components = getLifecycleComponents();
        for(Class<? extends LifecycleComponent> componentClass: components) {
            LOGGER.info("\tStarting component: {}", componentClass.getSimpleName());

            try {
                LifecycleComponent lifecyclecomponent = applicationContext.getBean(componentClass);
                lifecyclecomponent.start();
            } catch (Exception e) {
                LOGGER.error("An error occurs while starting component {}", componentClass.getSimpleName(), e);
            }
        }

        long endTime = System.currentTimeMillis(); // Get the end Time

        LOGGER.info("Gravitee Management [{} - {}] started in {} ms.", new Object[]{name(), ManifestUtils.getVersion(), (endTime - startTime)});
    }

    private List<Class<? extends LifecycleComponent>> getLifecycleComponents() {
        List<Class<? extends LifecycleComponent>> components = new ArrayList<>();

        components.add(JettyEmbeddedContainer.class);
        components.add(PluginRegistry.class);

        return components;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
