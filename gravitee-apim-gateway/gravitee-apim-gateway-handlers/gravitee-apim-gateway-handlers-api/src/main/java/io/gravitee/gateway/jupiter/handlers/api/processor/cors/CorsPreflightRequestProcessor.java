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
package io.gravitee.gateway.jupiter.handlers.api.processor.cors;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.Cors;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.handlers.api.processor.cors.CorsPreflightInvoker;
import io.gravitee.gateway.jupiter.api.context.ExecutionContext;
import io.gravitee.gateway.jupiter.api.context.HttpExecutionContext;
import io.gravitee.gateway.jupiter.api.context.HttpRequest;
import io.gravitee.gateway.jupiter.api.context.HttpResponse;
import io.gravitee.gateway.jupiter.api.context.RequestExecutionContext;
import io.gravitee.gateway.jupiter.api.context.Response;
import io.reactivex.Completable;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class CorsPreflightRequestProcessor extends AbstractCorsRequestProcessor {

    public static final String ID = "processor-cors-preflight-request";

    private CorsPreflightRequestProcessor() {}

    public static CorsPreflightRequestProcessor instance() {
        return Holder.INSTANCE;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Completable execute(final HttpExecutionContext ctx) {
        return Completable.defer(
            () -> {
                // Test if we are in the context of a preflight request
                if (isPreflightRequest(ctx.request())) {
                    Api api = ctx.getComponent(Api.class);
                    Cors cors = api.getProxy().getCors();
                    handlePreflightRequest(cors, ctx.request(), ctx.response());
                    // If we don't want to run policies, exit request processing
                    if (!cors.isRunPolicies()) {
                        return ctx.interrupt();
                    } else {
                        ctx.setAttribute("skip-security-chain", true);
                        ctx.setAttribute(ExecutionContext.ATTR_INVOKER, new CorsPreflightInvoker());
                        return Completable.complete();
                    }
                }
                return Completable.complete();
            }
        );
    }

    private boolean isPreflightRequest(final HttpRequest request) {
        String originHeader = request.headers().get(HttpHeaderNames.ORIGIN);
        String accessControlRequestMethod = request.headers().get(HttpHeaderNames.ACCESS_CONTROL_REQUEST_METHOD);
        return request.method() == HttpMethod.OPTIONS && originHeader != null && accessControlRequestMethod != null;
    }

    /**
     * See <a href="https://www.w3.org/TR/cors/#resource-preflight-requests">Preflight request</a>
     * @param cors Cors settings
     * @param request Incoming Request
     * @param response Client response
     */
    private void handlePreflightRequest(final Cors cors, final HttpRequest request, final HttpResponse response) {
        // In case of pre-flight request, we are not able to define what is the calling application.
        // Define it as unknown
        request.metrics().setApplication("1");

        // 1. If the Origin header is not present terminate this set of steps. The request is outside the scope of
        //  this specification.
        // 2. If the value of the Origin header is not a case-sensitive match for any of the values in list of
        //  origins, do not set any additional headers and terminate this set of steps.
        String originHeader = request.headers().get(HttpHeaderNames.ORIGIN);
        if (!isOriginAllowed(cors, originHeader)) {
            response.status(Cors.DEFAULT_ERROR_STATUS_CODE);
            request.metrics().setMessage(String.format("Origin '%s' is not allowed", originHeader));
            return;
        }

        // 3. Let method be the value as result of parsing the Access-Control-Request-Method header.
        // If there is no Access-Control-Request-Method header or if parsing failed, do not set any additional
        //  headers and terminate this set of steps. The request is outside the scope of this specification.
        String accessControlRequestMethod = request.headers().get(HttpHeaderNames.ACCESS_CONTROL_REQUEST_METHOD);
        if (!isRequestMethodsValid(cors, accessControlRequestMethod)) {
            response.status(Cors.DEFAULT_ERROR_STATUS_CODE);
            request.metrics().setMessage(String.format("Request method '%s' is not allowed", accessControlRequestMethod));
            return;
        }

        // 4.Let header field-names be the values as result of parsing the Access-Control-Request-Headers headers.
        String accessControlRequestHeaders = request.headers().get(HttpHeaderNames.ACCESS_CONTROL_REQUEST_HEADERS);
        if (!isRequestHeadersValid(cors, accessControlRequestHeaders)) {
            response.status(Cors.DEFAULT_ERROR_STATUS_CODE);
            request.metrics().setMessage(String.format("Request headers '%s' are not valid", accessControlRequestHeaders));
            return;
        }

        // 7. If the resource supports credentials add a single Access-Control-Allow-Credentials header with the case-sensitive
        // string "true" as value.
        // ALso, add a single Access-Control-Allow-Origin header with the value of the Origin header
        if (cors.isAccessControlAllowCredentials()) {
            response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_CREDENTIALS, Boolean.TRUE.toString());
        }
        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, originHeader);

        // 8. Optionally add a single Access-Control-Max-Age header with as value the amount of seconds the user agent
        // is allowed to cache the result of the request.
        if (cors.getAccessControlMaxAge() > -1) {
            response.headers().set(HttpHeaderNames.ACCESS_CONTROL_MAX_AGE, Integer.toString(cors.getAccessControlMaxAge()));
        }

        // 9. If method is a simple method this step may be skipped.
        // Add one or more Access-Control-Allow-Methods headers consisting of (a subset of) the list of methods.
        if (cors.getAccessControlAllowMethods() != null && !cors.getAccessControlAllowMethods().isEmpty()) {
            response
                .headers()
                .set(
                    HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS,
                    cors.getAccessControlAllowMethods().stream().map(String::toUpperCase).collect(Collectors.joining(JOINER_CHAR_SEQUENCE))
                );
        } else {
            response.status(Cors.DEFAULT_ERROR_STATUS_CODE);
            request.metrics().setMessage("CORS configuration invalid,  Access-Control-Allow-Methods cannot be null or empty.");
            return;
        }

        // 10. If each of the header field-names is a simple header and none is Content-Type, this step may be skipped.
        // Add one or more Access-Control-Allow-Headers headers consisting of (a subset of) the list of headers.
        if (cors.getAccessControlAllowHeaders() != null) {
            response
                .headers()
                .set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, String.join(JOINER_CHAR_SEQUENCE, cors.getAccessControlAllowHeaders()));
        }
        response.status(HttpStatusCode.OK_200);
    }

    private boolean isRequestMethodsValid(final Cors cors, final String accessControlRequestMethods) {
        return isRequestValid(accessControlRequestMethods, cors.getAccessControlAllowMethods());
    }

    private boolean isRequestHeadersValid(final Cors cors, final String accessControlRequestHeaders) {
        return isRequestValid(accessControlRequestHeaders, cors.getAccessControlAllowHeaders());
    }

    private boolean isRequestValid(final String incoming, final Set<String> configuredValues) {
        List<String> inputs = splitAndTrim(incoming);
        return (
            ((inputs == null || (inputs.size() == 1 && inputs.get(0).isEmpty()))) ||
            (configuredValues == null || configuredValues.isEmpty()) ||
            (configuredValues.containsAll(inputs))
        );
    }

    private List<String> splitAndTrim(final String value) {
        if (value == null) return null;
        return Arrays.stream(value.split(",")).map(String::trim).collect(Collectors.toList());
    }

    private static class Holder {

        private static final CorsPreflightRequestProcessor INSTANCE = new CorsPreflightRequestProcessor();
    }
}
