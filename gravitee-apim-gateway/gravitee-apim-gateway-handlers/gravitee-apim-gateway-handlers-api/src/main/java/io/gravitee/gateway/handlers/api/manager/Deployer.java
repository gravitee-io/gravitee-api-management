package io.gravitee.gateway.handlers.api.manager;

import io.gravitee.gateway.model.ReactableApi;

import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface Deployer<T extends ReactableApi<?>> {

    void prepare(T api);

    List<String> getPlans(T api);
}
