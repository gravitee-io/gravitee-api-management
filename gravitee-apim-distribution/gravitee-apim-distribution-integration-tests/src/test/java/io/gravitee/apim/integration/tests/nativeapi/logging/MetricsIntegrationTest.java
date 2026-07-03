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
package io.gravitee.apim.integration.tests.nativeapi.logging;

import static java.util.concurrent.TimeUnit.SECONDS;

import com.graviteesource.connector.nativekafka.endpoint.NativeKafkaEndpointConnectorFactory;
import com.graviteesource.connector.nativekafka.entrypoint.NativeKafkaEntrypointConnectorFactory;
import com.graviteesource.reactor.nativekafka.KafkaApiReactorFactory;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.configuration.GatewayConfigurationBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EndpointBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.gateway.tests.sdk.policy.PolicyBuilder;
import io.gravitee.apim.gateway.tests.sdk.reactor.ReactorBuilder;
import io.gravitee.apim.gateway.tests.sdk.reporter.FakeReporter;
import io.gravitee.apim.gateway.tests.sdk.utils.ResourceUtils;
import io.gravitee.apim.integration.tests.fake.KafkaFailingRequestPolicy;
import io.gravitee.apim.integration.tests.fake.KafkaInterruptingConnectPolicy;
import io.gravitee.apim.integration.tests.messages.AbstractKafkaEndpointIntegrationTest;
import io.gravitee.apim.plugin.reactor.ReactorPlugin;
import io.gravitee.definition.model.v4.nativeapi.NativeApi;
import io.gravitee.gateway.reactive.reactor.v4.reactor.ReactorFactory;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.policy.keyless.KeylessPolicy;
import io.gravitee.reporter.api.v4.metric.AdditionalMetric;
import io.gravitee.reporter.api.v4.metric.Metrics;
import io.reactivex.rxjava3.subjects.ReplaySubject;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.kafka.client.producer.KafkaProducer;
import io.vertx.rxjava3.kafka.client.producer.KafkaProducerRecord;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.*;

/**
 * End-to-end tests asserting the gateway's native-Kafka reactor emits a {@link Metrics} reportable with the
 * canonical {@code keyword_native-kafka_*} additional-metric keys and the right {@code connection-status} for each
 * connection lifecycle outcome. A real Kafka client connects through the native-Kafka entrypoint over TLS+SNI.
 *
 * <p>Four scenarios — one per status of {@code NativeKafkaConnectionStatus}:
 *
 * <ul>
 *   <li>{@link BrokerStopMidSession} (Order 1) — first connection succeeds, then the testcontainers Kafka backend
 *       is stopped: next connection emits {@code INTERNAL_ERROR}. Container is restarted in {@code finally} so
 *       the remaining scenarios still see a healthy broker.</li>
 *   <li>{@link HealthyConnection} (Order 2) — gateway reaches a healthy backend; emits {@code CONNECTED}.</li>
 *   <li>{@link ConnectPolicyInterrupts} (Order 3) — a test-only policy on the {@code entrypointConnect} flow phase
 *       throws {@code InterruptConnectionException}. Bypasses the request-flow policy-chain wrapping; the reactor
 *       emits {@code CONNECTION_ERROR}.</li>
 *   <li>{@link RequestPolicyFails} (Order 4) — a test-only policy on the {@code interact} flow phase throws on
 *       every request. The reactor wraps the throw in {@code KafkaException} and emits {@code SESSION_ERROR}
 *       on the post-CONNECTED metric.</li>
 * </ul>
 *
 * <p>The {@code connection-duration-ms} additional-metric key is exercised by
 * {@code NativeApiMetricsFindResponseAdapterTest} (round-trip through an ES document); it isn't asserted here because
 * the close-event metric requires a healthy proxied protocol exchange that this self-contained IT can't reliably
 * produce.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@TestClassOrder(ClassOrderer.OrderAnnotation.class)
class MetricsIntegrationTest {

    private static final String PASSTHROUGH_API_ID = "passthrough";
    private static final String INTERRUPTING_CONNECT_API_ID = "interrupting-connect";
    private static final String FAILING_REQUEST_API_ID = "failing-request";

    private static final int GATEWAY_KAFKA_PORT = 19092;
    private static final String API_HOST = "foo";
    private static final String GATEWAY_BOOTSTRAP = API_HOST + ".localhost:" + GATEWAY_KAFKA_PORT;
    private static final String CLIENT_ID = "kafka-test-client";

    private static final String STATUS_KEY = "keyword_native-kafka_connection-status";
    private static final String CLIENT_ID_KEY = "keyword_native-kafka_client-id";
    private static final String BROKER_ID_KEY = "keyword_native-kafka_broker-id";

    private static final String EXPECTED_ENTRYPOINT_ID = "native-kafka";

    @Nested
    @GatewayTest
    @DeployApi("/apis/v4/native/passthrough.json")
    @Order(1)
    class BrokerStopMidSession extends AbstractTest {

        @Test
        void broker_stop_emits_internal_error() throws Exception {
            triggerKafkaConnection();
            var openMetric = awaitFirstMetricFor(PASSTHROUGH_API_ID);

            kafka.stop();
            try {
                triggerKafkaConnection();
                var failedMetric = awaitNextMetricFor(PASSTHROUGH_API_ID);

                SoftAssertions.assertSoftly(soft -> {
                    // first connection (CONNECTED)
                    soft.assertThat(openMetric.getEntrypointId()).isEqualTo(EXPECTED_ENTRYPOINT_ID);
                    soft.assertThat(statusOf(openMetric)).isEqualTo("CONNECTED");
                    soft.assertThat(keywordValueOf(openMetric, CLIENT_ID_KEY)).isEqualTo(CLIENT_ID);
                    soft.assertThat(keywordValueOf(openMetric, BROKER_ID_KEY)).isNotBlank();
                    soft.assertThat(openMetric.getErrorKey()).isNull();
                    soft.assertThat(openMetric.getErrorMessage()).isNull();

                    // post-broker-stop (INTERNAL_ERROR — close-event after broker became unreachable)
                    soft.assertThat(failedMetric.getEntrypointId()).isEqualTo(EXPECTED_ENTRYPOINT_ID);
                    soft.assertThat(statusOf(failedMetric)).isEqualTo("INTERNAL_ERROR");
                    soft.assertThat(failedMetric.getErrorKey()).isEqualTo("UNKNOWN_SERVER_ERROR");
                    soft.assertThat(failedMetric.getErrorMessage()).containsIgnoringCase("Connection refused");
                });
            } finally {
                kafka.start();
            }
        }
    }

    @Nested
    @GatewayTest
    @DeployApi("/apis/v4/native/passthrough.json")
    @Order(2)
    class HealthyConnection extends AbstractTest {

        @Test
        void connection_open_emits_connected_status() throws Exception {
            triggerKafkaConnection();

            var metric = awaitFirstMetricFor(PASSTHROUGH_API_ID);

            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(metric.getApiId()).isEqualTo(PASSTHROUGH_API_ID);
                soft.assertThat(metric.getRequestId()).isNotBlank();
                soft.assertThat(metric.getEntrypointId()).isEqualTo(EXPECTED_ENTRYPOINT_ID);
                soft.assertThat(keysOf(metric)).contains(CLIENT_ID_KEY, BROKER_ID_KEY, STATUS_KEY);
                soft.assertThat(statusOf(metric)).isEqualTo("CONNECTED");
                soft.assertThat(keywordValueOf(metric, CLIENT_ID_KEY)).isEqualTo(CLIENT_ID);
                soft.assertThat(keywordValueOf(metric, BROKER_ID_KEY)).isNotBlank();
                soft.assertThat(metric.getErrorKey()).isNull();
                soft.assertThat(metric.getErrorMessage()).isNull();
            });
        }
    }

    @Nested
    @GatewayTest
    @DeployApi("/apis/v4/native/interrupting-connect-policy.json")
    @Order(3)
    class ConnectPolicyInterrupts extends AbstractTest {

        @Override
        public void configurePolicies(Map<String, PolicyPlugin> policies) {
            super.configurePolicies(policies);
            policies.put(
                KafkaInterruptingConnectPolicy.ID,
                PolicyBuilder.build(KafkaInterruptingConnectPolicy.ID, KafkaInterruptingConnectPolicy.class)
            );
        }

        @Test
        void connect_interrupt_emits_connection_error() throws Exception {
            triggerKafkaConnection();

            var metric = awaitFirstMetricFor(INTERRUPTING_CONNECT_API_ID);

            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(metric.getApiId()).isEqualTo(INTERRUPTING_CONNECT_API_ID);
                soft.assertThat(metric.getRequestId()).isNotBlank();
                soft.assertThat(metric.getEntrypointId()).isEqualTo(EXPECTED_ENTRYPOINT_ID);
                soft.assertThat(statusOf(metric)).isEqualTo("CONNECTION_ERROR");
                // interrupt fires before client-id capture; broker is resolved by the entrypoint chain regardless
                soft.assertThat(keysOf(metric)).contains(BROKER_ID_KEY).doesNotContain(CLIENT_ID_KEY);
                soft.assertThat(metric.getErrorKey()).isEqualTo("UNKNOWN_SERVER_ERROR");
                soft.assertThat(metric.getErrorMessage()).isEqualTo(KafkaInterruptingConnectPolicy.FAILURE_MESSAGE);
            });
        }
    }

    @Nested
    @GatewayTest
    @DeployApi("/apis/v4/native/failing-request-policy.json")
    @Order(4)
    class RequestPolicyFails extends AbstractTest {

        @Override
        public void configurePolicies(Map<String, PolicyPlugin> policies) {
            super.configurePolicies(policies);
            policies.put(KafkaFailingRequestPolicy.ID, PolicyBuilder.build(KafkaFailingRequestPolicy.ID, KafkaFailingRequestPolicy.class));
        }

        @Test
        void request_failure_emits_session_error() throws Exception {
            triggerKafkaConnection();

            // First metric is the CONNECTED open-event. The next metric is the post-request one — the policy fires,
            // KafkaApiReactorPolicyChains wraps the throw in KafkaException → SESSION_ERROR.
            var metric = awaitNextMetricFor(FAILING_REQUEST_API_ID);

            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(metric.getApiId()).isEqualTo(FAILING_REQUEST_API_ID);
                soft.assertThat(metric.getRequestId()).isNotBlank();
                soft.assertThat(metric.getEntrypointId()).isEqualTo(EXPECTED_ENTRYPOINT_ID);
                soft.assertThat(statusOf(metric)).isEqualTo("SESSION_ERROR");
                soft.assertThat(keywordValueOf(metric, CLIENT_ID_KEY)).isEqualTo(CLIENT_ID);
                soft.assertThat(metric.getErrorKey()).isEqualTo("UNKNOWN_SERVER_ERROR");
                soft.assertThat(metric.getErrorMessage()).isEqualTo(KafkaFailingRequestPolicy.FAILURE_MESSAGE);
            });
        }
    }

    @Slf4j
    abstract static class AbstractTest extends AbstractKafkaEndpointIntegrationTest {

        protected ReplaySubject<Metrics> metricsSubject;

        @BeforeEach
        void initMetricsReporter() {
            metricsSubject = ReplaySubject.create();
            FakeReporter fakeReporter = getBean(FakeReporter.class);
            fakeReporter.setReportableHandler(reportable -> {
                if (reportable instanceof Metrics metrics) {
                    log.info("REPORT {}", metrics);
                    metricsSubject.onNext(metrics.toBuilder().build());
                }
            });
        }

        @Override
        protected void configureGateway(GatewayConfigurationBuilder cfg) {
            cfg
                .set("kafka.enabled", true)
                .set("kafka.secured", true)
                .set("kafka.host", "0.0.0.0")
                .set("kafka.port", GATEWAY_KAFKA_PORT)
                .set("kafka.instances", 1)
                .set("kafka.ssl.sni", true)
                .set("kafka.ssl.keystore.type", "jks")
                .set("kafka.ssl.keystore.path", ResourceUtils.toPath("certs/keystore01.jks"))
                .set("kafka.ssl.keystore.password", "password")
                .set("kafka.routingMode", "host")
                .set("kafka.routingHostMode.defaultDomain", "localhost");
        }

        @Override
        public void configureReactors(Set<ReactorPlugin<? extends ReactorFactory<?>>> reactors) {
            reactors.add(ReactorBuilder.build(KafkaApiReactorFactory.class));
        }

        @Override
        public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
            entrypoints.putIfAbsent("native-kafka", EntrypointBuilder.build("native-kafka", NativeKafkaEntrypointConnectorFactory.class));
        }

        @Override
        public void configureEndpoints(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {
            endpoints.putIfAbsent("native-kafka", EndpointBuilder.build("native-kafka", NativeKafkaEndpointConnectorFactory.class));
        }

        @Override
        public void configurePolicies(Map<String, PolicyPlugin> policies) {
            super.configurePolicies(policies);
            policies.put("key-less", PolicyBuilder.build("key-less", KeylessPolicy.class));
        }

        @Override
        public void configureApi(ReactableApi<?> api, Class<?> definitionClass) {
            if (api.getDefinition() instanceof NativeApi nativeApi) {
                // testcontainers returns "PLAINTEXT://host:port" — the native-kafka endpoint expects a bare host:port pair.
                var backendBootstrap = kafka.getBootstrapServers().replaceFirst("^[A-Z_]+://", "");
                nativeApi
                    .getEndpointGroups()
                    .stream()
                    .flatMap(eg -> eg.getEndpoints().stream())
                    .filter(endpoint -> "native-kafka".equals(endpoint.getType()))
                    .forEach(endpoint ->
                        endpoint.setConfiguration(endpoint.getConfiguration().replace("bootstrap-server", backendBootstrap))
                    );
            }
        }

        protected void triggerKafkaConnection() throws InterruptedException {
            Vertx producerVertx = Vertx.vertx();
            KafkaProducer<String, byte[]> producer = createGatewayKafkaProducer(producerVertx);
            try {
                producer
                    .rxSend(KafkaProducerRecord.create(TEST_TOPIC, "k", "hello".getBytes()))
                    .ignoreElement()
                    .doOnError(e -> log.warn("kafka producer send failed", e))
                    .onErrorComplete()
                    .blockingAwait(10, SECONDS);
            } finally {
                producer
                    .rxClose()
                    .doOnError(e -> log.warn("kafka producer close failed", e))
                    .onErrorComplete()
                    .blockingAwait(5, SECONDS);
                producerVertx
                    .rxClose()
                    .doOnError(e -> log.warn("vertx close failed", e))
                    .onErrorComplete()
                    .blockingAwait(5, SECONDS);
            }
        }

        protected Metrics awaitFirstMetricFor(String apiId) {
            return metricsSubject
                .filter(m -> apiId.equals(m.getApiId()))
                .filter(m -> m.getAdditionalMetrics() != null && !m.getAdditionalMetrics().isEmpty())
                .take(1)
                .test()
                .awaitDone(15, SECONDS)
                .assertValueCount(1)
                .values()
                .getFirst();
        }

        protected Metrics awaitNextMetricFor(String apiId) {
            return metricsSubject
                .filter(m -> apiId.equals(m.getApiId()))
                .filter(m -> m.getAdditionalMetrics() != null && !m.getAdditionalMetrics().isEmpty())
                .skip(1)
                .take(1)
                .timeout(45, SECONDS)
                .blockingFirst();
        }

        protected static List<String> keysOf(Metrics metric) {
            return metric.getAdditionalMetrics().stream().map(AdditionalMetric::name).toList();
        }

        protected static String statusOf(Metrics metric) {
            return keywordValueOf(metric, STATUS_KEY);
        }

        protected static String keywordValueOf(Metrics metric, String key) {
            return metric.keywordAdditionalMetrics().getOrDefault(key, "<missing>");
        }

        private KafkaProducer<String, byte[]> createGatewayKafkaProducer(Vertx vertx) {
            Map<String, String> config = new HashMap<>();
            config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, GATEWAY_BOOTSTRAP);
            config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
            config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
            config.put(ProducerConfig.CLIENT_ID_CONFIG, CLIENT_ID);
            config.put("security.protocol", "SSL");
            config.put("ssl.truststore.location", ResourceUtils.toPath("certs/truststore01.jks"));
            config.put("ssl.truststore.password", "password");
            config.put("ssl.endpoint.identification.algorithm", "");
            return KafkaProducer.create(vertx, config);
        }
    }
}
