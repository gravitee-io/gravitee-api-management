/*
 * *
 *  * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *         http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package io.gravitee.gateway.reactive.handlers.api.v4.certificates;

import static io.gravitee.gateway.reactive.handlers.api.v4.certificates.TestFixtures.CERT;
import static io.gravitee.gateway.reactive.handlers.api.v4.certificates.TestFixtures.KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.gravitee.definition.model.v4.listener.ListenerType;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.listener.tcp.TcpListener;
import io.gravitee.definition.model.v4.listener.tls.ClientPemCertificate;
import io.gravitee.definition.model.v4.listener.tls.PemKeyPair;
import io.gravitee.definition.model.v4.listener.tls.Tls;
import io.gravitee.gateway.reactive.handlers.api.v4.Api;
import io.gravitee.gateway.reactive.handlers.api.v4.certificates.loaders.ApiKeyStoreLoader;
import io.gravitee.gateway.reactive.handlers.api.v4.certificates.loaders.ApiTrustStoreLoader;
import io.gravitee.node.api.certificate.KeyStoreLoader;
import io.gravitee.node.api.server.DefaultServerManager;
import io.gravitee.node.certificates.KeyStoreLoaderManager;
import io.gravitee.node.certificates.TrustStoreLoaderManager;
import io.gravitee.node.vertx.server.tcp.VertxTcpServer;
import io.gravitee.repository.ratelimit.api.RateLimitRepository;
import io.gravitee.repository.ratelimit.model.RateLimit;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class ApiKeyStoreLoaderManagerTest {

    private static final String SERVER1_ID = "tcp_1";
    private static final String SERVER2_ID = "tcp_2";

    @Mock
    VertxTcpServer server1;

    @Mock
    VertxTcpServer server2;

    @Mock
    Api api;

    @Mock
    private KeyStoreLoaderManager kslm1;

    @Mock
    private KeyStoreLoaderManager kslm2;

    @Mock
    private TrustStoreLoaderManager tslm1;

    @Mock
    private TrustStoreLoaderManager tslm2;

    private ApiKeyStoreLoaderManager cut;

    @BeforeEach
    void begin() {
        DefaultServerManager serverManager = new DefaultServerManager();
        lenient().when(api.getId()).thenReturn(UUID.randomUUID().toString());
        lenient().when(api.getDeployedAt()).thenReturn(new Date());

        lenient().when(server1.id()).thenReturn(SERVER1_ID);
        lenient().when(server1.keyStoreLoaderManager()).thenReturn(kslm1);
        lenient().when(server1.trustStoreLoaderManager()).thenReturn(tslm1);

        lenient().when(server2.id()).thenReturn(SERVER2_ID);
        lenient().when(server2.keyStoreLoaderManager()).thenReturn(kslm2);
        lenient().when(server2.trustStoreLoaderManager()).thenReturn(tslm2);

        // set no-op as handler to avoid crashes
        Answer<Void> answer = invocation -> {
            ((KeyStoreLoader) invocation.getArgument(0)).setEventHandler(ignore -> {});
            return null;
        };
        lenient().doAnswer(answer).when(kslm1).registerLoader(any(ApiKeyStoreLoader.class));
        lenient().doAnswer(answer).when(kslm2).registerLoader(any(ApiKeyStoreLoader.class));
        lenient().doAnswer(answer).when(tslm1).registerLoader(any(ApiTrustStoreLoader.class));
        lenient().doAnswer(answer).when(tslm2).registerLoader(any(ApiTrustStoreLoader.class));

        serverManager.register(server1);
        serverManager.register(server2);
        cut = new ApiKeyStoreLoaderManager(serverManager, ListenerType.TCP, api);
    }

    record Expectations(int server1KeyStoreLoader, int server1TrustStoreLoader, int server2KeyStoreLoader, int server2TrustStoreLoader) {
        int keyStoreLoaderSum() {
            return server1KeyStoreLoader + server2KeyStoreLoader;
        }
        int trustStoreLoaderSum() {
            return server1TrustStoreLoader + server2TrustStoreLoader;
        }
    }

    record TestData(io.gravitee.definition.model.v4.Api apiDef) {}

    public static Stream<Arguments> apis() {
        return Stream.of(
            arguments(
                "1 Listener (1 TLS + 0 mTLS : on all)",
                new TestData(
                    io.gravitee.definition.model.v4.Api
                        .builder()
                        .listeners(
                            List.of(
                                TcpListener
                                    .builder()
                                    .tls(
                                        Tls
                                            .builder()
                                            .pemKeyPairs(List.of(PemKeyPair.builder().certificateChain(CERT).privateKey(KEY).build()))
                                            .build()
                                    )
                                    .build()
                            )
                        )
                        .build()
                ),
                new Expectations(1, 0, 1, 0)
            ),
            arguments(
                "1 Listener (1 TLS + 1 mTLS : on all)",
                new TestData(
                    io.gravitee.definition.model.v4.Api
                        .builder()
                        .listeners(
                            List.of(
                                TcpListener
                                    .builder()
                                    .tls(
                                        Tls
                                            .builder()
                                            .pemKeyPairs(List.of(PemKeyPair.builder().certificateChain(CERT).privateKey(KEY).build()))
                                            .clientPemCertificates(List.of(ClientPemCertificate.builder().certificate(CERT).build()))
                                            .build()
                                    )
                                    .build()
                            )
                        )
                        .build()
                ),
                new Expectations(1, 1, 1, 1)
            ),
            arguments(
                "1 Listener (2 TLS +  1 mTLS : on all)",
                new TestData(
                    io.gravitee.definition.model.v4.Api
                        .builder()
                        .listeners(
                            List.of(
                                TcpListener
                                    .builder()
                                    .tls(
                                        Tls
                                            .builder()
                                            .pemKeyPairs(
                                                List.of(
                                                    PemKeyPair.builder().certificateChain(CERT).privateKey(KEY).build(),
                                                    PemKeyPair.builder().certificateChain(CERT).privateKey(KEY).build()
                                                )
                                            )
                                            .clientPemCertificates(List.of(ClientPemCertificate.builder().certificate(CERT).build()))
                                            .build()
                                    )
                                    .build()
                            )
                        )
                        .build()
                ),
                new Expectations(1, 1, 1, 1)
            ),
            arguments(
                "1 Listener (1 TLS + 2 mTLS : on all)",
                new TestData(
                    io.gravitee.definition.model.v4.Api
                        .builder()
                        .listeners(
                            List.of(
                                TcpListener
                                    .builder()
                                    .tls(
                                        Tls
                                            .builder()
                                            .pemKeyPairs(List.of(PemKeyPair.builder().certificateChain(CERT).privateKey(KEY).build()))
                                            .clientPemCertificates(
                                                List.of(
                                                    ClientPemCertificate.builder().certificate(CERT).build(),
                                                    ClientPemCertificate.builder().certificate(CERT).build()
                                                )
                                            )
                                            .build()
                                    )
                                    .build()
                            )
                        )
                        .build()
                ),
                new Expectations(1, 1, 1, 1)
            ),
            arguments(
                "2 Listeners (1 TLS + 1 mTLS : server 1, 1 TLS + 0 mTLS : all servers)",
                new TestData(
                    io.gravitee.definition.model.v4.Api
                        .builder()
                        .listeners(
                            List.of(
                                TcpListener
                                    .builder()
                                    .servers(List.of(SERVER1_ID))
                                    .tls(
                                        Tls
                                            .builder()
                                            .pemKeyPairs(List.of(PemKeyPair.builder().certificateChain(CERT).privateKey(KEY).build()))
                                            .clientPemCertificates(List.of(ClientPemCertificate.builder().certificate(CERT).build()))
                                            .build()
                                    )
                                    .build(),
                                TcpListener
                                    .builder()
                                    .tls(
                                        Tls
                                            .builder()
                                            .pemKeyPairs(List.of(PemKeyPair.builder().certificateChain(CERT).privateKey(KEY).build()))
                                            .build()
                                    )
                                    .build()
                            )
                        )
                        .build()
                ),
                new Expectations(2, 1, 1, 0)
            ),
            arguments(
                "2 Listeners (1 TLS + 0 mTLS : server 1, 1 TLS + 1 mTLS : all servers)",
                new TestData(
                    io.gravitee.definition.model.v4.Api
                        .builder()
                        .listeners(
                            List.of(
                                TcpListener
                                    .builder()
                                    .servers(List.of(SERVER1_ID))
                                    .tls(
                                        Tls
                                            .builder()
                                            .pemKeyPairs(List.of(PemKeyPair.builder().certificateChain(CERT).privateKey(KEY).build()))
                                            .build()
                                    )
                                    .build(),
                                TcpListener
                                    .builder()
                                    .tls(
                                        Tls
                                            .builder()
                                            .pemKeyPairs(List.of(PemKeyPair.builder().certificateChain(CERT).privateKey(KEY).build()))
                                            .clientPemCertificates(List.of(ClientPemCertificate.builder().certificate(CERT).build()))
                                            .build()
                                    )
                                    .build()
                            )
                        )
                        .build()
                ),
                new Expectations(2, 1, 1, 1)
            ),
            arguments(
                "2 Listeners (1 TLS + 0 mTLS : server 1, 0 TLS + 1 mTLS : server 2)",
                new TestData(
                    io.gravitee.definition.model.v4.Api
                        .builder()
                        .listeners(
                            List.of(
                                TcpListener
                                    .builder()
                                    .servers(List.of(SERVER1_ID))
                                    .tls(
                                        Tls
                                            .builder()
                                            .pemKeyPairs(List.of(PemKeyPair.builder().certificateChain(CERT).privateKey(KEY).build()))
                                            .build()
                                    )
                                    .build(),
                                TcpListener
                                    .builder()
                                    .servers(List.of(SERVER2_ID))
                                    .tls(
                                        Tls
                                            .builder()
                                            .clientPemCertificates(List.of(ClientPemCertificate.builder().certificate(CERT).build()))
                                            .build()
                                    )
                                    .build()
                            )
                        )
                        .build()
                ),
                new Expectations(1, 0, 0, 1)
            ),
            arguments("0 Listeners", new TestData(io.gravitee.definition.model.v4.Api.builder().build()), new Expectations(0, 0, 0, 0)),
            arguments(
                "1 HTTP Listener (1 TLS + 1 mTLS : on all)",
                new TestData(
                    io.gravitee.definition.model.v4.Api
                        .builder()
                        .listeners(
                            List.of(
                                HttpListener
                                    .builder()
                                    .tls(
                                        Tls
                                            .builder()
                                            .pemKeyPairs(List.of(PemKeyPair.builder().certificateChain(CERT).privateKey(KEY).build()))
                                            .clientPemCertificates(List.of(ClientPemCertificate.builder().certificate(CERT).build()))
                                            .build()
                                    )
                                    .build()
                            )
                        )
                        .build()
                ),
                new Expectations(0, 0, 0, 0)
            )
        );
    }

    @MethodSource("apis")
    @ParameterizedTest(name = "{0}")
    void should_register_key_store_loaders(String _name, TestData testData, Expectations expect) {
        when(api.getDefinition()).thenReturn(testData.apiDef());
        cut.start();
        assertThat(cut.getApiKeyStoreLoaders()).hasSize(expect.keyStoreLoaderSum());
        assertThat(cut.getApiTrustStoreLoaders()).hasSize(expect.trustStoreLoaderSum());
        verify(kslm1, times(expect.server1KeyStoreLoader())).registerLoader(any(ApiKeyStoreLoader.class));
        verify(kslm2, times(expect.server2KeyStoreLoader())).registerLoader(any(ApiKeyStoreLoader.class));
        verify(tslm1, times(expect.server1TrustStoreLoader())).registerLoader(any(ApiTrustStoreLoader.class));
        verify(tslm2, times(expect.server2TrustStoreLoader())).registerLoader(any(ApiTrustStoreLoader.class));
        cut.stop();
        assertThat(cut.getApiKeyStoreLoaders()).isEmpty();
        assertThat(cut.getApiTrustStoreLoaders()).isEmpty();
    }
}
