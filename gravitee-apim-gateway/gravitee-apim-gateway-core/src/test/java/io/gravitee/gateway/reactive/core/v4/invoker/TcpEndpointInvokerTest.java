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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.gateway.reactive.api.connector.endpoint.TcpEndpointConnector;
import io.gravitee.gateway.reactive.api.connector.entrypoint.TcpEntrypointConnector;
import io.gravitee.gateway.reactive.api.context.tcp.TcpExecutionContext;
import io.gravitee.gateway.reactive.core.v4.endpoint.EndpointManager;
import io.gravitee.gateway.reactive.core.v4.endpoint.ManagedEndpoint;
import io.reactivex.rxjava3.core.Completable;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class TcpEndpointInvokerTest {

    @Mock
    private EndpointManager endpointManager;

    @Mock
    private TcpExecutionContext ctx;

    private TcpEndpointInvoker cut;

    @BeforeEach
    void init() {
        cut = new TcpEndpointInvoker(endpointManager);
    }

    @Test
    void should_not_connect_to_endpoint_connector_not_resolved() {
        final TcpEntrypointConnector tcpEntrypointConnector = mock(TcpEntrypointConnector.class);
        when(endpointManager.next()).thenReturn(null);
        cut
            .invoke(ctx)
            .test()
            .awaitDone(10, TimeUnit.SECONDS)
            .assertError(t -> {
                assertThat(t).isInstanceOf(IllegalStateException.class).hasMessage(TcpEndpointInvoker.NO_ENDPOINT_FOUND_KEY);
                return true;
            });
    }

    @Test
    void should_connect_to_endpoint_connector() {
        final TcpEntrypointConnector tcpEntrypointConnector = mock(TcpEntrypointConnector.class);

        final ManagedEndpoint managedEndpoint = mock(ManagedEndpoint.class);
        when(endpointManager.next()).thenReturn(managedEndpoint);
        final TcpEndpointConnector endpoint = mock(TcpEndpointConnector.class);
        when(managedEndpoint.getConnector()).thenReturn(endpoint);
        when(endpoint.id()).thenReturn("connector-id");
        when(endpoint.connect(any())).thenReturn(Completable.complete());
        cut.invoke(ctx).test().awaitDone(10, TimeUnit.SECONDS).assertComplete();

        verify(endpoint).connect(ctx);
    }
}
