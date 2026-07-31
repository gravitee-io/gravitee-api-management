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
package io.gravitee.plugin.endpoint.http.proxy.client;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.v4.http.HttpClientOptions;
import io.gravitee.definition.model.v4.http.ProtocolVersion;
import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.node.vertx.client.http.VertxHttpClientOptions;
import io.gravitee.node.vertx.client.http.VertxHttpProtocolVersion;
import io.gravitee.plugin.endpoint.http.proxy.configuration.HttpProxyEndpointConnectorConfiguration;
import io.gravitee.plugin.endpoint.http.proxy.configuration.HttpProxyEndpointConnectorSharedConfiguration;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.http.HttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GrpcHttpClientFactoryTest extends HttpClientFactoryTest {

    @BeforeEach
    public void beforeEach() {
        cut = new GrpcHttpClientFactory();
        configuration = new HttpProxyEndpointConnectorConfiguration();
        configuration.setTarget("grpc://target");
        sharedConfiguration = new HttpProxyEndpointConnectorSharedConfiguration();
    }

    @Test
    void should_not_mutate_the_shared_http_options_when_forcing_http_2() {
        // The shared configuration is the very same object the plain HTTP client factory of the endpoint group
        // reads from, so a single gRPC-classified request must not switch the whole endpoint to HTTP/2.
        final HttpClientOptions sharedHttpOptions = sharedConfiguration.getHttpOptions();
        sharedHttpOptions.setVersion(ProtocolVersion.HTTP_1_1);
        sharedHttpOptions.setClearTextUpgrade(true);

        when(ctx.getComponent(Vertx.class)).thenReturn(mock(Vertx.class));
        when(ctx.getComponent(Configuration.class)).thenReturn(mock(Configuration.class));
        cut.getOrBuildHttpClient(ctx, configuration, sharedConfiguration);

        assertSame(sharedHttpOptions, sharedConfiguration.getHttpOptions());
        assertEquals(ProtocolVersion.HTTP_1_1, sharedHttpOptions.getVersion());
        assertTrue(sharedHttpOptions.isClearTextUpgrade());
    }

    @Test
    void should_build_the_grpc_client_over_http_2_without_clear_text_upgrade() {
        sharedConfiguration.getHttpOptions().setVersion(ProtocolVersion.HTTP_1_1);
        sharedConfiguration.getHttpOptions().setClearTextUpgrade(true);
        when(ctx.getComponent(Vertx.class)).thenReturn(mock(Vertx.class));
        when(ctx.getComponent(Configuration.class)).thenReturn(mock(Configuration.class));

        final var builder = cut.buildHttpClient(ctx, configuration, sharedConfiguration);
        final var grpcHttpOptions = (VertxHttpClientOptions) ReflectionTestUtils.getField(builder, "httpOptions");

        assertNotNull(grpcHttpOptions);
        assertEquals(VertxHttpProtocolVersion.HTTP_2, grpcHttpOptions.getVersion());
        assertFalse(grpcHttpOptions.isClearTextUpgrade());
    }
}
