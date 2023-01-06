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

import static io.gravitee.gateway.jupiter.reactor.v4.subscription.DefaultSubscriptionDispatcher.ATTR_INTERNAL_TRACING_SPAN;
import static io.gravitee.gateway.jupiter.reactor.v4.subscription.DefaultSubscriptionDispatcher.TRACING_SPAN_NAME;
import static java.util.Calendar.MILLISECOND;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.api.service.SubscriptionService;
import io.gravitee.gateway.jupiter.api.ExecutionPhase;
import io.gravitee.gateway.jupiter.api.context.InternalContextAttributes;
import io.gravitee.gateway.jupiter.api.exception.MessageProcessingException;
import io.gravitee.gateway.jupiter.core.context.MutableExecutionContext;
import io.gravitee.gateway.jupiter.core.context.MutableResponse;
import io.gravitee.gateway.jupiter.core.processor.ProcessorChain;
import io.gravitee.gateway.jupiter.reactor.ApiReactor;
import io.gravitee.gateway.jupiter.reactor.processor.SubscriptionPlatformProcessorChainFactory;
import io.gravitee.gateway.jupiter.reactor.v4.subscription.exceptions.SubscriptionConnectionException;
import io.gravitee.gateway.jupiter.reactor.v4.subscription.exceptions.SubscriptionNotDispatchedException;
import io.gravitee.gateway.reactor.handler.Acceptor;
import io.gravitee.tracing.api.Span;
import io.gravitee.tracing.api.Tracer;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.TestScheduler;
import io.vertx.core.Vertx;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class DefaultSubscriptionDispatcherTest {

    static final String SUBSCRIPTION_ID = "sub-id";

    private DefaultSubscriptionDispatcher dispatcher;

    @Mock
    private Subscription subscription;

    @Mock
    private SubscriptionAcceptorResolver resolver;

    @Mock
    private SubscriptionExecutionRequestFactory factory;

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private SubscriptionPlatformProcessorChainFactory platformProcessorChainFactory;

    @Mock
    private ProcessorChain preProcessorChain;

    @Mock
    private ProcessorChain postProcessorChain;

    @Mock
    private MutableExecutionContext context;

    @Mock
    private ApiReactor reactor;

    private TestScheduler testScheduler;
    private Vertx vertx;

    @BeforeEach
    void init() {
        lenient().when(platformProcessorChainFactory.preProcessorChain()).thenReturn(preProcessorChain);
        lenient().when(platformProcessorChainFactory.postProcessorChain()).thenReturn(postProcessorChain);

        // by default, mock full subscription chain, that just completes
        DefaultSubscriptionAcceptor acceptor = mock(DefaultSubscriptionAcceptor.class);
        lenient().when(factory.create(any())).thenReturn(context);
        lenient().when(acceptor.reactor()).thenReturn(reactor);
        lenient().when(resolver.resolve(any())).thenReturn(acceptor);
        lenient().when(reactor.handle(context)).thenReturn(Completable.complete());
        lenient().when(preProcessorChain.execute(any(), any())).thenReturn(Completable.complete());
        lenient().when(postProcessorChain.execute(any(), any())).thenReturn(Completable.complete());

        testScheduler = new TestScheduler();

        vertx = Vertx.vertx();
        dispatcher = new DefaultSubscriptionDispatcher(resolver, factory, platformProcessorChainFactory, false, vertx);
    }

    @Test
    void shouldNotHandleNotAcceptedSubscription() {
        when(subscription.getId()).thenReturn(SUBSCRIPTION_ID);
        when(subscription.getStatus()).thenReturn("rejected");

        dispatcher.dispatch(subscription).test().assertComplete();

        verifyNoInteractions(resolver);
        verifyNoInteractions(factory);
    }

    @Test
    void shouldNotHandleAcceptedSubscriptionExpired() {
        when(subscription.getId()).thenReturn(SUBSCRIPTION_ID);
        when(subscription.getEndingAt()).thenReturn(addMillisecondsToCurrentDate(-100));
        when(subscription.getStatus()).thenReturn("ACCEPTED");
        when(subscription.getConsumerStatus()).thenReturn(Subscription.ConsumerStatus.STARTED);

        dispatcher.dispatch(subscription).test().assertComplete();

        verifyNoInteractions(resolver);
        verifyNoInteractions(factory);
    }

    @Test
    @DisplayName("Should not handle subscription when no acceptor has been resolved")
    void shouldNotHandleUnresolvedSubscription() {
        when(subscription.getId()).thenReturn(SUBSCRIPTION_ID);
        when(subscription.getStatus()).thenReturn("ACCEPTED");
        when(subscription.getConsumerStatus()).thenReturn(Subscription.ConsumerStatus.STARTED);
        when(resolver.resolve(subscription)).thenReturn(null);

        dispatcher.dispatch(subscription).test().assertError(SubscriptionNotDispatchedException.class);

        verify(resolver, times(1)).resolve(any());
        verifyNoInteractions(factory);
    }

    @Test
    @DisplayName("Should not handle subscription when acceptor has no reactor")
    void shouldNotHandleUnresolvedSubscriptionBecauseNoReactor() {
        when(subscription.getId()).thenReturn(SUBSCRIPTION_ID);
        when(subscription.getStatus()).thenReturn("ACCEPTED");
        when(subscription.getConsumerStatus()).thenReturn(Subscription.ConsumerStatus.STARTED);
        Acceptor<SubscriptionAcceptor> acceptorAcceptor = mock(Acceptor.class);
        when(resolver.resolve(subscription)).thenReturn(acceptorAcceptor);
        when(acceptorAcceptor.reactor()).thenReturn(null);

        dispatcher.dispatch(subscription).test().assertError(SubscriptionNotDispatchedException.class);

        verify(resolver, times(1)).resolve(any());
        verifyNoInteractions(factory);
    }

    @Test
    void shouldNotHandleSubscriptionNoConfiguration() {
        when(subscription.getId()).thenReturn(SUBSCRIPTION_ID);
        when(subscription.getStatus()).thenReturn("ACCEPTED");
        when(subscription.getConsumerStatus()).thenReturn(Subscription.ConsumerStatus.STARTED);

        dispatcher.dispatch(subscription).test().assertError(SubscriptionNotDispatchedException.class);

        verify(resolver, times(1)).resolve(any());
        verifyNoInteractions(factory);
    }

    @Test
    void shouldNotHandleSubscriptionNoConfigurationType() {
        when(subscription.getId()).thenReturn(SUBSCRIPTION_ID);
        when(subscription.getStatus()).thenReturn("ACCEPTED");
        when(subscription.getConsumerStatus()).thenReturn(Subscription.ConsumerStatus.STARTED);
        when(subscription.getConfiguration()).thenReturn("{}");

        dispatcher.dispatch(subscription).test().assertError(SubscriptionNotDispatchedException.class);

        verify(resolver, times(1)).resolve(any());
        verifyNoInteractions(factory);
    }

    @ParameterizedTest
    @ValueSource(ints = { 400, 500 })
    void shouldHandleSubscriptionWithResponseError(int responseStatus) {
        try {
            DefaultSubscriptionAcceptor acceptor = mock(DefaultSubscriptionAcceptor.class);
            ApiReactor reactor = mock(ApiReactor.class);
            MutableExecutionContext context = mock(MutableExecutionContext.class);
            MutableResponse response = mock(MutableResponse.class);

            when(acceptor.reactor()).thenReturn(reactor);
            when(reactor.handle(context)).thenReturn(Completable.complete());

            when(factory.create(subscription)).thenReturn(context);
            when(resolver.resolve(subscription)).thenReturn(acceptor);

            when(subscription.getId()).thenReturn(SUBSCRIPTION_ID);
            when(subscription.getStatus()).thenReturn("ACCEPTED");
            when(subscription.getConsumerStatus()).thenReturn(Subscription.ConsumerStatus.STARTED);
            when(subscription.getConfiguration()).thenReturn("{\"entrypointId\": \"webhook\"}");
            when(context.response()).thenReturn(response);
            if (responseStatus == 400) {
                when(response.isStatus4xx()).thenReturn(true);
            }
            if (responseStatus == 500) {
                when(response.isStatus4xx()).thenReturn(false);
                when(response.isStatus5xx()).thenReturn(true);
            }
            when(response.body()).thenReturn(Maybe.just(Buffer.buffer("Error message")));

            final TestScheduler testScheduler = new TestScheduler();
            RxJavaPlugins.setComputationSchedulerHandler(s -> testScheduler);

            final TestObserver<Void> obs = dispatcher.dispatch(subscription).subscribeOn(testScheduler).test();

            testScheduler.advanceTimeBy(1, SECONDS);
            assertThat(dispatcher.getActiveDisposables()).hasSize(1);
            testScheduler.advanceTimeBy(30, SECONDS);
            assertThat(dispatcher.getActiveDisposables()).isEmpty();

            verify(resolver, times(1)).resolve(any());
            verify(factory, times(6)).create(subscription);
            verify(context, times(6)).setInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_SUBSCRIPTION_TYPE, "webhook");
            verify(context, times(6)).setInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_SUBSCRIPTION, subscription);

            Map<String, Subscription> activeSubscriptions = dispatcher.getActiveSubscriptions();
            assertEquals(1, activeSubscriptions.size());
            assertSame(subscription, activeSubscriptions.get(SUBSCRIPTION_ID));

            obs.assertError(
                t -> {
                    assertThat(t).isInstanceOf(SubscriptionConnectionException.class).hasMessage("Connection error: Error message");
                    return true;
                }
            );
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    void shouldHandleSubscriptionWithConfiguration() {
        DefaultSubscriptionAcceptor acceptor = mock(DefaultSubscriptionAcceptor.class);
        ApiReactor reactor = mock(ApiReactor.class);
        MutableExecutionContext context = mock(MutableExecutionContext.class);
        MutableResponse response = mock(MutableResponse.class);

        when(acceptor.reactor()).thenReturn(reactor);
        when(reactor.handle(context)).thenReturn(Completable.complete());

        when(factory.create(subscription)).thenReturn(context);
        when(resolver.resolve(subscription)).thenReturn(acceptor);

        when(subscription.getId()).thenReturn(SUBSCRIPTION_ID);
        when(subscription.getStatus()).thenReturn("ACCEPTED");
        when(subscription.getConsumerStatus()).thenReturn(Subscription.ConsumerStatus.STARTED);
        when(subscription.getConfiguration()).thenReturn("{\"entrypointId\": \"webhook\"}");
        when(context.response()).thenReturn(response);
        when(response.isStatus4xx()).thenReturn(false);
        when(response.isStatus5xx()).thenReturn(false);

        dispatcher.dispatch(subscription).test().assertComplete();

        verify(resolver, times(1)).resolve(any());
        verify(factory, times(1)).create(subscription);
        verify(context).setInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_SUBSCRIPTION_TYPE, "webhook");
        verify(context).setInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_SUBSCRIPTION, subscription);

        Map<String, Subscription> activeSubscriptions = dispatcher.getActiveSubscriptions();
        assertEquals(1, activeSubscriptions.size());
        assertSame(subscription, activeSubscriptions.get(SUBSCRIPTION_ID));

        assertEquals(1, dispatcher.getActiveDisposables().size());
    }

    @Test
    @DisplayName("Should execute pre processors, then api reactor, then postprocessors")
    void shouldExecutePreprocessorsAndPostprocessors() throws InterruptedException {
        DefaultSubscriptionAcceptor acceptor = mock(DefaultSubscriptionAcceptor.class);
        ApiReactor reactor = mock(ApiReactor.class);
        MutableExecutionContext context = mock(MutableExecutionContext.class);
        MutableResponse response = mock(MutableResponse.class);

        when(acceptor.reactor()).thenReturn(reactor);
        when(reactor.handle(context)).thenReturn(Completable.complete());

        when(factory.create(subscription)).thenReturn(context);
        when(resolver.resolve(subscription)).thenReturn(acceptor);

        when(subscription.getId()).thenReturn(SUBSCRIPTION_ID);
        when(subscription.getStatus()).thenReturn("ACCEPTED");
        when(subscription.getConsumerStatus()).thenReturn(Subscription.ConsumerStatus.STARTED);
        when(subscription.getConfiguration()).thenReturn("{\"entrypointId\": \"webhook\"}");
        when(context.response()).thenReturn(response);
        when(response.isStatus4xx()).thenReturn(false);
        when(response.isStatus5xx()).thenReturn(false);

        dispatcher.dispatch(subscription).test().assertComplete();

        InOrder orderedChain = inOrder(preProcessorChain, reactor, postProcessorChain);
        orderedChain.verify(preProcessorChain).execute(context, ExecutionPhase.REQUEST);
        orderedChain.verify(reactor).handle(context);
        // postProcessorChain is executing in a doFinally, meaning another thread, so we had a timeout to ensure we finish it.
        orderedChain.verify(postProcessorChain, timeout(1000)).execute(context, ExecutionPhase.RESPONSE);

        // no interaction with tracer, nor tracing span
        verify(context, never()).getComponent(Tracer.class);
        verify(context, never()).putInternalAttribute(eq(ATTR_INTERNAL_TRACING_SPAN), any());
    }

    @Test
    @DisplayName("Should init subscription tracing span when tracing is enabled")
    void shouldHandleTracing() {
        dispatcher.tracingEnabled = true;

        DefaultSubscriptionAcceptor acceptor = mock(DefaultSubscriptionAcceptor.class);
        ApiReactor reactor = mock(ApiReactor.class);
        MutableExecutionContext context = mock(MutableExecutionContext.class);
        MutableResponse response = mock(MutableResponse.class);

        when(acceptor.reactor()).thenReturn(reactor);
        when(reactor.handle(context)).thenReturn(Completable.complete());

        when(factory.create(subscription)).thenReturn(context);
        when(resolver.resolve(subscription)).thenReturn(acceptor);

        when(subscription.getId()).thenReturn(SUBSCRIPTION_ID);
        when(subscription.getStatus()).thenReturn("ACCEPTED");
        when(subscription.getConsumerStatus()).thenReturn(Subscription.ConsumerStatus.STARTED);
        when(subscription.getConfiguration()).thenReturn("{\"entrypointId\": \"webhook\"}");
        when(context.response()).thenReturn(response);
        when(response.isStatus4xx()).thenReturn(false);
        when(response.isStatus5xx()).thenReturn(false);

        // mock Tracer component in context, and his frame
        Tracer tracer = mock(Tracer.class);
        when(context.getComponent(Tracer.class)).thenReturn(tracer);
        Span span = mock(Span.class);
        when(tracer.span(TRACING_SPAN_NAME)).thenReturn(span);
        when(context.getInternalAttribute(ATTR_INTERNAL_TRACING_SPAN)).thenReturn(span);

        dispatcher.dispatch(subscription).test().assertComplete();

        // subscription span has been created and put in context attributes
        InOrder orderedChain = inOrder(reactor, tracer, context, preProcessorChain, postProcessorChain);
        orderedChain.verify(preProcessorChain).execute(context, ExecutionPhase.REQUEST);
        orderedChain.verify(reactor).handle(context);
        orderedChain.verify(tracer).span(TRACING_SPAN_NAME);
        orderedChain.verify(context).putInternalAttribute(ATTR_INTERNAL_TRACING_SPAN, span);
        // postProcessorChain and endTracing is executing in a doFinally, meaning another thread, so we had a timeout to ensure we finish it.
        orderedChain.verify(postProcessorChain, timeout(1000)).execute(context, ExecutionPhase.RESPONSE);
        orderedChain.verify(context, timeout(1000)).removeInternalAttribute(ATTR_INTERNAL_TRACING_SPAN);
    }

    @Test
    @DisplayName("Should redispatch subscription without configuration change : keep the active subscription and disposable as it is")
    void shouldRedispatchSubscriptionWithoutConfigurationChange() {
        Subscription originSubscription = new Subscription();
        originSubscription.setId(SUBSCRIPTION_ID);
        originSubscription.setStatus("ACCEPTED");
        originSubscription.setConfiguration("{\"entrypointId\": \"webhook\"}");

        // dispatcher already contains a subscription/disposable in his internal maps
        Disposable originSubscriptionDisposable = mock(Disposable.class);
        dispatcher.getActiveSubscriptions().put(originSubscription.getId(), originSubscription);
        dispatcher.getActiveDisposables().put(originSubscription.getId(), originSubscriptionDisposable);

        // dispatch a subscription with same configuration
        Subscription updatedSubscription = new Subscription();
        updatedSubscription.setId(SUBSCRIPTION_ID);
        updatedSubscription.setStatus("ACCEPTED");
        updatedSubscription.setConfiguration("{\"entrypointId\": \"webhook\"}");

        dispatcher.dispatch(updatedSubscription).test().assertComplete();

        // ensure the disposable of the origin subscription has not been disposed
        verify(originSubscriptionDisposable, never()).dispose();
        assertEquals(1, dispatcher.getActiveDisposables().size());
        assertSame(originSubscriptionDisposable, dispatcher.getActiveDisposables().get(originSubscription.getId()));

        // ensure there is only 1 active subscription with origin configuration
        Map<String, Subscription> activeSubscriptions = dispatcher.getActiveSubscriptions();
        assertEquals(1, activeSubscriptions.size());
        assertEquals("{\"entrypointId\": \"webhook\"}", activeSubscriptions.get(SUBSCRIPTION_ID).getConfiguration());
    }

    @Test
    @DisplayName("Should redispatch subscription with a configuration change : dispose the old subscription and activate the new one")
    void shouldRedispatchSubscriptionWithConfigurationChange() {
        DefaultSubscriptionAcceptor acceptor = mock(DefaultSubscriptionAcceptor.class);
        ApiReactor reactor = mock(ApiReactor.class);
        MutableExecutionContext context = mock(MutableExecutionContext.class);
        MutableResponse response = mock(MutableResponse.class);

        when(acceptor.reactor()).thenReturn(reactor);

        when(factory.create(any())).thenReturn(context);
        when(resolver.resolve(any())).thenReturn(acceptor);
        when(reactor.handle(context)).thenReturn(Completable.complete());
        when(context.response()).thenReturn(response);
        when(response.isStatus4xx()).thenReturn(false);
        when(response.isStatus5xx()).thenReturn(false);

        Subscription originSubscription = new Subscription();
        originSubscription.setId(SUBSCRIPTION_ID);
        originSubscription.setStatus("ACCEPTED");
        originSubscription.setConfiguration("{\"entrypointId\": \"webhook\"}");

        // dispatcher already contains a subscription/disposable in his internal maps
        Disposable originSubscriptionDisposable = mock(Disposable.class);
        dispatcher.getActiveSubscriptions().put(originSubscription.getId(), originSubscription);
        dispatcher.getActiveDisposables().put(originSubscription.getId(), originSubscriptionDisposable);

        // update the subscription configuration, and dispatch it
        Subscription updatedSubscription = new Subscription();
        updatedSubscription.setId(SUBSCRIPTION_ID);
        updatedSubscription.setStatus("ACCEPTED");
        updatedSubscription.setConfiguration("{\"entrypointId\": \"webhook\", \"test\": \"this configuration changed\"}");

        dispatcher.dispatch(updatedSubscription).test().assertComplete();

        // ensure the disposable of the origin subscription has been disposed
        verify(originSubscriptionDisposable).dispose();
        assertEquals(1, dispatcher.getActiveDisposables().size());
        assertNotSame(originSubscriptionDisposable, dispatcher.getActiveDisposables().get(originSubscription.getId()));

        // ensure there is only 1 active subscription with updated configuration
        Map<String, Subscription> activeSubscriptions = dispatcher.getActiveSubscriptions();
        assertEquals(1, activeSubscriptions.size());
        assertEquals(
            "{\"entrypointId\": \"webhook\", \"test\": \"this configuration changed\"}",
            activeSubscriptions.get(SUBSCRIPTION_ID).getConfiguration()
        );
    }

    @Test
    void shouldRedispatchSubscriptionWithForceDispatch() {
        DefaultSubscriptionAcceptor acceptor = mock(DefaultSubscriptionAcceptor.class);
        ApiReactor reactor = mock(ApiReactor.class);
        MutableExecutionContext context = mock(MutableExecutionContext.class);
        MutableResponse response = mock(MutableResponse.class);

        when(acceptor.reactor()).thenReturn(reactor);

        when(factory.create(any())).thenReturn(context);
        when(resolver.resolve(any())).thenReturn(acceptor);
        when(reactor.handle(context)).thenReturn(Completable.complete());
        when(context.response()).thenReturn(response);
        when(response.isStatus4xx()).thenReturn(false);
        when(response.isStatus5xx()).thenReturn(false);

        Subscription originSubscription = new Subscription();
        originSubscription.setId(SUBSCRIPTION_ID);
        originSubscription.setStatus("ACCEPTED");
        originSubscription.setConfiguration("{\"entrypointId\": \"webhook\"}");

        // dispatcher already contains a subscription/disposable in his internal maps
        Disposable originSubscriptionDisposable = mock(Disposable.class);
        dispatcher.getActiveSubscriptions().put(originSubscription.getId(), originSubscription);
        dispatcher.getActiveDisposables().put(originSubscription.getId(), originSubscriptionDisposable);

        // update the subscription configuration, and dispatch it
        Subscription updatedSubscription = new Subscription();
        updatedSubscription.setId(SUBSCRIPTION_ID);
        updatedSubscription.setStatus("ACCEPTED");
        updatedSubscription.setConfiguration("{\"entrypointId\": \"webhook\"}");
        updatedSubscription.setForceDispatch(true);
        dispatcher.dispatch(updatedSubscription).test().assertComplete();

        // ensure the disposable of the origin subscription has been disposed
        verify(originSubscriptionDisposable).dispose();
        assertEquals(1, dispatcher.getActiveDisposables().size());
        assertNotSame(originSubscriptionDisposable, dispatcher.getActiveDisposables().get(originSubscription.getId()));

        // ensure there is only 1 active subscription with updated configuration
        Map<String, Subscription> activeSubscriptions = dispatcher.getActiveSubscriptions();
        assertThat(activeSubscriptions).hasSize(1);
        assertThat(activeSubscriptions.get(SUBSCRIPTION_ID).getConfiguration()).isEqualTo("{\"entrypointId\": \"webhook\"}");
    }

    @Test
    @DisplayName("Should dispose subscription when its end date is reached")
    void shouldDisposeSubscriptionWhenEndDateReached() {
        DefaultSubscriptionAcceptor acceptor = mock(DefaultSubscriptionAcceptor.class);
        ApiReactor reactor = mock(ApiReactor.class);
        MutableExecutionContext context = mock(MutableExecutionContext.class);

        when(acceptor.reactor()).thenReturn(reactor);
        when(reactor.handle(context)).thenReturn(mock(Completable.class));

        when(factory.create(subscription)).thenReturn(context);
        when(resolver.resolve(subscription)).thenReturn(acceptor);

        when(subscription.getId()).thenReturn(SUBSCRIPTION_ID);
        when(subscription.getStatus()).thenReturn("ACCEPTED");
        when(subscription.getConsumerStatus()).thenReturn(Subscription.ConsumerStatus.STARTED);
        // subscription ends in 100 ms
        when(subscription.getEndingAt()).thenReturn(addMillisecondsToCurrentDate(5000));
        when(subscription.getConfiguration()).thenReturn("{\"entrypointId\": \"webhook\"}");

        final TestScheduler testScheduler = new TestScheduler();
        RxJavaPlugins.setComputationSchedulerHandler(s -> testScheduler);

        final TestObserver<Void> obs = dispatcher.dispatch(subscription).subscribeOn(testScheduler).test();

        obs.assertNotComplete();
        testScheduler.advanceTimeBy(10, SECONDS);
        obs.assertComplete();

        Disposable disposable = dispatcher.getActiveDisposables().get(SUBSCRIPTION_ID);
        assertNull(disposable);
    }

    @Test
    @DisplayName("Should dispose subscription when retried more times than the limit")
    void shouldDisposeSubscriptionWhenRetryFails() {
        try {
            final TestScheduler testScheduler = new TestScheduler();
            RxJavaPlugins.setComputationSchedulerHandler(s -> testScheduler);

            DefaultSubscriptionAcceptor acceptor = mock(DefaultSubscriptionAcceptor.class);
            ApiReactor reactor = mock(ApiReactor.class);
            MutableExecutionContext context = mock(MutableExecutionContext.class);

            when(acceptor.reactor()).thenReturn(reactor);
            AtomicInteger atomicCpt = new AtomicInteger(0);
            when(reactor.handle(context))
                .thenReturn(
                    Completable.create(
                        emitter -> {
                            int cpt = atomicCpt.incrementAndGet();
                            if (cpt <= 10) {
                                emitter.onError(new RuntimeException("exception " + cpt));
                            } else {
                                emitter.onComplete();
                            }
                        }
                    )
                );

            when(factory.create(subscription)).thenReturn(context);
            when(resolver.resolve(subscription)).thenReturn(acceptor);

            when(subscription.getId()).thenReturn(SUBSCRIPTION_ID);
            when(subscription.getStatus()).thenReturn("ACCEPTED");
            when(subscription.getConsumerStatus()).thenReturn(Subscription.ConsumerStatus.STARTED);
            when(subscription.getConfiguration()).thenReturn("{\"entrypointId\": \"webhook\"}");

            final TestObserver<Void> obs = dispatcher.dispatch(subscription).test();

            // for now, subscription is not disposed
            Disposable disposable = dispatcher.getActiveDisposables().get(SUBSCRIPTION_ID);
            assertFalse(disposable.isDisposed());

            // Advance time by 30 seconds because we have max 5 retry every 3 seconds
            testScheduler.advanceTimeBy(30, SECONDS);
            assertFalse(dispatcher.getActiveDisposables().containsKey(SUBSCRIPTION_ID));
            obs.assertError(t -> t instanceof RuntimeException && t.getMessage().equals("exception 6"));
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    @DisplayName("Should dispose subscription when a non retryable exception occurs")
    void shouldDisposeSubscriptionWhenMessageProcessingException() {
        try {
            final TestScheduler testScheduler = new TestScheduler();
            RxJavaPlugins.setComputationSchedulerHandler(s -> testScheduler);

            DefaultSubscriptionAcceptor acceptor = mock(DefaultSubscriptionAcceptor.class);
            ApiReactor reactor = mock(ApiReactor.class);
            MutableExecutionContext context = mock(MutableExecutionContext.class);

            when(acceptor.reactor()).thenReturn(reactor);
            AtomicInteger atomicCpt = new AtomicInteger(0);
            when(reactor.handle(context))
                .thenReturn(
                    Completable.create(
                        emitter -> {
                            int cpt = atomicCpt.incrementAndGet();
                            if (cpt <= 10) {
                                emitter.onError(new MessageProcessingException("exception " + cpt));
                            } else {
                                emitter.onComplete();
                            }
                        }
                    )
                );

            when(factory.create(subscription)).thenReturn(context);
            when(resolver.resolve(subscription)).thenReturn(acceptor);

            when(subscription.getId()).thenReturn(SUBSCRIPTION_ID);
            when(subscription.getStatus()).thenReturn("ACCEPTED");
            when(subscription.getConsumerStatus()).thenReturn(Subscription.ConsumerStatus.STARTED);
            when(subscription.getConfiguration()).thenReturn("{\"entrypointId\": \"webhook\"}");

            final TestObserver<Void> obs = dispatcher.dispatch(subscription).test();

            Disposable disposable = dispatcher.getActiveDisposables().get(SUBSCRIPTION_ID);
            assertTrue(disposable.isDisposed());

            // No need to wait, exception stops immediately the flow
            obs.assertError(t -> t instanceof MessageProcessingException && t.getMessage().equals("exception 1"));
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    @DisplayName("Should subscribe when its start date is reached")
    void shouldSubscribeSubscriptionWhenStartDateReached() {
        DefaultSubscriptionAcceptor acceptor = mock(DefaultSubscriptionAcceptor.class);
        ApiReactor reactor = mock(ApiReactor.class);
        MutableExecutionContext context = mock(MutableExecutionContext.class);
        MutableResponse response = mock(MutableResponse.class);

        when(acceptor.reactor()).thenReturn(reactor);
        AtomicBoolean subscribed = new AtomicBoolean(false);
        Completable completable = Completable.complete().doOnSubscribe(s -> subscribed.set(true));
        when(reactor.handle(context)).thenReturn(completable);

        when(factory.create(subscription)).thenReturn(context);
        when(resolver.resolve(subscription)).thenReturn(acceptor);

        when(subscription.getId()).thenReturn(SUBSCRIPTION_ID);
        when(subscription.getStatus()).thenReturn("ACCEPTED");
        when(subscription.getConsumerStatus()).thenReturn(Subscription.ConsumerStatus.STARTED);

        // subscription starts in 5000 ms
        when(subscription.getStartingAt()).thenReturn(addMillisecondsToCurrentDate(5000));
        when(subscription.getConfiguration()).thenReturn("{\"entrypointId\": \"webhook\"}");
        when(context.response()).thenReturn(response);
        when(response.isStatus4xx()).thenReturn(false);
        when(response.isStatus5xx()).thenReturn(false);

        final TestScheduler testScheduler = new TestScheduler();
        RxJavaPlugins.setComputationSchedulerHandler(s -> testScheduler);

        final TestObserver<Void> obs = dispatcher.dispatch(subscription).subscribeOn(testScheduler).test();

        // for now, it's not subscribed
        assertThat(subscribed.get()).isFalse();

        testScheduler.advanceTimeBy(6000, MILLISECONDS);

        assertThat(subscribed.get()).isTrue();

        obs.assertComplete();
    }

    @Test
    void shouldDisposeSubscription() throws InterruptedException {
        DefaultSubscriptionAcceptor acceptor = mock(DefaultSubscriptionAcceptor.class);
        ApiReactor reactor = mock(ApiReactor.class);
        MutableExecutionContext context = mock(MutableExecutionContext.class);
        MutableResponse response = mock(MutableResponse.class);

        when(acceptor.reactor()).thenReturn(reactor);
        when(reactor.handle(context)).thenReturn(Completable.complete());

        when(factory.create(subscription)).thenReturn(context);
        when(resolver.resolve(subscription)).thenReturn(acceptor);

        when(subscription.getId()).thenReturn(SUBSCRIPTION_ID);
        when(subscription.getStatus()).thenReturn("ACCEPTED");
        when(subscription.getConfiguration()).thenReturn("{\"entrypointId\": \"webhook\"}");
        when(context.response()).thenReturn(response);
        when(response.isStatus4xx()).thenReturn(false);
        when(response.isStatus5xx()).thenReturn(false);

        // Dispatch accepted subscription
        dispatcher.dispatch(subscription).test().await().assertComplete();

        // Dispose an existing subscription
        when(subscription.getStatus()).thenReturn("CANCELED");
        dispatcher.dispatch(subscription).test().await().assertComplete();

        assertTrue(dispatcher.getActiveSubscriptions().isEmpty());
        assertTrue(dispatcher.getActiveDisposables().isEmpty());

        when(subscription.getStatus()).thenReturn("ACCEPTED");
        when(subscription.getConsumerStatus()).thenReturn(Subscription.ConsumerStatus.STARTED);
        dispatcher.dispatch(subscription).test().await().assertComplete();
        assertThat(dispatcher.getActiveSubscriptions()).hasSize(1);

        when(subscription.getConsumerStatus()).thenReturn(Subscription.ConsumerStatus.STOPPED);
        dispatcher.dispatch(subscription).test().await().assertComplete();
        assertThat(dispatcher.getActiveSubscriptions()).isEmpty();

        when(subscription.getStatus()).thenReturn("PAUSED");
        when(subscription.getConsumerStatus()).thenReturn(Subscription.ConsumerStatus.STOPPED);
        dispatcher.dispatch(subscription).test().await().assertComplete();
        assertThat(dispatcher.getActiveSubscriptions()).isEmpty();

        when(subscription.getStatus()).thenReturn("PAUSED");
        when(subscription.getConsumerStatus()).thenReturn(Subscription.ConsumerStatus.STARTED);
        dispatcher.dispatch(subscription).test().await().assertComplete();
        assertThat(dispatcher.getActiveSubscriptions()).isEmpty();

        when(subscription.getStatus()).thenReturn("ACCEPTED");
        when(subscription.getConsumerStatus()).thenReturn(Subscription.ConsumerStatus.STARTED);
        dispatcher.dispatch(subscription).test().await().assertComplete();
        assertThat(dispatcher.getActiveSubscriptions()).hasSize(1);
    }

    @Test
    void shouldDisposeSubscriptions() throws Exception {
        DefaultSubscriptionAcceptor acceptor = mock(DefaultSubscriptionAcceptor.class);
        ApiReactor reactor = mock(ApiReactor.class);
        MutableExecutionContext context = mock(MutableExecutionContext.class);
        MutableResponse response = mock(MutableResponse.class);

        when(acceptor.reactor()).thenReturn(reactor);
        when(reactor.handle(context)).thenReturn(Completable.complete());

        when(factory.create(subscription)).thenReturn(context);
        when(resolver.resolve(subscription)).thenReturn(acceptor);

        when(subscription.getId()).thenReturn(SUBSCRIPTION_ID);
        when(subscription.getStatus()).thenReturn("ACCEPTED");
        when(subscription.getConsumerStatus()).thenReturn(Subscription.ConsumerStatus.STARTED);
        when(subscription.getConfiguration()).thenReturn("{\"entrypointId\": \"webhook\"}");
        when(context.response()).thenReturn(response);
        when(response.isStatus4xx()).thenReturn(false);
        when(response.isStatus5xx()).thenReturn(false);

        // Dispatch accepted subscription
        dispatcher.dispatch(subscription).test().assertComplete();

        // Dispose all subscriptions
        dispatcher.doStop();

        assertTrue(dispatcher.getActiveSubscriptions().isEmpty());
        assertTrue(dispatcher.getActiveDisposables().isEmpty());
    }

    private Date addMillisecondsToCurrentDate(int milliseconds) {
        Calendar cal = Calendar.getInstance();
        cal.add(MILLISECOND, milliseconds);
        return cal.getTime();
    }
}
