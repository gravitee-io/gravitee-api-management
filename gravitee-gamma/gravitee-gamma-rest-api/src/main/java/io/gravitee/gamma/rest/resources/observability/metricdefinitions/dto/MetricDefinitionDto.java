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
import io.gravitee.gamma.rest.core.observability.catalog.model.AdditionalMetricDefinition;

/** Wire shape of one record. {@code label}, {@code description} and {@code unit} are omitted when the record sets none. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MetricDefinitionDto(
    String name,
    String rawKey,
    String valueType,
    String label,
    String description,
    String unit,
    boolean exposed,
    String createdAt,
    String updatedAt
) {
    public static MetricDefinitionDto from(AdditionalMetricDefinition definition) {
        return new MetricDefinitionDto(
            definition.field().name(),
            definition.field().rawKey(),
            definition.field().valueType().name(),
            definition.label(),
            definition.description(),
            definition.unit(),
            definition.exposed(),
            definition.createdAt() == null ? null : definition.createdAt().toString(),
            definition.updatedAt() == null ? null : definition.updatedAt().toString()
        );
    }
}
