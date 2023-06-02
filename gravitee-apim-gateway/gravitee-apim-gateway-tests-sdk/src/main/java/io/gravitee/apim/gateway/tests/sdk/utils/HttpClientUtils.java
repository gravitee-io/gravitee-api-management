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
package io.gravitee.apim.gateway.tests.sdk.utils;

import io.vertx.rxjava3.core.http.HttpClientResponse;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Provides utility methods for {@link io.vertx.rxjava3.core.http.HttpClient}
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HttpClientUtils {

    private HttpClientUtils() {
        // Utility class
    }

    /**
     * Extracts headers from {@link HttpClientResponse} to a {@link Map <String, String>} to ease the assertions
     * @param response on which extract the headers
     * @return a {@link Map<String, String>} of the headers
     */
    public static Map<String, String> extractHeaders(HttpClientResponse response) {
        return response.headers().entries().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
