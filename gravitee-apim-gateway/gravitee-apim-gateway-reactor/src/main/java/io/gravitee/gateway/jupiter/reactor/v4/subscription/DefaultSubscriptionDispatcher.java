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
package io.gravitee.gateway.jupiter.reactor.v4.subscription;

import static io.gravitee.gateway.jupiter.api.context.InternalContextAttributes.ATTR_INTERNAL_LISTENER_TYPE;
import static io.gravitee.gateway.jupiter.api.context.InternalContextAttributes.ATTR_INTERNAL_SECURITY_SKIP;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.service.AbstractService;
import io.gravitee.common.utils.RxHelper;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.jupiter.api.ExecutionPhase;
import io.gravitee.gateway.jupiter.api.ListenerType;
import io.gravitee.gateway.jupiter.api.context.ContextAttributes;
import io.gravitee.gateway.jupiter.api.context.InternalContextAttributes;
import io.gravitee.gateway.jupiter.api.exception.MessageProcessingException;
import io.gravitee.gateway.jupiter.api.hook.ChainHook;
import io.gravitee.gateway.jupiter.core.context.MutableExecutionContext;
import io.gravitee.gateway.jupiter.core.hook.HookHelper;
import io.gravitee.gateway.jupiter.core.processor.ProcessorChain;
import io.gravitee.gateway.jupiter.core.tracing.TracingHook;
import io.gravitee.gateway.jupiter.reactor.ApiReactor;
import io.gravitee.gateway.jupiter.reactor.processor.SubscriptionPlatformProcessorChainFactory;
import io.gravitee.gateway.jupiter.reactor.v4.subscription.exceptions.SubscriptionConnectionException;
import io.gravitee.gateway.jupiter.reactor.v4.subscription.exceptions.SubscriptionExpiredException;
import io.gravitee.gateway.jupiter.reactor.v4.subscription.exceptions.SubscriptionNotDispatchedException;
import io.gravitee.tracing.api.Span;
import io.gravitee.tracing.api.Tracer;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.CompletableEmitter;
import io.reactivex.rxjava3.core.CompletableTransformer;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Predicate;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DefaultSubscriptionDispatcher extends AbstractService<SubscriptionDispatcher> implements SubscriptionDispatcher {

    public static final int ON_SUBSCRIPTION_ERROR_RETRY_COUNT = 5;
    public static final int ON_SUBSCRIPTION_ERROR_RETRY_DELAY_MS = 3_000;

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultSubscriptionDispatcher.class);
    private static final String SUBSCRIPTION_ENTRYPOINT_FIELD = "entrypointId";

    protected static final String TRACING_SPAN_NAME = "SUBSCRIPTION";
    protected static final String ATTR_INTERNAL_TRACING_SPAN = "subscription-tracing-span";

    private final Map<String, Subscription> activeSubscriptions = new ConcurrentHashMap<>();
    private final Map<String, Disposable> activeDisposables = new ConcurrentHashMap<>();

    private final SubscriptionAcceptorResolver subscriptionAcceptorResolver;
    private final ObjectMapper mapper = new ObjectMapper();
    private final SubscriptionExecutionContextFactory subscriptionExecutionContextFactory;
    private final SubscriptionPlatformProcessorChainFactory platformProcessorChainFactory;
    private final Vertx vertx;
    private final List<ChainHook> processorChainHooks;
    protected boolean tracingEnabled;

    public DefaultSubscriptionDispatcher(
        SubscriptionAcceptorResolver subscriptionAcceptorResolver,
        SubscriptionExecutionContextFactory subscriptionExecutionContextFactory,
        SubscriptionPlatformProcessorChainFactory platformProcessorChainFactory,
        boolean tracingEnabled,
        Vertx vertx
    ) {
        this.subscriptionAcceptorResolver = subscriptionAcceptorResolver;
        this.subscriptionExecutionContextFactory = subscriptionExecutionContextFactory;
        this.platformProcessorChainFactory = platformProcessorChainFactory;
        this.tracingEnabled = tracingEnabled;
        this.processorChainHooks = tracingEnabled ? List.of(new TracingHook("processor-chain")) : new ArrayList<>();
        this.vertx = vertx;
    }

    @Override
    public Completable dispatch(Subscription subscriptionToDispatch) {
        return Completable.create(
            emitter -> {
                if (
                    statusIsAccepted(subscriptionToDispatch) &&
                    consumerStatusIsActive(subscriptionToDispatch) &&
                    !isExpired(subscriptionToDispatch)
                ) {
                    try {
                        activeSubscriptions.compute(
                            subscriptionToDispatch.getId(),
                            (subscriptionId, activeSubscription) -> {
                                // activate subscription if not yet active
                                if (activeSubscription == null) {
                                    return activateSubscription(subscriptionToDispatch, emitter);
                                }
                                // if subscription already active modified, update it
                                else if (!subscriptionToDispatch.equals(activeSubscription) || subscriptionToDispatch.isForceDispatch()) {
                                    return updateSubscription(subscriptionToDispatch, emitter);
                                }
                                // otherwise, keep active subscription as it is
                                emitter.onComplete();
                                return activeSubscription;
                            }
                        );
                    } catch (SubscriptionNotDispatchedException e) {
                        emitter.onError(e);
                    }
                }
                // dispose subscription if it's not accepted, or already expired
                else {
                    disposeSubscription(subscriptionToDispatch.getId());
                    emitter.onComplete();
                }
            }
        );
    }

    private static boolean statusIsAccepted(Subscription subscriptionToDispatch) {
        return "ACCEPTED".equalsIgnoreCase(subscriptionToDispatch.getStatus());
    }

    private static boolean consumerStatusIsActive(Subscription subscriptionToDispatch) {
        return Subscription.ConsumerStatus.STARTED.equals(subscriptionToDispatch.getConsumerStatus());
    }

    private Subscription activateSubscription(Subscription subscription, CompletableEmitter emitter) {
        SubscriptionAcceptor subscriptionAcceptor = subscriptionAcceptorResolver.resolve(subscription);

        if (subscriptionAcceptor != null && subscriptionAcceptor.reactor() != null) {
            ApiReactor apiReactor = (ApiReactor) subscriptionAcceptor.reactor();

            String configuration = subscription.getConfiguration();
            try {
                // Extract the type from the configuration
                String type = mapper.readTree(configuration).path(SUBSCRIPTION_ENTRYPOINT_FIELD).asText();
                if (type == null || type.trim().isEmpty()) {
                    LOGGER.error("Unable to handle subscription without known entrypoint id");
                } else {
                    Completable subscriptionObs = handleSubscription(subscription, apiReactor, type)
                        .onErrorComplete(
                            throwable -> {
                                emitter.onError(throwable);
                                return true;
                            }
                        )
                        .doOnSubscribe(disposable -> LOGGER.debug("Subscription [{}] activated", subscription.getId()))
                        .doOnComplete(
                            () -> {
                                activeDisposables.remove(subscription.getId());
                                emitter.onComplete();
                            }
                        );

                    activeDisposables.put(subscription.getId(), subscriptionObs.subscribe());

                    return subscription;
                }
            } catch (Exception ex) {
                throw new SubscriptionNotDispatchedException(ex);
            }
        }
        throw new SubscriptionNotDispatchedException(String.format("No acceptor available for subscription [%s]", subscription.getId()));
    }

    private Completable handleSubscription(Subscription subscription, ApiReactor apiReactor, String type) {
        return Completable
            .defer(
                () -> {
                    MutableExecutionContext context = subscriptionExecutionContextFactory.create(subscription);

                    // This attribute is used by connectors
                    context.setInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_REACTABLE_API, apiReactor.api());
                    context.setInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_SUBSCRIPTION_TYPE, type);
                    context.setInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_SUBSCRIPTION, subscription);
                    context.setAttribute(ContextAttributes.ATTR_PLAN, subscription.getPlan());
                    context.setAttribute(ContextAttributes.ATTR_APPLICATION, subscription.getApplication());
                    context.setAttribute(ContextAttributes.ATTR_SUBSCRIPTION_ID, subscription.getId());

                    // Skip the security chain (currently requires to be an attribute as long as we support v3).
                    context.setInternalAttribute(ATTR_INTERNAL_SECURITY_SKIP, true);

                    context.setInternalAttribute(ATTR_INTERNAL_LISTENER_TYPE, ListenerType.SUBSCRIPTION);
                    return executeSubscriptionChain(apiReactor, context)
                        // Apply a delay before starting the subscription if it has a starting date
                        .compose(delayToStartDate(subscription))
                        // Apply a timeout to the subscription if it has an ending date
                        .compose(timeoutAtEndingDate(subscription))
                        .compose(verifyHttpResponseError(subscription, context))
                        // Manage functional errors, for example when a subscription expires
                        .onErrorComplete(shouldCompleteOnError());
                }
            )
            .compose(
                RxHelper.retry(
                    ON_SUBSCRIPTION_ERROR_RETRY_COUNT,
                    ON_SUBSCRIPTION_ERROR_RETRY_DELAY_MS,
                    MILLISECONDS,
                    isThrowableRetryable()
                )
            );
    }

    private Completable executeSubscriptionChain(ApiReactor reactorHandler, MutableExecutionContext ctx) {
        Context vertxContext = vertx.getOrCreateContext();
        // initialize tracing span if tracing is enabled
        return initTracingSpan(ctx)
            // execute pre processor chain
            .andThen(executePreProcessorChain(ctx))
            // execute reactor handler
            .andThen(Completable.defer(() -> reactorHandler.handle(ctx)))
            // execute post processors
            // we have to run it on the same vertx context as subscriber's, cause tracing mechanism is relying on vertx context
            .doFinally(
                () ->
                    vertxContext.runOnContext(
                        v -> executePostProcessorChain(ctx).andThen(endTracingSpan(ctx)).onErrorComplete().subscribe()
                    )
            );
    }

    private Completable executePreProcessorChain(MutableExecutionContext ctx) {
        ProcessorChain preProcessorChain = platformProcessorChainFactory.preProcessorChain();
        return HookHelper.hook(
            () -> preProcessorChain.execute(ctx, ExecutionPhase.REQUEST),
            preProcessorChain.getId(),
            processorChainHooks,
            ctx,
            ExecutionPhase.REQUEST
        );
    }

    private Completable executePostProcessorChain(MutableExecutionContext ctx) {
        ProcessorChain postProcessorChain = platformProcessorChainFactory.postProcessorChain();
        return HookHelper
            .hook(
                () -> postProcessorChain.execute(ctx, ExecutionPhase.RESPONSE),
                postProcessorChain.getId(),
                processorChainHooks,
                ctx,
                ExecutionPhase.RESPONSE
            )
            .onErrorComplete();
    }

    private Completable initTracingSpan(MutableExecutionContext ctx) {
        return Completable.fromRunnable(
            () -> {
                if (tracingEnabled) {
                    Tracer tracer = ctx.getComponent(Tracer.class);
                    if (tracer != null) {
                        Span span = tracer.span(TRACING_SPAN_NAME);
                        ctx.putInternalAttribute(ATTR_INTERNAL_TRACING_SPAN, span);
                    }
                }
            }
        );
    }

    private Completable endTracingSpan(MutableExecutionContext ctx) {
        return Completable.fromRunnable(
            () -> {
                if (tracingEnabled) {
                    Span span = ctx.getInternalAttribute(ATTR_INTERNAL_TRACING_SPAN);
                    if (span != null) {
                        span.end();
                        ctx.removeInternalAttribute(ATTR_INTERNAL_TRACING_SPAN);
                    }
                }
            }
        );
    }

    /**
     * @return a Predicate testing if a throwable is retryable or not.
     */
    private java.util.function.Predicate<Throwable> isThrowableRetryable() {
        return throwable -> !(throwable instanceof MessageProcessingException);
    }

    private CompletableTransformer delayToStartDate(Subscription subscription) {
        return upstream -> {
            if (subscription.getStartingAt() != null) {
                long subscriptionStartDelay = getMillisecondsTo(subscription.getStartingAt());
                return upstream.delaySubscription(subscriptionStartDelay, MILLISECONDS);
            }
            return upstream;
        };
    }

    private CompletableTransformer timeoutAtEndingDate(Subscription subscription) {
        return upstream -> {
            if (subscription.getEndingAt() != null) {
                long subscriptionEndDelay = getMillisecondsTo(subscription.getEndingAt());
                return upstream.timeout(
                    subscriptionEndDelay,
                    MILLISECONDS,
                    Completable.error(new SubscriptionExpiredException(subscription))
                );
            }
            return upstream;
        };
    }

    private static Predicate<Throwable> shouldCompleteOnError() {
        return throwable -> {
            if (throwable instanceof SubscriptionExpiredException) {
                LOGGER.debug(throwable.getMessage());
                return true;
            }
            // manage functional error to complete normally here
            return false;
        };
    }

    private CompletableTransformer verifyHttpResponseError(Subscription subscription, MutableExecutionContext context) {
        return upstream ->
            upstream.andThen(
                Completable.defer(
                    () -> {
                        if (context.response().isStatus4xx() || context.response().isStatus5xx()) {
                            return context
                                .response()
                                .body()
                                .flatMapCompletable(buffer -> Completable.error(new SubscriptionConnectionException(buffer.toString())));
                        }
                        return Completable.complete();
                    }
                )
            );
    }

    /**
     * Dispose all active subscriptions.
     */
    private void disposeAllSubscriptions() {
        for (String subscriptionId : activeDisposables.keySet()) {
            disposeSubscription(subscriptionId);
        }
    }

    /**
     * Dispose the specified subscription.
     *
     * @param subscriptionId
     */
    private void disposeSubscription(String subscriptionId) {
        Disposable disposable = activeDisposables.remove(subscriptionId);
        if (disposable != null) {
            try {
                disposable.dispose();
            } catch (Exception e) {
                LOGGER.warn("Unexpected exception while disposing subscription [{}]: {}", subscriptionId, e.getMessage());
            }
        }
        activeSubscriptions.remove(subscriptionId);
    }

    /**
     * Update the specified subscription.
     *
     * @param subscription The subscription to update
     * @param emitter
     * @return updated subscription
     */
    private Subscription updateSubscription(Subscription subscription, CompletableEmitter emitter) {
        // dispose old subscription, but keep it in map
        // as it will be replaced by subsequent call to activateSubscription
        if (activeDisposables.containsKey(subscription.getId())) {
            activeDisposables.get(subscription.getId()).dispose();
        }
        return activateSubscription(subscription, emitter);
    }

    /**
     * Get delay from current time to specified date, in milliseconds.
     *
     * @param to
     * @return delay in milliseconds. 0 if to is before current time.
     */
    private long getMillisecondsTo(Date to) {
        long delay = to.getTime() - Calendar.getInstance().getTimeInMillis();
        return delay > 0 ? delay : 0;
    }

    /**
     * Is the specified subscription expired at current time ?
     *
     * @param subscription
     * @return true if it's expired, false otherwise
     */
    private boolean isExpired(Subscription subscription) {
        return subscription.getEndingAt() != null && subscription.getEndingAt().before(Calendar.getInstance().getTime());
    }

    @Override
    protected void doStop() throws Exception {
        this.disposeAllSubscriptions();
    }

    protected Map<String, Subscription> getActiveSubscriptions() {
        return activeSubscriptions;
    }

    protected Map<String, Disposable> getActiveDisposables() {
        return activeDisposables;
    }
}
