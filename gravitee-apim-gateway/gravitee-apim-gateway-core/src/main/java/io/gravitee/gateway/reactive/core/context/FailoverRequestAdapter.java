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
package io.gravitee.gateway.reactive.core.context;

import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.reactive.api.context.http.HttpRequest;
import io.reactivex.rxjava3.core.Maybe;

/**
 * Adapter that provides access to the request body without triggering buffer interceptors.
 * <p>
 * This is used by the failover mechanism to capture the original request body for replay on retry,
 * without prematurely consuming interceptors (e.g. logging) that should only fire when the body
 * actually flows to the backend endpoint.
 * <p>
 * Lives in the {@code context} package to access {@link AbstractRequest#lazyBufferFlow()} (protected).
 *
 * @see io.gravitee.gateway.reactive.core.BufferFlow#bodyIgnoringInterceptors()
 */
public class FailoverRequestAdapter {

    private final AbstractRequest delegate;

    public static FailoverRequestAdapter forRequest(HttpRequest request) {
        return new FailoverRequestAdapter((AbstractRequest) request);
    }

    private FailoverRequestAdapter(AbstractRequest delegate) {
        this.delegate = delegate;
    }

    /**
     * Reads and caches the request body without applying buffer interceptors.
     * Interceptors remain registered and will be applied on subsequent body access (e.g. by the endpoint connector).
     */
    public Maybe<Buffer> body() {
        return delegate.lazyBufferFlow().bodyIgnoringInterceptors();
    }
}
