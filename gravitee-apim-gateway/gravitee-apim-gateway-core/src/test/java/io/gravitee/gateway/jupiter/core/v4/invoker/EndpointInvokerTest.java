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
package io.gravitee.gateway.jupiter.core.v4.invoker;

import static io.gravitee.gateway.jupiter.core.v4.invoker.EndpointInvoker.NO_ENDPOINT_FOUND_KEY;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gateway.jupiter.api.ExecutionFailure;
import io.gravitee.gateway.jupiter.api.connector.endpoint.EndpointConnector;
import io.gravitee.gateway.jupiter.api.context.ExecutionContext;
import io.gravitee.gateway.jupiter.core.context.interruption.InterruptionFailureException;
import io.gravitee.gateway.jupiter.core.v4.endpoint.DefaultEndpointConnectorResolver;
import io.reactivex.Completable;
import io.reactivex.observers.TestObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class EndpointInvokerTest {

    @Mock
    private DefaultEndpointConnectorResolver endpointConnectorResolver;

    @Mock
    private EndpointConnector endpointConnector;

    @Mock
    private ExecutionContext ctx;

    private EndpointInvoker cut;

    @BeforeEach
    void init() {
        cut = new EndpointInvoker(endpointConnectorResolver);
    }

    @Test
    void shouldConnectToEndpointConnector() {
        when(endpointConnectorResolver.resolve(ctx)).thenReturn(endpointConnector);
        when(endpointConnector.connect(ctx)).thenReturn(Completable.complete());

        final TestObserver<Void> obs = cut.invoke(ctx).test();

        obs.assertNoValues();
    }

    @Test
    void shouldFailWith404WhenNoEndpointConnectorHasBeenResolved() {
        when(endpointConnectorResolver.resolve(ctx)).thenReturn(null);
        when(ctx.interruptWith(any(ExecutionFailure.class)))
            .thenAnswer(i -> Completable.error(new InterruptionFailureException(i.getArgument(0))));

        final TestObserver<Void> obs = cut.invoke(ctx).test();

        obs.assertError(e -> {
            assertTrue(e instanceof InterruptionFailureException);
            final InterruptionFailureException failureException = (InterruptionFailureException) e;
            assertEquals(HttpStatusCode.NOT_FOUND_404, failureException.getExecutionFailure().statusCode());
            assertEquals(NO_ENDPOINT_FOUND_KEY, failureException.getExecutionFailure().key());
            assertNotNull(failureException.getExecutionFailure().message());
            return true;
        });
    }
}
