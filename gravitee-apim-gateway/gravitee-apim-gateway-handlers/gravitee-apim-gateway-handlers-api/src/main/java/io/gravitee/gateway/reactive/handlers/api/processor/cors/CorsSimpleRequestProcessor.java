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
package io.gravitee.gateway.reactive.handlers.api.processor.cors;

import io.gravitee.definition.model.Cors;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.reactive.api.context.http.HttpBaseRequest;
import io.gravitee.gateway.reactive.api.context.http.HttpBaseResponse;
import io.gravitee.gateway.reactive.core.context.HttpExecutionContextInternal;
import io.reactivex.rxjava3.core.Completable;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class CorsSimpleRequestProcessor extends AbstractCorsRequestProcessor {

    public static final String ID = "processor-cors-simple-request";

    private CorsSimpleRequestProcessor() {}

    public static CorsSimpleRequestProcessor instance() {
        return Holder.INSTANCE;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Completable execute(final HttpExecutionContextInternal ctx) {
        return Completable.fromRunnable(() -> {
            Cors cors = getCors(ctx);
            handleSimpleCrossOriginRequest(cors, ctx.request(), ctx.response());
        });
    }

    private void handleSimpleCrossOriginRequest(final Cors cors, final HttpBaseRequest request, final HttpBaseResponse response) {
        // 1. If the Origin header is not present terminate this set of steps. The request is outside the scope of
        // this specification.
        // 2. If the value of the Origin header is not a case-sensitive match for any of the values in list of origins,
        // do not set any additional headers and terminate this set of steps.
        String originHeader = request.headers().get(HttpHeaderNames.ORIGIN);
        if (isOriginAllowed(cors, originHeader)) {
            // 3. If the resource supports credentials add a single Access-Control-Allow-Credentials header with the
            // case-sensitive string "true" as value.
            // Also add a single Access-Control-Allow-Origin header with the value of the Origin header
            if (cors.isAccessControlAllowCredentials()) {
                response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_CREDENTIALS, Boolean.TRUE.toString());
            }
            response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, originHeader);

            // 4. If the list of exposed headers is not empty add one or more Access-Control-Expose-Headers headers,
            // with as values the header field names given in the list of exposed headers.
            if (cors.getAccessControlExposeHeaders() != null && !cors.getAccessControlExposeHeaders().isEmpty()) {
                response
                    .headers()
                    .set(
                        HttpHeaderNames.ACCESS_CONTROL_EXPOSE_HEADERS,
                        String.join(JOINER_CHAR_SEQUENCE, cors.getAccessControlExposeHeaders())
                    );
            }
        } else {
            // Ensure that no CORS headers are defined by upstream
            response.headers().remove(HttpHeaderNames.ACCESS_CONTROL_ALLOW_CREDENTIALS);
            response.headers().remove(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN);
            response.headers().remove(HttpHeaderNames.ACCESS_CONTROL_EXPOSE_HEADERS);
        }
    }

    private static class Holder {

        private static final CorsSimpleRequestProcessor INSTANCE = new CorsSimpleRequestProcessor();
    }
}
