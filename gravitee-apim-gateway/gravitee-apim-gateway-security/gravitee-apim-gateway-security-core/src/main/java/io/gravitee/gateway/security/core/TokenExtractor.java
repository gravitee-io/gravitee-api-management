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
package io.gravitee.gateway.security.core;

import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import org.springframework.util.StringUtils;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class TokenExtractor {

    static final String BEARER = "Bearer";
    static final String ACCESS_TOKEN = "access_token";

    /**
     * Extract JWT from the request.
     * First attempt to extract if from standard Authorization header.
     * If none, then try to extract it from access_token query param.
     * @param request Request
     * @return String Json Web Token.
     */
    public static String extract(Request request) {
        String authorization = request.headers().getFirst(HttpHeaderNames.AUTHORIZATION);

        if (authorization != null && !authorization.isEmpty() && StringUtils.startsWithIgnoreCase(authorization, BEARER)) {
            final String authToken = authorization.substring(BEARER.length()).trim();
            if (!authToken.isEmpty()) {
                return authToken;
            }
        }

        return request.parameters() != null ? request.parameters().getFirst(ACCESS_TOKEN) : null;
    }
}
