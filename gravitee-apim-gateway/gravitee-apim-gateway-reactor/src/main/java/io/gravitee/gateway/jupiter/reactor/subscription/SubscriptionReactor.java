package io.gravitee.gateway.jupiter.reactor.subscription;

import io.gravitee.definition.model.v4.Api;
import io.gravitee.gateway.jupiter.core.context.MutableRequestExecutionContext;
import io.gravitee.gateway.jupiter.reactor.ApiReactor;
import io.reactivex.Completable;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SubscriptionReactor implements ApiReactor {

    private final Api api;

    public SubscriptionReactor(final Api api) {
        this.api = api;
    }

    @Override
    public Completable handle(MutableRequestExecutionContext ctx) {
        return Completable.complete();
    }
}
