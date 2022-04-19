package io.gravitee.gateway.reactive.handlers.api.adapter.invoker;

import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.proxy.ProxyConnection;
import io.gravitee.gateway.api.proxy.ProxyResponse;
import io.gravitee.gateway.reactive.api.context.sync.SyncExecutionContext;
import io.reactivex.Flowable;
import io.reactivex.internal.subscriptions.EmptySubscription;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class FlowableProxyResponse extends Flowable<Buffer> {

    private final Logger log = LoggerFactory.getLogger(FlowableProxyResponse.class);

    private final AtomicLong demand = new AtomicLong(0);
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    private ProxyResponse proxyResponse;
    private SyncExecutionContext ctx;
    private ProxyConnection connection;

    private Subscription subscription;
    private Subscriber<? super Buffer> subscriber;

    public void initialize(SyncExecutionContext ctx, ProxyConnection connection, ProxyResponse proxyResponse) {
        this.ctx = ctx;
        this.connection = connection;
        this.proxyResponse = proxyResponse;

        // Always start by pausing the response.
        pauseProxyResponse();
    }

    private void release() {
        Subscription sub = subscription;
        if (sub != null) {
            try {
                proxyResponse.endHandler(null);
                proxyResponse.bodyHandler(null);
            } catch (Exception ignore) {
                cancelProxyResponse();
            }
        }
    }

    @Override
    protected void subscribeActual(Subscriber<? super Buffer> subscriber) {
        if (subscription != null) {
            EmptySubscription.error(new IllegalStateException("This processor allows only a single Subscriber"), subscriber);
            return;
        }

        this.subscriber = subscriber;

        // Pause the proxy response.
        pauseProxyResponse();

        this.subscription = new ProxyResponseSubscription();

        // Capture all the chunks and write them in the response.
        proxyResponse.bodyHandler(this::handleChunk);

        // When complete, propagate the complete event to the reactive chain.
        proxyResponse.endHandler(v -> handleEnd());

        // Finally, pass the subscription to the subscriber, so it can invoke onNext on it.
        subscriber.onSubscribe(subscription);
    }

    private void handleChunk(Buffer chunk) {
        try {
            if (ctx.isInterrupted() || ctx.response().ended()) {
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
        }
    }

    private void handleEnd() {
        release();
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
            if (!cancelled.compareAndSet(false, true)) {
                proxyResponse.cancel();
                connection.cancel();
            }
        } catch (Exception e) {
            log.warn("Unable to properly cancel the proxy response.", e);
        }
    }

    class ProxyResponseSubscription implements Subscription {

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
