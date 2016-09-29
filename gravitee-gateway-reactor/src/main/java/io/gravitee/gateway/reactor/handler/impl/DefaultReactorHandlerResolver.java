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

import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.reactor.handler.ReactorHandler;
import io.gravitee.gateway.reactor.handler.ReactorHandlerRegistry;
import io.gravitee.gateway.reactor.handler.ReactorHandlerResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DefaultReactorHandlerResolver implements ReactorHandlerResolver {

    private final Logger LOGGER = LoggerFactory.getLogger(DefaultReactorHandlerResolver.class);

    @Autowired
    private ReactorHandlerRegistry handlerRegistry;

    @Override
    public ReactorHandler resolve(Request request) {
        StringBuilder path = new StringBuilder(request.path());

        if (path.charAt(path.length() - 1) != '/') {
            path.append('/');
        }

        Optional<ReactorHandler> handlers = handlerRegistry.getReactorHandlers().stream().filter(
                handler -> path.toString().startsWith(handler.contextPath())).findFirst();

        if (handlers.isPresent()) {
            ReactorHandler handler = handlers.get();
            LOGGER.debug("Returning the first handler matching path {} : {}", path, handler);
            return handler;
        }

        return null;
    }
}
