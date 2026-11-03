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
package io.gravitee.gamma.rest.resources.observability.metricdefinitions.dto;

import io.gravitee.gamma.rest.core.observability.catalog.model.AdditionalMetricCatalog;
import java.util.List;

/**
 * The environment's additional metric catalog. {@code refreshedAt} is when the discovery pass behind the
 * discovered entries and {@code lastSeen} ran.
 */
public record MetricDefinitionsResponseDto(List<MetricDefinitionEntryDto> data, String refreshedAt, boolean truncated) {
    public static MetricDefinitionsResponseDto from(AdditionalMetricCatalog catalog) {
        return new MetricDefinitionsResponseDto(
            catalog.entries().stream().map(MetricDefinitionEntryDto::from).toList(),
            catalog.refreshedAt() == null ? null : catalog.refreshedAt().toString(),
            catalog.truncated()
        );
    }
}
