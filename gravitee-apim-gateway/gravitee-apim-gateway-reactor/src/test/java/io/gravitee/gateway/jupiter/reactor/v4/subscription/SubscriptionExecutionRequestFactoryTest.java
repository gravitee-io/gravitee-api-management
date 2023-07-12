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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.jupiter.api.context.MessageExecutionContext;
import org.junit.jupiter.api.Test;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SubscriptionExecutionRequestFactoryTest {

    private final SubscriptionExecutionRequestFactory factory = new SubscriptionExecutionRequestFactory();

    @Test
    public void shouldCreateExecutionContext() {
        Subscription subscription = mock(Subscription.class);
        when(subscription.getId()).thenReturn("sub-id");
        MessageExecutionContext context = factory.create(subscription);

        assertNotNull(context);
        assertEquals(subscription.getId(), context.request().transactionId());
        assertEquals(subscription.getId(), context.request().id());
    }
}
