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
package io.gravitee.gateway.handlers.api.processor.cors;

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.definition.model.Cors;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class CorsPreflightRequestProcessor extends CorsRequestProcessor {

    public CorsPreflightRequestProcessor(Cors cors) {
        super(cors);
    }

    @Override
    public void handle(ExecutionContext context) {
        if (isPreflightRequest(context.request())) {
            handlePreflightRequest(context.request(), context.response());
            // If we don't want to run policies, exit request processing
            if (!cors.isRunPolicies()) {
                exitHandler.handle(null);
            } else {
                context.setAttribute("skip-security-chain", true);
                context.setAttribute(ExecutionContext.ATTR_INVOKER, new CorsPreflightInvoker());
                next.handle(context);
            }
        } else {
            // We are in the context of a simple request, let's continue request processing
            next.handle(context);
        }
    }

    /**
     * See <a href="https://www.w3.org/TR/cors/#resource-preflight-requests">Preflight request</a>
     * @param request Incoming Request
     * @param response Client response
     */
    private void handlePreflightRequest(Request request, Response response) {
        // In case of pre-flight request, we are not able to define what is the calling application.
        // Define it as unknown
        request.metrics().setApplication("1");

        // 1. If the Origin header is not present terminate this set of steps. The request is outside the scope of
        //  this specification.
        // 2. If the value of the Origin header is not a case-sensitive match for any of the values in list of
        //  origins, do not set any additional headers and terminate this set of steps.
        String originHeader = request.headers().getFirst(HttpHeaders.ORIGIN);
        if (!isOriginAllowed(originHeader)) {
            response.status(Cors.DEFAULT_ERROR_STATUS_CODE);
            request.metrics().setMessage(String.format("Origin '%s' is not allowed", originHeader));
            return;
        }

        // 3. Let method be the value as result of parsing the Access-Control-Request-Method header.
        // If there is no Access-Control-Request-Method header or if parsing failed, do not set any additional
        //  headers and terminate this set of steps. The request is outside the scope of this specification.
        String accessControlRequestMethod = request.headers().getFirst(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD);
        if (!isRequestMethodsValid(accessControlRequestMethod)) {
            response.status(Cors.DEFAULT_ERROR_STATUS_CODE);
            request.metrics().setMessage(String.format("Request method '%s' is not allowed", accessControlRequestMethod));
            return;
        }

        // 4.Let header field-names be the values as result of parsing the Access-Control-Request-Headers headers.
        String accessControlRequestHeaders = request.headers().getFirst(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
        if (!isRequestHeadersValid(accessControlRequestHeaders)) {
            response.status(Cors.DEFAULT_ERROR_STATUS_CODE);
            request.metrics().setMessage(String.format("Request headers '%s' are not valid", accessControlRequestHeaders));
            return;
        }

        // 7. If the resource supports credentials add a single Access-Control-Allow-Origin header, with the value of
        // the Origin header as value, and add a single Access-Control-Allow-Credentials header with the case-sensitive
        // string "true" as value.
        // Otherwise, add a single Access-Control-Allow-Origin header, with either the value of the Origin header or
        // the string "*" as value.
        if (cors.isAccessControlAllowCredentials()) {
            response.headers().set(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, Boolean.TRUE.toString());
            response.headers().set(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, request.headers().getFirst(HttpHeaders.ORIGIN));
        } else {
            response.headers().set(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOW_ORIGIN_PUBLIC_WILDCARD);
        }

        if (cors.getAccessControlAllowMethods().contains("*")) {
            response.headers().set(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "*");
        } else {
            response
                .headers()
                .set(
                    HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
                    cors.getAccessControlAllowMethods().stream().map(String::toUpperCase).collect(Collectors.joining(JOINER_CHAR_SEQUENCE))
                );
        }

        if (cors.getAccessControlAllowHeaders().contains("*")) {
            response.headers().set(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "*");
        } else {
            response
                .headers()
                .set(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, String.join(JOINER_CHAR_SEQUENCE, cors.getAccessControlAllowHeaders()));
        }

        // 8. Optionally add a single Access-Control-Max-Age header with as value the amount of seconds the user agent
        // is allowed to cache the result of the request.
        if (cors.getAccessControlMaxAge() > -1) {
            response.headers().set(HttpHeaders.ACCESS_CONTROL_MAX_AGE, Integer.toString(cors.getAccessControlMaxAge()));
        }

        // 9. If method is a simple method this step may be skipped.
        // Add one or more Access-Control-Allow-Methods headers consisting of (a subset of) the list of methods.
        response
            .headers()
            .set(
                HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
                cors.getAccessControlAllowMethods().stream().map(String::toUpperCase).collect(Collectors.joining(JOINER_CHAR_SEQUENCE))
            );

        // 10. If each of the header field-names is a simple header and none is Content-Type, this step may be skipped.
        // Add one or more Access-Control-Allow-Headers headers consisting of (a subset of) the list of headers.
        response
            .headers()
            .set(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, String.join(JOINER_CHAR_SEQUENCE, cors.getAccessControlAllowHeaders()));

        response.status(HttpStatusCode.OK_200);
    }

    private boolean isRequestHeadersValid(String accessControlRequestHeaders) {
        return (
            cors.getAccessControlAllowHeaders().contains("*") ||
            isRequestValid(accessControlRequestHeaders, cors.getAccessControlAllowHeaders(), false)
        );
    }

    private boolean isRequestMethodsValid(String accessControlRequestMethods) {
        return (
            cors.getAccessControlAllowMethods().contains("*") ||
            isRequestValid(accessControlRequestMethods, cors.getAccessControlAllowMethods(), true)
        );
    }

    private boolean isRequestValid(String incoming, Set<String> configuredValues, boolean required) {
        String[] inputs = splitAndTrim(incoming, ",");
        if ((inputs == null || (inputs.length == 1 && inputs[0].isEmpty()))) {
            return true;
        }
        return (
            (inputs == null && (configuredValues == null || configuredValues.isEmpty())) ||
            (inputs != null && containsAll(configuredValues, inputs))
        );
    }

    private static String[] splitAndTrim(String value, String regex) {
        if (value == null) return null;

        String[] values = value.split(regex);
        String[] ret = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            ret[i] = values[i].trim();
        }

        return ret;
    }

    private static boolean containsAll(Collection<String> col, String[] values) {
        if (col == null) {
            return false;
        }

        for (String val : values) {
            if (!col.contains(val)) {
                return false;
            }
        }

        return true;
    }

    private boolean isPreflightRequest(Request request) {
        String originHeader = request.headers().getFirst(HttpHeaders.ORIGIN);
        String accessControlRequestMethod = request.headers().getFirst(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD);
        return request.method() == HttpMethod.OPTIONS && originHeader != null && accessControlRequestMethod != null;
    }
}
