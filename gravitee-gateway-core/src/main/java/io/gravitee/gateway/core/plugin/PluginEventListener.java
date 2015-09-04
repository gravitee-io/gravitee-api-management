package io.gravitee.gateway.core.plugin;

import io.gravitee.common.event.Event;
import io.gravitee.common.event.EventListener;
import io.gravitee.common.event.EventManager;
import io.gravitee.gateway.core.service.AbstractService;
import io.gravitee.plugin.api.Plugin;
import io.gravitee.plugin.api.PluginEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class PluginEventListener extends AbstractService implements EventListener<PluginEvent, io.gravitee.plugin.api.Plugin> {

    protected final Logger LOGGER = LoggerFactory.getLogger(PluginEventListener.class);

    @Autowired
    private Collection<PluginHandler> pluginHandlers;

    @Autowired
    private EventManager eventManager;

    @Override
    public void onEvent(Event<PluginEvent, Plugin> event) {
        switch (event.type()) {
            case DEPLOYED:
                LOGGER.debug("Receive an event for plugin {} [{}]", event.content().id(), event.type());
                deploy(event.content());
                break;
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
