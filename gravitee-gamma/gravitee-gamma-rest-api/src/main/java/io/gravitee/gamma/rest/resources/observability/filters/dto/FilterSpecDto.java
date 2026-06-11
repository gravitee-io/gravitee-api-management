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
package io.gravitee.gamma.rest.resources.observability.filters.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterSpec;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Wire shape for one observability filter spec.
 *
 * <p>{@code type} is emitted lowercase to match the {@code @gravitee/gamma-lib-observability}
 * {@code FilterType} union; {@code operators} are emitted UPPERCASE to match the apim management v2
 * analytics/logs wire convention. {@code enumValues} carry {@code {value, label}} so the front
 * renders readable chips without hardcoding value→label maps. {@code signals} and {@code apiTypes}
 * are emitted UPPERCASE and sorted for a stable payload so the consumer can route a filter to the
 * right panel. Optional fields ({@code enumValues}, {@code range}) are omitted when null.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FilterSpecDto(
    String name,
    String label,
    String type,
    List<String> operators,
    List<EnumValueDto> enumValues,
    RangeDto range,
    List<String> signals,
    List<String> apiTypes
) {
    public record EnumValueDto(String value, String label) {}

    public record RangeDto(@JsonInclude(JsonInclude.Include.NON_NULL) Number min, @JsonInclude(JsonInclude.Include.NON_NULL) Number max) {}

    public static FilterSpecDto from(FilterSpec spec) {
        return new FilterSpecDto(
            spec.name(),
            spec.label(),
            spec.type().name().toLowerCase(java.util.Locale.ROOT),
            spec.operators().stream().map(Enum::name).toList(),
            spec.enumValues() == null
                ? null
                : spec
                    .enumValues()
                    .stream()
                    .map(v -> new EnumValueDto(v.value(), v.label()))
                    .toList(),
            spec.range() == null ? null : new RangeDto(spec.range().min(), spec.range().max()),
            sortedNames(spec.signals()),
            sortedNames(spec.apiTypes())
        );
    }

    private static <E extends Enum<E>> List<String> sortedNames(Set<E> values) {
        return values == null ? List.of() : values.stream().map(Enum::name).sorted(Comparator.naturalOrder()).toList();
    }
}
