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
import io.gravitee.gamma.rest.core.observability.filter.model.FilterValue;

/**
 * Wire shape for one selectable filter value. {@code label} is omitted when null (value and label
 * identical) to match the lib's optional-field shape.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FilterValueDto(String value, String label) {
    public static FilterValueDto from(FilterValue value) {
        // Collapse a label identical to the value to null so the wire stays compact.
        String label = value.label() == null || value.label().equals(value.value()) ? null : value.label();
        return new FilterValueDto(value.value(), label);
    }
}
