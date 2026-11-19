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

import io.gravitee.apim.reporter.common.MetricsType;
import io.gravitee.apim.reporter.common.bulk.backpressure.BulkDropper;
import io.gravitee.apim.reporter.common.bulk.backpressure.DroppedReportsListener;
import io.gravitee.reporter.api.Reportable;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Locale;

/**
 * Counts the documents the reporter drops, by reason and type, in {@value #NAME}: those reported before the setup succeeds,
 * and those of the bulks dropped on overflow, after the last retry, or because Elasticsearch rejected them.
 */
public class DroppedDocumentsMetric implements DroppedReportsListener {

    static final String NAME = "reporter_elasticsearch_dropped_documents";
    private static final String SETUP = "setup";

    private final MeterRegistry registry;

    /**
     * @param registry the registry of the gateway, or null when the gateway exposes no metrics
     */
    public DroppedDocumentsMetric(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void dropped(BulkDropper.Reason reason, String type, int count) {
        increment(reason.name().toLowerCase(Locale.ROOT), type, count);
    }

    public void droppedDuringSetup(Reportable reportable) {
        increment(SETUP, MetricsType.typeOf(reportable.getClass()), 1);
    }

    private void increment(String reason, String type, int count) {
        if (registry == null) {
            return;
        }
        Counter.builder(NAME)
            .description("Documents the Elasticsearch reporter dropped instead of writing them")
            .tag("reason", reason)
            .tag("type", type)
            .register(registry)
            .increment(count);
    }
}
