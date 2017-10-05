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
package io.gravitee.gateway.handlers.api.cors;

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.definition.model.Cors;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.handler.Handler;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class CorsHandler extends AbstractCorsHandler {

    public final static String ALLOW_ORIGIN_PUBLIC_WILDCARD = "*";

    private final static String JOINER_CHAR_SEQUENCE = ", ";

    private Handler<Response> responseHandler;

    private Cors cors;

    public CorsHandler(final Cors cors) {
        this.cors = cors;
    }

    public void handle(Request request, Response response, Handler<Response> handler) {
        if (cors.isAccessControlAllowCredentials()) {
            response.headers().set(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS,
                    Boolean.TRUE.toString());
            response.headers().set(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                    request.headers().getFirst(HttpHeaders.ORIGIN));
        } else {
            response.headers().set(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOW_ORIGIN_PUBLIC_WILDCARD);
        }

        if (isPreflightRequest(request)) {
            response.headers().set(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                    String.join(JOINER_CHAR_SEQUENCE, cors.getAccessControlAllowHeaders()));

            response.headers().set(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
                    cors.getAccessControlAllowMethods()
                            .stream()
                            .map(String::toUpperCase)
                            .collect(Collectors.joining(JOINER_CHAR_SEQUENCE)));

            if (cors.getAccessControlMaxAge() > -1) {
                response.headers().set(HttpHeaders.ACCESS_CONTROL_MAX_AGE,
                        Integer.toString(cors.getAccessControlMaxAge()));
            }

            // 1. If the Origin header is not present terminate this set of steps. The request is outside the scope of
            //  this specification.
            // 2. If the value of the Origin header is not a case-sensitive match for any of the values in list of
            //  origins, do not set any additional headers and terminate this set of steps.
            String originHeader = request.headers().getFirst(HttpHeaders.ORIGIN);
            if (! isOriginAllowed(originHeader)) {
                response.status(Cors.DEFAULT_ERROR_STATUS_CODE);
            }

            // 3. Let method be the value as result of parsing the Access-Control-Request-Method header.
            // If there is no Access-Control-Request-Method header or if parsing failed, do not set any additional
            //  headers and terminate this set of steps. The request is outside the scope of this specification.
            String accessControlRequestMethod = request.headers().getFirst(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD);
            if (! isRequestMethodsValid(accessControlRequestMethod)) {
                response.status(Cors.DEFAULT_ERROR_STATUS_CODE);
            }

            String accessControlRequestHeaders = request.headers().getFirst(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
            if (! isRequestHeadersValid(accessControlRequestHeaders)) {
                response.status(Cors.DEFAULT_ERROR_STATUS_CODE);
            }

            response.end();
            handler.handle(response);
        } else {
            if (cors.getAccessControlExposeHeaders() != null && ! cors.getAccessControlExposeHeaders().isEmpty()) {
                response.headers().set(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
                        String.join(JOINER_CHAR_SEQUENCE, cors.getAccessControlExposeHeaders()));
            }

            responseHandler.handle(response);
        }
    }

    private boolean isOriginAllowed(String origin) {
        return cors.getAccessControlAllowOrigin().contains(ALLOW_ORIGIN_PUBLIC_WILDCARD) ||
                cors.getAccessControlAllowOrigin().contains(origin);
    }

    boolean isRequestHeadersValid(String accessControlRequestHeaders) {
        return isRequestValid(accessControlRequestHeaders, cors.getAccessControlAllowHeaders(), false);
    }

    boolean isRequestMethodsValid(String accessControlRequestMethods) {
        return isRequestValid(accessControlRequestMethods, cors.getAccessControlAllowMethods(), true);
    }

    private boolean isRequestValid(String incoming, Set<String> configuredValues, boolean required) {
        String [] inputs = splitAndTrim(incoming, ",");
        if ((inputs == null || (inputs.length == 1 && inputs[0].isEmpty()))) {
            return true;
        }
        return (inputs == null && (configuredValues == null || configuredValues.isEmpty())) ||
                (inputs != null && containsAll(configuredValues, inputs));
    }

    private static String[] splitAndTrim(String value, String regex) {
        if (value == null)
            return null;

        String [] values = value.split(regex);
        String [] ret = new String[values.length];
        for (int i = 0 ; i < values.length ; i++) {
            ret[i] = values[i].trim();
        }

        return ret;
    }

    private static boolean containsAll(Collection<String> col, String [] values) {
        if (col == null) {
            return false;
        }

        for (String val: values) {
            if (! col.contains(val)) {
                return false;
            }
        }

        return true;
    }

    public CorsHandler responseHandler(Handler<Response> responseHandler) {
        this.responseHandler = responseHandler;
        return this;
    }

    private boolean isPreflightRequest(Request request) {
        String originHeader = request.headers().getFirst(HttpHeaders.ORIGIN);
        String accessControlRequestMethod = request.headers().getFirst(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD);
        return request.method() == HttpMethod.OPTIONS &&
                originHeader != null &&
                accessControlRequestMethod != null;
    }
}
