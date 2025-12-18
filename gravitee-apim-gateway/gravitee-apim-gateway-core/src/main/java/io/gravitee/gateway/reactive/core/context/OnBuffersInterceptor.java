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
import io.reactivex.rxjava3.core.FlowableTransformer;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface OnBuffersInterceptor {
    /**
     * Register a <code>onBuffers</code> interceptor.
     * This allows to apply custom operations on the stream of buffers even if the stream changes later during the chain (e.g. onChunks or onBody).
     * Main usage is to capture the stream of buffers at the endpoint level for logging purpose.
     *
     * @param buffersInterceptor the interceptor to register.
     */
    void registerBuffersInterceptor(final FlowableTransformer<Buffer, Buffer> buffersInterceptor);
}
