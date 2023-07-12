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
package io.gravitee.gateway.jupiter.reactor.v4.subscription;

import static io.reactivex.rxjava3.core.Observable.interval;
import static java.util.Calendar.MILLISECOND;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.jupiter.api.context.InternalContextAttributes;
import io.gravitee.gateway.jupiter.core.context.MutableExecutionContext;
import io.gravitee.gateway.jupiter.reactor.ApiReactor;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.TestScheduler;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

    @BeforeEach
    void init() {
        dispatcher = new DefaultSubscriptionDispatcher(resolver, factory);
    }

    @Test
    void shouldNotHandleNotAcceptedSubscription() {
        when(subscription.getId()).thenReturn(SUBSCRIPTION_ID);
        when(subscription.getStatus()).thenReturn("rejected");

        dispatcher.dispatch(subscription);

        verifyNoInteractions(resolver);
        verifyNoInteractions(factory);
    }

    @Test
    void shouldNotHandleAcceptedSubscriptionExpired() {
        when(subscription.getId()).thenReturn(SUBSCRIPTION_ID);
        when(subscription.getEndingAt()).thenReturn(addMillisecondsToCurrentDate(-100));
        when(subscription.getStatus()).thenReturn("ACCEPTED");
        when(subscription.getConsumerStatus()).thenReturn(Subscription.ConsumerStatus.STARTED);

        dispatcher.dispatch(subscription);

        verifyNoInteractions(resolver);
        verifyNoInteractions(factory);
    }

    @Test
    void shouldNotHandleUnresolvedSubscription() {
        when(subscription.getId()).thenReturn(SUBSCRIPTION_ID);
        when(subscription.getStatus()).thenReturn("ACCEPTED");
        when(subscription.getConsumerStatus()).thenReturn(Subscription.ConsumerStatus.STARTED);
        when(resolver.resolve(subscription)).thenReturn(null);

        dispatcher.dispatch(subscription);

        verify(resolver, times(1)).resolve(any());
        verifyNoInteractions(factory);
    }

    @Test
    void shouldHandleSubscriptionNoConfiguration() {
        DefaultSubscriptionAcceptor acceptor = mock(DefaultSubscriptionAcceptor.class);
        ApiReactor reactor = mock(ApiReactor.class);
        when(acceptor.reactor()).thenReturn(reactor);

        when(resolver.resolve(subscription)).thenReturn(acceptor);

        when(subscription.getId()).thenReturn(SUBSCRIPTION_ID);
        when(subscription.getStatus()).thenReturn("ACCEPTED");
        when(subscription.getConsumerStatus()).thenReturn(Subscription.ConsumerStatus.STARTED);

        dispatcher.dispatch(subscription);

        verify(resolver, times(1)).resolve(any());
        verifyNoInteractions(factory);
    }

    @Test
    void shouldHandleSubscriptionNoConfigurationType() {
        DefaultSubscriptionAcceptor acceptor = mock(DefaultSubscriptionAcceptor.class);
        ApiReactor reactor = mock(ApiReactor.class);
        when(acceptor.reactor()).thenReturn(reactor);

        when(resolver.resolve(subscription)).thenReturn(acceptor);

        when(subscription.getId()).thenReturn(SUBSCRIPTION_ID);
        when(subscription.getStatus()).thenReturn("ACCEPTED");
        when(subscription.getConsumerStatus()).thenReturn(Subscription.ConsumerStatus.STARTED);
        when(subscription.getConfiguration()).thenReturn("{}");

        dispatcher.dispatch(subscription);

        verify(resolver, times(1)).resolve(any());
        verifyNoInteractions(factory);
    }

    @Test
    void shouldHandleSubscriptionWithConfiguration() {
        DefaultSubscriptionAcceptor acceptor = mock(DefaultSubscriptionAcceptor.class);
        ApiReactor reactor = mock(ApiReactor.class);
        MutableExecutionContext context = mock(MutableExecutionContext.class);

        when(acceptor.reactor()).thenReturn(reactor);
        when(reactor.handle(context)).thenReturn(Completable.complete());

        when(factory.create(subscription)).thenReturn(context);
        when(resolver.resolve(subscription)).thenReturn(acceptor);

        when(subscription.getId()).thenReturn(SUBSCRIPTION_ID);
        when(subscription.getStatus()).thenReturn("ACCEPTED");
        when(subscription.getConsumerStatus()).thenReturn(Subscription.ConsumerStatus.STARTED);
        when(subscription.getConfiguration()).thenReturn("{\"entrypointId\": \"webhook\"}");

        dispatcher.dispatch(subscription);

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
        dispatcher.dispatch(updatedSubscription);

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

        when(acceptor.reactor()).thenReturn(reactor);

        when(factory.create(any())).thenReturn(context);
        when(resolver.resolve(any())).thenReturn(acceptor);
        when(reactor.handle(context)).thenReturn(Completable.complete());

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
        dispatcher.dispatch(updatedSubscription);

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

        when(acceptor.reactor()).thenReturn(reactor);

        when(factory.create(any())).thenReturn(context);
        when(resolver.resolve(any())).thenReturn(acceptor);
        when(reactor.handle(context)).thenReturn(Completable.complete());

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
        dispatcher.dispatch(updatedSubscription);

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
        when(subscription.getEndingAt()).thenReturn(addMillisecondsToCurrentDate(100));
        when(subscription.getConfiguration()).thenReturn("{\"entrypointId\": \"webhook\"}");

        dispatcher.dispatch(subscription);

        // for now, subscription is not disposed
        Disposable disposable = dispatcher.getActiveDisposables().get(SUBSCRIPTION_ID);
        assertFalse(disposable.isDisposed());

        // wait for the subscription to be disposed (timeouts and fails after 300 ms)
        interval(50, MILLISECONDS).takeWhile(i -> !disposable.isDisposed()).test().awaitDone(300, MILLISECONDS).assertComplete();
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
                    Completable.create(emitter -> {
                        int cpt = atomicCpt.incrementAndGet();
                        if (cpt <= 10) {
                            emitter.onError(new RuntimeException("exception " + cpt));
                        } else {
                            emitter.onComplete();
                        }
                    })
                );

            when(factory.create(subscription)).thenReturn(context);
            when(resolver.resolve(subscription)).thenReturn(acceptor);

            when(subscription.getId()).thenReturn(SUBSCRIPTION_ID);
            when(subscription.getStatus()).thenReturn("ACCEPTED");
            when(subscription.getConsumerStatus()).thenReturn(Subscription.ConsumerStatus.STARTED);
            when(subscription.getConfiguration()).thenReturn("{\"entrypointId\": \"webhook\"}");

            dispatcher.dispatch(subscription);

            // for now, subscription is not disposed
            Disposable disposable = dispatcher.getActiveDisposables().get(SUBSCRIPTION_ID);
            assertFalse(disposable.isDisposed());

            // Advance time by 30 seconds because we have max 5 retry every 3 seconds
            testScheduler.advanceTimeBy(30, SECONDS);
            assertFalse(dispatcher.getActiveDisposables().containsKey(SUBSCRIPTION_ID));
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

        when(acceptor.reactor()).thenReturn(reactor);
        AtomicBoolean subscribed = new AtomicBoolean(false);
        Completable completable = Completable.complete().doOnSubscribe(s -> subscribed.set(true));
        when(reactor.handle(context)).thenReturn(completable);

        when(factory.create(subscription)).thenReturn(context);
        when(resolver.resolve(subscription)).thenReturn(acceptor);

        when(subscription.getId()).thenReturn(SUBSCRIPTION_ID);
        when(subscription.getStatus()).thenReturn("ACCEPTED");
        when(subscription.getConsumerStatus()).thenReturn(Subscription.ConsumerStatus.STARTED);
        // subscription starts in 100 ms
        when(subscription.getStartingAt()).thenReturn(addMillisecondsToCurrentDate(100));
        when(subscription.getConfiguration()).thenReturn("{\"entrypointId\": \"webhook\"}");

        dispatcher.dispatch(subscription);

        // for now, it's not subscribed
        assertFalse(subscribed.get());

        // wait for the subscription to be done (timeouts and fails after 300 ms)
        interval(50, MILLISECONDS).takeWhile(i -> !subscribed.get()).test().awaitDone(300, MILLISECONDS).assertComplete();
    }

    @Test
    void shouldDisposeSubscription() {
        DefaultSubscriptionAcceptor acceptor = mock(DefaultSubscriptionAcceptor.class);
        ApiReactor reactor = mock(ApiReactor.class);
        MutableExecutionContext context = mock(MutableExecutionContext.class);

        when(acceptor.reactor()).thenReturn(reactor);
        when(reactor.handle(context)).thenReturn(Completable.complete());

        when(factory.create(subscription)).thenReturn(context);
        when(resolver.resolve(subscription)).thenReturn(acceptor);

        when(subscription.getId()).thenReturn(SUBSCRIPTION_ID);
        when(subscription.getStatus()).thenReturn("ACCEPTED");
        when(subscription.getConfiguration()).thenReturn("{\"entrypointId\": \"webhook\"}");

        // Dispatch accepted subscription
        dispatcher.dispatch(subscription);

        // Dispose an existing subscription
        when(subscription.getStatus()).thenReturn("CANCELED");
        dispatcher.dispatch(subscription);

        assertTrue(dispatcher.getActiveSubscriptions().isEmpty());
        assertTrue(dispatcher.getActiveDisposables().isEmpty());

        when(subscription.getStatus()).thenReturn("ACCEPTED");
        when(subscription.getConsumerStatus()).thenReturn(Subscription.ConsumerStatus.STARTED);
        dispatcher.dispatch(subscription);
        assertThat(dispatcher.getActiveSubscriptions()).hasSize(1);

        when(subscription.getConsumerStatus()).thenReturn(Subscription.ConsumerStatus.STOPPED);
        dispatcher.dispatch(subscription);
        assertThat(dispatcher.getActiveSubscriptions()).isEmpty();

        when(subscription.getStatus()).thenReturn("PAUSED");
        when(subscription.getConsumerStatus()).thenReturn(Subscription.ConsumerStatus.STOPPED);
        dispatcher.dispatch(subscription);
        assertThat(dispatcher.getActiveSubscriptions()).isEmpty();

        when(subscription.getStatus()).thenReturn("PAUSED");
        when(subscription.getConsumerStatus()).thenReturn(Subscription.ConsumerStatus.STARTED);
        dispatcher.dispatch(subscription);
        assertThat(dispatcher.getActiveSubscriptions()).isEmpty();

        when(subscription.getStatus()).thenReturn("ACCEPTED");
        when(subscription.getConsumerStatus()).thenReturn(Subscription.ConsumerStatus.STARTED);
        dispatcher.dispatch(subscription);
        assertThat(dispatcher.getActiveSubscriptions()).hasSize(1);
    }

    @Test
    void shouldDisposeSubscriptions() throws Exception {
        DefaultSubscriptionAcceptor acceptor = mock(DefaultSubscriptionAcceptor.class);
        ApiReactor reactor = mock(ApiReactor.class);
        MutableExecutionContext context = mock(MutableExecutionContext.class);

        when(acceptor.reactor()).thenReturn(reactor);
        when(reactor.handle(context)).thenReturn(Completable.complete());

        when(factory.create(subscription)).thenReturn(context);
        when(resolver.resolve(subscription)).thenReturn(acceptor);

        when(subscription.getId()).thenReturn(SUBSCRIPTION_ID);
        when(subscription.getStatus()).thenReturn("ACCEPTED");
        when(subscription.getConsumerStatus()).thenReturn(Subscription.ConsumerStatus.STARTED);
        when(subscription.getConfiguration()).thenReturn("{\"entrypointId\": \"webhook\"}");

        // Dispatch accepted subscription
        dispatcher.dispatch(subscription);

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
