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
package io.gravitee.gamma.rest.core.observability.analytics.port;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.gamma.rest.core.observability.analytics.model.MetricDefinition;
import io.gravitee.gamma.rest.core.observability.analytics.port.service_provider.AnalyticsDefinitionPort;
import io.gravitee.gamma.rest.core.observability.catalog.model.CatalogOrigin;
import io.gravitee.gamma.rest.core.observability.filter.model.ApiType;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * The behaviour every {@link AnalyticsDefinitionPort} implementation must honour, whatever the catalog behind it.
 * {@link #port()} holds a catalog where at least one metric exists for several API kinds.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public abstract class AnalyticsDefinitionPortContractTest {

    protected abstract AnalyticsDefinitionPort port();

    @Test
    void should_answer_only_metrics_that_exist_for_the_asked_kind() {
        for (var kind : ApiType.API_KINDS) {
            assertThat(port().metricsOf(kind)).allSatisfy(metric -> assertThat(metric.apiTypes()).contains(kind));
        }
    }

    @Test
    void should_list_each_metric_once_per_kind() {
        for (var kind : ApiType.API_KINDS) {
            assertThat(port().metricsOf(kind)).extracting(MetricDefinition::name).doesNotHaveDuplicates();
        }
    }

    /** The use case keeps the first copy of a metric met under several kinds: every copy must be the same. */
    @Test
    void should_describe_a_metric_shared_by_several_kinds_identically_for_each() {
        Map<String, MetricDefinition> seen = new HashMap<>();
        for (var kind : ApiType.API_KINDS) {
            for (var metric : port().metricsOf(kind)) {
                var first = seen.putIfAbsent(metric.name(), metric);
                if (first != null) {
                    assertThat(metric).isEqualTo(first);
                }
            }
        }
        assertThat(seen.values()).anySatisfy(metric -> assertThat(metric.apiTypes()).hasSizeGreaterThan(1));
    }

    @Test
    void should_label_every_metric_and_facet_and_offer_at_least_one_measure() {
        for (var kind : ApiType.API_KINDS) {
            assertThat(port().metricsOf(kind)).allSatisfy(metric -> {
                assertThat(metric.label()).isNotBlank();
                assertThat(metric.measures()).isNotEmpty();
                assertThat(metric.facets()).allSatisfy(facet -> assertThat(facet.label()).isNotBlank());
            });
        }
    }

    @Test
    void should_mark_every_metric_and_facet_as_built_in() {
        for (var kind : ApiType.API_KINDS) {
            assertThat(port().metricsOf(kind)).allSatisfy(metric -> {
                assertThat(metric.origin()).isEqualTo(CatalogOrigin.BUILT_IN);
                assertThat(metric.facets()).allSatisfy(facet -> assertThat(facet.origin()).isEqualTo(CatalogOrigin.BUILT_IN));
            });
        }
    }
}
