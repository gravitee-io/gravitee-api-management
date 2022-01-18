/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.reporter.file.formatter.csv;

import io.gravitee.node.api.monitor.Monitor;
import io.gravitee.reporter.api.Reportable;
import io.gravitee.reporter.api.health.EndpointStatus;
import io.gravitee.reporter.api.http.Metrics;
import io.gravitee.reporter.api.log.Log;
import io.gravitee.reporter.file.formatter.Formatter;
import io.vertx.core.buffer.Buffer;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class CsvFormatter<T extends Reportable> implements Formatter<T> {

    private static final EndpointStatusFormatter ENDPOINT_STATUS_FORMATTER = new EndpointStatusFormatter();
    private static final LogFormatter LOG_FORMATTER = new LogFormatter();
    private static final MetricsFormatter METRICS_FORMATTER = new MetricsFormatter();
    private static final MonitorFormatter MONITOR_FORMATTER = new MonitorFormatter();

    @Override
    public Buffer format(T data) {
        if (data instanceof Metrics) {
            return METRICS_FORMATTER.format((Metrics) data);
        } else if (data instanceof Log) {
            return LOG_FORMATTER.format((Log) data);
        } else if (data instanceof EndpointStatus) {
            return ENDPOINT_STATUS_FORMATTER.format((EndpointStatus) data);
        } else if (data instanceof Monitor) {
            return MONITOR_FORMATTER.format((Monitor) data);
        }
        return null;
    }
}
