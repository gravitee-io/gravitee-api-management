package io.gravitee.gateway.jupiter.reactor.subscription.request;

import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.jupiter.core.context.MutableRequestExecutionContext;
import io.gravitee.gateway.jupiter.reactor.handler.context.DefaultRequestExecutionContext;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SubscriptionExecutionRequestFactory {

    public MutableRequestExecutionContext create(Subscription subscription) {
        return new DefaultRequestExecutionContext(null, null);
    }
}
