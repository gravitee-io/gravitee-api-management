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
package io.gravitee.plugin.endpoint.http.proxy.client;

import io.gravitee.common.util.MultiValueMap;
import io.gravitee.node.vertx.client.http.VertxHttpClientFactory;
import io.vertx.core.http.RequestOptions;
import java.net.URL;
import java.util.StringJoiner;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UriHelper {

    public static final String URI_PARAM_SEPARATOR = "&";
    public static final char URI_PARAM_SEPARATOR_CHAR = '&';
    public static final char URI_PARAM_VALUE_SEPARATOR_CHAR = '=';
    public static final char URI_QUERY_DELIMITER_CHAR = '?';
    public static final CharSequence URI_QUERY_DELIMITER_CHAR_SEQUENCE = "?";

    public static RequestOptions configureAbsoluteUri(RequestOptions requestOptions, String uri, MultiValueMap<String, String> parameters) {
        final URL target = VertxHttpClientFactory.buildUrl(buildFinalUri(uri, parameters));
        final boolean secureProtocol = VertxHttpClientFactory.isSecureProtocol(target.getProtocol());

        return requestOptions
            .setURI(target.getQuery() == null ? target.getPath() : target.getPath() + URI_QUERY_DELIMITER_CHAR + target.getQuery())
            .setPort(VertxHttpClientFactory.getPort(target, secureProtocol))
            .setSsl(secureProtocol)
            .setHost(target.getHost());
    }

    public static RequestOptions configureRelativeUri(RequestOptions requestOptions, String uri, MultiValueMap<String, String> parameters) {
        return requestOptions.setURI(buildFinalUri(uri, parameters));
    }

    /**
     * Joins two path segments, normalizing slashes to avoid double slashes.
     * @param prefix the base path (trailing slash will be removed if present)
     * @param suffix the path to append (will ensure it starts with /)
     * @return the joined path
     */
    public static String joinPaths(String prefix, String suffix) {
        String normalizedPrefix = prefix != null && prefix.endsWith("/") ? prefix.substring(0, prefix.length() - 1) : prefix;
        String normalizedSuffix = suffix != null && !suffix.isEmpty() && !suffix.startsWith("/") ? "/" + suffix : suffix;
        return (normalizedPrefix != null ? normalizedPrefix : "") + (normalizedSuffix != null ? normalizedSuffix : "");
    }

    private static String buildFinalUri(String targetUri, MultiValueMap<String, String> parameters) {
        if (parameters != null && !parameters.isEmpty()) {
            final StringJoiner parametersAsString = new StringJoiner(URI_PARAM_SEPARATOR);
            parameters.forEach((paramName, paramValues) -> {
                if (paramValues != null) {
                    for (String paramValue : paramValues) {
                        if (paramValue == null) {
                            parametersAsString.add(paramName);
                        } else {
                            parametersAsString.add(paramName + URI_PARAM_VALUE_SEPARATOR_CHAR + paramValue);
                        }
                    }
                }
            });

            if (targetUri.contains(URI_QUERY_DELIMITER_CHAR_SEQUENCE)) {
                return targetUri + URI_PARAM_SEPARATOR_CHAR + parametersAsString;
            } else {
                return targetUri + URI_QUERY_DELIMITER_CHAR + parametersAsString;
            }
        } else {
            return targetUri;
        }
    }
}
