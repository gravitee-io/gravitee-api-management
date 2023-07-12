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

import static io.gravitee.gateway.jupiter.api.context.InternalContextAttributes.ATTR_INTERNAL_ENTRYPOINT_CONNECTOR;
import static io.gravitee.gateway.jupiter.core.v4.invoker.EndpointInvoker.INCOMPATIBLE_QOS_CAPABILITIES_KEY;
import static io.gravitee.gateway.jupiter.core.v4.invoker.EndpointInvoker.INCOMPATIBLE_QOS_KEY;
import static io.gravitee.gateway.jupiter.core.v4.invoker.EndpointInvoker.NO_ENDPOINT_FOUND_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gateway.jupiter.api.ApiType;
import io.gravitee.gateway.jupiter.api.ExecutionFailure;
import io.gravitee.gateway.jupiter.api.connector.endpoint.EndpointConnector;
import io.gravitee.gateway.jupiter.api.connector.endpoint.async.EndpointAsyncConnector;
import io.gravitee.gateway.jupiter.api.connector.entrypoint.async.EntrypointAsyncConnector;
import io.gravitee.gateway.jupiter.api.context.ExecutionContext;
import io.gravitee.gateway.jupiter.api.qos.Qos;
import io.gravitee.gateway.jupiter.api.qos.QosCapability;
import io.gravitee.gateway.jupiter.api.qos.QosRequirement;
import io.gravitee.gateway.jupiter.core.context.interruption.InterruptionFailureException;
import io.gravitee.gateway.jupiter.core.v4.endpoint.DefaultEndpointConnectorResolver;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.observers.TestObserver;
import java.util.Set;
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

    @Test
    void shouldConnectToEndpointConnectorWhenEntrypointAndEndpointQosAreCompatible() {
        EndpointAsyncConnector endpointAsyncConnector = mock(EndpointAsyncConnector.class);
        when(endpointAsyncConnector.supportedApi()).thenReturn(ApiType.ASYNC);
        when(endpointAsyncConnector.supportedQos()).thenReturn(Set.of(Qos.AT_LEAST_ONCE));
        when(endpointConnectorResolver.resolve(ctx)).thenReturn(endpointAsyncConnector);
        EntrypointAsyncConnector entrypointAsyncConnector = mock(EntrypointAsyncConnector.class);
        when(entrypointAsyncConnector.qosRequirement()).thenReturn(QosRequirement.builder().qos(Qos.AT_LEAST_ONCE).build());
        when(ctx.getInternalAttribute(ATTR_INTERNAL_ENTRYPOINT_CONNECTOR)).thenReturn(entrypointAsyncConnector);

        when(endpointAsyncConnector.connect(ctx)).thenReturn(Completable.complete());

        final TestObserver<Void> obs = cut.invoke(ctx).test();

        obs.assertNoValues();
    }

    @Test
    void shouldFailWith400WhenEntrypointAndEndpointQosAreIncompatibleBecauseOfMissingRequirements() {
        EndpointAsyncConnector endpointAsyncConnector = mock(EndpointAsyncConnector.class);
        when(endpointAsyncConnector.supportedApi()).thenReturn(ApiType.ASYNC);
        when(endpointConnectorResolver.resolve(ctx)).thenReturn(endpointAsyncConnector);
        EntrypointAsyncConnector entrypointAsyncConnector = mock(EntrypointAsyncConnector.class);
        when(ctx.getInternalAttribute(ATTR_INTERNAL_ENTRYPOINT_CONNECTOR)).thenReturn(entrypointAsyncConnector);
        when(ctx.interruptWith(any(ExecutionFailure.class)))
            .thenAnswer(i -> Completable.error(new InterruptionFailureException(i.getArgument(0))));

        final TestObserver<Void> obs = cut.invoke(ctx).test();

        obs.assertError(e -> {
            assertTrue(e instanceof InterruptionFailureException);
            final InterruptionFailureException failureException = (InterruptionFailureException) e;
            assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR_500, failureException.getExecutionFailure().statusCode());
            assertNotNull(failureException.getExecutionFailure().message());
            return true;
        });
    }

    @Test
    void shouldFailWith400WhenEntrypointAndEndpointQosAreIncompatible() {
        EndpointAsyncConnector endpointAsyncConnector = mock(EndpointAsyncConnector.class);
        when(endpointAsyncConnector.supportedApi()).thenReturn(ApiType.ASYNC);
        when(endpointAsyncConnector.supportedQos()).thenReturn(Set.of(Qos.AT_LEAST_ONCE));
        when(endpointConnectorResolver.resolve(ctx)).thenReturn(endpointAsyncConnector);
        EntrypointAsyncConnector entrypointAsyncConnector = mock(EntrypointAsyncConnector.class);
        when(entrypointAsyncConnector.qosRequirement()).thenReturn(QosRequirement.builder().qos(Qos.AUTO).build());
        when(ctx.getInternalAttribute(ATTR_INTERNAL_ENTRYPOINT_CONNECTOR)).thenReturn(entrypointAsyncConnector);
        when(ctx.interruptWith(any(ExecutionFailure.class)))
            .thenAnswer(i -> Completable.error(new InterruptionFailureException(i.getArgument(0))));

        final TestObserver<Void> obs = cut.invoke(ctx).test();

        obs.assertError(e -> {
            assertTrue(e instanceof InterruptionFailureException);
            final InterruptionFailureException failureException = (InterruptionFailureException) e;
            assertEquals(HttpStatusCode.BAD_REQUEST_400, failureException.getExecutionFailure().statusCode());
            assertEquals(INCOMPATIBLE_QOS_KEY, failureException.getExecutionFailure().key());
            assertNotNull(failureException.getExecutionFailure().message());
            return true;
        });
    }

    @Test
    void shouldFailWith400WhenEntrypointAndEndpointQosAreIncompatibleBecauseOfMissingCapabilities() {
        EndpointAsyncConnector endpointAsyncConnector = mock(EndpointAsyncConnector.class);
        when(endpointAsyncConnector.supportedApi()).thenReturn(ApiType.ASYNC);
        when(endpointAsyncConnector.supportedQos()).thenReturn(Set.of(Qos.AT_LEAST_ONCE));
        when(endpointAsyncConnector.supportedQosCapabilities()).thenReturn(Set.of());
        when(endpointConnectorResolver.resolve(ctx)).thenReturn(endpointAsyncConnector);
        EntrypointAsyncConnector entrypointAsyncConnector = mock(EntrypointAsyncConnector.class);
        when(entrypointAsyncConnector.qosRequirement())
            .thenReturn(QosRequirement.builder().qos(Qos.AT_LEAST_ONCE).capabilities(Set.of(QosCapability.AUTO_ACK)).build());
        when(ctx.getInternalAttribute(ATTR_INTERNAL_ENTRYPOINT_CONNECTOR)).thenReturn(entrypointAsyncConnector);
        when(ctx.interruptWith(any(ExecutionFailure.class)))
            .thenAnswer(i -> Completable.error(new InterruptionFailureException(i.getArgument(0))));

        final TestObserver<Void> obs = cut.invoke(ctx).test();

        obs.assertError(e -> {
            assertTrue(e instanceof InterruptionFailureException);
            final InterruptionFailureException failureException = (InterruptionFailureException) e;
            assertEquals(HttpStatusCode.BAD_REQUEST_400, failureException.getExecutionFailure().statusCode());
            assertEquals(INCOMPATIBLE_QOS_CAPABILITIES_KEY, failureException.getExecutionFailure().key());
            assertNotNull(failureException.getExecutionFailure().message());
            return true;
        });
    }

    @Test
    void shouldFailWith400WhenEntrypointAndEndpointQosAreIncompatibleBecauseOfEndpointQosNull() {
        EndpointAsyncConnector endpointAsyncConnector = mock(EndpointAsyncConnector.class);
        when(endpointAsyncConnector.supportedApi()).thenReturn(ApiType.ASYNC);
        when(endpointAsyncConnector.supportedQos()).thenReturn(null);
        when(endpointConnectorResolver.resolve(ctx)).thenReturn(endpointAsyncConnector);
        EntrypointAsyncConnector entrypointAsyncConnector = mock(EntrypointAsyncConnector.class);
        when(entrypointAsyncConnector.qosRequirement()).thenReturn(QosRequirement.builder().qos(Qos.AUTO).build());
        when(ctx.getInternalAttribute(ATTR_INTERNAL_ENTRYPOINT_CONNECTOR)).thenReturn(entrypointAsyncConnector);
        when(ctx.interruptWith(any(ExecutionFailure.class)))
            .thenAnswer(i -> Completable.error(new InterruptionFailureException(i.getArgument(0))));

        final TestObserver<Void> obs = cut.invoke(ctx).test();

        obs.assertError(e -> {
            assertTrue(e instanceof InterruptionFailureException);
            final InterruptionFailureException failureException = (InterruptionFailureException) e;
            assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR_500, failureException.getExecutionFailure().statusCode());
            assertNotNull(failureException.getExecutionFailure().message());
            return true;
        });
    }
}
