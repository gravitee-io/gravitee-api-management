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
package io.gravitee.apim.reporter.elasticsearch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import io.gravitee.apim.reporter.common.bulk.backpressure.BulkDropper;
import io.gravitee.reporter.api.v4.metric.event.TopicEventMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class DroppedDocumentsMetricTest {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final DroppedDocumentsMetric metric = new DroppedDocumentsMetric(registry);

    @Test
    void should_count_the_documents_a_bulk_dropper_drops_by_reason_and_type() {
        metric.dropped(BulkDropper.Reason.OVERFLOW, "v4-metrics", 2);
        metric.dropped(BulkDropper.Reason.OVERFLOW, "v4-metrics", 3);
        metric.dropped(BulkDropper.Reason.REJECTED, "v4-log", 1);

        assertThat(count("overflow", "v4-metrics")).isEqualTo(5);
        assertThat(count("rejected", "v4-log")).isEqualTo(1);
    }

    @Test
    void should_count_the_documents_reported_before_the_setup_succeeds_with_their_type() {
        metric.droppedDuringSetup(topicEventMetrics());

        assertThat(count("setup", "topic-event-metrics")).isEqualTo(1);
    }

    @Test
    void should_count_nothing_when_the_gateway_exposes_no_metrics() {
        final DroppedDocumentsMetric withoutRegistry = new DroppedDocumentsMetric(null);

        assertThatNoException().isThrownBy(() -> {
            withoutRegistry.dropped(BulkDropper.Reason.ERROR, "request", 1);
            withoutRegistry.droppedDuringSetup(topicEventMetrics());
        });
    }

    private static TopicEventMetrics topicEventMetrics() {
        return TopicEventMetrics.builder()
            .gatewayId("gateway")
            .organizationId("organization")
            .environmentId("environment")
            .apiId("api")
            .topic("orders")
            .build();
    }

    private double count(String reason, String type) {
        return registry.get("reporter_elasticsearch_dropped_documents").tag("reason", reason).tag("type", type).counter().count();
    }
}
