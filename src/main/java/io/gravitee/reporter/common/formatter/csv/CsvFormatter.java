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

import io.gravitee.reporter.api.Reportable;
import io.gravitee.reporter.api.health.EndpointStatus;
import io.gravitee.reporter.api.http.Metrics;
import io.gravitee.reporter.api.log.Log;
import io.gravitee.reporter.api.monitor.Monitor;
import io.gravitee.reporter.api.v4.log.MessageLog;
import io.gravitee.reporter.api.v4.metric.EventMetrics;
import io.gravitee.reporter.api.v4.metric.MessageMetrics;
import io.gravitee.reporter.common.formatter.AbstractFormatter;
import io.gravitee.reporter.common.formatter.csv.v4.*;
import io.gravitee.reporter.common.formatter.csv.v4.LogFormatter;
import io.gravitee.reporter.common.formatter.csv.v4.MetricsFormatter;
import io.vertx.core.buffer.Buffer;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class CsvFormatter<T extends Reportable> extends AbstractFormatter<T> {

  private static final EndpointStatusFormatter ENDPOINT_STATUS_FORMATTER =
    new EndpointStatusFormatter();
  private static final io.gravitee.reporter.common.formatter.csv.LogFormatter LOG_FORMATTER =
    new io.gravitee.reporter.common.formatter.csv.LogFormatter();
  private static final io.gravitee.reporter.common.formatter.csv.MetricsFormatter METRICS_FORMATTER =
    new io.gravitee.reporter.common.formatter.csv.MetricsFormatter();
  private static final MonitorFormatter MONITOR_FORMATTER =
    new MonitorFormatter();

  private static final LogFormatter LOG_V4_FORMATTER = new LogFormatter();
  private static final MessageLogFormatter MESSAGE_LOG_FORMATTER =
    new MessageLogFormatter();
  private static final MetricsFormatter METRICS_V4_FORMATTER =
    new MetricsFormatter();
  private static final MessageMetricsFormatter MESSAGE_METRICS_FORMATTER =
    new MessageMetricsFormatter();
  private static final EventMetricsFormatter EVENT_METRICS_FORMATTER =
    new EventMetricsFormatter();

  @Override
  public Buffer format0(T reportable) {
    if (reportable instanceof Metrics metrics) {
      return METRICS_FORMATTER.format(metrics);
    } else if (reportable instanceof Log log) {
      return LOG_FORMATTER.format(log);
    } else if (reportable instanceof EndpointStatus endpointStatus) {
      return ENDPOINT_STATUS_FORMATTER.format(endpointStatus);
    } else if (reportable instanceof Monitor monitor) {
      return MONITOR_FORMATTER.format(monitor);
    }

    if (
      reportable instanceof io.gravitee.reporter.api.v4.metric.Metrics metrics
    ) {
      return METRICS_V4_FORMATTER.format(metrics);
    } else if (reportable instanceof MessageMetrics metrics) {
      return MESSAGE_METRICS_FORMATTER.format(metrics);
    } else if (reportable instanceof io.gravitee.reporter.api.v4.log.Log log) {
      return LOG_V4_FORMATTER.format(log);
    } else if (reportable instanceof MessageLog log) {
      return MESSAGE_LOG_FORMATTER.format(log);
    } else if (reportable instanceof EventMetrics metrics) {
      return EVENT_METRICS_FORMATTER.format(metrics);
    }

    return null;
  }
}
