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
package io.gravitee.gateway.handlers.api.processor.cors;

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.definition.model.Cors;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class CorsSimpleRequestProcessor extends CorsRequestProcessor {

    public CorsSimpleRequestProcessor(Cors cors) {
        super(cors);
    }

    @Override
    public void handle(ExecutionContext context) {
        handleSimpleCrossOriginRequest(context.request(), context.response());
        next.handle(context);
    }

    private void handleSimpleCrossOriginRequest(Request request, Response response) {
        // 1. If the Origin header is not present terminate this set of steps. The request is outside the scope of
        // this specification.
        // 2. If the value of the Origin header is not a case-sensitive match for any of the values in list of origins,
        // do not set any additional headers and terminate this set of steps.
        String originHeader = request.headers().getFirst(HttpHeaders.ORIGIN);
        if (isOriginAllowed(originHeader)) {
            // 3. If the resource supports credentials add a single Access-Control-Allow-Origin header, with the value
            // of the Origin header as value, and add a single Access-Control-Allow-Credentials header with the
            // case-sensitive string "true" as value.
            // Otherwise, add a single Access-Control-Allow-Origin header, with either the value of the Origin header
            // or the string "*" as value.
            if (cors.isAccessControlAllowCredentials()) {
                response.headers().set(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS,
                        Boolean.TRUE.toString());
                response.headers().set(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                        request.headers().getFirst(HttpHeaders.ORIGIN));
            } else {
                response.headers().set(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOW_ORIGIN_PUBLIC_WILDCARD);
            }

            // 4. If the list of exposed headers is not empty add one or more Access-Control-Expose-Headers headers,
            // with as values the header field names given in the list of exposed headers.
            if (cors.getAccessControlExposeHeaders() != null && ! cors.getAccessControlExposeHeaders().isEmpty()) {
                response.headers().set(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
                        String.join(JOINER_CHAR_SEQUENCE, cors.getAccessControlExposeHeaders()));
            }
        } else {
            // Insure that no CORS headers are defined by upstream
            response.headers().remove(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS);
            response.headers().remove(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN);
            response.headers().remove(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS);
        }
    }
}
