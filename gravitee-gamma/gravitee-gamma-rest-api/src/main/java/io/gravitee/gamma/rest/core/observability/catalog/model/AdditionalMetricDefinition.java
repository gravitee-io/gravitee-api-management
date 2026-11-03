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
package io.gravitee.gamma.rest.core.observability.catalog.model;

import java.time.Instant;

/**
 * What an environment says about one additional metric key: its label, description and unit, and whether the
 * pickers offer it. One record per key and environment; it exists whether or not documents carry the key yet.
 *
 * @param label   {@code null} when the record leaves the label to the derived one.
 * @param unit    a named unit, a free suffix, or {@code null}; see {@link AdditionalMetricUnit}.
 * @param exposed {@code false} keeps the entry out of the catalogs pickers read (the filter catalog today); the query
 *                validators do not read records.
 *
 * @author GraviteeSource Team
 */
public record AdditionalMetricDefinition(
    String id,
    String environmentId,
    AdditionalMetricField field,
    String label,
    String description,
    String unit,
    boolean exposed,
    Instant createdAt,
    Instant updatedAt
) {
    public String effectiveLabel() {
        return label != null ? label : field.derivedLabel();
    }

    public AdditionalMetricDefinition with(MetricDefinitionChanges changes, Instant now) {
        return new AdditionalMetricDefinition(
            id,
            environmentId,
            field,
            changes.label(),
            changes.description(),
            changes.unit(),
            changes.exposed(),
            createdAt,
            now
        );
    }
}
