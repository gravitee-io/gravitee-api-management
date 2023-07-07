/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.reactor.handler.impl;

import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.reactor.handler.AcceptorResolver;
import io.gravitee.gateway.reactor.handler.HttpAcceptor;
import io.gravitee.gateway.reactor.handler.ReactorHandlerRegistry;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DefaultAcceptorResolver implements AcceptorResolver {

    // TODO This attribute should be renamed/removed as entrypoint has been renamed to Acceptor
    public static final String ATTR_ENTRYPOINT = ExecutionContext.ATTR_PREFIX + "entrypoint";

    private final ReactorHandlerRegistry handlerRegistry;

    public DefaultAcceptorResolver(ReactorHandlerRegistry handlerRegistry) {
        this.handlerRegistry = handlerRegistry;
    }

    @Override
    public HttpAcceptor resolve(ExecutionContext context, String serverId) {
        for (HttpAcceptor acceptor : handlerRegistry.getAcceptors(HttpAcceptor.class)) {
            final Request request = context.request();

            if (acceptor.accept(request.host(), request.path(), serverId)) {
                context.setAttribute(ATTR_ENTRYPOINT, acceptor);

                return acceptor;
            }
        }

        return null;
    }
}
