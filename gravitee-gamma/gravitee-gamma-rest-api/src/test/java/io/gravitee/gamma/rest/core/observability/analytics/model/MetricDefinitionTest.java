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
package io.gravitee.gamma.rest.core.observability.analytics.model;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.gamma.rest.core.observability.catalog.model.CatalogOrigin;
import io.gravitee.gamma.rest.core.observability.filter.model.ApiType;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class MetricDefinitionTest {

    @Test
    void should_group_a_built_in_by_its_api_kinds() {
        assertThat(aMetric(List.of(ApiType.LLM), CatalogOrigin.BUILT_IN).family()).isEqualTo(MetricFamily.LLM);
        assertThat(aMetric(List.of(ApiType.HTTP_PROXY, ApiType.LLM), CatalogOrigin.BUILT_IN).family()).isEqualTo(MetricFamily.GATEWAY);
    }

    /** An additional metric field is served for the HTTP kinds, which would make it a gateway metric: its origin wins. */
    @ParameterizedTest
    @EnumSource(value = CatalogOrigin.class, names = { "DECLARED", "DISCOVERED" })
    void should_group_an_additional_metric_field_under_the_additional_family_whatever_its_kinds(CatalogOrigin origin) {
        assertThat(aMetric(List.of(ApiType.HTTP_PROXY, ApiType.LLM), origin).family()).isEqualTo(MetricFamily.ADDITIONAL);
        assertThat(aMetric(List.of(ApiType.LLM), origin).family()).isEqualTo(MetricFamily.ADDITIONAL);
    }

    private static MetricDefinition aMetric(List<ApiType> apiTypes, CatalogOrigin origin) {
        return new MetricDefinition("metric", "Metric", null, apiTypes, List.of("AVG"), List.of(), List.of(), origin);
    }
}
