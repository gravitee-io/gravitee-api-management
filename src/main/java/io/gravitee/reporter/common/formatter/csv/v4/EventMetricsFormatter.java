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

import io.gravitee.reporter.api.v4.metric.EventMetrics;
import io.gravitee.reporter.common.formatter.csv.SingleValueFormatter;
import io.vertx.core.buffer.Buffer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EventMetricsFormatter extends SingleValueFormatter<EventMetrics> {

  @Override
  protected Buffer format0(EventMetrics data) {
    final Buffer buffer = Buffer.buffer();
    appendLong(buffer, data.getTimestamp());

    // Append base dimensions
    appendString(buffer, data.getGatewayId());
    appendString(buffer, data.getOrganizationId());
    appendString(buffer, data.getEnvironmentId());
    appendString(buffer, data.getApiId());
    appendString(buffer, data.getPlanId());
    appendString(buffer, data.getApplicationId());
    appendString(buffer, data.getTopic());

    //Append metrics
    Number downstreamPublishMessagesTotal =
      data.getDownstreamPublishMessagesTotal();
    appendLong(buffer, getValue(downstreamPublishMessagesTotal));
    Number downstreamPublishMessageBytes =
      data.getDownstreamPublishMessageBytes();
    appendLong(buffer, getValue(downstreamPublishMessageBytes));
    Number upstreamPublishMessagesTotal =
      data.getUpstreamPublishMessagesTotal();
    appendLong(buffer, getValue(upstreamPublishMessagesTotal));
    Number upstreamPublishMessageBytes = data.getUpstreamPublishMessageBytes();
    appendLong(buffer, getValue(upstreamPublishMessageBytes));
    Number downstreamSubscribeMessagesTotal =
      data.getDownstreamSubscribeMessagesTotal();
    appendLong(buffer, getValue(downstreamSubscribeMessagesTotal));
    Number downstreamSubscribeMessageBytes =
      data.getDownstreamSubscribeMessageBytes();
    appendLong(buffer, getValue(downstreamSubscribeMessageBytes));
    Number upstreamSubscribeMessagesTotal =
      data.getUpstreamSubscribeMessagesTotal();
    appendLong(buffer, getValue(upstreamSubscribeMessagesTotal));
    Number upstreamSubscribeMessageBytes =
      data.getUpstreamSubscribeMessageBytes();
    appendLong(buffer, getValue(upstreamSubscribeMessageBytes));
    Number downstreamActiveConnections = data.getDownstreamActiveConnections();
    appendLong(buffer, getValue(downstreamActiveConnections));
    Number upstreamActiveConnections = data.getUpstreamActiveConnections();
    appendLong(buffer, getValue(upstreamActiveConnections));
    Number upstreamAuthenticatedConnections =
      data.getUpstreamAuthenticatedConnections();
    appendLong(buffer, getValue(upstreamAuthenticatedConnections));
    Number downstreamAuthenticatedConnections =
      data.getDownstreamAuthenticatedConnections();
    appendLong(buffer, getValue(downstreamAuthenticatedConnections));
    Number downstreamAuthenticationFailuresTotal =
      data.getDownstreamAuthenticationFailuresTotal();
    appendLong(buffer, getValue(downstreamAuthenticationFailuresTotal));
    Number upstreamAuthenticationFailuresTotal =
      data.getUpstreamAuthenticationFailuresTotal();
    appendLong(buffer, getValue(upstreamAuthenticationFailuresTotal));
    Number downstreamAuthorizationSuccessesTotal =
      data.getDownstreamAuthorizationSuccessesTotal();
    appendLong(buffer, getValue(downstreamAuthorizationSuccessesTotal));
    Number upstreamAuthorizationSuccessesTotal =
      data.getUpstreamAuthorizationSuccessesTotal();
    appendLong(buffer, getValue(upstreamAuthorizationSuccessesTotal));

    return buffer;
  }

  private static long getValue(Number number) {
    return number != null ? number.longValue() : 0;
  }
}
