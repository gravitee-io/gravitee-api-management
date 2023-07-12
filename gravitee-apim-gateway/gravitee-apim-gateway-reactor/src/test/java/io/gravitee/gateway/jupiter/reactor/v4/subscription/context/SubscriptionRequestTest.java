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
package io.gravitee.gateway.jupiter.reactor.v4.subscription.context;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.service.Subscription;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import java.util.Collections;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SubscriptionRequestTest {

    @Test
    public void checkSubscriptionRequest() {
        Subscription subscription = mock(Subscription.class);
        when(subscription.getId()).thenReturn("sub-id1");

        SubscriptionRequest request = new SubscriptionRequest(subscription);

        Assertions.assertEquals("sub-id1", request.id());
        Assertions.assertEquals("sub-id1", request.transactionId());
        Assertions.assertThrows(IllegalStateException.class, request::contextPath);
        Assertions.assertThrows(IllegalStateException.class, request::uri);
        Assertions.assertThrows(IllegalStateException.class, request::path);
        Assertions.assertThrows(IllegalStateException.class, request::pathInfo);
        Assertions.assertEquals(Collections.emptyMap(), request.parameters());
        Assertions.assertEquals(Collections.emptyMap(), request.pathParameters());
        Assertions.assertThrows(IllegalStateException.class, request::method);
        Assertions.assertThrows(IllegalStateException.class, request::scheme);
        Assertions.assertThrows(IllegalStateException.class, request::version);
        Assertions.assertThrows(IllegalStateException.class, request::remoteAddress);
        Assertions.assertThrows(IllegalStateException.class, request::localAddress);
        Assertions.assertThrows(IllegalStateException.class, request::sslSession);
        Assertions.assertNull(request.metrics());
        Assertions.assertFalse(request.ended());
        Assertions.assertEquals(Maybe.empty(), request.body());
        Assertions.assertEquals(Flowable.empty(), request.messages());
    }
}
