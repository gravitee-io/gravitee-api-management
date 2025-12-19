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
import io.gravitee.reporter.api.monitor.Monitor;
import io.gravitee.reporter.api.v4.log.MessageLog;
import io.gravitee.reporter.api.v4.metric.EventMetrics;
import io.gravitee.reporter.api.v4.metric.MessageMetrics;
import io.gravitee.reporter.api.v4.metric.event.ApiEventMetrics;
import io.gravitee.reporter.api.v4.metric.event.ApplicationEventMetrics;
import io.gravitee.reporter.api.v4.metric.event.OperationEventMetrics;
import io.gravitee.reporter.api.v4.metric.event.TopicEventMetrics;
import io.gravitee.reporter.common.formatter.AbstractFormatter;
import io.gravitee.reporter.common.formatter.csv.v4.*;
import io.gravitee.reporter.common.formatter.csv.v4.LogFormatter;
import io.gravitee.reporter.common.formatter.csv.v4.MetricsFormatter;
import io.vertx.core.buffer.Buffer;
import java.util.Map;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class CsvFormatter<T extends Reportable> extends AbstractFormatter<T> {

  private static final Map<
    Class<? extends Reportable>,
    AbstractFormatter<? extends Reportable>
  > FORMATTERS = new java.util.HashMap<>();

  static {
    FORMATTERS.put(EndpointStatus.class, new EndpointStatusFormatter());
    FORMATTERS.put(
      io.gravitee.reporter.api.log.Log.class,
      new io.gravitee.reporter.common.formatter.csv.LogFormatter()
    );
    FORMATTERS.put(
      io.gravitee.reporter.api.http.Metrics.class,
      new io.gravitee.reporter.common.formatter.csv.MetricsFormatter()
    );
    FORMATTERS.put(Monitor.class, new MonitorFormatter());
    FORMATTERS.put(
      io.gravitee.reporter.api.v4.log.Log.class,
      new LogFormatter()
    );
    FORMATTERS.put(MessageLog.class, new MessageLogFormatter());
    FORMATTERS.put(
      io.gravitee.reporter.api.v4.metric.Metrics.class,
      new MetricsFormatter()
    );
    FORMATTERS.put(MessageMetrics.class, new MessageMetricsFormatter());
    FORMATTERS.put(EventMetrics.class, new EventMetricsFormatter());
    FORMATTERS.put(
      OperationEventMetrics.class,
      new OperationEventMetricsFormatter()
    );
    FORMATTERS.put(TopicEventMetrics.class, new TopicEventMetricsFormatter());
    FORMATTERS.put(ApiEventMetrics.class, new ApiEventMetricsFormatter());
    FORMATTERS.put(
      ApplicationEventMetrics.class,
      new ApplicationEventMetricsFormatter()
    );
  }

  @Override
  public Buffer format0(T reportable) {
    AbstractFormatter<T> formatter = (AbstractFormatter<T>) FORMATTERS.get(
      reportable.getClass()
    );
    if (formatter != null) {
      return formatter.format(reportable);
    }
    return null;
  }
}
