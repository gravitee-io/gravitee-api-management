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
import io.gravitee.gamma.rest.core.tracing.model.TraceFilterValue;

/**
 * One row in the filter-values endpoint response. {@code label} is optional and only set for
 * opaque-id filters where the stored {@code value} isn't directly human-readable — none exist in
 * the slim cut, but the field is forward-compat with KEYWORD filters arriving in a follow-up.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TraceFilterValueDto(String value, String label) {
    public static TraceFilterValueDto from(TraceFilterValue value) {
        return new TraceFilterValueDto(value.value(), value.label());
    }
}
