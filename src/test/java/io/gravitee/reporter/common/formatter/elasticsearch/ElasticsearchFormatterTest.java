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
package io.gravitee.reporter.common.formatter.elasticsearch;

import static io.gravitee.reporter.common.formatter.Mappers.JSON;
import static io.gravitee.reporter.common.formatter.Mappers.JSON_LINES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

import io.gravitee.reporter.api.monitor.Monitor;
import io.gravitee.reporter.common.formatter.AbstractFormatterTest;
import io.gravitee.reporter.common.formatter.Type;
import java.io.IOException;
import java.time.ZoneId;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
class ElasticsearchFormatterTest extends AbstractFormatterTest {

  @Override
  protected Type type() {
    return Type.ELASTICSEARCH;
  }

  @CsvSource(
    {
      "log, log.Log, log.json, elasticsearch/log.json",
      "metrics, http.Metrics, metrics.json, elasticsearch/metrics.json",
      "endpoint status, health.EndpointStatus, endpoint-status.json, elasticsearch/endpoint-status.json",
      "v4-log, v4.log.Log, v4/log.json, elasticsearch/v4/log.json",
      "v4 metrics, v4.metric.Metrics, v4/metrics.json, elasticsearch/v4/metrics.json",
      "v4 metrics with invalid remote address, v4.metric.Metrics, v4/metrics-with-invalid-remote-address.json, elasticsearch/v4/metrics-with-invalid-remote-address.json",
      "message metrics, v4.metric.MessageMetrics, v4/message-metrics.json, elasticsearch/v4/message-metrics.json",
      "message log, v4.log.MessageLog, v4/message-log.json, elasticsearch/v4/message-log.json",
      "v4 metrics with additional, v4.metric.Metrics, v4/metrics-with-additional.json, elasticsearch/v4/metrics-with-additional.json",
      "event metrics, v4.metric.EventMetrics, v4/event-metrics.json, elasticsearch/v4/event-metrics.json",
    }
  )
  @ParameterizedTest(name = "{0}")
  @SuppressWarnings({ "unused", "ResultOfMethodCallIgnored" })
  void should_format(
    String testName,
    String className,
    String input,
    String output
  ) throws IOException {
    var utcZone = ZoneId.of("UTC");

    try (var javaTime = mockStatic(ZoneId.class)) {
      javaTime.when(ZoneId::systemDefault).thenReturn(utcZone);

      resetFormatter();

      var given = readGiven(input, className);
      var expected = readExpected(output);

      assertThat(JSON.readTree(formatter.format(given).getBytes()))
        .usingRecursiveComparison()
        .isEqualTo(JSON.readTree(expected));
    }
  }

  @CsvSource(
    {
      "log, log.Log, log.json, elasticsearch/log.jsonl",
      "metrics, http.Metrics, metrics.json, elasticsearch/metrics.jsonl",
      "endpoint status, health.EndpointStatus, endpoint-status.json, elasticsearch/endpoint-status.jsonl",
      "v4 log, v4.log.Log, v4/log.json, elasticsearch/v4/log.jsonl",
      "v4 metrics, v4.metric.Metrics, v4/metrics.json, elasticsearch/v4/metrics.jsonl",
      "v4 metrics with invalid remote address, v4.metric.Metrics, v4/metrics-with-invalid-remote-address.json, elasticsearch/v4/metrics-with-invalid-remote-address.jsonl",
      "message metrics, v4.metric.MessageMetrics, v4/message-metrics.json, elasticsearch/v4/message-metrics.jsonl",
      "message log, v4.log.MessageLog, v4/message-log.json, elasticsearch/v4/message-log.jsonl",
      "event metrics, v4.metric.EventMetrics, v4/event-metrics.json, elasticsearch/v4/event-metrics.jsonl",
    }
  )
  @ParameterizedTest(name = "{0}")
  @SuppressWarnings({ "unused", "ResultOfMethodCallIgnored" })
  void should_format_with_options(
    String testName,
    String className,
    String input,
    String output
  ) throws IOException {
    var utcZone = ZoneId.of("UTC");

    try (var javaTime = mockStatic(ZoneId.class)) {
      javaTime.when(ZoneId::systemDefault).thenReturn(utcZone);

      resetFormatter();

      Object indexName = "gravitee-" + className + "-2023.08.28";
      Object pipeline = "my-pipeline";

      var options = Map.of("index", indexName, "pipeline", pipeline);
      var given = readGiven(input, className);
      var expected = readExpected(output);

      assertThat(
        JSON_LINES.readLines(formatter.format(given, options).getBytes())
      )
        .usingRecursiveComparison()
        .isEqualTo(JSON_LINES.readLines(expected));
    }
  }

  @Test
  @SuppressWarnings("ResultOfMethodCallIgnored")
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
  @SuppressWarnings("ResultOfMethodCallIgnored")
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

      assertThat(
        JSON_LINES.readLines(formatter.format(given, options).getBytes())
      )
        .usingRecursiveComparison()
        .ignoringFields("_children._id._value") // Ignored since it is randomly generated
        .ignoringFields("_children.hostname._value") // Ignored since it is statically initialized
        .isEqualTo(JSON_LINES.readLines(expected));
    }
  }
}
