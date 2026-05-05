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

import io.gravitee.repository.analytics.engine.api.metric.Metric;
import io.gravitee.repository.analytics.engine.api.query.Facet;
import io.gravitee.repository.analytics.engine.api.query.Filter;
import io.gravitee.repository.elasticsearch.v4.analytics.engine.adapter.api.FieldResolver;
import io.gravitee.repository.elasticsearch.v4.log.adapter.connection.RequestV2MetricsV4Fields;
import io.gravitee.repository.log.v4.model.connection.NativeApiMetricKeys;

public class NativeApiFieldResolver implements FieldResolver {

    private static final String CONNECTION_STATUS_FIELD =
        RequestV2MetricsV4Fields.ADDITIONAL_METRICS + "." + NativeApiMetricKeys.CONNECTION_STATUS;

    @Override
    public String fromMetric(Metric metric) {
        return switch (metric) {
            case NATIVE_CONNECTIONS_SUMMARY -> RequestV2MetricsV4Fields.TIMESTAMP;
            default -> throw new UnsupportedOperationException(
                "NativeApiFieldResolver supports only Metric.NATIVE_CONNECTIONS_SUMMARY but got " + metric
            );
        };
    }

    @Override
    public String fromFilter(Filter filter) {
        return switch (filter.name()) {
            case API -> RequestV2MetricsV4Fields.API_ID.v4Metrics();
            case APPLICATION -> RequestV2MetricsV4Fields.APPLICATION_ID.v4Metrics();
            case PLAN -> RequestV2MetricsV4Fields.PLAN_ID.v4Metrics();
            case NATIVE_CONNECTION_STATUS -> CONNECTION_STATUS_FIELD;
            default -> throw new UnsupportedOperationException(
                "NativeApiFieldResolver does not support filter '" +
                    filter.name() +
                    "' — supported names: API, APPLICATION, PLAN, NATIVE_CONNECTION_STATUS"
            );
        };
    }

    @Override
    public String fromFacet(Facet facet) {
        return switch (facet) {
            case NATIVE_CONNECTION_STATUS -> CONNECTION_STATUS_FIELD;
            default -> throw new UnsupportedOperationException(
                "NativeApiFieldResolver supports only Facet.NATIVE_CONNECTION_STATUS but got " + facet
            );
        };
    }
}
