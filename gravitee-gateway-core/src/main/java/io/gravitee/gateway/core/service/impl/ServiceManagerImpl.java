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
package io.gravitee.gateway.core.service.impl;

import io.gravitee.common.service.AbstractService;
import io.gravitee.gateway.core.plugin.PluginContextFactory;
import io.gravitee.gateway.core.plugin.PluginHandler;
import io.gravitee.gateway.core.service.ServiceManager;
import io.gravitee.plugin.api.Plugin;
import io.gravitee.plugin.api.PluginType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.List;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class ServiceManagerImpl extends AbstractService implements ServiceManager, PluginHandler {

    private final static Logger LOGGER = LoggerFactory.getLogger(ServiceManagerImpl.class);

    @Autowired
    private PluginContextFactory pluginContextFactory;

    private final List<AbstractService> services = new ArrayList<>();

    @Override
    public boolean canHandle(Plugin plugin) {
        return plugin.type() == PluginType.SERVICE;
    }

    @Override
    public void handle(Plugin plugin) {
        try {
            LOGGER.info("Register a new service: {}" + plugin.clazz());
            ApplicationContext context = pluginContextFactory.create(plugin);
            services.add((AbstractService) context.getBean(plugin.clazz()));

        } catch (Exception iae) {
            LOGGER.error("Unexpected error while create reporter instance", iae);
            // Be sure that the context does not exist anymore.
            pluginContextFactory.remove(plugin);
        }
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();

        if (! services.isEmpty()) {
            for (AbstractService service : services) {
                try {
                    service.start();
                } catch (Exception ex) {
                    LOGGER.error("Unexpected error while starting service", ex);
                }
            }
        } else {
            LOGGER.info("\tThere is no service to start");
        }
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();

        for(AbstractService service: services) {
            try {
                service.stop();
            } catch (Exception ex) {
                LOGGER.error("Unexpected error while starting service", ex);
            }
        }
    }

    @Override
    protected String name() {
        return "Gateway Services Manager";
    }
}
