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
package io.gravitee.apim.plugin.apiservice.healthcheck.http.context;

import io.gravitee.apim.plugin.apiservice.healthcheck.http.HttpHealthCheckServiceConfiguration;
import io.gravitee.common.http.HttpHeader;
import io.gravitee.common.http.IdGenerator;
import io.gravitee.common.util.LinkedMultiValueMap;
import io.gravitee.common.utils.UUID;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.reactive.core.context.AbstractRequest;
import io.gravitee.reporter.api.http.Metrics;
import java.util.List;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
class HttpHealthCheckRequest extends AbstractRequest {

    public HttpHealthCheckRequest(HttpHealthCheckServiceConfiguration configuration) {
        this.path = configuration.getTarget();
        this.pathInfo = "";
        this.method = configuration.getMethod();
        this.headers = HttpHeaders.create();
        this.parameters = new LinkedMultiValueMap<>();
        this.pathParameters = new LinkedMultiValueMap<>();
        this.timestamp = System.currentTimeMillis();
        this.id = UUID.random().toString();

        final List<HttpHeader> configHeaders = configuration.getHeaders();

        if (configHeaders != null && !configHeaders.isEmpty()) {
            configHeaders.forEach(header -> headers.set(header.getName(), header.getValue()));
        }

        if (configuration.getBody() != null) {
            final Buffer body = Buffer.buffer(configuration.getBody());
            this.body(body);
            // The endpoint connector only forwards a body announced by Content-Length or Transfer-Encoding.
            if (
                body.length() > 0 &&
                !headers.contains(HttpHeaderNames.CONTENT_LENGTH) &&
                !headers.contains(HttpHeaderNames.TRANSFER_ENCODING)
            ) {
                headers.set(HttpHeaderNames.CONTENT_LENGTH, Integer.toString(body.length()));
            }
        }
    }
}
