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
package io.gravitee.gateway.reactive.tcp;

import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.reactive.api.context.GenericExecutionContext;
import io.gravitee.gateway.reactive.api.context.http.HttpBaseExecutionContext;
import io.gravitee.gateway.reactive.core.context.AbstractResponse;
import io.reactivex.rxjava3.core.Completable;

/**
 * This class represents the outbound TCP connection. It serves as an end of the tcp-server -> backend -> tcp-server round trip and handles the bidirectional data flow creation.
 *
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public class VertxTcpResponse extends AbstractResponse {

    private final VertxTcpRequest request;
    private Completable downstreamPipe;

    public VertxTcpResponse(VertxTcpRequest request) {
        super();
        this.request = request;
        this.headers = HttpHeaders.create();
        this.trailers = HttpHeaders.create();
    }

    @Override
    public void pipeDownstream() {
        // Read response chunks and write to proxy socket
        this.downstreamPipe = VertxReadStreamUtil.toVertxRxReadStream(this.chunks()).rxPipeTo(request.getWriteStream());
    }

    /**
     * This method send upstream traffic from the client via {@link VertxTcpRequest#chunks()} towards the backend (prepared by the endpoint)
     * and simultaneously reads downstream traffic from the backend via {@link #chunks()} back to the client using a vertx {@link io.vertx.rxjava3.core.streams.Pipe}
     * to handle automatically the backpressure. Using of chunks method allow policies to manipulate buffers in policy during request and response phases.
     * @param ctx the execution context
     * @return a Completable that performs the piping at subscription time.
     */
    @Override
    public Completable end(HttpBaseExecutionContext ctx) {
        return Completable
            .defer(() -> Completable.mergeArray(request.upstreamPipe(), this.downstreamPipe))
            .doFinally(() -> this.ended = true);
    }

    @Override
    public boolean isStreaming() {
        return true;
    }
}
