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
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DefaultSubscriptionDispatcherTest {

    static final String SUBSCRIPTION_ID = "sub-id";

    private DefaultSubscriptionDispatcher dispatcher;

    private Subscription subscription;

    @Mock
    private SubscriptionAcceptorResolver resolver;

    @Mock
    private SubscriptionExecutionContextFactory factory;

    @Mock
    private ProcessorChain preProcessorChain;

    @Mock
    private ProcessorChain postProcessorChain;

    @Mock
    private MutableExecutionContext context;

    @Mock
    private MutableResponse response;

    @Mock
    private ApiReactor reactor;

    @Mock
    private DefaultSubscriptionAcceptor acceptor;

    @Mock
    private SubscriptionPlatformProcessorChainFactory platformProcessorChainFactory;

    private TestScheduler testScheduler;
    private Vertx vertx;

    @BeforeEach
    void init() {
        lenient().when(preProcessorChain.execute(any(), any())).thenReturn(Completable.complete());
        lenient().when(preProcessorChain.getId()).thenReturn("preProcessorChain");
        lenient().when(postProcessorChain.execute(any(), any())).thenReturn(Completable.complete());
        lenient().when(postProcessorChain.getId()).thenReturn("postProcessorChain");

        lenient().when(platformProcessorChainFactory.preProcessorChain()).thenReturn(preProcessorChain);
        lenient().when(platformProcessorChainFactory.postProcessorChain()).thenReturn(postProcessorChain);

        lenient().when(resolver.resolve(any())).thenReturn(acceptor);

        testScheduler = new TestScheduler();
        // ensure we use the same TestScheduler everywhere
        RxJavaPlugins.setComputationSchedulerHandler(s -> testScheduler);

        vertx = Vertx.vertx();
        dispatcher = new DefaultSubscriptionDispatcher(resolver, factory, platformProcessorChainFactory, false, vertx);

        subscription = new Subscription();
        subscription.setId(SUBSCRIPTION_ID);
    }

    @AfterEach
    void tearDown() {
        RxJavaPlugins.reset();
    }

    @Nested
    class DispatchOnce {

        @Nested
        class RejectedSubscription {

            @Test
            void should_not_be_handled() {
                subscription.setStatus("REJECTED");

                dispatcher.dispatch(subscription).test().assertComplete();

                verifyNoInteractions(resolver);
                verifyNoInteractions(factory);
            }
        }

        @Nested
        class ExpiredSubscription {

            @Test
            void should_not_be_handled() {
                subscription.setStatus("ACCEPTED");
                subscription.setConsumerStatus(Subscription.ConsumerStatus.STARTED);
                subscription.setEndingAt(addMillisecondsToCurrentDate(-100));

                dispatcher.dispatch(subscription).test().assertComplete();

                verifyNoInteractions(resolver);
                verifyNoInteractions(factory);
            }
        }

        @Nested
        class AcceptedSubscription {

            @BeforeEach
            void setUp() {
                subscription.setStatus("ACCEPTED");
                subscription.setConsumerStatus(Subscription.ConsumerStatus.STARTED);
                subscription.setConfiguration("{\"entrypointId\": \"webhook\"}");
            }

            @Test
            void should_throw_when_no_acceptor_has_been_resolved() {
                when(resolver.resolve(subscription)).thenReturn(null);

                dispatcher.dispatch(subscription).test().assertError(SubscriptionNotDispatchedException.class);

                verify(resolver, times(1)).resolve(any());
                verifyNoInteractions(factory);
            }

            @Test
            void should_throw_when_acceptor_has_no_reactor() {
                when(acceptor.reactor()).thenReturn(null);

                dispatcher.dispatch(subscription).test().assertError(SubscriptionNotDispatchedException.class);

                verifyNoInteractions(factory);
            }

            @ParameterizedTest
            @ValueSource(strings = { "{}" })
            @NullAndEmptySource
            void should_throw_when_subscription_has_incorrect_configuration(String configuration) {
                subscription.setConfiguration(configuration);

                dispatcher.dispatch(subscription).test().assertError(SubscriptionNotDispatchedException.class);

                verifyNoInteractions(factory);
            }

            @Test
            void should_keep_a_reference_on_dispatched_subscription() {
                mockSubscriptionChainToJustComplete();
                dispatcher.dispatch(subscription).test().assertComplete();

                verify(factory, times(1)).create(subscription);
                verify(context).setInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_SUBSCRIPTION_TYPE, "webhook");
                verify(context).setInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_SUBSCRIPTION, subscription);

                Map<String, Subscription> activeSubscriptions = dispatcher.getActiveSubscriptions();
                assertEquals(1, activeSubscriptions.size());
                assertSame(subscription, activeSubscriptions.get(SUBSCRIPTION_ID));

                assertEquals(1, dispatcher.getActiveDisposables().size());
            }

            @Test
            void should_execute_pre_processors_then_api_reactor_then_postprocessors() {
                mockSubscriptionChainToJustComplete();
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
            void should_init_subscription_tracing_when_tracing_is_enabled() {
                dispatcher.tracingEnabled = true;

                // mock Tracer component in context, and his frame
                Tracer tracer = mock(Tracer.class);
                when(context.getComponent(Tracer.class)).thenReturn(tracer);
                Span span = mock(Span.class);
                when(tracer.span(TRACING_SPAN_NAME)).thenReturn(span);
                when(context.getInternalAttribute(ATTR_INTERNAL_TRACING_SPAN)).thenReturn(span);

                mockSubscriptionChainToJustComplete();
                dispatcher.dispatch(subscription).test().assertComplete();

                // subscription span has been created and put in context attributes
                InOrder orderedChain = inOrder(preProcessorChain, tracer, reactor, context, postProcessorChain, span);
                orderedChain.verify(preProcessorChain).execute(context, ExecutionPhase.REQUEST);
                orderedChain.verify(tracer).span(TRACING_SPAN_NAME);
                orderedChain.verify(context).putInternalAttribute(ATTR_INTERNAL_TRACING_SPAN, span);
                orderedChain.verify(reactor).handle(context);
                // postProcessorChain and endTracing is executing in a doFinally, meaning another thread, so we had a timeout to ensure we finish it.
                orderedChain.verify(postProcessorChain, timeout(1000)).execute(context, ExecutionPhase.RESPONSE);
                orderedChain.verify(span).end();
                orderedChain.verify(context, timeout(1000)).removeInternalAttribute(ATTR_INTERNAL_TRACING_SPAN);
            }

            @Test
            void should_delay_subscribing_until_start_date_is_reached() {
                AtomicBoolean subscribed = new AtomicBoolean(false);
                Completable completable = Completable.complete().doOnSubscribe(s -> subscribed.set(true));
                mockSubscriptionChain(completable);

                // subscription starts in 5000 ms
                subscription.setStartingAt(addMillisecondsToCurrentDate(5000));

                final TestObserver<Void> obs = dispatcher.dispatch(subscription).subscribeOn(testScheduler).test();

                // for now, it's not subscribed
                assertThat(subscribed.get()).isFalse();

                testScheduler.advanceTimeBy(6000, MILLISECONDS);

                assertThat(subscribed.get()).isTrue();

                obs.assertComplete();
            }

            @ParameterizedTest
            @ValueSource(ints = { 400, 500 })
            void should_retry_when_subscription_fails(int responseStatus) {
                mockSubscriptionChainToJustComplete();
                if (responseStatus == 400) {
                    when(response.isStatus4xx()).thenReturn(true);
                }
                if (responseStatus == 500) {
                    when(response.isStatus4xx()).thenReturn(false);
                    when(response.isStatus5xx()).thenReturn(true);
                }
                when(response.body()).thenReturn(Maybe.just(Buffer.buffer("Error message")));

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
            }

            @Test
            void should_dispose_subscription_when_end_date_is_reached() {
                mockSubscriptionChain(mock(Completable.class));

                // subscription ends in 5s
                subscription.setEndingAt(addMillisecondsToCurrentDate(5000));

                final TestObserver<Void> obs = dispatcher.dispatch(subscription).subscribeOn(testScheduler).test();

                obs.assertNotComplete();
                testScheduler.advanceTimeBy(10, SECONDS);
                obs.assertComplete();

                Disposable disposable = dispatcher.getActiveDisposables().get(SUBSCRIPTION_ID);
                assertNull(disposable);
            }

            @Test
            void should_dispose_subscription_when_retries_reach_the_limit() {
                AtomicInteger atomicCpt = new AtomicInteger(0);
                mockSubscriptionChain(
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
                final TestObserver<Void> obs = dispatcher.dispatch(subscription).test();

                // for now, subscription is not disposed
                Disposable disposable = dispatcher.getActiveDisposables().get(SUBSCRIPTION_ID);
                assertFalse(disposable.isDisposed());

                // Advance time by 30 seconds because we have max 5 retry every 3 seconds
                testScheduler.advanceTimeBy(30, SECONDS);
                assertFalse(dispatcher.getActiveDisposables().containsKey(SUBSCRIPTION_ID));
                obs.assertError(t -> t instanceof RuntimeException && t.getMessage().equals("exception 6"));
            }

            @Test
            void should_dispose_immediately_when_non_retryable_error_occurs() {
                AtomicInteger atomicCpt = new AtomicInteger(0);
                mockSubscriptionChain(
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

                final TestObserver<Void> obs = dispatcher.dispatch(subscription).test();

                Disposable disposable = dispatcher.getActiveDisposables().get(SUBSCRIPTION_ID);
                assertTrue(disposable.isDisposed());

                // No need to wait, exception stops immediately the flow
                obs.assertError(t -> t instanceof MessageProcessingException && t.getMessage().equals("exception 1"));
            }
        }
    }

    @Nested
    class DispatchMultipleTimes {

        Subscription originSubscription;
        Disposable originSubscriptionDisposable;

        @BeforeEach
        void setUp() {
            originSubscription = new Subscription();
            originSubscription.setId(SUBSCRIPTION_ID);
            originSubscription.setStatus("ACCEPTED");
            originSubscription.setConfiguration("{\"entrypointId\": \"webhook\"}");

            mockSubscriptionChainToJustComplete();

            // dispatcher already contains a subscription/disposable in his internal maps
            originSubscriptionDisposable = mock(Disposable.class);
            dispatcher.getActiveSubscriptions().put(originSubscription.getId(), originSubscription);
            dispatcher.getActiveDisposables().put(originSubscription.getId(), originSubscriptionDisposable);
        }

        @Test
        void should_keep_the_active_subscription_and_same_disposable_when_the_configuration_has_not_changed() {
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
        void should_dispose_previous_subscription_and_active_the_new_one_when_the_configuration_has_changed() {
            MutableResponse response = mock(MutableResponse.class);
            when(context.response()).thenReturn(response);
            when(response.isStatus4xx()).thenReturn(false);
            when(response.isStatus5xx()).thenReturn(false);

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
        void should_dispose_previous_subscription_and_active_the_new_one_when_dispatch_is_forced() {
            MutableResponse response = mock(MutableResponse.class);
            when(context.response()).thenReturn(response);
            when(response.isStatus4xx()).thenReturn(false);
            when(response.isStatus5xx()).thenReturn(false);

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

        @ParameterizedTest
        @CsvSource(value = { "CANCELED:STARTED", "PAUSED:STARTED", "ACCEPTED:STOPPED" }, delimiter = ':')
        void should_dispose_subscription_when_it_is_cancelled(String status, String consumerStatus) throws InterruptedException {
            Subscription updatedSubscription = new Subscription();
            updatedSubscription.setId(SUBSCRIPTION_ID);
            updatedSubscription.setStatus(status);
            updatedSubscription.setConsumerStatus(Subscription.ConsumerStatus.valueOf(consumerStatus));
            updatedSubscription.setConfiguration("{\"entrypointId\": \"webhook\"}");

            dispatcher.dispatch(updatedSubscription).test().await().assertComplete();

            SoftAssertions.assertSoftly(
                softly -> {
                    softly.assertThat(dispatcher.getActiveSubscriptions()).isEmpty();
                    softly.assertThat(dispatcher.getActiveDisposables()).isEmpty();
                }
            );
        }
    }

    @Nested
    class DoStop {

        @BeforeEach
        void setUp() {
            subscription.setStatus("ACCEPTED");
            subscription.setConsumerStatus(Subscription.ConsumerStatus.STARTED);
            subscription.setConfiguration("{\"entrypointId\": \"webhook\"}");
        }

        @Test
        void should_dispose_all_actives_subscriptions() throws Exception {
            // Dispatch accepted subscription
            mockSubscriptionChainToJustComplete();
            dispatcher.dispatch(subscription).test().assertComplete();

            // Dispose all subscriptions
            dispatcher.doStop();

            assertTrue(dispatcher.getActiveSubscriptions().isEmpty());
            assertTrue(dispatcher.getActiveDisposables().isEmpty());
        }
    }

    private void mockSubscriptionChainToJustComplete() {
        mockSubscriptionChain(Completable.complete());
    }

    private void mockSubscriptionChain(Completable reactorHandler) {
        // by default, mock full subscription chain, that just completes
        lenient().when(factory.create(any())).thenReturn(context);
        lenient().when(reactor.handle(context)).thenReturn(reactorHandler);

        lenient().when(acceptor.reactor()).thenReturn(reactor);

        lenient().when(context.response()).thenReturn(response);
        lenient().when(response.isStatus4xx()).thenReturn(false);
        lenient().when(response.isStatus5xx()).thenReturn(false);
    }

    private Date addMillisecondsToCurrentDate(int milliseconds) {
        Calendar cal = Calendar.getInstance();
        cal.add(MILLISECOND, milliseconds);
        return cal.getTime();
    }
}
