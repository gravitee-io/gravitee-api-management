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
package io.gravitee.apim.reporter.common.formatter.elasticsearch;

import static io.gravitee.apim.reporter.common.formatter.Mappers.JSON;
import static io.gravitee.apim.reporter.common.formatter.Mappers.JSON_LINES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gravitee.apim.reporter.common.formatter.AbstractFormatterTest;
import io.gravitee.apim.reporter.common.formatter.Type;
import io.gravitee.reporter.api.monitor.Monitor;
import java.io.IOException;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
class ElasticsearchFormatterTest extends AbstractFormatterTest {

    private static final String GENERATED_ID = "<generated>";

    @Override
    protected Type type() {
        return Type.ELASTICSEARCH;
    }

    @CsvSource(
        {
            "log, log.Log, log.json, elasticsearch/log.json",
            "metrics, http.Metrics, metrics.json, elasticsearch/metrics.json",
            "metrics with additional, http.Metrics, metrics-with-additional.json, elasticsearch/metrics-with-additional.json",
            "endpoint status, health.EndpointStatus, endpoint-status.json, elasticsearch/endpoint-status.json",
            "v4-log, v4.log.Log, v4/log.json, elasticsearch/v4/log.json",
            "v4 metrics, v4.metric.Metrics, v4/metrics.json, elasticsearch/v4/metrics.json",
            "v4 metrics with invalid remote address, v4.metric.Metrics, v4/metrics-with-invalid-remote-address.json, elasticsearch/v4/metrics-with-invalid-remote-address.json",
            "v4 metrics with null transaction id, v4.metric.Metrics, v4/metrics-with-null-transaction-id.json, elasticsearch/v4/metrics-with-null-transaction-id.json",
            "v4 metrics of a message connection opening, v4.metric.Metrics, v4/metrics-message-connection-opening.json, elasticsearch/v4/metrics-message-connection-opening.json",
            "v4 metrics of a message connection ended, v4.metric.Metrics, v4/metrics-message-connection-ended.json, elasticsearch/v4/metrics-message-connection-ended.json",
            "v4 log of a connection opening, v4.log.Log, v4/log-connection-opening.json, elasticsearch/v4/log-connection-opening.json",
            "message metrics, v4.metric.MessageMetrics, v4/message-metrics.json, elasticsearch/v4/message-metrics.json",
            "message metrics with connection dimensions, v4.metric.MessageMetrics, v4/message-metrics-with-connection-dimensions.json, elasticsearch/v4/message-metrics-with-connection-dimensions.json",
            "message log, v4.log.MessageLog, v4/message-log.json, elasticsearch/v4/message-log.json",
            "v4 metrics with warnings, v4.metric.Metrics, v4/metrics-with-warnings.json, elasticsearch/v4/metrics-with-warnings.json",
            "v4 metrics with decisions, v4.metric.Metrics, v4/metrics-with-decisions.json, elasticsearch/v4/metrics-with-decisions.json",
            "v4 metrics with additional, v4.metric.Metrics, v4/metrics-with-additional.json, elasticsearch/v4/metrics-with-additional.json",
            "message metrics with additional, v4.metric.MessageMetrics, v4/message-metrics-with-additional.json, elasticsearch/v4/message-metrics-with-additional.json",
            "message metrics with apostrophe in json additional, v4.metric.MessageMetrics, v4/message-metrics-with-apostrophe-in-json-additional.json, elasticsearch/v4/message-metrics-with-apostrophe-in-json-additional.json",
            "api event metrics, v4.metric.event.ApiEventMetrics, v4/api-event-metrics.json, elasticsearch/v4/api-event-metrics.json",
            "application event metrics, v4.metric.event.ApplicationEventMetrics, v4/application-event-metrics.json, elasticsearch/v4/application-event-metrics.json",
            "topic event metrics, v4.metric.event.TopicEventMetrics, v4/topic-event-metrics.json, elasticsearch/v4/topic-event-metrics.json",
            "topic event metrics with null api, v4.metric.event.TopicEventMetrics, v4/topic-event-metrics-null-api.json, elasticsearch/v4/topic-event-metrics-null-api.json",
            "operation event metrics, v4.metric.event.OperationEventMetrics, v4/operation-event-metrics.json, elasticsearch/v4/operation-event-metrics.json",
            "decision report, v4.report.DecisionReport, v4/decision-report.json, elasticsearch/v4/decision-report.json",
            "decision report with quotes, v4.report.DecisionReport, v4/decision-report-with-quotes.json, elasticsearch/v4/decision-report-with-quotes.json",
        }
    )
    @ParameterizedTest(name = "{0}")
    void should_format(String testName, String className, String input, String output) throws IOException {
        var utcZone = ZoneId.of("UTC");

        try (var javaTime = mockStatic(ZoneId.class)) {
            javaTime.when(ZoneId::systemDefault).thenReturn(utcZone);

            resetFormatter();

            var given = readGiven(input, className);
            var expected = readExpected(output);

            var actual = JSON.readTree(formatter.format(given).getBytes());
            var expectedTree = JSON.readTree(expected);
            acceptGeneratedId((ObjectNode) actual, (ObjectNode) expectedTree);

            assertThat(actual).usingRecursiveComparison().isEqualTo(expectedTree);
        }
    }

    @CsvSource(
        {
            "log, log.Log, log.json, elasticsearch/log.jsonl",
            "metrics, http.Metrics, metrics.json, elasticsearch/metrics.jsonl",
            "metrics with additional, http.Metrics, metrics-with-additional.json, elasticsearch/metrics-with-additional.jsonl",
            "endpoint status, health.EndpointStatus, endpoint-status.json, elasticsearch/endpoint-status.jsonl",
            "v4 log, v4.log.Log, v4/log.json, elasticsearch/v4/log.jsonl",
            "v4 metrics, v4.metric.Metrics, v4/metrics.json, elasticsearch/v4/metrics.jsonl",
            "v4 metrics with invalid remote address, v4.metric.Metrics, v4/metrics-with-invalid-remote-address.json, elasticsearch/v4/metrics-with-invalid-remote-address.jsonl",
            "v4 metrics with null transaction id, v4.metric.Metrics, v4/metrics-with-null-transaction-id.json, elasticsearch/v4/metrics-with-null-transaction-id.jsonl",
            "v4 metrics of a message connection opening, v4.metric.Metrics, v4/metrics-message-connection-opening.json, elasticsearch/v4/metrics-message-connection-opening.jsonl",
            "v4 metrics of a message connection ended, v4.metric.Metrics, v4/metrics-message-connection-ended.json, elasticsearch/v4/metrics-message-connection-ended.jsonl",
            "v4 log of a connection opening, v4.log.Log, v4/log-connection-opening.json, elasticsearch/v4/log-connection-opening.jsonl",
            "message metrics, v4.metric.MessageMetrics, v4/message-metrics.json, elasticsearch/v4/message-metrics.jsonl",
            "message log, v4.log.MessageLog, v4/message-log.json, elasticsearch/v4/message-log.jsonl",
            "api event metrics, v4.metric.event.ApiEventMetrics, v4/api-event-metrics.json, elasticsearch/v4/api-event-metrics.jsonl",
            "decision report, v4.report.DecisionReport, v4/decision-report.json, elasticsearch/v4/decision-report.jsonl",
            "application event metrics, v4.metric.event.ApplicationEventMetrics, v4/application-event-metrics.json, elasticsearch/v4/application-event-metrics.jsonl",
            "topic event metrics, v4.metric.event.TopicEventMetrics, v4/topic-event-metrics.json, elasticsearch/v4/topic-event-metrics.jsonl",
            "operation event metrics, v4.metric.event.OperationEventMetrics, v4/operation-event-metrics.json, elasticsearch/v4/operation-event-metrics.jsonl",
        }
    )
    @ParameterizedTest(name = "{0}")
    void should_format_with_options(String testName, String className, String input, String output) throws IOException {
        var utcZone = ZoneId.of("UTC");

        try (var javaTime = mockStatic(ZoneId.class)) {
            javaTime.when(ZoneId::systemDefault).thenReturn(utcZone);

            resetFormatter();

            Object indexName = "gravitee-" + className + "-2023.08.28";
            Object pipeline = "my-pipeline";

            var options = Map.of("index", indexName, "pipeline", pipeline);
            var given = readGiven(input, className);
            var expected = readExpected(output);

            var actual = JSON_LINES.readLines(formatter.format(given, options).getBytes());
            var expectedLines = JSON_LINES.readLines(expected);
            acceptGeneratedId(action(actual.getFirst()), action(expectedLines.getFirst()));

            assertThat(actual).usingRecursiveComparison().isEqualTo(expectedLines);
        }
    }

    @CsvSource(
        { "message metrics, v4.metric.MessageMetrics, v4/message-metrics.json", "message log, v4.log.MessageLog, v4/message-log.json" }
    )
    @ParameterizedTest(name = "{0}")
    void should_give_each_message_document_an_id_of_its_own(String testName, String className, String input) throws IOException {
        resetFormatter();
        var options = Map.<String, Object>of("index", "gravitee-v4-message-2023.08.28");

        var first = JSON_LINES.readLines(formatter.format(readGiven(input, className), options).getBytes());
        var second = JSON_LINES.readLines(formatter.format(readGiven(input, className), options).getBytes());

        assertThat(first.getFirst().at("/index/_id").asText()).isNotEqualTo(second.getFirst().at("/index/_id").asText());
    }

    /**
     * Message documents get a random id when formatted, which a fixture cannot spell: it says {@value GENERATED_ID}
     * instead, and the actual id only has to be a UUID.
     */
    /** The action line holds a single operation — index or create — whose metadata carries the id. */
    private static ObjectNode action(JsonNode actionLine) {
        return (ObjectNode) actionLine.elements().next();
    }

    private static void acceptGeneratedId(ObjectNode actual, ObjectNode expected) {
        if (GENERATED_ID.equals(expected.path("_id").asText())) {
            assertThat(UUID.fromString(actual.path("_id").asText())).isNotNull();
            actual.put("_id", GENERATED_ID);
        }
    }

    @Test
    void should_format_monitor() throws IOException {
        var utcZone = ZoneId.of("UTC");

        try (var javaTime = mockStatic(ZoneId.class)) {
            javaTime.when(ZoneId::systemDefault).thenReturn(utcZone);

            resetFormatter();

            var given = readGiven("monitor.json", Monitor.class);
            var expected = readExpected("elasticsearch/monitor.json");

            assertThat(JSON.readTree(formatter.format(given).getBytes()))
                .usingRecursiveComparison()
                .ignoringFields("_children._id._value") // Ignored since it is randomly generated
                .ignoringFields("_children.hostname._value") // Ignored since it is statically initialized
                .isEqualTo(JSON.readTree(expected));
        }
    }

    @Test
    void should_format_monitor_with_options() throws IOException {
        var utcZone = ZoneId.of("UTC");

        try (var javaTime = mockStatic(ZoneId.class)) {
            javaTime.when(ZoneId::systemDefault).thenReturn(utcZone);

            resetFormatter();

            Object indexName = "gravitee-monitor-2023.08.28";
            Object pipeline = "my-pipeline";

            var options = Map.of("index", indexName, "pipeline", pipeline);
            var given = readGiven("monitor.json", Monitor.class);
            var expected = readExpected("elasticsearch/monitor.jsonl");

            assertThat(JSON_LINES.readLines(formatter.format(given, options).getBytes()))
                .usingRecursiveComparison()
                .ignoringFields("_children._id._value") // Ignored since it is randomly generated
                .ignoringFields("_children.hostname._value") // Ignored since it is statically initialized
                .isEqualTo(JSON_LINES.readLines(expected));
        }
    }
}
