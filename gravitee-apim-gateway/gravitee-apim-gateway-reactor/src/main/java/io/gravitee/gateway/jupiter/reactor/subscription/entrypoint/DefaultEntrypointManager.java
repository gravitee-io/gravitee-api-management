package io.gravitee.gateway.jupiter.reactor.subscription.entrypoint;

import io.gravitee.gateway.jupiter.reactor.subscription.entrypoint.webhook.WebhookEntrypoint;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DefaultEntrypointManager implements EntrypointManager {

    @Override
    public PushEntrypoint getEntrypoint(String type) {
        //TODO: to complete with entrypoint plugins
        return new WebhookEntrypoint();
    }
}
