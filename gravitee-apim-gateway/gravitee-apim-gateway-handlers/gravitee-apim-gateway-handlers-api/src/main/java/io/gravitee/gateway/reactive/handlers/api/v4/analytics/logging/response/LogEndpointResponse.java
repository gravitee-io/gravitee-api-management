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
package io.gravitee.gateway.reactive.handlers.api.v4.analytics.logging.response;

import io.gravitee.gateway.reactive.api.context.HttpResponse;
import io.gravitee.gateway.reactive.api.context.http.HttpPlainResponse;
import io.gravitee.gateway.reactive.core.v4.analytics.LoggingContext;

/**
 * Allows to log the response status, headers and body returned by the backend endpoint depending on what is configured on the {@link LoggingContext}.
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class LogEndpointResponse extends LogResponse {

    public LogEndpointResponse(LoggingContext loggingContext, HttpPlainResponse response) {
        super(loggingContext, response);
    }

    protected boolean isLogPayload() {
        return loggingContext.endpointResponsePayload();
    }

    protected boolean isLogHeaders() {
        return loggingContext.endpointResponseHeaders();
    }
}
