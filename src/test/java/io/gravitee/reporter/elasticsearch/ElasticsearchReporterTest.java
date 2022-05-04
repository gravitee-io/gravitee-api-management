/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.reporter.elasticsearch;

import static io.gravitee.reporter.api.http.SecurityType.API_KEY;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.elasticsearch.version.ElasticsearchInfo;
import io.gravitee.elasticsearch.version.Version;
import io.gravitee.gateway.api.http.HttpHeaders;
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
import io.gravitee.reporter.elasticsearch.spring.ElasticsearchReporterConfigurationTest;
import io.gravitee.reporter.elasticsearch.spring.context.*;
import io.reactivex.observers.TestObserver;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.TestScheduler;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { ElasticsearchReporterTest.TestConfig.class })
public class ElasticsearchReporterTest {

    @Autowired
    private ElasticsearchReporter reporter;

    private TestScheduler testScheduler;

    @Configuration
    @Import(ElasticsearchReporterConfigurationTest.class) // the actual configuration
    public static class TestConfig {

        @Bean
        public ElasticsearchReporter reporter() {
            return new ElasticsearchReporter();
        }
    }

    @Before
    public void setUp() throws Exception {
        testScheduler = new TestScheduler();
        RxJavaPlugins.setComputationSchedulerHandler(ignore -> testScheduler);

        this.reporter.start();
    }

    @After
    public void tearsDown() throws Exception {
        this.reporter.stop();

        // reset it
        RxJavaPlugins.setComputationSchedulerHandler(null);
    }

    @Test
    public void shouldReportMetrics() {
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

        // bulk of three line
        TestObserver metrics1 = reporter.rxReport(requestMetrics).test();
        TestObserver metrics2 = reporter.rxReport(requestMetrics).test();
        TestObserver metrics3 = reporter.rxReport(requestMetrics).test();

        // advance time manually
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);

        metrics1.awaitTerminalEvent();
        metrics2.awaitTerminalEvent();
        metrics3.awaitTerminalEvent();

        metrics1.assertNoErrors();
        metrics2.assertNoErrors();
        metrics3.assertNoErrors();
    }

    @Test
    public void shoutReportHealth() {
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

        TestObserver metrics1 = reporter.rxReport(endpointHealthStatus).test();

        // advance time manually
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);

        metrics1.awaitTerminalEvent();
        metrics1.assertNoErrors();
    }

    @Test
    public void shouldReportMonitor() throws Exception {
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

        TestObserver metrics1 = reporter.rxReport(monitor).test();

        // advance time manually
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);

        metrics1.awaitTerminalEvent();
        metrics1.assertNoErrors();
    }

    @Test
    public void shouldReportLog() {
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

        TestObserver logObs = reporter.rxReport(log).test();

        // advance time manually
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);

        logObs.awaitTerminalEvent();
        logObs.assertNoErrors();
    }

    @Test
    public void reportTest() {
        TestObserver metrics1 = reporter.rxReport(mockRequestMetrics()).test();

        // advance time manually
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);

        metrics1.awaitTerminalEvent();
        metrics1.assertNoErrors();
    }

    @Test
    public void getBeanRegistrer_should_instantiate_es5_registrer_when_major_version_is_5() {
        Version version = new Version();
        version.setNumber("5.12.7");
        ElasticsearchInfo elasticsearchInfo = new ElasticsearchInfo();
        elasticsearchInfo.setVersion(version);

        AbstractElasticBeanRegistrer beanRegistrer = reporter.getBeanRegistrerFromElasticsearchInfo(elasticsearchInfo);

        assertTrue(beanRegistrer instanceof Elastic5xBeanRegistrer);
    }

    @Test
    public void getBeanRegistrer_should_instantiate_es6_registrer_when_major_version_is_6() {
        Version version = new Version();
        version.setNumber("6.12.7");
        ElasticsearchInfo elasticsearchInfo = new ElasticsearchInfo();
        elasticsearchInfo.setVersion(version);

        AbstractElasticBeanRegistrer beanRegistrer = reporter.getBeanRegistrerFromElasticsearchInfo(elasticsearchInfo);

        assertTrue(beanRegistrer instanceof Elastic6xBeanRegistrer);
    }

    @Test
    public void getBeanRegistrer_should_instantiate_es7_registrer_when_major_version_is_7() {
        Version version = new Version();
        version.setNumber("7.12.7");
        ElasticsearchInfo elasticsearchInfo = new ElasticsearchInfo();
        elasticsearchInfo.setVersion(version);

        AbstractElasticBeanRegistrer beanRegistrer = reporter.getBeanRegistrerFromElasticsearchInfo(elasticsearchInfo);

        assertTrue(beanRegistrer instanceof Elastic7xBeanRegistrer);
    }

    @Test
    public void getBeanRegistrer_should_instantiate_opensearch_registrer_when_opensearch_distribution_version_1() {
        Version version = new Version();
        version.setNumber("1.12.7");
        version.setDistribution("opensearch");
        ElasticsearchInfo elasticsearchInfo = new ElasticsearchInfo();
        elasticsearchInfo.setVersion(version);

        AbstractElasticBeanRegistrer beanRegistrer = reporter.getBeanRegistrerFromElasticsearchInfo(elasticsearchInfo);

        assertTrue(beanRegistrer instanceof OpenSearchBeanRegistrer);
    }

    @Test
    public void getBeanRegistrer_should_instantiate_opensearch_registrer_when_opensearch_distribution_higher_version() {
        Version version = new Version();
        version.setNumber("2.12.7");
        version.setDistribution("opensearch");
        ElasticsearchInfo elasticsearchInfo = new ElasticsearchInfo();
        elasticsearchInfo.setVersion(version);

        AbstractElasticBeanRegistrer beanRegistrer = reporter.getBeanRegistrerFromElasticsearchInfo(elasticsearchInfo);

        assertNull(beanRegistrer);
    }

    @Test
    public void getBeanRegistrer_should_return_null_when_unknown_major_version() {
        Version version = new Version();
        version.setNumber("9.12.7");
        ElasticsearchInfo elasticsearchInfo = new ElasticsearchInfo();
        elasticsearchInfo.setVersion(version);

        AbstractElasticBeanRegistrer beanRegistrer = reporter.getBeanRegistrerFromElasticsearchInfo(elasticsearchInfo);

        assertNull(beanRegistrer);
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

        /*
        final HttpHeaders clientRequestHeaders= new HttpHeaders();
        clientRequestHeaders.add("Host", "localhost:8082");
        clientRequestHeaders.add("Connection", "keep-alive");
        clientRequestHeaders.add("X-gravitee-api-key","e14cfcb8-188d-4cb9-ad06-002aea5aab12");
        clientRequestHeaders.add("User-Agent","Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_4) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36");
        clientRequestHeaders.add("Accept-Encoding","gzip");
        clientRequestHeaders.add("Accept-Encoding","deflate");
        clientRequestHeaders.add("Accept-Encoding","sdch");
        clientRequestHeaders.add("Accept-Encoding","br");
        clientRequestHeaders.add("X-Gravitee-Transaction-Id","ba571368-f5e6-48b7-9713-68f5e698b761");
        requestMetrics.setClientRequestHeaders(clientRequestHeaders);

        final HttpHeaders clientResponseHeaders= new HttpHeaders();
        clientResponseHeaders.add("X-Gravitee-Transaction-Id","ba571368-f5e6-48b7-9713-68f5e698b761");
        clientResponseHeaders.add("Content-Length", "700");
        clientResponseHeaders.add("Content-Type", "application/json");
        clientResponseHeaders.add("Date", "Fri, 09 Jun 2017 17:32:15 GMT");
        requestMetrics.setClientResponseHeaders(clientResponseHeaders);

        final HttpHeaders proxyRequestHeaders= new HttpHeaders();
        proxyRequestHeaders.add("Host", "localhost:8082");
        proxyRequestHeaders.add("Connection", "keep-alive");
        proxyRequestHeaders.add("X-gravitee-api-key","e14cfcb8-188d-4cb9-ad06-002aea5aab12");
        proxyRequestHeaders.add("User-Agent","Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_4) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36");
        proxyRequestHeaders.add("Accept-Encoding","gzip");
        proxyRequestHeaders.add("Accept-Encoding","deflate");
        proxyRequestHeaders.add("Accept-Encoding","sdch");
        proxyRequestHeaders.add("Accept-Encoding","br");
        proxyRequestHeaders.add("X-Gravitee-Transaction-Id","ba571368-f5e6-48b7-9713-68f5e698b761");
        requestMetrics.setProxyRequestHeaders(proxyRequestHeaders);

        final HttpHeaders proxyResponseHeaders= new HttpHeaders();
        proxyResponseHeaders.add("Content-Length", "700");
        proxyResponseHeaders.add("Content-Type", "application/json");
        proxyResponseHeaders.add("Date", "Fri, 09 Jun 2017 17:32:15 GMT");
        requestMetrics.setProxyResponseHeaders(proxyResponseHeaders);
        */

        return requestMetrics;
    }
}
