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
package io.gravitee.apim.integration.tests.tcp.logging;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.gateway.tests.sdk.AbstractWebsocketGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.configuration.GatewayConfigurationBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EndpointBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.gateway.tests.sdk.parameters.GatewayDynamicConfig;
import io.gravitee.apim.gateway.tests.sdk.reporter.FakeReporter;
import io.gravitee.apim.integration.tests.tcp.EtcHostsConfigurer;
import io.gravitee.definition.model.v4.analytics.Analytics;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.gravitee.plugin.endpoint.tcp.proxy.TcpProxyEndpointConnectorFactory;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.plugin.entrypoint.tcp.proxy.TcpProxyEntrypointConnectorFactory;
import io.gravitee.reporter.api.v4.metric.Metrics;
import io.reactivex.rxjava3.subjects.ReplaySubject;
import io.vertx.core.VertxOptions;
import io.vertx.core.dns.AddressResolverOptions;
import io.vertx.core.net.NetClientOptions;
import io.vertx.junit5.VertxTestContext;
import io.vertx.rxjava3.core.Vertx;
import java.util.Map;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author Aditya Goyal
 * @author GraviteeSource Team
 */
@GatewayTest
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class MetricsIntegrationTest extends AbstractWebsocketGatewayTest {

    ReplaySubject<Metrics> metricsSubject;

    @Override
    public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
        entrypoints.putIfAbsent("tcp-proxy", EntrypointBuilder.build("tcp-proxy", TcpProxyEntrypointConnectorFactory.class));
    }

    @Override
    public void configureEndpoints(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {
        endpoints.putIfAbsent("tcp-proxy", EndpointBuilder.build("tcp-proxy", TcpProxyEndpointConnectorFactory.class));
    }

    @Override
    protected void configureGateway(GatewayConfigurationBuilder gatewayConfigurationBuilder) {
        super.configureGateway(gatewayConfigurationBuilder);
        gatewayConfigurationBuilder.enableTcpGateway().set("tcp.ssl.keystore.type", "self-signed");
    }

    @Override
    public void configureApi(ReactableApi<?> api, Class<?> definitionClass) {
        super.configureApi(api, definitionClass);
        if (api.getDefinition() instanceof io.gravitee.definition.model.v4.Api v4Api) {
            var analytics = new Analytics();
            analytics.setEnabled(true);
            v4Api.setAnalytics(analytics);
        }
    }

    @BeforeEach
    void setUp() {
        metricsSubject = ReplaySubject.create();
        FakeReporter fakeReporter = getBean(FakeReporter.class);
        fakeReporter.setReportableHandler(reportable -> {
            if (reportable instanceof Metrics metrics && !metrics.isRequestEnded()) {
                metricsSubject.onNext(metrics.toBuilder().build());
            }
        });
    }

    @Test
    @DeployApi("/apis/v4/tcp/api-logging.json")
    @SneakyThrows
    void should_report_connection_metrics_on_tcp_connect(VertxTestContext testContext, GatewayDynamicConfig.TcpConfig tcpConfig) {
        try (var etcHostsConfigurer = new EtcHostsConfigurer()) {
            etcHostsConfigurer.addHost("127.0.0.1", "api-logging.tcp.local");

            AddressResolverOptions resolverOptions = new AddressResolverOptions()
                .setHostsPath(etcHostsConfigurer.generateHostsFile())
                .setOptResourceEnabled(true)
                .setCacheMinTimeToLive(0)
                .setQueryTimeout(5000);

            Vertx clientVertx = Vertx.vertx(new VertxOptions().setAddressResolverOptions(resolverOptions));
            try {
                NetClientOptions netClientOptions = new NetClientOptions()
                    .setSsl(true)
                    .setTrustAll(true)
                    .setHostnameVerificationAlgorithm("");

                clientVertx
                    .createNetClient(netClientOptions)
                    .connect(tcpConfig.tcpPort(), "api-logging.tcp.local")
                    .flatMapCompletable(socket -> socket.close())
                    .doOnError(testContext::failNow)
                    .subscribe();

                Metrics metrics = metricsSubject.timeout(5, SECONDS).firstOrError().blockingGet();

                assertThat(metrics.isRequestEnded()).isFalse();
                assertThat(metrics.getApiId()).isEqualTo("tcp-logging-api");
                assertThat(metrics.getApiName()).isEqualTo("TCP Logging API");
                assertThat(metrics.getApiType()).isEqualTo("proxy");
                assertThat(metrics.getEntrypointId()).isEqualTo("tcp-proxy");
                assertThat(metrics.getOrganizationId()).isNotNull();
                assertThat(metrics.getEnvironmentId()).isNotNull();
                assertThat(metrics.getRemoteAddress()).isNotNull();

                testContext.completeNow();
            } finally {
                clientVertx.close().blockingAwait();
            }
        }
    }
}
