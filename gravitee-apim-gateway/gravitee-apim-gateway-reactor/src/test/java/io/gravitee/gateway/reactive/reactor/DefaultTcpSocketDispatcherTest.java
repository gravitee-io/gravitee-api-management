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
package io.gravitee.gateway.reactive.reactor;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.common.utils.UUID;
import io.gravitee.definition.model.v4.Api;
import io.gravitee.gateway.core.component.CustomComponentProvider;
import io.gravitee.gateway.reactive.core.context.DefaultExecutionContext;
import io.gravitee.gateway.reactive.reactor.handler.DefaultTcpAcceptorResolver;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.gateway.reactor.handler.DefaultTcpAcceptor;
import io.gravitee.gateway.reactor.handler.ReactorHandlerRegistry;
import io.gravitee.gateway.reactor.handler.TcpAcceptor;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.vertx.rxjava3.core.net.NetSocket;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class DefaultTcpSocketDispatcherTest {

    DefaultTcpSocketDispatcher cut;

    @Mock
    ReactorHandlerRegistry handlerRegistry;

    @Mock
    NetSocket netSocket;

    @Mock
    ApiReactor<ReactableApi<Api>> reactorHandler;

    @BeforeEach
    void before() {
        DefaultTcpAcceptor acceptor = new DefaultTcpAcceptor(reactorHandler, "foo", null);
        when(handlerRegistry.getAcceptors(TcpAcceptor.class)).thenReturn(List.of(acceptor));
        DefaultTcpAcceptorResolver resolver = new DefaultTcpAcceptorResolver(handlerRegistry);
        cut = new DefaultTcpSocketDispatcher(resolver, new CustomComponentProvider(), new UUID());
    }

    @Test
    void should_dispatch() {
        when(netSocket.indicatedServerName()).thenReturn("foo");
        when(netSocket.toFlowable()).thenReturn(Flowable.empty());
        when(reactorHandler.handle(any())).thenReturn(Completable.complete());
        cut.dispatch(netSocket, "server1").test().assertComplete().awaitDone(1, TimeUnit.SECONDS);
    }

    @Test
    void should_fail_dispatching_no_acceptor() {
        when(netSocket.indicatedServerName()).thenReturn("turlututu");
        cut
            .dispatch(netSocket, "server1")
            .doFinally(() -> verify(reactorHandler.handle(any(DefaultExecutionContext.class)), never()))
            .test()
            .assertError(err -> err.getMessage().contains("SNI: turlututu"))
            .awaitDone(100, TimeUnit.MILLISECONDS);
    }

    @Test
    void should_fail_dispatching_exception() {
        IllegalStateException unexpected = new IllegalStateException("unexpected");
        when(netSocket.indicatedServerName()).thenReturn("foo");
        when(netSocket.pause()).thenThrow(unexpected);
        cut
            .dispatch(netSocket, "server1")
            .doFinally(() -> verify(reactorHandler.handle(any(DefaultExecutionContext.class)), never()))
            .test()
            .assertError(unexpected)
            .awaitDone(100, TimeUnit.MILLISECONDS);
    }
}
