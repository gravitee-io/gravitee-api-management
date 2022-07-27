package io.gravitee.gateway.jupiter.reactor.subscription;

import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.listener.ListenerType;
import io.gravitee.gateway.jupiter.reactor.ApiReactor;
import io.gravitee.gateway.jupiter.reactor.async.AsyncReactorFactory;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SubscriptionReactorFactory extends AsyncReactorFactory {

    @Override
    public boolean canHandle(Api api) {
        // Check that the API contains subscription listeners
        return
                super.canHandle(api) &&
                api.getListeners().stream().anyMatch(listener -> listener.getType() == ListenerType.SUBSCRIPTION);
    }

    @Override
    public ApiReactor create(Api api) {
        return new SubscriptionReactor(api);
    }
}
