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
package io.gravitee.reporter.common.formatter.csv.v4;

import io.gravitee.reporter.api.v4.metric.MessageMetrics;
import io.gravitee.reporter.common.formatter.csv.SingleValueFormatter;
import io.vertx.core.buffer.Buffer;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class MessageMetricsFormatter
  extends SingleValueFormatter<MessageMetrics> {

  @Override
  protected Buffer format0(MessageMetrics metrics) {
    final Map<String, String> customMetrics = metrics.getCustomMetrics() == null
      ? Map.of()
      : metrics.getCustomMetrics();

    final Buffer buffer = Buffer.buffer();

    appendString(buffer, metrics.getCorrelationId());
    appendString(buffer, metrics.getParentCorrelationId());
    appendString(buffer, metrics.getRequestId());
    appendLong(buffer, metrics.timestamp().toEpochMilli());
    appendString(buffer, metrics.getApiId());
    appendString(buffer, metrics.getApiName());
    appendString(
      buffer,
      metrics.getOperation() != null ? metrics.getOperation().name() : null
    );
    appendString(
      buffer,
      metrics.getConnectorType() != null
        ? metrics.getConnectorType().name()
        : null
    );
    appendString(buffer, metrics.getConnectorId());
    appendLong(buffer, metrics.getContentLength());
    appendLong(buffer, metrics.getCount());
    appendLong(buffer, metrics.getErrorCount());
    appendLong(buffer, metrics.getCountIncrement());
    appendLong(buffer, metrics.getErrorCountIncrement());
    appendBoolean(buffer, metrics.isError());
    appendLong(buffer, metrics.getGatewayLatencyMs());

    for (Iterator<String> i = customMetrics.keySet().iterator(); i.hasNext();) {
      appendString(buffer, customMetrics.get(i.next()), true, !i.hasNext());
    }

    return buffer;
  }
}
