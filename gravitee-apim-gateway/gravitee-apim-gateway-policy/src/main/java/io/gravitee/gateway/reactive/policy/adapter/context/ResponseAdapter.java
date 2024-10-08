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
package io.gravitee.gateway.reactive.policy.adapter.context;

import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.api.stream.WriteStream;
import io.gravitee.gateway.reactive.api.context.http.HttpPlainResponse;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ResponseAdapter implements io.gravitee.gateway.api.Response {

    private final HttpPlainResponse response;

    public ResponseAdapter(HttpPlainResponse response) {
        this.response = response;
    }

    @Override
    public io.gravitee.gateway.api.Response status(int statusCode) {
        response.status(statusCode);
        return this;
    }

    @Override
    public int status() {
        return response.status();
    }

    @Override
    public String reason() {
        return response.reason();
    }

    @Override
    public io.gravitee.gateway.api.Response reason(String message) {
        response.reason(message);
        return this;
    }

    @Override
    public HttpHeaders headers() {
        return response.headers();
    }

    @Override
    public HttpHeaders trailers() {
        return response.trailers();
    }

    @Override
    public boolean ended() {
        return response.ended();
    }

    @Override
    public WriteStream<Buffer> write(Buffer content) {
        return this;
    }
}
