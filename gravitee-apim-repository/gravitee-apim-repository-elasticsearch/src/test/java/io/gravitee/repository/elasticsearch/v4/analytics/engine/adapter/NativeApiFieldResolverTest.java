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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.gravitee.repository.analytics.engine.api.metric.Metric;
import io.gravitee.repository.analytics.engine.api.query.Facet;
import io.gravitee.repository.analytics.engine.api.query.Filter;
import io.gravitee.repository.log.v4.model.connection.NativeApiMetricKeys;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class NativeApiFieldResolverTest {

    private final NativeApiFieldResolver fieldResolver = new NativeApiFieldResolver();

    @Test
    void resolves_native_connections_metric_to_timestamp() {
        assertThat(fieldResolver.fromMetric(Metric.NATIVE_CONNECTIONS_SUMMARY)).isEqualTo("@timestamp");
    }

    @Test
    void resolves_native_connection_status_facet_to_additional_metrics_keyword() {
        assertThat(fieldResolver.fromFacet(Facet.NATIVE_CONNECTION_STATUS)).isEqualTo(
            "additional-metrics." + NativeApiMetricKeys.CONNECTION_STATUS
        );
    }

    @Test
    void resolves_api_filter_to_api_id() {
        assertThat(fieldResolver.fromFilter(new Filter(Filter.Name.API, Filter.Operator.IN, List.of("api-1")))).isEqualTo("api-id");
    }

    @Test
    void resolves_application_filter_to_application_id() {
        assertThat(fieldResolver.fromFilter(new Filter(Filter.Name.APPLICATION, Filter.Operator.IN, List.of("app-1")))).isEqualTo(
            "application-id"
        );
    }

    @Test
    void resolves_plan_filter_to_plan_id() {
        assertThat(fieldResolver.fromFilter(new Filter(Filter.Name.PLAN, Filter.Operator.IN, List.of("plan-1")))).isEqualTo("plan-id");
    }

    @Test
    void resolves_native_connection_status_filter_to_additional_metrics_keyword() {
        assertThat(
            fieldResolver.fromFilter(new Filter(Filter.Name.NATIVE_CONNECTION_STATUS, Filter.Operator.IN, List.of("CONNECTED")))
        ).isEqualTo("additional-metrics." + NativeApiMetricKeys.CONNECTION_STATUS);
    }

    @Test
    void throws_UnsupportedOperationException_on_http_metric() {
        assertThrows(UnsupportedOperationException.class, () -> fieldResolver.fromMetric(Metric.HTTP_REQUESTS));
    }

    @Test
    void throws_UnsupportedOperationException_on_http_filter() {
        assertThrows(UnsupportedOperationException.class, () ->
            fieldResolver.fromFilter(new Filter(Filter.Name.HTTP_STATUS, Filter.Operator.EQ, 200))
        );
    }

    @Test
    void throws_UnsupportedOperationException_on_http_facet() {
        assertThrows(UnsupportedOperationException.class, () -> fieldResolver.fromFacet(Facet.HTTP_STATUS));
    }
}
