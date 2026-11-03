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
package io.gravitee.gamma.rest.core.observability.catalog.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.gamma.rest.core.observability.catalog.domain_service.MetricDefinitionKeyDomainService;
import io.gravitee.gamma.rest.core.observability.catalog.exception.DuplicateMetricDefinitionException;
import io.gravitee.gamma.rest.core.observability.catalog.exception.MetricDefinitionNotFoundException;
import io.gravitee.gamma.rest.core.observability.catalog.model.AdditionalMetricDefinition;
import io.gravitee.gamma.rest.core.observability.catalog.model.AdditionalMetricField;
import io.gravitee.gamma.rest.core.observability.catalog.model.MetricDefinitionChanges;
import io.gravitee.gamma.rest.core.observability.catalog.port.repository.AdditionalMetricDefinitionRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;

/**
 * Writes the record on one key: replaces it when the environment holds one, creates it otherwise. Editing a
 * discovered key is how it becomes declared.
 *
 * @author GraviteeSource Team
 */
@UseCase
@AllArgsConstructor
public class SaveMetricDefinitionUseCase {

    private final AdditionalMetricDefinitionRepository repository;
    private final MetricDefinitionKeyDomainService keys;

    public record Input(String environmentId, String name, String label, String description, String unit, Boolean exposed) {}

    /** @param created {@code true} when no record existed on the key. */
    public record Output(AdditionalMetricDefinition definition, boolean created) {}

    public Output execute(Input input) {
        AdditionalMetricField field = keys.writableField(input.name());
        MetricDefinitionChanges changes = MetricDefinitionChanges.of(input.label(), input.description(), input.unit(), input.exposed());
        Instant now = TimeProvider.instantNow();
        return repository
            .findByEnvironmentIdAndRawKey(input.environmentId(), field.rawKey())
            .map(existing -> replace(existing, changes, now))
            .orElseGet(() -> create(input.environmentId(), field, changes, now));
    }

    /** A record deleted since it was read is written again: the caller asked for the key to hold these fields. */
    private Output replace(AdditionalMetricDefinition existing, MetricDefinitionChanges changes, Instant now) {
        try {
            return new Output(repository.update(existing.with(changes, now)), false);
        } catch (MetricDefinitionNotFoundException deletedMeanwhile) {
            return new Output(repository.create(fresh(existing.environmentId(), existing.field(), changes, now)), true);
        }
    }

    /** A record created since the read by a concurrent write is replaced, as if this write had come second. */
    private Output create(String environmentId, AdditionalMetricField field, MetricDefinitionChanges changes, Instant now) {
        try {
            return new Output(repository.create(fresh(environmentId, field, changes, now)), true);
        } catch (DuplicateMetricDefinitionException createdMeanwhile) {
            AdditionalMetricDefinition existing = repository
                .findByEnvironmentIdAndRawKey(environmentId, field.rawKey())
                .orElseThrow(() -> createdMeanwhile);
            return new Output(repository.update(existing.with(changes, now)), false);
        }
    }

    private static AdditionalMetricDefinition fresh(
        String environmentId,
        AdditionalMetricField field,
        MetricDefinitionChanges changes,
        Instant now
    ) {
        return new AdditionalMetricDefinition(
            UUID.randomUUID().toString(),
            environmentId,
            field,
            changes.label(),
            changes.description(),
            changes.unit(),
            changes.exposed(),
            now,
            now
        );
    }
}
