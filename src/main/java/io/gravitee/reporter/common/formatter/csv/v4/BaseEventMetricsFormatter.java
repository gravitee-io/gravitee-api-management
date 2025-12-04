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

import io.gravitee.reporter.api.v4.metric.event.BaseEventMetrics;
import io.gravitee.reporter.common.formatter.csv.SingleValueFormatter;
import io.vertx.core.buffer.Buffer;

/**
 * @author Anthony CALLAERT (anthony.callaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class BaseEventMetricsFormatter<T extends BaseEventMetrics>
  extends SingleValueFormatter<T> {

  @Override
  protected Buffer format0(T data) {
    final Buffer buffer = Buffer.buffer();
    appendLong(buffer, data.getTimestamp());

    // Append base dimensions
    appendString(buffer, data.getGatewayId());
    appendString(buffer, data.getOrganizationId());
    appendString(buffer, data.getEnvironmentId());
    appendString(buffer, data.getApiId());
    appendString(buffer, data.getPlanId());
    appendString(buffer, data.getApplicationId());
    return buffer;
  }

  protected static long getValue(Number number) {
    return number != null ? number.longValue() : 0;
  }

  protected void appendLong(Buffer buffer, Number value) {
    super.appendLong(buffer, getValue(value));
  }
}
