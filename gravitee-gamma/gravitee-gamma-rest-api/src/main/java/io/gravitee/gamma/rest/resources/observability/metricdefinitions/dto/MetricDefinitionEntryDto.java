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

import com.fasterxml.jackson.annotation.JsonInclude;
import io.gravitee.gamma.rest.core.observability.catalog.model.AdditionalMetricCatalogEntry;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Wire shape of one entry of the environment's additional metric catalog. {@code type} is the filter type of the
 * built-in spec serving the entry, else the one the value type gives ({@code keyword}, {@code number},
 * {@code boolean}), absent for the detail-only {@code STRING} and {@code JSON}. Absent values are omitted.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MetricDefinitionEntryDto(
    String name,
    String label,
    String description,
    String unit,
    String type,
    List<String> apiTypes,
    String origin,
    String rawKey,
    String valueType,
    String lastSeen,
    Boolean exposed
) {
    public static MetricDefinitionEntryDto from(AdditionalMetricCatalogEntry entry) {
        return new MetricDefinitionEntryDto(
            entry.name(),
            entry.label(),
            entry.description(),
            entry.unit(),
            Optional.ofNullable(entry.type())
                .or(() -> entry.field().valueType().filterType())
                .map(type -> type.name().toLowerCase(Locale.ROOT))
                .orElse(null),
            entry.apiTypes().stream().map(Enum::name).sorted(Comparator.naturalOrder()).toList(),
            entry.origin().name(),
            entry.field().rawKey(),
            entry.field().valueType().name(),
            entry.lastSeen() == null ? null : entry.lastSeen().toString(),
            entry.exposed()
        );
    }
}
