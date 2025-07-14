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
package io.gravitee.reporter.common.formatter.msgpack;

import static io.gravitee.reporter.common.formatter.Mappers.JSON;
import static io.gravitee.reporter.common.formatter.Mappers.MESSAGE_PACK;
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.reporter.common.formatter.AbstractFormatterTest;
import io.gravitee.reporter.common.formatter.Type;
import java.io.IOException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
class MsgPackFormatterTest extends AbstractFormatterTest {

  @Override
  protected Type type() {
    return Type.MESSAGE_PACK;
  }

  @CsvSource(
    {
      "log, log.Log, log.json, json/log.json",
      "metrics, http.Metrics, metrics.json, json/metrics.json",
      "endpoint status, health.EndpointStatus, endpoint-status.json, json/endpoint-status.json",
      "monitor, monitor.Monitor, monitor.json, json/monitor.json",
      "v4 log, v4.log.Log, v4/log.json, json/v4/log.json",
      "v4 metrics, v4.metric.Metrics, v4/metrics.json, json/v4/metrics.json",
      "message metrics, v4.metric.MessageMetrics, v4/message-metrics.json, json/v4/message-metrics.json",
      "message log, v4.log.MessageLog, v4/message-log.json, json/v4/message-log.json",
      "event metrics, v4.metric.EventMetrics, v4/event-metrics.json, json/v4/event-metrics.json",
    }
  )
  @ParameterizedTest(name = "{0}")
  @SuppressWarnings("unused")
  void should_format(
    String testName,
    String className,
    String input,
    String output
  ) throws IOException {
    var given = readGiven(input, className);
    var expected = readExpected(output);
    assertThat(MESSAGE_PACK.readTree(formatter.format(given).getBytes()))
      .usingRecursiveComparison()
      .isEqualTo(JSON.readTree(expected));
  }
}
