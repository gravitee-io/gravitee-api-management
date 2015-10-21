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
package io.gravitee.gateway.core.reporter.impl;

import io.gravitee.gateway.api.reporter.Reporter;
import io.gravitee.gateway.core.plugin.PluginContextFactory;
import io.gravitee.gateway.core.plugin.PluginHandler;
import io.gravitee.gateway.core.reporter.ReporterManager;
import io.gravitee.plugin.api.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class ReporterManagerImpl implements ReporterManager, PluginHandler {

    private final Logger LOGGER = LoggerFactory.getLogger(ReporterManagerImpl.class);

    private final Collection<Reporter> reporters = new ArrayList<>();

    @Autowired
    private Environment environment;

    @Autowired
    private PluginContextFactory pluginContextFactory;

    @Override
    public Collection<Reporter> getReporters() {
        return reporters;
    }

    @Override
    public boolean canHandle(Plugin plugin) {
        try {
            Assert.isAssignable(Reporter.class, plugin.clazz());
            return true;
        } catch (Exception iae) {
            return false;
        }
    }

    @Override
    public void handle(Plugin plugin) {
        boolean enabled = isEnabled(plugin);
        if (enabled) {
            try {
                ApplicationContext context = pluginContextFactory.create(plugin);
                reporters.add(context.getBean(Reporter.class));
            } catch (Exception iae) {
                LOGGER.error("Unexpected error while create reporter instance", iae);
                // Be sure that the context does not exist anymore.
                pluginContextFactory.remove(plugin);
            }
        } else {
            LOGGER.warn("Plugin {} is disabled. Please have a look to your configuration to re-enable it", plugin.id());
        }
    }

    private boolean isEnabled(Plugin reporterPlugin) {
        boolean enabled = environment.getProperty("reporter." + reporterPlugin.id() + ".enabled", Boolean.class, true);
        LOGGER.debug("Plugin {} configuration: {}", reporterPlugin.id(), enabled);
        return enabled;
    }
}
