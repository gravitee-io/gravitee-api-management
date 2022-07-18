package io.gravitee.gateway.jupiter.reactor.subscription.entrypoint.webhook.configuration;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class Retry {

    private int attempts = 1;

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }
}
