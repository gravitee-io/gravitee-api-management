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
import io.gravitee.gateway.api.proxy.ProxyConnection;
import io.gravitee.gateway.api.proxy.ProxyResponse;
import io.gravitee.gateway.reactive.api.context.http.HttpPlainExecutionContext;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.internal.subscriptions.EmptySubscription;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlowableProxyResponse extends Flowable<Buffer> {

    private final Logger log = LoggerFactory.getLogger(FlowableProxyResponse.class);

    private final AtomicLong demand = new AtomicLong(0);
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    private ProxyResponse proxyResponse;
    private HttpPlainExecutionContext ctx;
    private ProxyConnection connection;

    private Subscription subscription;
    private Subscriber<? super Buffer> subscriber;
    private Runnable onComplete;

    public FlowableProxyResponse initialize(HttpPlainExecutionContext ctx, ProxyConnection connection, ProxyResponse proxyResponse) {
        this.ctx = ctx;
        this.connection = connection;
        this.proxyResponse = proxyResponse;

        // Always start by pausing the response.
        pauseProxyResponse();

        return this;
    }

    public FlowableProxyResponse doOnComplete(Runnable onComplete) {
        this.onComplete = onComplete;
        return this;
    }

    private void release() {
        proxyResponse.endHandler(null);
        proxyResponse.bodyHandler(null);
    }

    @Override
    protected void subscribeActual(Subscriber<? super Buffer> subscriber) {
        if (subscription != null) {
            EmptySubscription.error(new IllegalStateException("This processor allows only a single Subscriber"), subscriber);
            return;
        }

        log.debug("Subscribing to proxy response");

        this.subscriber = subscriber;
        this.subscription = new ProxyResponseSubscription();

        if (proxyResponse != null) {
            // Capture all the chunks and write them in the response.
            proxyResponse.bodyHandler(this::handleChunk);

            // When complete, propagate the complete event to the reactive chain.
            proxyResponse.endHandler(v -> handleEnd());

            // Finally, pass the subscription to the subscriber, so it can invoke onNext on it.
            subscriber.onSubscribe(subscription);
        } else {
            log.debug("Proxy response not defined, completing without any value.");
            EmptySubscription.complete(subscriber);
        }
    }

    private void handleChunk(Buffer chunk) {
        try {
            if (ctx.response().ended()) {
                // The current response is already ended for some reason (ex: connection close by the client). Close the proxy connection to avoid getting useless chunks.
                cancelProxyResponse();
                subscriber.onComplete();
            } else {
                subscriber.onNext(chunk);

                // Flow control.
                if (demand.decrementAndGet() == 0L) {
                    pauseProxyResponse();
                }
            }
        } catch (Throwable t) {
            subscriber.onError(t);
            cancelProxyResponse();
        }
    }

    private void handleEnd() {
        release();
        if (onComplete != null) {
            onComplete.run();
        }
        subscriber.onComplete();
    }

    private void pauseProxyResponse() {
        // Pause the response to stop receiving the body.
        proxyResponse.pause();
    }

    private void resumeProxyResponse() {
        // Resume the response to start receiving the body.
        proxyResponse.resume();
    }

    private void cancelProxyResponse() {
        try {
            if (cancelled.compareAndSet(false, true)) {
                log.debug("Cancelling proxy response");
                proxyResponse.cancel();
                connection.cancel();
            }
        } catch (Exception e) {
            log.warn("Unable to properly cancel the proxy response.", e);
        }
    }

    final class ProxyResponseSubscription implements Subscription {

        @Override
        public void request(long l) {
            if (demand.addAndGet(l) > 0L) {
                resumeProxyResponse();
            }
        }

        @Override
        public void cancel() {
            cancelProxyResponse();
        }
    }
}
