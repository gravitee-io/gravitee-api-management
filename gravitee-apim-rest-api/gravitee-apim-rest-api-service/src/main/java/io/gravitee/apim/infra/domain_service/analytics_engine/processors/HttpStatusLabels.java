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
package io.gravitee.apim.infra.domain_service.analytics_engine.processors;

import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * Resolves display labels for {@code HTTP_STATUS} facet buckets using Netty's canonical
 * reason phrases — the same source as the gateway data plane.
 */
final class HttpStatusLabels {

    private HttpStatusLabels() {}

    static String labelForCode(String statusCodeKey) {
        if (statusCodeKey == null || statusCodeKey.isBlank()) {
            return statusCodeKey;
        }
        try {
            var code = Integer.parseInt(statusCodeKey);
            var reasonPhrase = normalizeReasonPhrase(code, statusCodeKey, HttpResponseStatus.valueOf(code).reasonPhrase());
            if (reasonPhrase == null) {
                return statusCodeKey;
            }
            if (reasonPhrase.startsWith(statusCodeKey + " ")) {
                return reasonPhrase;
            }
            return code + " " + reasonPhrase;
        } catch (NumberFormatException e) {
            return statusCodeKey;
        }
    }

    /**
     * Netty appends {@code (code)} to synthetic reason phrases (e.g. {@code "999 Unknown Status (999)"}).
     * Strip that suffix and, when Netty also prefixed the code, avoid duplicating it in the final label.
     */
    private static String normalizeReasonPhrase(int code, String statusCodeKey, String reasonPhrase) {
        var suffix = " (" + code + ")";
        if (!reasonPhrase.endsWith(suffix)) {
            return reasonPhrase;
        }
        var trimmed = reasonPhrase.substring(0, reasonPhrase.length() - suffix.length()).trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
