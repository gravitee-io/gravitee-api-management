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
package io.gravitee.gateway.reactive.handlers.api.v4;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.listener.entrypoint.Entrypoint;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.listener.tcp.TcpListener;
import io.gravitee.gateway.env.RequestTimeoutConfiguration;
import io.gravitee.gateway.reactor.handler.TcpAcceptor;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.node.opentelemetry.OpenTelemetryFactory;
import io.gravitee.node.opentelemetry.configuration.OpenTelemetryConfiguration;
import io.gravitee.plugin.endpoint.EndpointConnectorPluginManager;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPluginManager;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class TcpApiReactorFactoryTest {

    TcpApiReactorFactory cut;

    @Mock
    Configuration configuration;

    @Mock
    Node node;

    @Mock
    EntrypointConnectorPluginManager entrypoints;

    @Mock
    EndpointConnectorPluginManager endpoints;

    @Mock
    RequestTimeoutConfiguration timeoutConfig;

    @Mock
    OpenTelemetryConfiguration openTelemetryConfiguration;

    @Mock
    OpenTelemetryFactory openTelemetryFactory;

    @BeforeEach
    void before() {
        cut =
            new TcpApiReactorFactory(
                configuration,
                node,
                entrypoints,
                endpoints,
                timeoutConfig,
                openTelemetryConfiguration,
                openTelemetryFactory,
                List.of()
            );
    }

    static Stream<Arguments> apis() {
        io.gravitee.definition.model.v4.Api api1 = new io.gravitee.definition.model.v4.Api();
        api1.setDefinitionVersion(DefinitionVersion.V4);
        api1.setListeners(List.of(new TcpListener()));
        api1.setType(ApiType.PROXY);

        io.gravitee.definition.model.v4.Api api2 = new io.gravitee.definition.model.v4.Api();
        api2.setDefinitionVersion(DefinitionVersion.V2);
        api2.setListeners(List.of(new TcpListener()));
        api2.setType(ApiType.PROXY);

        io.gravitee.definition.model.v4.Api api3 = new io.gravitee.definition.model.v4.Api();
        api3.setDefinitionVersion(DefinitionVersion.V4);
        api3.setListeners(List.of(new TcpListener()));
        api3.setType(ApiType.MESSAGE);

        io.gravitee.definition.model.v4.Api api4 = new io.gravitee.definition.model.v4.Api();
        api4.setDefinitionVersion(DefinitionVersion.V4);
        api4.setListeners(List.of(new HttpListener()));
        api4.setType(ApiType.MESSAGE);

        return Stream.of(
            arguments(new Api(api1), true),
            arguments(new Api(api2), false),
            arguments(new Api(api3), false),
            arguments(new Api(api4), false)
        );
    }

    @MethodSource("apis")
    @ParameterizedTest
    void should_call_can_create(Api api, boolean canCreate) {
        assertThat(cut.canCreate(api)).isEqualTo(canCreate);
    }

    @Test
    void should_create() {
        io.gravitee.definition.model.v4.Api apiDef = new io.gravitee.definition.model.v4.Api();
        apiDef.setDefinitionVersion(DefinitionVersion.V4);
        TcpListener listener = new TcpListener();
        listener.setHosts(List.of("foo"));
        listener.setEntrypoints(List.of(new Entrypoint()));
        apiDef.setListeners(List.of(listener));
        apiDef.setType(ApiType.PROXY);

        // avoid TcpApiReactor to complain
        when(configuration.getProperty(any(), eq(Long.class), any())).thenReturn(0L);

        assertThat(cut.create(new Api(apiDef)).acceptors()).hasSize(1);
        assertThat(((TcpAcceptor) cut.create(new Api(apiDef)).acceptors().get(0)).host()).isEqualTo("foo");
    }
}
