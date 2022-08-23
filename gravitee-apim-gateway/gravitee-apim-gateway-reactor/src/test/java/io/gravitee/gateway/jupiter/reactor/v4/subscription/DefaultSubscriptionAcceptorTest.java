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

import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.reactor.handler.ReactorHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
public class DefaultSubscriptionAcceptorTest {

    private DefaultSubscriptionAcceptor acceptor;

    @Mock
    private Subscription subscription;

    @Mock
    private ReactorHandler handler;

    @BeforeEach
    public void init() {
        acceptor = new DefaultSubscriptionAcceptor(handler, "api-id2");
    }

    @Test
    public void shouldNotAccept() {
        Mockito.when(subscription.getApi()).thenReturn("api-id1");
        boolean accept = acceptor.accept(subscription);

        assertFalse(accept);
    }

    @Test
    public void shouldAccept() {
        Mockito.when(subscription.getApi()).thenReturn("api-id2");
        boolean accept = acceptor.accept(subscription);

        assertTrue(accept);
        assertEquals(0, acceptor.compareTo(null));
        assertEquals(handler, acceptor.reactor());
    }
}
