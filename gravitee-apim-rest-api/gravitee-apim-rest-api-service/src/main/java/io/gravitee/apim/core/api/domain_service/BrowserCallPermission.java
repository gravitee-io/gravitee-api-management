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
package io.gravitee.apim.core.api.domain_service;

import io.gravitee.definition.model.Cors;
import java.util.Collection;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

/**
 * Whether an API's CORS lets a browser page on a given origin make a call, judged the way the gateway answers the
 * preflight.
 */
public final class BrowserCallPermission {

    private static final String WILDCARD = "*";

    private BrowserCallPermission() {}

    public static boolean allows(Cors cors, String origin, String method, Collection<String> headers) {
        if (cors == null || !cors.isEnabled() || origin == null) {
            return false;
        }
        return (
            allowsOrigin(cors, origin) &&
            allowsMethod(cors.getAccessControlAllowMethods(), method) &&
            allowsHeaders(cors.getAccessControlAllowHeaders(), headers)
        );
    }

    private static boolean allowsOrigin(Cors cors, String origin) {
        Set<String> allowed = cors.getAccessControlAllowOrigin();
        if (allowed == null) {
            return false;
        }
        String normalizedOrigin = normalizeOrigin(origin);
        for (String entry : allowed) {
            if (WILDCARD.equals(entry) || normalizeOrigin(entry).equals(normalizedOrigin) || matchesPattern(entry, origin)) {
                return true;
            }
        }
        Set<Pattern> patterns = cors.getAccessControlAllowOriginRegex();
        return patterns != null && patterns.stream().anyMatch(pattern -> pattern.matcher(origin).matches());
    }

    private static boolean matchesPattern(String entry, String origin) {
        if (!(entry.contains("(") || entry.contains("[") || entry.contains(WILDCARD))) {
            return false;
        }
        try {
            return Pattern.compile(entry).matcher(origin).matches();
        } catch (PatternSyntaxException e) {
            return false;
        }
    }

    private static boolean allowsMethod(Set<String> allowed, String method) {
        return allowed != null && (allowed.contains(WILDCARD) || allowed.contains(method));
    }

    private static boolean allowsHeaders(Set<String> allowed, Collection<String> headers) {
        if (allowed == null) {
            return false;
        }
        if (allowed.contains(WILDCARD)) {
            return true;
        }
        Set<String> lowerCased = allowed
            .stream()
            .map(header -> header.toLowerCase(Locale.ROOT))
            .collect(Collectors.toSet());
        return headers.stream().allMatch(header -> lowerCased.contains(header.toLowerCase(Locale.ROOT)));
    }

    private static String normalizeOrigin(String origin) {
        return origin.trim().replaceAll("/+$", "").toLowerCase(Locale.ROOT);
    }
}
