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

import io.gravitee.gamma.rest.core.observability.filter.use_case.ResolveObservabilityFilterLabelsUseCase;
import java.util.List;
import java.util.Map;

/**
 * Response for {@code POST /observability/filters/resolve}: per requested filter, a map of
 * {@code id → label}. Matches the apim management v2 {@code ResolveFilterLabelsResponse} shape.
 */
public record ResolveFilterLabelsResponseDto(List<EntryDto> entries) {
    public record EntryDto(String filterName, Map<String, String> labels) {}

    public static ResolveFilterLabelsResponseDto from(ResolveObservabilityFilterLabelsUseCase.Output output) {
        return new ResolveFilterLabelsResponseDto(
            output
                .entries()
                .stream()
                .map(e -> new EntryDto(e.filterName(), e.labels()))
                .toList()
        );
    }
}
