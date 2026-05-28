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
package io.gravitee.gamma.rest.resources.tracing.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.gravitee.gamma.rest.core.tracing.model.TraceFilterSpec;
import java.util.List;

/**
 * Wire shape for one filter spec. {@code type} is emitted lowercase to match the
 * {@code @gravitee/gamma-lib-observability} {@code FilterType} union. {@code operators} are emitted
 * UPPERCASE to match the apim management v2 wire convention (analytics + logs) — the lib's TS
 * {@code FilterOperator} union is lowercase, but the lib's {@code toWireFilter} uppercases before
 * sending. Optional fields ({@code enumValues}, {@code range}) are omitted when null.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TraceFilterSpecDto(String name, String label, String type, List<String> operators, List<String> enumValues, RangeDto range) {
    public record RangeDto(@JsonInclude(JsonInclude.Include.NON_NULL) Number min, @JsonInclude(JsonInclude.Include.NON_NULL) Number max) {}

    public static TraceFilterSpecDto from(TraceFilterSpec spec) {
        return new TraceFilterSpecDto(
            spec.name(),
            spec.label(),
            spec.type().name().toLowerCase(),
            spec.operators().stream().map(Enum::name).toList(),
            spec.enumValues(),
            spec.range() == null ? null : new RangeDto(spec.range().min(), spec.range().max())
        );
    }
}
