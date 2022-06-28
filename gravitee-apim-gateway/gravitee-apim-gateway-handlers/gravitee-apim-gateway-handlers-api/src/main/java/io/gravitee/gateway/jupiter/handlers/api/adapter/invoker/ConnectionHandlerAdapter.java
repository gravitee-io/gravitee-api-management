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
package io.gravitee.gateway.jupiter.handlers.api.adapter.invoker;

import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.proxy.ProxyConnection;
import io.gravitee.gateway.api.proxy.ProxyResponse;
import io.gravitee.gateway.jupiter.api.context.RequestExecutionContext;
import io.reactivex.CompletableEmitter;
import io.reactivex.Flowable;

/**
 * The {@link ConnectionHandlerAdapter} allows to manage the response chunks coming from the upstream.
 * This adapter is able to start consuming the chunks at subscription time when the client response needs to be sent or if any actors in the request
 * processing needs to load the response in order to apply a transformation of replace it.
 *
 * The adapter is also able to perform flow control when writing the chunks from the upstream to the downstream as well as cancel the proxy connection in case the client connection has been lost.
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ConnectionHandlerAdapter implements Handler<ProxyConnection> {

    private final RequestExecutionContext ctx;
    private final CompletableEmitter nextEmitter;
    private final FlowableProxyResponse chunks;

    /**
     * Creates a {@link ConnectionHandlerAdapter}
     *
     * @param ctx the current execution context.
     * @param nextEmitter the emitter that can be used to notify when completed or in error and allow the reactive chain to continue.
     */
    public ConnectionHandlerAdapter(RequestExecutionContext ctx, CompletableEmitter nextEmitter) {
        this.ctx = ctx;
        this.nextEmitter = nextEmitter;
        this.chunks = new FlowableProxyResponse();
    }

    @Override
    public void handle(final ProxyConnection connection) {
        // Set response handler to capture the response from the proxy connection.
        connection.responseHandler(proxyResponse -> handleProxyResponse(connection, proxyResponse));
    }

    /**
     * Returns the response chunks coming from the proxy connection.
     * These chunks can them be used to replace the current response and then continue the chain.
     *
     * @return the proxy connection chunks.
     */
    public Flowable<Buffer> getChunks() {
        return chunks;
    }

    private void handleProxyResponse(ProxyConnection connection, ProxyResponse proxyResponse) {
        try {
            // In case of connectivity error, a 502 error may already have been returned to the client and the response is complete.
            if (proxyResponse.connected() && !ctx.response().ended()) {
                // Set the response status with the status coming from the invoker.
                ctx.response().status(proxyResponse.status());

                // Capture invoker headers and copy them to the response.
                proxyResponse.headers().forEach(entry -> ctx.response().headers().add(entry.getKey(), entry.getValue()));

                // Keep a reference on the proxy response to be able to resume it when a subscription will occur on the Flowable<Buffer> chunks.
                chunks.initialize(ctx, connection, proxyResponse);
            } else {
                tryCancel(proxyResponse);
            }

            nextEmitter.onComplete();
        } catch (Throwable t) {
            nextEmitter.tryOnError(t);
        }
    }

    private void tryCancel(ProxyResponse proxyResponse) {
        try {
            proxyResponse.cancel();
        } catch (Throwable ignored) {}
    }
}
