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

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.common.lifecycle.LazyCloseable;
import io.gravitee.gateway.reactive.api.context.http.HttpExecutionContext;
import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.node.vertx.client.http.VertxHttpClientFactory;
import io.gravitee.plugin.endpoint.http.proxy.configuration.HttpProxyEndpointConnectorConfiguration;
import io.gravitee.plugin.endpoint.http.proxy.configuration.HttpProxyEndpointConnectorSharedConfiguration;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.http.HttpClient;
import java.util.concurrent.CountDownLatch;
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
    protected HttpExecutionContext ctx;

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
    @SuppressWarnings("unchecked")
    void should_stop_http_client_when_stopping() {
        HttpClient httpClient = mock(HttpClient.class);
        ((LazyCloseable<HttpClient>) ReflectionTestUtils.getField(cut, "httpClient")).getOrBuild(() -> httpClient);

        cut.close();
        verify(httpClient).close();
    }

    @Test
    void should_not_throw_if_http_client_not_created() {
        assertDoesNotThrow(() -> cut.close());
    }

    @Test
    void should_close_the_http_client_being_built_when_stopping() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        VertxHttpClientFactory vertxHttpClientFactory = mock(VertxHttpClientFactory.class);
        when(vertxHttpClientFactory.createHttpClient()).thenReturn(httpClient);
        VertxHttpClientFactory.VertxHttpClientFactoryBuilder builder = mock(VertxHttpClientFactory.VertxHttpClientFactoryBuilder.class);
        when(builder.build()).thenReturn(vertxHttpClientFactory);
        CountDownLatch building = new CountDownLatch(1);
        CountDownLatch built = new CountDownLatch(1);
        cut = new HttpClientFactory() {
            @Override
            protected VertxHttpClientFactory.VertxHttpClientFactoryBuilder buildHttpClient(
                HttpExecutionContext ctx,
                HttpProxyEndpointConnectorConfiguration configuration,
                HttpProxyEndpointConnectorSharedConfiguration sharedConfiguration
            ) {
                building.countDown();
                await()
                    .atMost(TIMEOUT_SECONDS, SECONDS)
                    .until(() -> built.getCount() == 0);
                return builder;
            }
        };

        Thread request = new Thread(() -> cut.getOrBuildHttpClient(ctx, configuration, sharedConfiguration));
        request.start();
        assertTrue(building.await(TIMEOUT_SECONDS, SECONDS));
        Thread stop = new Thread(cut::close);
        stop.start();
        // close() needs the lock held by the build: it can only wait for the client being built.
        await()
            .atMost(TIMEOUT_SECONDS, SECONDS)
            .until(() -> stop.getState() == Thread.State.BLOCKED);
        built.countDown();
        request.join(SECONDS.toMillis(TIMEOUT_SECONDS));
        stop.join(SECONDS.toMillis(TIMEOUT_SECONDS));

        verify(httpClient).close();
    }
}
