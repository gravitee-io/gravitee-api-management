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
package io.gravitee.plugin.endpoint.kafka.error;

import java.util.Map;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import reactor.core.publisher.Sinks;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class AbstractKafkaErrorTransformer {

    protected static <T> void mayThrowConnectionClosedException(
        final Sinks.Many<T> kafkaErrorSink,
        final Map<MetricName, ? extends Metric> metrics
    ) {
        metrics
            .values()
            .stream()
            .filter(metric -> metric.metricName().name().equals("connection-close-total") && metric.metricValue() != null)
            .map(metric -> Double.parseDouble(metric.metricValue().toString()))
            .filter(connectionCloseTotal -> connectionCloseTotal > 0)
            .findFirst()
            .ifPresent(
                metric -> {
                    kafkaErrorSink.tryEmitError(new KafkaConnectionClosedException());
                    throw new KafkaConnectionClosedException();
                }
            );
    }
}
