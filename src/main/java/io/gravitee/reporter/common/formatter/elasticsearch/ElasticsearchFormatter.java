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
package io.gravitee.reporter.common.formatter.elasticsearch;

import io.gravitee.common.templating.FreeMarkerComponent;
import io.gravitee.common.utils.UUID;
import io.gravitee.node.api.Node;
import io.gravitee.reporter.api.Reportable;
import io.gravitee.reporter.api.health.EndpointStatus;
import io.gravitee.reporter.api.http.Metrics;
import io.gravitee.reporter.api.log.Log;
import io.gravitee.reporter.api.monitor.Monitor;
import io.gravitee.reporter.api.v4.log.MessageLog;
import io.gravitee.reporter.api.v4.metric.MessageMetrics;
import io.gravitee.reporter.common.formatter.AbstractFormatter;
import io.vertx.core.buffer.Buffer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ElasticsearchFormatter<T extends Reportable>
  extends AbstractFormatter<T> {

  private static final String TEMPLATES_BASE_PATTERN =
    "/freemarker/es%dx/index/";

  /** Index simple date format **/
  private final DateTimeFormatter dtf;
  private final DateTimeFormatter sdf;

  private static String hostname;

  static {
    try {
      hostname = InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      hostname = "unknown";
    }
  }

  private final Node node;

  private final FreeMarkerComponent freeMarkerComponent;

  public ElasticsearchFormatter(Node node, int elasticSearchVersion) {
    this.node = node;
    this.dtf = dtfWithDefaultZone("yyyy-MM-dd'T'HH:mm:ss.SSS[XXX]");
    this.sdf = dtfWithDefaultZone("yyyy.MM.dd");

    this.freeMarkerComponent =
      FreeMarkerComponent
        .builder()
        .classLoader(getClass().getClassLoader())
        .classLoaderTemplateBase(
          String.format(TEMPLATES_BASE_PATTERN, elasticSearchVersion)
        )
        .build();
  }

  private static DateTimeFormatter dtfWithDefaultZone(String format) {
    return DateTimeFormatter.ofPattern(format).withZone(ZoneId.systemDefault());
  }

  @Override
  public Buffer format0(T reportable) {
    return format0(reportable, null);
  }

  @Override
  public Buffer format0(T reportable, Map<String, Object> esOptions) {
    if (reportable instanceof Metrics metrics) {
      return getSource(metrics, esOptions);
    } else if (reportable instanceof EndpointStatus endpointStatus) {
      return getSource(endpointStatus, esOptions);
    } else if (reportable instanceof Monitor monitor) {
      return getSource(monitor, esOptions);
    } else if (reportable instanceof Log log) {
      return getSource(log, esOptions);
    }

    if (
      reportable instanceof io.gravitee.reporter.api.v4.metric.Metrics metrics
    ) {
      return getSource(metrics, esOptions);
    } else if (reportable instanceof MessageMetrics metrics) {
      return getSource(metrics, esOptions);
    } else if (reportable instanceof io.gravitee.reporter.api.v4.log.Log log) {
      return getSource(log, esOptions);
    } else if (reportable instanceof MessageLog log) {
      return getSource(log, esOptions);
    }

    return null;
  }

  /**
   * Convert a {@link Metrics} into an ES bulk line.
   *
   * @param metrics A request metrics
   * @return ES bulk line
   */
  private Buffer getSource(
    final Metrics metrics,
    Map<String, Object> esOptions
  ) {
    final Map<String, Object> data = new HashMap<>(10);

    addCommonFields(data, metrics, esOptions);

    data.put("metrics", metrics);

    data.put(
      "apiResponseTime",
      metrics.getApiResponseTimeMs() >= 0
        ? metrics.getApiResponseTimeMs()
        : null
    );
    data.put(
      "proxyLatency",
      metrics.getProxyLatencyMs() >= 0 ? metrics.getProxyLatencyMs() : null
    );
    data.put(
      "requestContentLength",
      metrics.getRequestContentLength() >= 0
        ? metrics.getRequestContentLength()
        : null
    );
    data.put(
      "responseContentLength",
      metrics.getResponseContentLength() >= 0
        ? metrics.getResponseContentLength()
        : null
    );

    return generateData("request.ftl", data);
  }

  /**
   * Convert a {@link Log} into an ES bulk line.
   *
   * @param log A request log
   * @return ES bulk line
   */
  private Buffer getSource(final Log log, Map<String, Object> esOptions) {
    final Map<String, Object> data = new HashMap<>(5);

    addCommonFields(data, log, esOptions);

    data.put("log", log);

    return generateData("log.ftl", data);
  }

  /**
   * Convert a {@link EndpointStatus} into an ES bulk line.
   * @param endpointStatus the healthStatus
   * @return ES bulk line
   */
  private Buffer getSource(
    final EndpointStatus endpointStatus,
    Map<String, Object> esOptions
  ) {
    final Map<String, Object> data = new HashMap<>(5);

    addCommonFields(data, endpointStatus, esOptions);

    data.put("status", endpointStatus);

    return generateData("health.ftl", data);
  }

  /**
   * Convert a monitor into a ES bulk line.
   * @param monitor the monitor metric
   * @return ES bulk line
   */
  private Buffer getSource(
    final Monitor monitor,
    Map<String, Object> esOptions
  ) {
    final Map<String, Object> data = new HashMap<>();

    addCommonFields(data, monitor, esOptions);

    data.put("id", UUID.random().toString());
    data.put(Fields.HOSTNAME, hostname);

    if (monitor.getOs() != null) {
      if (monitor.getOs().cpu != null) {
        data.put(Fields.PERCENT, monitor.getOs().cpu.getPercent());

        if (
          monitor.getOs().cpu.getLoadAverage() != null &&
          Arrays
            .stream(monitor.getOs().cpu.getLoadAverage())
            .anyMatch(load -> load != -1)
        ) {
          if (monitor.getOs().cpu.getLoadAverage()[0] != -1) {
            data.put(
              Fields.LOAD_AVERAGE_1M,
              monitor.getOs().cpu.getLoadAverage()[0]
            );
          }
          if (monitor.getOs().cpu.getLoadAverage()[1] != -1) {
            data.put(
              Fields.LOAD_AVERAGE_5M,
              monitor.getOs().cpu.getLoadAverage()[1]
            );
          }
          if (monitor.getOs().cpu.getLoadAverage()[2] != -1) {
            data.put(
              Fields.LOAD_AVERAGE_15M,
              monitor.getOs().cpu.getLoadAverage()[2]
            );
          }
        }
      }

      if (monitor.getOs().mem != null) {
        data.put(
          "mem_" + Fields.TOTAL_IN_BYTES,
          monitor.getOs().mem.getTotal()
        );
        data.put("mem_" + Fields.FREE_IN_BYTES, monitor.getOs().mem.getFree());
        data.put("mem_" + Fields.USED_IN_BYTES, monitor.getOs().mem.getUsed());
        data.put(
          "mem_" + Fields.FREE_PERCENT,
          monitor.getOs().mem.getFreePercent()
        );
        data.put(
          "mem_" + Fields.USED_PERCENT,
          monitor.getOs().mem.getUsedPercent()
        );
      }
    }

    if (monitor.getProcess() != null) {
      data.put("process_" + Fields.TIMESTAMP, monitor.getProcess().timestamp);
      data.put(
        Fields.OPEN_FILE_DESCRIPTORS,
        monitor.getProcess().openFileDescriptors
      );
      data.put(
        Fields.MAX_FILE_DESCRIPTORS,
        monitor.getProcess().maxFileDescriptors
      );
      data.put(
        "process_" + Fields.PERCENT,
        monitor.getProcess().cpu.getPercent()
      );
    }

    if (monitor.getJvm() != null) {
      data.put("jvm_" + Fields.TIMESTAMP, monitor.getJvm().timestamp);
      data.put(Fields.UPTIME_IN_MILLIS, monitor.getJvm().uptime);

      if (monitor.getJvm().mem != null) {
        data.put(Fields.HEAP_USED_IN_BYTES, monitor.getJvm().mem.heapUsed);
        if (monitor.getJvm().mem.getHeapUsedPercent() >= 0) {
          data.put(
            Fields.HEAP_USED_PERCENT,
            monitor.getJvm().mem.getHeapUsedPercent()
          );
        }
        data.put(
          Fields.HEAP_COMMITTED_IN_BYTES,
          monitor.getJvm().mem.heapCommitted
        );
        data.put(Fields.HEAP_MAX_IN_BYTES, monitor.getJvm().mem.heapMax);
        data.put(
          Fields.NON_HEAP_USED_IN_BYTES,
          monitor.getJvm().mem.nonHeapUsed
        );
        data.put(
          Fields.NON_HEAP_COMMITTED_IN_BYTES,
          monitor.getJvm().mem.nonHeapCommitted
        );

        data.put(Fields.POOLS, monitor.getJvm().mem.pools);
      }

      if (monitor.getJvm().threads != null) {
        data.put(Fields.COUNT, monitor.getJvm().threads.getCount());
        data.put(Fields.PEAK_COUNT, monitor.getJvm().threads.getPeakCount());
      }

      if (monitor.getJvm().gc != null) {
        data.put(Fields.COLLECTORS, monitor.getJvm().gc.collectors);
      }
    }

    return generateData("monitor.ftl", data);
  }

  private Buffer getSource(
    io.gravitee.reporter.api.v4.metric.Metrics metrics,
    Map<String, Object> esOptions
  ) {
    final Map<String, Object> data = new HashMap<>(10);

    addCommonFields(data, metrics, esOptions);

    data.put("metrics", metrics);

    data.put(
      "endpointResponseTimeMs",
      metrics.getEndpointResponseTimeMs() >= 0
        ? metrics.getEndpointResponseTimeMs()
        : null
    );
    data.put(
      "gatewayResponseTimeMs",
      metrics.getGatewayResponseTimeMs() >= 0
        ? metrics.getGatewayResponseTimeMs()
        : null
    );
    data.put(
      "gatewayLatencyMs",
      metrics.getGatewayLatencyMs() >= 0 ? metrics.getGatewayLatencyMs() : null
    );
    data.put(
      "requestContentLength",
      metrics.getRequestContentLength() >= 0
        ? metrics.getRequestContentLength()
        : null
    );
    data.put(
      "responseContentLength",
      metrics.getResponseContentLength() >= 0
        ? metrics.getResponseContentLength()
        : null
    );

    return generateData("v4-metrics.ftl", data);
  }

  private Buffer getSource(
    MessageMetrics metrics,
    Map<String, Object> esOptions
  ) {
    final Map<String, Object> data = new HashMap<>(10);

    addCommonFields(data, metrics, esOptions);

    data.put("metrics", metrics);
    data.put(
      "contentLength",
      metrics.getContentLength() >= 0 ? metrics.getContentLength() : null
    );
    data.put("count", metrics.getCount() >= 0 ? metrics.getCount() : null);
    data.put(
      "errorCount",
      metrics.getErrorCount() >= 0 ? metrics.getErrorCount() : null
    );
    data.put(
      "countIncrement",
      metrics.getCountIncrement() >= 0 ? metrics.getCountIncrement() : null
    );
    data.put(
      "errorCountIncrement",
      metrics.getErrorCountIncrement() >= 0
        ? metrics.getErrorCountIncrement()
        : null
    );
    data.put(
      "gatewayLatencyMs",
      metrics.getGatewayLatencyMs() >= 0 ? metrics.getGatewayLatencyMs() : null
    );

    return generateData("v4-message-metrics.ftl", data);
  }

  private Buffer getSource(
    io.gravitee.reporter.api.v4.log.Log log,
    Map<String, Object> esOptions
  ) {
    final Map<String, Object> data = new HashMap<>(5);

    addCommonFields(data, log, esOptions);

    data.put("log", log);

    return generateData("v4-log.ftl", data);
  }

  private Buffer getSource(MessageLog log, Map<String, Object> esOptions) {
    final Map<String, Object> data = new HashMap<>(5);

    addCommonFields(data, log, esOptions);

    data.put("log", log);

    return generateData("v4-message-log.ftl", data);
  }

  private Buffer generateData(String template, Map<String, Object> data) {
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      freeMarkerComponent.generateFromTemplate(
        template,
        data,
        new OutputStreamWriter(baos)
      );

      return Buffer.buffer(baos.toByteArray());
    } catch (IOException e) {
      return null;
    }
  }

  private void addCommonFields(
    Map<String, Object> data,
    Reportable reportable,
    Map<String, Object> esOptions
  ) {
    data.put(Fields.SPECIAL_TIMESTAMP, dtf.format(reportable.timestamp()));
    data.put(Fields.GATEWAY, node.id());

    if (esOptions != null) {
      if (esOptions.get("index") != null) {
        data.put("index", esOptions.get("index"));
      }
      if (esOptions.get("pipeline") != null) {
        data.put("pipeline", esOptions.get("pipeline"));
      }
      if (esOptions.get("date") == null) {
        data.put("date", sdf.format(reportable.timestamp()));
      }
    } else {
      data.put("date", sdf.format(reportable.timestamp()));
    }
  }

  static final class Fields {

    private Fields() {}

    static final String GATEWAY = "gateway";
    static final String HOSTNAME = "hostname";
    static final String SPECIAL_TIMESTAMP = "@timestamp";

    static final String TIMESTAMP = "timestamp";
    static final String PERCENT = "percent";
    static final String LOAD_AVERAGE_1M = "load_average_1m";
    static final String LOAD_AVERAGE_5M = "load_average_5m";
    static final String LOAD_AVERAGE_15M = "load_average_15m";

    static final String FREE_IN_BYTES = "free_in_bytes";
    static final String USED_IN_BYTES = "used_in_bytes";
    static final String TOTAL_IN_BYTES = "total_in_bytes";

    static final String FREE_PERCENT = "free_percent";
    static final String USED_PERCENT = "used_percent";

    static final String OPEN_FILE_DESCRIPTORS = "open_file_descriptors";
    static final String MAX_FILE_DESCRIPTORS = "max_file_descriptors";

    static final String UPTIME_IN_MILLIS = "uptime_in_millis";

    static final String HEAP_USED_IN_BYTES = "heap_used_in_bytes";
    static final String HEAP_USED_PERCENT = "heap_used_percent";
    static final String HEAP_MAX_IN_BYTES = "heap_max_in_bytes";
    static final String HEAP_COMMITTED_IN_BYTES = "heap_committed_in_bytes";

    static final String NON_HEAP_USED_IN_BYTES = "non_heap_used_in_bytes";
    static final String NON_HEAP_COMMITTED_IN_BYTES =
      "non_heap_committed_in_bytes";

    static final String POOLS = "pools";

    static final String COUNT = "count";
    static final String PEAK_COUNT = "peak_count";

    static final String COLLECTORS = "collectors";
  }
}
