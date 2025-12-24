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
package io.gravitee.reporter.common.formatter.csv;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.reporter.common.formatter.AbstractFormatterTest;
import io.gravitee.reporter.common.formatter.Type;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CsvFormatterTest extends AbstractFormatterTest {

  @Override
  protected Type type() {
    return Type.CSV;
  }

  @CsvSource(
    {
      "log, log.Log, log.json, csv/log.csv",
      "metrics, http.Metrics, metrics.json, csv/metrics.csv",
      "metrics with additional, http.Metrics, metrics-with-additional.json, csv/metrics-with-additional.csv",
      "metrics with invalid remote address, http.Metrics, metrics-with-invalid-remote-address.json, csv/metrics-with-invalid-remote-address.csv",
      "endpoint status, health.EndpointStatus, endpoint-status.json, csv/endpoint-status.csv",
      "monitor, monitor.Monitor, monitor.json, csv/monitor.csv",
      "v4 log, v4.log.Log, v4/log.json, csv/v4/log.csv",
      "v4 metrics, v4.metric.Metrics, v4/metrics.json, csv/v4/metrics.csv",
      "v4 metrics with additional, v4.metric.Metrics, v4/metrics-with-additional.json, csv/v4/metrics-with-additional.csv",
      "v4 metrics with invalid remote address, v4.metric.Metrics, v4/metrics-with-invalid-remote-address.json, csv/v4/metrics-with-invalid-remote-address.csv",
      "message metrics, v4.metric.MessageMetrics, v4/message-metrics.json, csv/v4/message-metrics.csv",
      "message metrics with additional, v4.metric.MessageMetrics, v4/message-metrics-with-additional.json, csv/v4/message-metrics-with-additional.csv",
      "message log, v4.log.MessageLog, v4/message-log.json, csv/v4/message-log.csv",
      "api event metrics, v4.metric.event.ApiEventMetrics, v4/api-event-metrics.json, csv/v4/api-event-metrics.csv",
      "application event metrics, v4.metric.event.ApplicationEventMetrics, v4/application-event-metrics.json, csv/v4/application-event-metrics.csv",
      "topic event metrics, v4.metric.event.TopicEventMetrics, v4/topic-event-metrics.json, csv/v4/topic-event-metrics.csv",
      "operation event metrics, v4.metric.event.OperationEventMetrics, v4/operation-event-metrics.json, csv/v4/operation-event-metrics.csv",
    }
  )
  @ParameterizedTest(name = "{0}")
  @SuppressWarnings("unused")
  void should_format(
    String testName,
    String className,
    String input,
    String output
  ) {
    var given = readGiven(input, className);
    var expected = readExpected(output);
    assertThat(formatter.format(given)).hasToString(expected);
  }
}
