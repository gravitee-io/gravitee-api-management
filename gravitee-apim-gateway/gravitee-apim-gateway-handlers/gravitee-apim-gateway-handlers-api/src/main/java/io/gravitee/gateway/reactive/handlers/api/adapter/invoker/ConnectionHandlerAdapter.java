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
package io.gravitee.gateway.reactive.handlers.api.adapter.invoker;

import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.api.processor.ProcessorFailure;
import io.gravitee.gateway.api.proxy.ProxyConnection;
import io.gravitee.gateway.api.proxy.ProxyResponse;
import io.gravitee.gateway.reactive.api.ExecutionFailure;
import io.gravitee.gateway.reactive.api.context.http.HttpPlainExecutionContext;
import io.gravitee.gateway.reactive.core.context.interruption.InterruptionFailureException;
import io.reactivex.rxjava3.core.CompletableEmitter;
import io.reactivex.rxjava3.core.Flowable;

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

    private final HttpPlainExecutionContext ctx;
    private final CompletableEmitter nextEmitter;
    private final FlowableProxyResponse chunks;

    /**
     * Creates a {@link ConnectionHandlerAdapter}
     *
     * @param ctx the current execution context.
     * @param nextEmitter the emitter that can be used to notify when completed or in error and allow the reactive chain to continue.
     */
    public ConnectionHandlerAdapter(HttpPlainExecutionContext ctx, CompletableEmitter nextEmitter) {
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
            if (proxyResponse.connected()) {
                handleResponse(connection, proxyResponse);
            } else {
                handleConnectionError(proxyResponse);
            }
        } catch (Throwable t) {
            nextEmitter.tryOnError(t);
        }
    }

    private void handleResponse(ProxyConnection connection, ProxyResponse proxyResponse) {
        // In case of connectivity error, a 502 error may already have been returned to the client and the response is complete.
        if (!ctx.response().ended()) {
            // Set the response status and reason with the ones coming from the invoker.
            ctx.response().status(proxyResponse.status());
            ctx.response().reason(proxyResponse.reason());

            // Capture invoker headers and copy them to the response.
            proxyResponse.headers().forEach(entry -> ctx.response().headers().add(entry.getKey(), entry.getValue()));

            // Keep a reference on the proxy response to be able to resume it when a subscription will occur on the Flowable<Buffer> chunks.
            chunks
                .initialize(ctx, connection, proxyResponse)
                .doOnComplete(
                    (Runnable) () -> {
                        // Capture invoker trailers and copy them to the response.
                        final HttpHeaders trailers = proxyResponse.trailers();
                        if (trailers != null && !trailers.isEmpty()) {
                            trailers.forEach((entry -> ctx.response().trailers().add(entry.getKey(), entry.getValue())));
                        }
                    }
                );
        } else {
            tryCancel(proxyResponse);
        }

        nextEmitter.onComplete();
    }

    private void handleConnectionError(ProxyResponse proxyResponse) {
        if (proxyResponse instanceof ProcessorFailure) {
            final ProcessorFailure failureResponse = (ProcessorFailure) proxyResponse;
            nextEmitter.tryOnError(new InterruptionFailureException(toExecutionFailure(failureResponse)));
        } else {
            nextEmitter.tryOnError(new InterruptionFailureException(new ExecutionFailure(proxyResponse.status())));
        }
    }

    private ExecutionFailure toExecutionFailure(ProcessorFailure failureResponse) {
        return new ExecutionFailure(failureResponse.statusCode())
            .key(failureResponse.key())
            .message(failureResponse.message())
            .contentType(failureResponse.contentType())
            .parameters(failureResponse.parameters());
    }

    private void tryCancel(ProxyResponse proxyResponse) {
        try {
            proxyResponse.cancel();
        } catch (Throwable ignored) {}
    }
}
