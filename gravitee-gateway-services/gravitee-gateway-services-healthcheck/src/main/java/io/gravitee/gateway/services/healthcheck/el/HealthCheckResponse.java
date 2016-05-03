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
package io.gravitee.gateway.services.healthcheck.el;

import io.netty.handler.codec.http.HttpHeaders;
import org.asynchttpclient.Response;

/**
 * @author David BRASSELY (david at gravitee.io)
 * @author GraviteeSource Team
 */
public final class HealthCheckResponse {

    private final Response response;

    public HealthCheckResponse(final Response response) {
        this.response = response;
    }

    public int getStatus() {
        return response.getStatusCode();
    }

    public String getContent() {
        return response.getResponseBody();
    }

    public HttpHeaders getHeaders() {
        return response.getHeaders();
    }
}
