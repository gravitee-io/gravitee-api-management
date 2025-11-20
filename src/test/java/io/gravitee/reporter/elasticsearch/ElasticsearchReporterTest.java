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
package io.gravitee.reporter.elasticsearch;

import static io.gravitee.reporter.api.http.SecurityType.API_KEY;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.common.templating.FreeMarkerComponent;
import io.gravitee.elasticsearch.client.Client;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.node.api.Node;
import io.gravitee.reporter.api.Reportable;
import io.gravitee.reporter.api.common.Request;
import io.gravitee.reporter.api.common.Response;
import io.gravitee.reporter.api.health.EndpointStatus;
import io.gravitee.reporter.api.health.Step;
import io.gravitee.reporter.api.http.Metrics;
import io.gravitee.reporter.api.log.Log;
import io.gravitee.reporter.api.monitor.JvmInfo;
import io.gravitee.reporter.api.monitor.Monitor;
import io.gravitee.reporter.api.monitor.OsInfo;
import io.gravitee.reporter.api.monitor.ProcessInfo;
import io.gravitee.reporter.api.v4.common.MessageConnectorType;
import io.gravitee.reporter.api.v4.common.MessageOperation;
import io.gravitee.reporter.api.v4.metric.AdditionalMetric;
import io.gravitee.reporter.api.v4.metric.MessageMetrics;
import io.gravitee.reporter.elasticsearch.config.PipelineConfiguration;
import io.gravitee.reporter.elasticsearch.config.ReporterConfiguration;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.TestScheduler;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { ElasticsearchReporterTest.TestConfig.class })
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ElasticsearchReporterTest {

    @Autowired
    private ElasticsearchReporter reporter;

    private TestScheduler testScheduler;

    @Configuration
    @Import(IntegrationTestConfiguration.class) // the actual configuration
    public static class TestConfig {

        @Bean
        public ElasticsearchReporter reporter(
            final Node node,
            final Client client,
            final ReporterConfiguration reporterConfiguration,
            final PipelineConfiguration pipelineConfiguration,
            final FreeMarkerComponent freeMarkerComponent
        ) {
            return new ElasticsearchReporter(node, reporterConfiguration, pipelineConfiguration, freeMarkerComponent, client);
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        testScheduler = new TestScheduler();
        RxJavaPlugins.setComputationSchedulerHandler(ignore -> testScheduler);

        this.reporter.start();
    }

    @AfterEach
    void tearsDown() throws Exception {
        this.reporter.stop();

        // reset it
        RxJavaPlugins.reset();
    }

    @Test
    void should_report_metrics() throws InterruptedException {
        final Metrics requestMetrics = Metrics.on(Instant.now().toEpochMilli()).build();
        requestMetrics.setTransactionId("transactionId");
        requestMetrics.setTenant("tenant");
        requestMetrics.setStatus(200);
        requestMetrics.setResponseContentLength(10);
        requestMetrics.setUri("uri");
        requestMetrics.setRemoteAddress("remoteAddress");
        requestMetrics.setLocalAddress("localAddress");
        requestMetrics.setRequestId("requestId");
        requestMetrics.setHttpMethod(HttpMethod.GET);
        requestMetrics.setRequestContentLength(20);
        requestMetrics.setProxyResponseTimeMs(10);

        requestMetrics.setProxyLatencyMs(100);
        requestMetrics.setPlan("plan");
        requestMetrics.setMessage("message");
        requestMetrics.setEndpoint("endPoint");

        requestMetrics.setApplication("application");
        requestMetrics.setApiResponseTimeMs(100);
        requestMetrics.setApi("api");
        requestMetrics.setSecurityType(API_KEY);
        requestMetrics.setSecurityToken("apiKey");

        requestMetrics.setCustomMetrics(Map.of("foo", "bar"));

        reportAndAssert(requestMetrics);
    }

    @Test
    void should_report_message_metrics() throws InterruptedException {
        final MessageMetrics messageMetrics = MessageMetrics
            .builder()
            .apiId("api")
            .apiName("API Name")
            .clientIdentifier("client")
            .connectorId("webhook")
            .connectorType(MessageConnectorType.ENTRYPOINT)
            .contentLength(42)
            .correlationId("123456789-a")
            .count(1)
            .countIncrement(8)
            .error(false)
            .errorCount(0)
            .errorCountIncrement(2)
            .gatewayLatencyMs(3L)
            .operation(MessageOperation.PUBLISH)
            .parentCorrelationId("b-987654321")
            .requestId("0000000-r")
            .timestamp(Instant.now().toEpochMilli())
            .customMetrics(Map.of("foo", "bar"))
            .additionalMetrics(
                Set.of(
                    new AdditionalMetric.StringMetric("string_additional", "on top of it"),
                    new AdditionalMetric.JSONMetric("json_additional", "{\"hello\":\"world\"}"),
                    new AdditionalMetric.DoubleMetric("double_additional", 3.14),
                    new AdditionalMetric.IntegerMetric("int_additional", 42),
                    new AdditionalMetric.LongMetric("long_additional", 1L),
                    new AdditionalMetric.KeywordMetric("keyword_additional", "alpha"),
                    new AdditionalMetric.BooleanMetric("bool_additional", true)
                )
            )
            .build();

        reportAndAssert(messageMetrics);
    }

    private void reportAndAssert(Reportable reportable) throws InterruptedException {
        // bulk of three line
        TestObserver<Void> metrics1 = Completable.fromRunnable(() -> reporter.report(reportable)).test();
        TestObserver<Void> metrics2 = Completable.fromRunnable(() -> reporter.report(reportable)).test();
        TestObserver<Void> metrics3 = Completable.fromRunnable(() -> reporter.report(reportable)).test();

        // advance time manually
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);

        metrics1.await();
        metrics2.await();
        metrics3.await();

        metrics1.assertNoErrors();
        metrics2.assertNoErrors();
        metrics3.assertNoErrors();
    }

    @Test
    void shout_report_health() throws InterruptedException {
        final Response defaultResponse = new Response();
        defaultResponse.setStatus(200);
        final Request defaultRequest = new Request();
        defaultRequest.setUri("https://api.gravitee.io/echo/echo");
        defaultRequest.setMethod(HttpMethod.GET);
        final Step defaultStep = EndpointStatus
            .forStep("default-step")
            .responseTime(117)
            .success()
            .request(defaultRequest)
            .response(defaultResponse)
            .build();

        final Response anotherResponse = new Response();
        anotherResponse.setStatus(500);
        final Request anotherRequest = new Request();
        anotherRequest.setUri("https://api.gravitee.io/echo/echo");
        anotherRequest.setMethod(HttpMethod.GET);

        final Step anotherStep = EndpointStatus
            .forStep("another-step")
            .responseTime(57)
            .fail("NPE")
            .request(defaultRequest)
            .response(defaultResponse)
            .build();

        final EndpointStatus endpointHealthStatus = EndpointStatus
            .forEndpoint("be0aa9c9-ca1c-4d0a-8aa9-c9ca1c5d0aab", "https://api.gravitee.io/echo/")
            .on(Instant.now().toEpochMilli())
            .step(defaultStep)
            .step(anotherStep)
            .build();

        endpointHealthStatus.setAvailable(true);
        endpointHealthStatus.setState(3);
        endpointHealthStatus.setResponseTime(175);

        TestObserver<Void> metrics1 = Completable.fromRunnable(() -> reporter.report(endpointHealthStatus)).test();

        // advance time manually
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);

        metrics1.await();
        metrics1.assertNoErrors();
    }

    @Test
    void should_report_monitor() throws Exception {
        final JvmInfo jvmInfo = new JvmInfo(100, 20000);

        JvmInfo.GarbageCollector youngInfo = new JvmInfo.GarbageCollector();
        youngInfo.collectionCount = 5;
        youngInfo.collectionTime = 199;
        youngInfo.name = "young";

        JvmInfo.GarbageCollector oldInfo = new JvmInfo.GarbageCollector();
        oldInfo.collectionCount = 2;
        oldInfo.collectionTime = 206;
        oldInfo.name = "old";

        jvmInfo.gc = new JvmInfo.GarbageCollectors();
        jvmInfo.gc.collectors = new JvmInfo.GarbageCollector[] { youngInfo, oldInfo };

        jvmInfo.mem = new JvmInfo.Mem();
        jvmInfo.mem.heapCommitted = 1;
        jvmInfo.mem.heapMax = 1;
        jvmInfo.mem.heapUsed = 1;
        jvmInfo.mem.nonHeapCommitted = 1;
        jvmInfo.mem.nonHeapUsed = 1;
        JvmInfo.MemoryPool young = new JvmInfo.MemoryPool("young", 85549752, 137363456, 134742016, 137363456);
        JvmInfo.MemoryPool survivor = new JvmInfo.MemoryPool("survivor", 16821208, 137363456, 134742016, 137363456);
        JvmInfo.MemoryPool old = new JvmInfo.MemoryPool("old", 17060240, 137363456, 134742016, 137363456);
        jvmInfo.mem.pools = new JvmInfo.MemoryPool[] { young, survivor, old };

        jvmInfo.threads = new JvmInfo.Threads();
        jvmInfo.threads.count = 3;
        jvmInfo.threads.peakCount = 3;

        final OsInfo osInfo = new OsInfo();
        osInfo.cpu = new OsInfo.Cpu();
        osInfo.cpu.loadAverage = null;
        osInfo.cpu.percent = 1;

        osInfo.mem = new OsInfo.Mem();
        osInfo.mem.free = 1;
        osInfo.mem.total = 10;

        osInfo.swap = new OsInfo.Swap();
        osInfo.swap.free = 1;
        osInfo.swap.total = 10;

        osInfo.timestamp = 10;

        final ProcessInfo processInfo = new ProcessInfo();
        processInfo.maxFileDescriptors = 1;
        processInfo.openFileDescriptors = 1;
        processInfo.timestamp = 10;

        final Monitor monitor = Monitor
            .on("b187fe8f-98fa-4aa9-87fe-8f98facaa956")
            .at(Instant.now().toEpochMilli())
            .jvm(jvmInfo)
            .os(osInfo)
            .process(processInfo)
            .build();

        TestObserver<Void> metrics1 = Completable.fromRunnable(() -> reporter.report(monitor)).test();

        // advance time manually
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);

        metrics1.await();
        metrics1.assertNoErrors();
    }

    @Test
    void should_report_log() throws InterruptedException {
        Log log = new Log(Instant.now().toEpochMilli());

        log.setApi("my-api");
        log.setRequestId("my-request-id");

        Request clientReq = new Request();
        clientReq.setUri("http//gateway-url");
        clientReq.setMethod(HttpMethod.POST);
        clientReq.setBody("request-payload");
        HttpHeaders clientReqHeaders = HttpHeaders.create();
        clientReqHeaders.add("my-header", "my-header-value");
        clientReq.setHeaders(clientReqHeaders);

        Request proxyReq = new Request();
        proxyReq.setUri("http//backend-url");
        proxyReq.setMethod(HttpMethod.POST);
        proxyReq.setBody("request-payload");
        HttpHeaders proxyReqHeaders = HttpHeaders.create();
        proxyReqHeaders.add("my-header", "my-header-value");
        proxyReq.setHeaders(proxyReqHeaders);

        Response proxyResp = new Response();
        proxyResp.setStatus(HttpStatusCode.OK_200);
        proxyResp.setBody("response-payload");
        HttpHeaders proxyRespHeaders = HttpHeaders.create();
        proxyRespHeaders.add("my-header", "my-header-value");
        proxyResp.setHeaders(proxyRespHeaders);

        Response clientResp = new Response();
        clientResp.setStatus(HttpStatusCode.OK_200);
        clientResp.setBody("response-payload");
        HttpHeaders clientRespHeaders = HttpHeaders.create();
        clientRespHeaders.add("my-header", "my-header-value");
        clientResp.setHeaders(clientRespHeaders);

        log.setClientRequest(clientReq);
        log.setProxyRequest(proxyReq);
        log.setProxyResponse(proxyResp);
        log.setClientResponse(clientResp);

        TestObserver<Void> logObs = Completable.fromRunnable(() -> reporter.report(log)).test();

        // advance time manually
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);

        logObs.await();
        logObs.assertNoErrors();
    }

    @Test
    void report_test() throws InterruptedException {
        TestObserver<Void> metrics1 = Completable.fromRunnable(() -> reporter.report(mockRequestMetrics())).test();

        // advance time manually
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);

        metrics1.await();
        metrics1.assertNoErrors();
    }

    private Metrics mockRequestMetrics() {
        Metrics requestMetrics = Metrics.on(new Date().getTime()).build();
        requestMetrics.setApi("4d8d6ca8-c2c7-4ab8-8d6c-a8c2c79ab8a1");
        requestMetrics.setSecurityType(API_KEY);
        requestMetrics.setSecurityToken("e14cfcb8-188d-4cb9-ad06-002aea5aab12");
        requestMetrics.setApiResponseTimeMs(50);
        requestMetrics.setApplication("31b0d824-4f6a-4f58-b0d8-244f6a4f58d7");
        requestMetrics.setEndpoint("31b0d824-4f6a-4f58-b0d8-244f6a4f58d7");
        requestMetrics.setMessage(null);
        requestMetrics.setPlan("1fe07b71-ae91-4c15-a07b-71ae919c1560");
        requestMetrics.setProxyLatencyMs(1);
        requestMetrics.setProxyResponseTimeMs(51);
        requestMetrics.setRequestContentLength(0);
        requestMetrics.setHttpMethod(HttpMethod.GET);
        requestMetrics.setRequestId("ac096af0-cc48-4264-896a-f0cc4872644e");
        requestMetrics.setLocalAddress("172.18.0.6");
        requestMetrics.setRemoteAddress("172.18.0.1");
        requestMetrics.setUri("/echo");
        requestMetrics.setResponseContentLength(700);
        requestMetrics.setStatus(HttpStatusCode.OK_200);
        requestMetrics.setTenant(null);
        requestMetrics.setTransactionId("ba571368-f5e6-48b7-9713-68f5e698b761");
        requestMetrics.addCustomMetric("CustomMetricKey1", "CustomMetricValue1");
        requestMetrics.addCustomMetric("CustomMetricKey2", "CustomMetricValue2");

        return requestMetrics;
    }
}
