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
package io.gravitee.repository.analytics.engine.api.metric;

import io.gravitee.repository.analytics.engine.api.query.AdditionalMetricField;

/**
 * A numeric additional metric field measured as a metric. Only the requests carrying the field count: a request
 * without it is no zero, so an average, a bound or a percentile reads none on a window where no request carries it.
 */
public record AdditionalMetric(AdditionalMetricField field) implements MetricName {
    public AdditionalMetric {
        switch (field.valueType()) {
            case LONG, INT, DOUBLE -> {}
            case KEYWORD, BOOL, STRING, JSON -> throw new IllegalArgumentException("Not a numeric additional metric: " + field.name());
        }
    }

    @Override
    public String name() {
        return field.name();
    }
}
