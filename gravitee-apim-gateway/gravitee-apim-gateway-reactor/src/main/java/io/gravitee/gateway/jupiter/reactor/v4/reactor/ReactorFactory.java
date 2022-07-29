package io.gravitee.gateway.jupiter.reactor.v4.reactor;

import io.gravitee.definition.model.v4.Api;
import io.gravitee.gateway.jupiter.reactor.ApiReactor;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface ReactorFactory {
    boolean canHandle(Api api);

    ApiReactor create(Api api);
}
