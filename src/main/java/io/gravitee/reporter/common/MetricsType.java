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
package io.gravitee.reporter.common;

import io.gravitee.reporter.api.Reportable;
import io.gravitee.reporter.api.health.EndpointStatus;
import io.gravitee.reporter.api.http.Metrics;
import io.gravitee.reporter.api.log.Log;
import io.gravitee.reporter.api.monitor.Monitor;
import io.gravitee.reporter.api.v4.log.MessageLog;
import io.gravitee.reporter.api.v4.metric.MessageMetrics;
import io.gravitee.reporter.api.v4.metric.eventnative.EventNativeMetrics;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
public enum MetricsType {
  REQUEST("request", Metrics.class),
  NODE_MONITOR("node", Monitor.class),
  HEALTH_CHECK("health-check", EndpointStatus.class),
  REQUEST_LOG("log", Log.class),
  V4_LOG("v4-log", io.gravitee.reporter.api.v4.log.Log.class),
  V4_METRICS("v4-metrics", io.gravitee.reporter.api.v4.metric.Metrics.class),
  V4_MESSAGE_METRICS("v4-message-metrics", MessageMetrics.class),
  V4_MESSAGE_LOG("v4-message-log", MessageLog.class),
  V4_EVENT_NATIVE_METRICS("v4-event-native-metrics", EventNativeMetrics.class);

  private final String type;
  private final Class<? extends Reportable> clazz;

  MetricsType(String type, Class<? extends Reportable> clazz) {
    this.type = type;
    this.clazz = clazz;
  }

  public String getType() {
    return type;
  }

  public Class<? extends Reportable> getClazz() {
    return clazz;
  }
}
