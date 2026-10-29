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
package io.gravitee.gamma.rest.resources.observability.analytics.dto;

import io.gravitee.gamma.rest.core.observability.analytics.model.AnalyticsDefinition;
import io.gravitee.gamma.rest.core.observability.analytics.model.DimensionDefinition;
import io.gravitee.gamma.rest.core.observability.analytics.model.MetricDefinition;
import java.util.List;

/**
 * Wire shape of the analytics definition: families of metrics whose facets are named only, their labels being
 * in {@code dimensions} once for all.
 *
 * @author GraviteeSource Team
 */
public record AnalyticsDefinitionResponseDto(List<MetricFamilyDto> families, List<DimensionDefinitionDto> dimensions) {
    public record MetricFamilyDto(String name, String label, List<MetricDefinitionDto> metrics) {}

    public record MetricDefinitionDto(
        String name,
        String label,
        String unit,
        List<String> apiTypes,
        List<String> measures,
        List<String> facets,
        List<String> filters,
        String origin
    ) {
        static MetricDefinitionDto from(MetricDefinition metric) {
            return new MetricDefinitionDto(
                metric.name(),
                metric.label(),
                metric.unit(),
                metric.apiTypes().stream().map(Enum::name).toList(),
                metric.measures(),
                metric.facets().stream().map(DimensionDefinition::name).toList(),
                metric.filters(),
                metric.origin().name()
            );
        }
    }

    public record DimensionDefinitionDto(String name, String label, String origin) {}

    public static AnalyticsDefinitionResponseDto from(AnalyticsDefinition definition) {
        return new AnalyticsDefinitionResponseDto(
            definition
                .families()
                .stream()
                .map(family ->
                    new MetricFamilyDto(
                        family.family().name(),
                        family.family().label(),
                        family.metrics().stream().map(MetricDefinitionDto::from).toList()
                    )
                )
                .toList(),
            definition
                .dimensions()
                .stream()
                .map(dimension -> new DimensionDefinitionDto(dimension.name(), dimension.label(), dimension.origin().name()))
                .toList()
        );
    }
}
