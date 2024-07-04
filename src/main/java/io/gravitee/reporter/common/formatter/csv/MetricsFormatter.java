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

import io.gravitee.reporter.api.http.Metrics;
import io.vertx.core.buffer.Buffer;
import java.util.Iterator;
import java.util.Map;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public final class MetricsFormatter extends SingleValueFormatter<Metrics> {

  public Buffer format0(Metrics metrics) {
    final Map<String, String> customMetrics = metrics.getCustomMetrics();

    final Buffer buffer = Buffer.buffer();

    appendString(buffer, metrics.getTransactionId());
    appendString(buffer, metrics.getRequestId());
    appendLong(buffer, metrics.timestamp().toEpochMilli());
    appendString(buffer, metrics.getRemoteAddress());
    appendString(buffer, metrics.getLocalAddress());
    appendString(buffer, metrics.getApi());
    appendString(buffer, metrics.getApiName());
    appendString(buffer, metrics.getApplication());
    appendString(buffer, metrics.getPlan());
    appendString(buffer, metrics.getSubscription());
    appendString(buffer, metrics.getUser());
    appendString(buffer, metrics.getTenant());
    appendString(buffer, metrics.getUri());
    appendString(buffer, metrics.getPath());
    appendString(buffer, metrics.getMappedPath());
    appendString(buffer, metrics.getHttpMethod().name());
    appendInt(buffer, metrics.getStatus());
    appendString(buffer, metrics.getEndpoint());
    appendString(buffer, metrics.getErrorKey());
    appendString(buffer, metrics.getMessage(), true, false);
    appendString(buffer, metrics.getUserAgent(), true, false);
    appendString(buffer, metrics.getHost());
    appendLong(buffer, metrics.getRequestContentLength());
    appendLong(buffer, metrics.getResponseContentLength());
    appendLong(buffer, metrics.getApiResponseTimeMs());
    appendLong(buffer, metrics.getProxyResponseTimeMs());
    appendLong(buffer, metrics.getProxyLatencyMs());
    appendString(
      buffer,
      metrics.getSecurityType() != null
        ? metrics.getSecurityType().name()
        : null
    );
    appendString(
      buffer,
      metrics.getSecurityToken() != null ? metrics.getApi() : null,
      customMetrics.isEmpty()
    );

    if (!customMetrics.isEmpty()) {
      for (
        Iterator<String> i = customMetrics.keySet().iterator();
        i.hasNext();
      ) {
        appendString(buffer, customMetrics.get(i.next()), true, !i.hasNext());
      }
    }

    return buffer;
  }
}
