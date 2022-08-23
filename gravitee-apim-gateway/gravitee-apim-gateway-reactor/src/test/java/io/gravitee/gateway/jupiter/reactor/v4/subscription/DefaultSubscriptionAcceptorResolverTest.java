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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.reactor.handler.Acceptor;
import io.gravitee.gateway.reactor.handler.ReactorHandlerRegistry;
import java.util.Arrays;
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
public class DefaultSubscriptionAcceptorResolverTest {

    private DefaultSubscriptionAcceptorResolver resolver;

    @Mock
    private ReactorHandlerRegistry registry;

    @Mock
    private Subscription subscription;

    @BeforeEach
    public void init() {
        resolver = new DefaultSubscriptionAcceptorResolver(registry);
    }

    @Test
    public void shouldReturnAcceptorForSubscription() {
        DefaultSubscriptionAcceptor acceptor1 = new DefaultSubscriptionAcceptor(null, "api-id1");
        DefaultSubscriptionAcceptor acceptor2 = new DefaultSubscriptionAcceptor(null, "api-id2");
        when(registry.getAcceptors(SubscriptionAcceptor.class)).thenReturn(Arrays.asList(acceptor1, acceptor2));
        when(subscription.getApi()).thenReturn("api-id2");

        Acceptor acceptor = resolver.resolve(subscription);

        assertNotNull(acceptor);
        assertEquals(acceptor, acceptor2);
    }

    @Test
    public void shouldReturnNullIfNoAcceptorMatch() {
        DefaultSubscriptionAcceptor acceptor1 = new DefaultSubscriptionAcceptor(null, "api-id1");
        DefaultSubscriptionAcceptor acceptor2 = new DefaultSubscriptionAcceptor(null, "api-id2");
        when(registry.getAcceptors(SubscriptionAcceptor.class)).thenReturn(Arrays.asList(acceptor1, acceptor2));
        when(subscription.getApi()).thenReturn("api-id3");

        Acceptor acceptor = resolver.resolve(subscription);

        assertNull(acceptor);
    }
}
