package io.gravitee.gateway.jupiter.reactor;

import io.gravitee.definition.model.v4.Api;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface ReactorFactory {

    boolean canHandle(Api api);

    ApiReactor create(Api api);
}
