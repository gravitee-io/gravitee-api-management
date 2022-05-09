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
package io.gravitee.gateway.reactive.policy.impl;

import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.reactive.api.message.Message;
import java.util.Map;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DummyMessage implements Message {

    private HttpHeaders headers;
    private Map<String, Object> metadata;
    private Buffer content;

    @Override
    public HttpHeaders headers() {
        return headers;
    }

    public DummyMessage headers(HttpHeaders headers) {
        this.headers = headers;
        return this;
    }

    @Override
    public Map<String, Object> metadata() {
        return metadata;
    }

    public DummyMessage metadata(Map<String, Object> metadata) {
        this.metadata = metadata;
        return this;
    }

    @Override
    public Buffer content() {
        return content;
    }

    public DummyMessage content(Buffer content) {
        this.content = content;
        return this;
    }
}
