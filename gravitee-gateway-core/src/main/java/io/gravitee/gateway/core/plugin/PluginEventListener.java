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
package io.gravitee.gateway.core.plugin;

import io.gravitee.common.event.Event;
import io.gravitee.common.event.EventListener;
import io.gravitee.common.event.EventManager;
import io.gravitee.plugin.api.Plugin;
import io.gravitee.plugin.api.PluginEvent;
import io.gravitee.common.service.AbstractService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class PluginEventListener extends AbstractService implements EventListener<PluginEvent, io.gravitee.plugin.api.Plugin> {

    private final static Logger LOGGER = LoggerFactory.getLogger(PluginEventListener.class);

    @Autowired
    private Collection<PluginHandler> pluginHandlers;

    @Autowired
    private EventManager eventManager;

    @Override
    public void onEvent(Event<PluginEvent, Plugin> event) {
        if (event.type() == PluginEvent.DEPLOYED) {
            LOGGER.debug("Receive an event for plugin {} [{}]", event.content().id(), event.type());
            deploy(event.content());
        }
    }

    private void deploy(Plugin plugin) {
        pluginHandlers.stream().filter(pluginHandler -> pluginHandler.canHandle(plugin)).forEach(pluginHandler -> {
            LOGGER.debug("Plugin {} has been managed by {}", plugin.id(), pluginHandler.getClass());
            pluginHandler.handle(plugin);
        });
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();

        eventManager.subscribeForEvents(this, PluginEvent.class);
    }
}
