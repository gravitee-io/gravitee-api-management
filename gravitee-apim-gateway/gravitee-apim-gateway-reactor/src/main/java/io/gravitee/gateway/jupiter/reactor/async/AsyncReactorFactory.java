package io.gravitee.gateway.jupiter.reactor.async;

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.gateway.jupiter.reactor.ReactorFactory;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AsyncReactorFactory implements ReactorFactory {

    @Override
    public boolean canHandle(Api api) {
        return api.getDefinitionVersion() == DefinitionVersion.V4 &&
                api.getType() == ApiType.ASYNC;
    }
}
