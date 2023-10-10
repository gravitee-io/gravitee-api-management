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
package io.gravitee.gateway.tcp.reactive;

import io.gravitee.gateway.api.buffer.Buffer;
import io.reactivex.rxjava3.core.Flowable;
import io.vertx.rxjava3.FlowableHelper;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public class VertxReadStreamUtil {

    private VertxReadStreamUtil() {
        // no op
    }

    /**
     * Boilerplate to convert a {@link Flowable} of Gravitee {@link Buffer}
     * into a {@link io.vertx.rxjava3.core.streams.ReadStream} of Vert.x RX {@link io.vertx.rxjava3.core.buffer.Buffer}
     * @param chunks the flowable to convert
     * @return the resulting read-stream of rx vertx buffer
     */
    public static io.vertx.rxjava3.core.streams.ReadStream<io.vertx.rxjava3.core.buffer.Buffer> toVertxRxReadStream(
        final Flowable<Buffer> chunks
    ) {
        return io.vertx.rxjava3.core.streams.ReadStream.newInstance(
            FlowableHelper.toReadStream(chunks.map(Buffer::getBytes).map(io.vertx.core.buffer.Buffer::buffer))
        );
    }
}
