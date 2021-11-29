/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.reactor.handler.impl;

import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.reactor.handler.EntrypointResolver;
import io.gravitee.gateway.reactor.handler.HandlerEntrypoint;
import io.gravitee.gateway.reactor.handler.ReactorHandlerRegistry;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DefaultEntrypointResolver implements EntrypointResolver {

    public static final String ATTR_ENTRYPOINT = ExecutionContext.ATTR_PREFIX + "entrypoint";

    private final ReactorHandlerRegistry handlerRegistry;

    public DefaultEntrypointResolver(ReactorHandlerRegistry handlerRegistry) {
        this.handlerRegistry = handlerRegistry;
    }

    @Override
    public HandlerEntrypoint resolve(ExecutionContext context) {
        for (HandlerEntrypoint entrypoint : handlerRegistry.getEntrypoints()) {
            if (entrypoint.accept(context.request())) {
                context.setAttribute(ATTR_ENTRYPOINT, entrypoint);

                return entrypoint;
            }
        }

        return null;
    }
}
