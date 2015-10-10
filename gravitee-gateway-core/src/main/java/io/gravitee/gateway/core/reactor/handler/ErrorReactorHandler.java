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
package io.gravitee.gateway.core.reactor.handler;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class ErrorReactorHandler extends AbstractReactorHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ErrorReactorHandler.class);

    @Override
    public void handle(Request request, Response response, io.gravitee.gateway.api.handler.Handler<Response> handler) {
        LOGGER.warn("No Gravitee handler can be found for request {}, returns NOT_FOUND(404)", request.path());

        response.status(HttpStatusCode.NOT_FOUND_404);
        handler.handle(response);
    }
}
