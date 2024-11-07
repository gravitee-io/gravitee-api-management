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
package io.gravitee.gateway.reactive.core.v4.invoker;

import static io.gravitee.gateway.api.ExecutionContext.ATTR_REQUEST_METHOD;
import static io.gravitee.gateway.reactive.api.context.ContextAttributes.ATTR_REQUEST_ENDPOINT;
import static io.gravitee.gateway.reactive.api.context.InternalContextAttributes.ATTR_INTERNAL_ENTRYPOINT_CONNECTOR;
import static io.gravitee.gateway.reactive.core.v4.invoker.HttpEndpointInvoker.NO_ENDPOINT_FOUND_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.el.TemplateEngine;
import io.gravitee.gateway.reactive.api.ExecutionFailure;
import io.gravitee.gateway.reactive.api.connector.endpoint.EndpointConnector;
import io.gravitee.gateway.reactive.api.connector.entrypoint.async.HttpEntrypointAsyncConnector;
import io.gravitee.gateway.reactive.api.context.ExecutionContext;
import io.gravitee.gateway.reactive.api.context.Request;
import io.gravitee.gateway.reactive.core.context.interruption.InterruptionFailureException;
import io.gravitee.gateway.reactive.core.v4.endpoint.EndpointCriteria;
import io.gravitee.gateway.reactive.core.v4.endpoint.EndpointManager;
import io.gravitee.gateway.reactive.core.v4.endpoint.ManagedEndpoint;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.observers.TestObserver;
import java.util.List;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.InstanceOfAssertFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class HttpEndpointInvokerTest {

    @Mock
    private EndpointManager endpointManager;

    @Mock
    private ManagedEndpoint managedEndpoint;

    @Mock
    private EndpointConnector endpointConnector;

    @Mock
    private ExecutionContext ctx;

    @Mock
    private TemplateEngine templateEngine;

    @Mock
    private Request request;

    private HttpEndpointInvoker cut;

    @BeforeEach
    void init() {
        cut = new HttpEndpointInvoker(endpointManager);
    }

    @Test
    void shouldReturnEndpointInvokerWhenGetId() {
        assertThat(cut.getId()).isEqualTo("endpoint-invoker");
    }

    @Test
    void shouldConnectToEndpointConnector() {
        final HttpEntrypointAsyncConnector HttpEntrypointAsyncConnector = mock(HttpEntrypointAsyncConnector.class);
        when(ctx.getInternalAttribute(ATTR_INTERNAL_ENTRYPOINT_CONNECTOR)).thenReturn(HttpEntrypointAsyncConnector);
        when(endpointManager.next(any(EndpointCriteria.class))).thenReturn(managedEndpoint);
        when(managedEndpoint.getConnector()).thenReturn(endpointConnector);
        when(endpointConnector.connect(ctx)).thenReturn(Completable.complete());

        final TestObserver<Void> obs = cut.invoke(ctx).test();

        obs.assertNoValues();
    }

    @ParameterizedTest
    @ValueSource(strings = { "custom", "c_u/s$t*o-m" })
    void shouldConnectToNamedEndpointConnectorWithCustomEndpointAttribute(String endpointName) {
        final HttpEntrypointAsyncConnector HttpEntrypointAsyncConnector = mock(HttpEntrypointAsyncConnector.class);
        when(ctx.getInternalAttribute(ATTR_INTERNAL_ENTRYPOINT_CONNECTOR)).thenReturn(HttpEntrypointAsyncConnector);
        when(ctx.getAttribute(ATTR_REQUEST_ENDPOINT)).thenReturn(endpointName + ":");
        when(ctx.getTemplateEngine()).thenReturn(templateEngine);
        when(templateEngine.getValue(anyString(), eq(String.class))).thenAnswer(i -> i.getArgument(0));
        when(endpointManager.next(any(EndpointCriteria.class))).thenReturn(managedEndpoint);
        when(managedEndpoint.getConnector()).thenReturn(endpointConnector);
        when(endpointConnector.connect(ctx)).thenReturn(Completable.complete());

        final TestObserver<Void> obs = cut.invoke(ctx).test();

        obs.assertNoValues();

        verify(endpointManager).next(argThat(criteria -> criteria.getName().equals(endpointName)));
        verify(ctx).setAttribute(ATTR_REQUEST_ENDPOINT, "");
    }

    @Test
    void shouldConnectToNamedEndpointConnectorWithCustomEndpointAttributeContainingColon() {
        final HttpEntrypointAsyncConnector HttpEntrypointAsyncConnector = mock(HttpEntrypointAsyncConnector.class);
        when(ctx.getInternalAttribute(ATTR_INTERNAL_ENTRYPOINT_CONNECTOR)).thenReturn(HttpEntrypointAsyncConnector);
        when(ctx.getAttribute(ATTR_REQUEST_ENDPOINT)).thenReturn("name:with:colon:");
        when(ctx.getTemplateEngine()).thenReturn(templateEngine);
        when(templateEngine.getValue(anyString(), eq(String.class))).thenAnswer(i -> i.getArgument(0));
        when(endpointManager.next(any(EndpointCriteria.class))).thenReturn(managedEndpoint);
        when(managedEndpoint.getConnector()).thenReturn(endpointConnector);
        when(endpointConnector.connect(ctx)).thenReturn(Completable.complete());

        final TestObserver<Void> obs = cut.invoke(ctx).test();

        obs.assertNoValues();

        verify(endpointManager).next(argThat(criteria -> criteria.getName().equals("name")));
        verify(ctx).setAttribute(ATTR_REQUEST_ENDPOINT, "with:colon:");
    }

    @Test
    void shouldConnectToNextEndpointConnectorWhenUrlEndpointAttributeIsDefined() {
        final HttpEntrypointAsyncConnector HttpEntrypointAsyncConnector = mock(HttpEntrypointAsyncConnector.class);
        when(ctx.getInternalAttribute(ATTR_INTERNAL_ENTRYPOINT_CONNECTOR)).thenReturn(HttpEntrypointAsyncConnector);
        when(ctx.getAttribute(ATTR_REQUEST_ENDPOINT)).thenReturn("http://api.gravitee.io/echo");
        when(ctx.getTemplateEngine()).thenReturn(templateEngine);
        when(templateEngine.getValue(anyString(), eq(String.class))).thenAnswer(i -> i.getArgument(0));
        when(endpointManager.next(any(EndpointCriteria.class))).thenReturn(managedEndpoint);
        when(managedEndpoint.getConnector()).thenReturn(endpointConnector);
        when(endpointConnector.connect(ctx)).thenReturn(Completable.complete());

        final TestObserver<Void> obs = cut.invoke(ctx).test();

        obs.assertNoValues();

        verify(endpointManager).next(argThat(criteria -> criteria.getName() == null));
    }

    @Test
    void shouldConnectAndReplaceWithEvaluatedEndpointAttributeWhenUrlEndpointAttributeIsDefined() {
        final HttpEntrypointAsyncConnector httpEntrypointAsyncConnector = mock(HttpEntrypointAsyncConnector.class);
        when(ctx.getInternalAttribute(ATTR_INTERNAL_ENTRYPOINT_CONNECTOR)).thenReturn(httpEntrypointAsyncConnector);
        when(ctx.getAttribute(ATTR_REQUEST_ENDPOINT)).thenReturn("{#api.properties['url']}");
        when(ctx.getTemplateEngine()).thenReturn(templateEngine);
        when(templateEngine.getValue(anyString(), eq(String.class))).thenReturn("http://api.gravitee.io/echo");
        when(endpointManager.next(any(EndpointCriteria.class))).thenReturn(managedEndpoint);
        when(managedEndpoint.getConnector()).thenReturn(endpointConnector);
        when(endpointConnector.connect(ctx)).thenReturn(Completable.complete());

        final TestObserver<Void> obs = cut.invoke(ctx).test();

        obs.assertNoValues();

        verify(endpointManager).next(argThat(criteria -> criteria.getName() == null));
        verify(ctx).setAttribute(ATTR_REQUEST_ENDPOINT, "http://api.gravitee.io/echo");
    }

    @Test
    void shouldFailWith503WhenNoEndpointConnectorHasBeenResolved() {
        final HttpEntrypointAsyncConnector httpEntrypointAsyncConnector = mock(HttpEntrypointAsyncConnector.class);
        when(ctx.getInternalAttribute(ATTR_INTERNAL_ENTRYPOINT_CONNECTOR)).thenReturn(httpEntrypointAsyncConnector);
        when(endpointManager.next(any(EndpointCriteria.class))).thenReturn(null);
        when(ctx.interruptWith(any(ExecutionFailure.class)))
            .thenAnswer(i -> Completable.error(new InterruptionFailureException(i.getArgument(0))));

        final TestObserver<Void> obs = cut.invoke(ctx).test();

        obs.assertError(e -> {
            assertThat(e)
                .isInstanceOf(InterruptionFailureException.class)
                .asInstanceOf(new InstanceOfAssertFactory<>(InterruptionFailureException.class, Assertions::assertThat))
                .extracting(InterruptionFailureException::getExecutionFailure)
                .isEqualTo(new ExecutionFailure(HttpStatusCode.SERVICE_UNAVAILABLE_503).key(NO_ENDPOINT_FOUND_KEY));

            return true;
        });
    }

    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("provideOverrideMethodAttributes")
    void shouldOverrideHttpMethodThanksToAttribute(Object attribute, String testName) {
        final HttpEntrypointAsyncConnector httpEntrypointAsyncConnector = mock(HttpEntrypointAsyncConnector.class);
        when(ctx.getInternalAttribute(ATTR_INTERNAL_ENTRYPOINT_CONNECTOR)).thenReturn(httpEntrypointAsyncConnector);
        when(endpointManager.next(any(EndpointCriteria.class))).thenReturn(managedEndpoint);
        when(managedEndpoint.getConnector()).thenReturn(endpointConnector);
        when(endpointConnector.connect(ctx)).thenReturn(Completable.complete());
        when(ctx.getAttribute(ATTR_REQUEST_ENDPOINT)).thenReturn(null);
        when(endpointConnector.connect(ctx)).thenReturn(Completable.complete());

        when(ctx.getAttribute(ATTR_REQUEST_METHOD)).thenReturn(attribute);
        when(ctx.request()).thenReturn(request);

        cut.invoke(ctx).test().assertComplete().assertNoValues();

        verify(request).method(HttpMethod.PUT);
    }

    @Test
    void shouldNotOverrideHttpMethodWhenAttributeIsNotValid() {
        final HttpEntrypointAsyncConnector httpEntrypointAsyncConnector = mock(HttpEntrypointAsyncConnector.class);
        when(ctx.getInternalAttribute(ATTR_INTERNAL_ENTRYPOINT_CONNECTOR)).thenReturn(httpEntrypointAsyncConnector);
        when(endpointManager.next(any(EndpointCriteria.class))).thenReturn(managedEndpoint);
        when(managedEndpoint.getConnector()).thenReturn(endpointConnector);
        when(ctx.getAttribute(ATTR_REQUEST_ENDPOINT)).thenReturn(null);

        // Here, return a random object from attribute and verify we end in error
        when(ctx.getAttribute(eq(ATTR_REQUEST_METHOD))).thenReturn(List.of("PUT"));

        when(ctx.interruptWith(any(ExecutionFailure.class)))
            .thenAnswer(i -> Completable.error(new InterruptionFailureException(i.getArgument(0))));

        cut
            .invoke(ctx)
            .test()
            .assertError(e -> {
                assertTrue(e instanceof InterruptionFailureException);
                final InterruptionFailureException failureException = (InterruptionFailureException) e;
                assertThat(failureException.getExecutionFailure().statusCode()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
                assertThat(failureException.getExecutionFailure().message())
                    .isNotNull()
                    .isEqualTo("Http method cannot be overridden because ATTR_REQUEST_METHOD attribute is invalid");
                assertThat(failureException.getExecutionFailure().key()).isEqualTo(HttpEndpointInvoker.INVALID_HTTP_METHOD);
                return true;
            });
    }

    private static Stream<Arguments> provideOverrideMethodAttributes() {
        return Stream.of(
            Arguments.of(HttpMethod.PUT, "Gravitee Common - HttpMethod"),
            Arguments.of(io.vertx.core.http.HttpMethod.PUT, "Vertx Core Http - HttpMethod"),
            Arguments.of("PUT", "Simple string")
        );
    }
}
