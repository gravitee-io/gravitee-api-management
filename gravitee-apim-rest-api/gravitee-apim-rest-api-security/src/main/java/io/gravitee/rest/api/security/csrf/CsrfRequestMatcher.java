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
package io.gravitee.rest.api.security.csrf;

import io.gravitee.rest.api.security.filter.TokenAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.http.HttpHeaders;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * CSRF is required only for non-safe methods and if the call is coming from the browser (ie. there is an existing
 * authorization cookie in the request).
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class CsrfRequestMatcher implements RequestMatcher {

    private final Set<String> allowedMethods = new HashSet<>(Arrays.asList("GET", "HEAD", "TRACE", "OPTIONS"));
    private final Set<Pattern> allowedPaths = new HashSet<>(Arrays.asList(Pattern.compile("^/organizations/[^/]+/user/login$")));

    @Override
    public boolean matches(HttpServletRequest request) {
        return (
            !allowedMethods.contains(request.getMethod()) &&
            !(
                request.getPathInfo() != null && allowedPaths.stream().anyMatch(pattern -> pattern.matcher(request.getPathInfo()).matches())
            ) &&
            (
                request.getHeader(HttpHeaders.REFERER) != null ||
                request.getHeader(HttpHeaders.ORIGIN) != null ||
                (
                    request.getCookies() != null &&
                    Arrays
                        .stream(request.getCookies())
                        .anyMatch(cookie -> TokenAuthenticationFilter.AUTH_COOKIE_NAME.equals(cookie.getName()))
                )
            )
        );
    }
}
