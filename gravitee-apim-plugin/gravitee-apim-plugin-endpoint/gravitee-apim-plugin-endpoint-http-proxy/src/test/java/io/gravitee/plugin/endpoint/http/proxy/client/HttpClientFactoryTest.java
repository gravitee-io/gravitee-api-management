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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.gateway.reactive.api.context.ExecutionContext;
import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.plugin.endpoint.http.proxy.configuration.HttpProxyEndpointConnectorConfiguration;
import io.gravitee.plugin.endpoint.http.proxy.configuration.HttpProxyEndpointConnectorSharedConfiguration;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.http.HttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class HttpClientFactoryTest {

    public static final int TIMEOUT_SECONDS = 60;

    @Mock
    protected ExecutionContext ctx;

    protected HttpClientFactory cut;
    protected HttpProxyEndpointConnectorConfiguration configuration;
    protected HttpProxyEndpointConnectorSharedConfiguration sharedConfiguration;

    @BeforeEach
    public void beforeEach() {
        cut = new HttpClientFactory();
        configuration = new HttpProxyEndpointConnectorConfiguration();
        configuration.setTarget("http://target");
        sharedConfiguration = new HttpProxyEndpointConnectorSharedConfiguration();
    }

    @Test
    void should_instantiate_http_client_once() {
        when(ctx.getComponent(Vertx.class)).thenReturn(mock(Vertx.class));
        when(ctx.getComponent(Configuration.class)).thenReturn(mock(Configuration.class));
        HttpClient prevHttpClient = cut.getOrBuildHttpClient(ctx, configuration, sharedConfiguration);
        for (int i = 0; i < TIMEOUT_SECONDS; i++) {
            HttpClient httpClient = cut.getOrBuildHttpClient(ctx, configuration, sharedConfiguration);
            assertSame(prevHttpClient, httpClient);
            prevHttpClient = httpClient;
        }
    }

    @Test
    void should_stop_http_client_when_stopping() {
        HttpClient httpClient = mock(HttpClient.class);
        ReflectionTestUtils.setField(cut, "httpClient", httpClient);

        cut.close();
        verify(httpClient).close();
    }

    @Test
    void should_not_throw_if_http_client_not_created() {
        assertDoesNotThrow(() -> cut.close());
    }
}
