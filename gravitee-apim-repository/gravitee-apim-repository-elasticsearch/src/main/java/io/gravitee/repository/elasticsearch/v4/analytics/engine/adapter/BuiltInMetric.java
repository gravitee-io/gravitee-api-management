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
package io.gravitee.repository.elasticsearch.v4.analytics.engine.adapter;

import io.gravitee.repository.analytics.engine.api.metric.AdditionalMetric;
import io.gravitee.repository.analytics.engine.api.metric.Metric;
import io.gravitee.repository.analytics.engine.api.metric.MetricName;

/**
 * The built-in a search path reading no additional metric field measures. Only the request documents carry those
 * fields, so any other path refuses one rather than aggregate a field its index does not hold, which Elasticsearch
 * would answer with an empty result instead of an error.
 */
final class BuiltInMetric {

    private BuiltInMetric() {}

    static Metric of(MetricName metric) {
        return switch (metric) {
            case Metric builtIn -> builtIn;
            case AdditionalMetric additionalMetric -> throw new UnsupportedOperationException(
                "Additional metric '" + additionalMetric.name() + "' is measured on the request documents only"
            );
        };
    }
}
