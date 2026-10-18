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
package io.gravitee.rest.api.service;

import io.gravitee.common.http.HttpMethod;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import java.util.Map;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface HttpClientService {
    /**
     * Returns the client for the given scheme and proxy setting. It is shared by every caller and must not be closed:
     * it lives as long as the Vert.x instance, which holds on to every client it creates.
     *
     * @param organizationId the organization owning the endpoint, or {@code null} for an endpoint the platform itself
     *                       owns (newsletter, reCAPTCHA).
     */
    HttpClient createHttpClient(String organizationId, String uriScheme, Boolean useSystemProxy);

    /**
     * Calls an endpoint configured by a user of the given organization.
     */
    Buffer request(
        String organizationId,
        HttpMethod method,
        final String uri,
        final Map<String, String> headers,
        String body,
        Boolean useSystemProxy
    );

    /**
     * Calls an endpoint the platform itself owns (newsletter, reCAPTCHA). Never use it for an endpoint a user
     * configures: use {@link #request(String, HttpMethod, String, Map, String, Boolean)} with the user's organization.
     */
    default Buffer request(HttpMethod method, final String uri, final Map<String, String> headers, String body, Boolean useSystemProxy) {
        return request(null, method, uri, headers, body, useSystemProxy);
    }
}
