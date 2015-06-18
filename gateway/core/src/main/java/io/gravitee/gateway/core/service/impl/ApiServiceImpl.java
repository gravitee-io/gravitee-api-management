package io.gravitee.gateway.core.service.impl;

import io.gravitee.gateway.api.Registry;
import io.gravitee.gateway.core.event.EventManager;
import io.gravitee.gateway.core.registry.RegistryEvent;
import io.gravitee.gateway.core.service.ApiService;
import io.gravitee.model.Api;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class ApiServiceImpl implements ApiService {

    @Autowired
    private EventManager eventManager;

    @Autowired
    private Registry registry;

    @Override
    public boolean start(String name) {
        Api api = registry.get(name);
        if (api == null) {
            return false;
        } else {
            eventManager.publishEvent(RegistryEvent.START, api);
            return true;
        }
    }

    @Override
    public boolean stop(String name) {
        Api api = registry.get(name);
        if (api == null) {
            return false;
        } else {
            eventManager.publishEvent(RegistryEvent.STOP, api);
            return true;
        }
    }

    @Override
    public boolean reload(String name) {
        stop(name);
        return start(name);
    }
}
