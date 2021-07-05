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
package io.gravitee.repository.bridge.client.http;

import io.vertx.core.MultiMap;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HttpResponse<T> {

    private final int statusCode;

    private final MultiMap headers;

    private final T payload;

    public HttpResponse(int statusCode, MultiMap headers) {
        this(statusCode, headers, null);
    }

    public HttpResponse(int statusCode, MultiMap headers, T payload) {
        this.statusCode = statusCode;
        this.headers = headers;
        this.payload = payload;
    }

    public int statusCode() {
        return statusCode;
    }

    public MultiMap headers() {
        return headers;
    }

    public T payload() {
        return payload;
    }
}
