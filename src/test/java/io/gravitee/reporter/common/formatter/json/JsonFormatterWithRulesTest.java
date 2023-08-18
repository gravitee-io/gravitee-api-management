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
package io.gravitee.reporter.common.formatter.json;

import static io.gravitee.reporter.common.formatter.Mappers.JSON;
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.reporter.api.health.EndpointStatus;
import io.gravitee.reporter.api.http.Metrics;
import io.gravitee.reporter.api.log.Log;
import io.gravitee.reporter.api.monitor.Monitor;
import io.gravitee.reporter.common.MetricsType;
import io.gravitee.reporter.common.formatter.AbstractFormatterTest;
import io.gravitee.reporter.common.formatter.Type;
import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
class JsonFormatterWithRulesTest extends AbstractFormatterTest {

  @Override
  protected Type type() {
    return Type.JSON;
  }

  @Test
  void should_format_logs() throws IOException {
    var given = readGiven("log.json", Log.class);
    var expected = readExpected("json/log-with-rules.json");

    resetFormatter(MetricsType.REQUEST_LOG);

    assertThat(JSON.readTree(formatter.format(given).getBytes()))
      .usingRecursiveComparison()
      .isEqualTo(JSON.readTree(expected));
  }

  @Test
  void should_format_metrics() throws IOException {
    var given = readGiven("metrics.json", Metrics.class);
    var expected = readExpected("json/metrics-with-rules.json");

    resetFormatter(MetricsType.REQUEST);

    assertThat(JSON.readTree(formatter.format(given).getBytes()))
      .usingRecursiveComparison()
      .isEqualTo(JSON.readTree(expected));
  }

  @Test
  void should_format_monitor() throws IOException {
    var given = readGiven("monitor.json", Monitor.class);
    var expected = readExpected("json/monitor-with-rules.json");

    resetFormatter(MetricsType.NODE_MONITOR);

    assertThat(JSON.readTree(formatter.format(given).getBytes()))
      .usingRecursiveComparison()
      .isEqualTo(JSON.readTree(expected));
  }

  @Test
  void should_format_endpoint_status() throws IOException {
    var given = readGiven("endpoint-status.json", EndpointStatus.class);
    var expected = readExpected("json/endpoint-status-with-rules.json");

    resetFormatter(MetricsType.HEALTH_CHECK);

    assertThat(JSON.readTree(formatter.format(given).getBytes()))
      .usingRecursiveComparison()
      .isEqualTo(JSON.readTree(expected));
  }
}
