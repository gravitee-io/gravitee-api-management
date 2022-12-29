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
package io.gravitee.reporter.elasticsearch.indexer;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.reporter.api.http.Metrics;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import java.time.Instant;
import java.util.Map;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
public class AbstractIndexerTest {

    @InjectMocks
    private AbstractIndexer indexer = new TestIndexer();

    @Test
    public void shouldIndexReportable_validRemoteAddress_ipv4() {
        Metrics metrics = Metrics.on(Instant.now().toEpochMilli()).build();
        metrics.setRemoteAddress("72.16.254.1");
        Buffer buffer = indexer.transform(metrics);
        JsonObject data = buffer.toJsonObject();
        Map<String, Object> metricsMap = (Map<String, Object>) data.getMap().get("metrics");
        assertThat(metricsMap).containsEntry("remoteAddress", "72.16.254.1");
    }

    @Test
    public void shouldIndexReportable_validRemoteAddress_ipv6() {
        Metrics metrics = Metrics.on(Instant.now().toEpochMilli()).build();
        metrics.setRemoteAddress("2001:db8:0:1234:0:567:8:1");
        Buffer buffer = indexer.transform(metrics);
        JsonObject data = buffer.toJsonObject();
        Map<String, Object> metricsMap = (Map<String, Object>) data.getMap().get("metrics");
        assertThat(metricsMap).containsEntry("remoteAddress", "2001:db8:0:1234:0:567:8:1");
    }

    @Test
    public void shouldIndexReportable_invalidRemoteAddress() {
        Metrics metrics = Metrics.on(Instant.now().toEpochMilli()).build();
        metrics.setRemoteAddress("remoteAddress");
        Buffer buffer = indexer.transform(metrics);
        JsonObject data = buffer.toJsonObject();
        Map<String, Object> metricsMap = (Map<String, Object>) data.getMap().get("metrics");
        assertThat(metricsMap).containsEntry("remoteAddress", "0.0.0.0");
    }

    @Test
    public void shouldIndexReportableMetricsV4_validRemoteAddress_ipv4() {
        io.gravitee.reporter.api.v4.metric.Metrics metrics = io.gravitee.reporter.api.v4.metric.Metrics
            .builder()
            .timestamp(Instant.now().toEpochMilli())
            .build();
        metrics.setRemoteAddress("72.16.254.1");
        Buffer buffer = indexer.transform(metrics);
        JsonObject data = buffer.toJsonObject();
        Map<String, Object> metricsMap = (Map<String, Object>) data.getMap().get("metrics");
        assertThat(metricsMap).containsEntry("remoteAddress", "72.16.254.1");
    }

    @Test
    public void shouldIndexReportableMetricsV4_validRemoteAddress_ipv6() {
        io.gravitee.reporter.api.v4.metric.Metrics metrics = io.gravitee.reporter.api.v4.metric.Metrics
            .builder()
            .timestamp(Instant.now().toEpochMilli())
            .build();
        metrics.setRemoteAddress("2001:db8:0:1234:0:567:8:1");
        Buffer buffer = indexer.transform(metrics);
        JsonObject data = buffer.toJsonObject();
        Map<String, Object> metricsMap = (Map<String, Object>) data.getMap().get("metrics");
        assertThat(metricsMap).containsEntry("remoteAddress", "2001:db8:0:1234:0:567:8:1");
    }

    @Test
    public void shouldIndexReportableMetricsV4_invalidRemoteAddress() {
        io.gravitee.reporter.api.v4.metric.Metrics metrics = io.gravitee.reporter.api.v4.metric.Metrics
            .builder()
            .timestamp(Instant.now().toEpochMilli())
            .build();
        metrics.setRemoteAddress("remoteAddress");
        Buffer buffer = indexer.transform(metrics);
        JsonObject data = buffer.toJsonObject();
        Map<String, Object> metricsMap = (Map<String, Object>) data.getMap().get("metrics");
        assertThat(metricsMap).containsEntry("remoteAddress", "0.0.0.0");
    }

    @Test
    public void shouldTransformReportableMetricsV4() {
        io.gravitee.reporter.api.v4.metric.Metrics metrics = io.gravitee.reporter.api.v4.metric.Metrics
            .builder()
            .timestamp(Instant.now().toEpochMilli())
            .remoteAddress("2001:db8:0:1234:0:567:8:1")
            .endpointResponseTimeMs(1)
            .gatewayResponseTimeMs(2)
            .gatewayLatencyMs(3)
            .requestContentLength(4)
            .responseContentLength(5)
            .build();
        Buffer buffer = indexer.transform(metrics);
        JsonObject data = buffer.toJsonObject();

        assertThat(data.getMap()).containsKey("index");
        assertThat(data.getMap()).containsKey(AbstractIndexer.Fields.SPECIAL_TIMESTAMP);
        assertThat(data.getMap()).containsKey(AbstractIndexer.Fields.GATEWAY);
        assertThat(data.getMap()).containsKey("metrics");
        assertThat(data.getMap()).containsEntry("endpointResponseTimeMs", 1);
        assertThat(data.getMap()).containsEntry("gatewayResponseTimeMs", 2);
        assertThat(data.getMap()).containsEntry("gatewayLatencyMs", 3);
        assertThat(data.getMap()).containsEntry("requestContentLength", 4);
        assertThat(data.getMap()).containsEntry("responseContentLength", 5);
    }

    @Test
    public void shouldTransformReportableLogV4() {
        io.gravitee.reporter.api.v4.log.Log log = io.gravitee.reporter.api.v4.log.Log
            .builder()
            .timestamp(Instant.now().toEpochMilli())
            .build();
        Buffer buffer = indexer.transform(log);
        JsonObject data = buffer.toJsonObject();

        assertThat(data.getMap()).containsKey("index");
        assertThat(data.getMap()).containsKey(AbstractIndexer.Fields.SPECIAL_TIMESTAMP);
        assertThat(data.getMap()).containsKey("log");
    }

    @Test
    public void shouldTransformReportableMessageMetricsV4() {
        io.gravitee.reporter.api.v4.metric.MessageMetrics messageMetrics = io.gravitee.reporter.api.v4.metric.MessageMetrics
            .builder()
            .timestamp(Instant.now().toEpochMilli())
            .count(1)
            .errorsCount(2)
            .contentLength(3)
            .gatewayLatencyMs(4)
            .build();
        Buffer buffer = indexer.transform(messageMetrics);
        JsonObject data = buffer.toJsonObject();

        assertThat(data.getMap()).containsKey("index");
        assertThat(data.getMap()).containsKey(AbstractIndexer.Fields.SPECIAL_TIMESTAMP);
        assertThat(data.getMap()).containsKey(AbstractIndexer.Fields.GATEWAY);
        assertThat(data.getMap()).containsKey("metrics");

        assertThat(data.getMap()).containsEntry("count", 1);
        assertThat(data.getMap()).containsEntry("errorsCount", 2);
        assertThat(data.getMap()).containsEntry("contentLength", 3);
        assertThat(data.getMap()).containsEntry("gatewayLatencyMs", 4);
    }

    @Test
    public void shouldTransformReportableMessageLogV4() {
        io.gravitee.reporter.api.v4.log.MessageLog messageLog = io.gravitee.reporter.api.v4.log.MessageLog
            .builder()
            .timestamp(Instant.now().toEpochMilli())
            .build();
        Buffer buffer = indexer.transform(messageLog);
        JsonObject data = buffer.toJsonObject();

        assertThat(data.getMap()).containsKey("index");
        assertThat(data.getMap()).containsKey(AbstractIndexer.Fields.SPECIAL_TIMESTAMP);
        assertThat(data.getMap()).containsKey("log");
    }

    private static class TestIndexer extends AbstractIndexer {

        @Override
        protected Buffer generateData(String templateName, Map<String, Object> data) {
            return JsonObject.mapFrom(data).toBuffer();
        }
    }
}
