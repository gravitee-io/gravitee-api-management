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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.jupiter.api.context.ContextAttributes;
import io.gravitee.gateway.jupiter.core.context.MutableExecutionContext;
import io.gravitee.gateway.jupiter.reactor.ApiReactor;
import io.reactivex.Completable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
public class DefaultSubscriptionDispatcherTest {

    private DefaultSubscriptionDispatcher dispatcher;

    @Mock
    private Subscription subscription;

    @Mock
    private SubscriptionAcceptorResolver resolver;

    @Mock
    private SubscriptionExecutionRequestFactory factory;

    @BeforeEach
    public void init() {
        dispatcher = new DefaultSubscriptionDispatcher(resolver, factory);
    }

    @Test
    public void shouldNotHandleNotAcceptedSubscription() {
        when(subscription.getId()).thenReturn("sub-id");
        when(subscription.getStatus()).thenReturn("rejected");

        dispatcher.dispatch(subscription);

        verifyNoInteractions(resolver);
        verifyNoInteractions(factory);
    }

    @Test
    public void shouldNotHandleUnresolvedSubscription() {
        when(subscription.getId()).thenReturn("sub-id");
        when(subscription.getStatus()).thenReturn("ACCEPTED");
        when(resolver.resolve(subscription)).thenReturn(null);

        dispatcher.dispatch(subscription);

        verify(resolver, times(1)).resolve(any());
        verifyNoInteractions(factory);
    }

    @Test
    public void shouldHandleSubscriptionNoConfiguration() {
        DefaultSubscriptionAcceptor acceptor = mock(DefaultSubscriptionAcceptor.class);
        ApiReactor reactor = mock(ApiReactor.class);
        when(acceptor.reactor()).thenReturn(reactor);

        when(resolver.resolve(subscription)).thenReturn(acceptor);

        when(subscription.getId()).thenReturn("sub-id");
        when(subscription.getStatus()).thenReturn("ACCEPTED");

        dispatcher.dispatch(subscription);

        verify(resolver, times(1)).resolve(any());
        verifyNoInteractions(factory);
    }

    @Test
    public void shouldHandleSubscriptionNoConfigurationType() {
        DefaultSubscriptionAcceptor acceptor = mock(DefaultSubscriptionAcceptor.class);
        ApiReactor reactor = mock(ApiReactor.class);
        when(acceptor.reactor()).thenReturn(reactor);

        when(resolver.resolve(subscription)).thenReturn(acceptor);

        when(subscription.getId()).thenReturn("sub-id");
        when(subscription.getStatus()).thenReturn("ACCEPTED");
        when(subscription.getConfiguration()).thenReturn("{}");

        dispatcher.dispatch(subscription);

        verify(resolver, times(1)).resolve(any());
        verifyNoInteractions(factory);
    }

    @Test
    public void shouldHandleSubscriptionWithConfiguration() {
        DefaultSubscriptionAcceptor acceptor = mock(DefaultSubscriptionAcceptor.class);
        ApiReactor reactor = mock(ApiReactor.class);
        MutableExecutionContext context = mock(MutableExecutionContext.class);

        when(acceptor.reactor()).thenReturn(reactor);
        when(reactor.handle(context)).thenReturn(Completable.complete());

        when(factory.create(subscription)).thenReturn(context);
        when(resolver.resolve(subscription)).thenReturn(acceptor);

        when(subscription.getId()).thenReturn("sub-id");
        when(subscription.getStatus()).thenReturn("ACCEPTED");
        when(subscription.getConfiguration()).thenReturn("{\"type\": \"webhook\"}");

        dispatcher.dispatch(subscription);

        verify(resolver, times(1)).resolve(any());
        verify(factory, times(1)).create(subscription);
        verify(context).setInternalAttribute(ContextAttributes.ATTR_SUBSCRIPTION_TYPE, "webhook");
        verify(context).setInternalAttribute(ContextAttributes.ATTR_SUBSCRIPTION, subscription);

        assertFalse(dispatcher.getActiveSubscriptions().isEmpty());
    }

    @Test
    public void shouldDisposeSubscription() {
        DefaultSubscriptionAcceptor acceptor = mock(DefaultSubscriptionAcceptor.class);
        ApiReactor reactor = mock(ApiReactor.class);
        MutableExecutionContext context = mock(MutableExecutionContext.class);

        when(acceptor.reactor()).thenReturn(reactor);
        when(reactor.handle(context)).thenReturn(Completable.complete());

        when(factory.create(subscription)).thenReturn(context);
        when(resolver.resolve(subscription)).thenReturn(acceptor);

        when(subscription.getId()).thenReturn("sub-id");
        when(subscription.getStatus()).thenReturn("ACCEPTED");
        when(subscription.getConfiguration()).thenReturn("{\"type\": \"webhook\"}");

        // Dispatch accepted subscription
        dispatcher.dispatch(subscription);

        // Dispose an existing subscription
        when(subscription.getStatus()).thenReturn("CANCELED");
        dispatcher.dispatch(subscription);

        assertTrue(dispatcher.getActiveSubscriptions().isEmpty());
    }

    @Test
    public void shouldDisposeSubscriptions() throws Exception {
        DefaultSubscriptionAcceptor acceptor = mock(DefaultSubscriptionAcceptor.class);
        ApiReactor reactor = mock(ApiReactor.class);
        MutableExecutionContext context = mock(MutableExecutionContext.class);

        when(acceptor.reactor()).thenReturn(reactor);
        when(reactor.handle(context)).thenReturn(Completable.complete());

        when(factory.create(subscription)).thenReturn(context);
        when(resolver.resolve(subscription)).thenReturn(acceptor);

        when(subscription.getId()).thenReturn("sub-id");
        when(subscription.getStatus()).thenReturn("ACCEPTED");
        when(subscription.getConfiguration()).thenReturn("{\"type\": \"webhook\"}");

        // Dispatch accepted subscription
        dispatcher.dispatch(subscription);

        // Dispose all subscriptions
        dispatcher.doStop();

        assertTrue(dispatcher.getActiveSubscriptions().isEmpty());
    }
}
