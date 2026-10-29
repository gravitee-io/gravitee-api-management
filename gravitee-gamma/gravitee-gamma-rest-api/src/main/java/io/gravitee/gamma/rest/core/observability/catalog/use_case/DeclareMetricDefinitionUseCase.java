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
import io.gravitee.gamma.rest.core.observability.catalog.model.AdditionalMetricDefinition;
import io.gravitee.gamma.rest.core.observability.catalog.model.AdditionalMetricField;
import io.gravitee.gamma.rest.core.observability.catalog.model.MetricDefinitionChanges;
import io.gravitee.gamma.rest.core.observability.catalog.port.repository.AdditionalMetricDefinitionRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;

/**
 * Declares a key, whether or not documents carry it yet. Refuses a key the environment already declares: the
 * declare form creates, editing goes through {@link SaveMetricDefinitionUseCase}.
 *
 * @author GraviteeSource Team
 */
@UseCase
@AllArgsConstructor
public class DeclareMetricDefinitionUseCase {

    private final AdditionalMetricDefinitionRepository repository;
    private final MetricDefinitionKeyDomainService keys;

    public record Input(String environmentId, String name, String label, String description, String unit, Boolean exposed) {}

    public record Output(AdditionalMetricDefinition definition) {}

    public Output execute(Input input) {
        AdditionalMetricField field = keys.writableField(input.name());
        MetricDefinitionChanges changes = MetricDefinitionChanges.of(input.label(), input.description(), input.unit(), input.exposed());
        if (repository.findByEnvironmentIdAndRawKey(input.environmentId(), field.rawKey()).isPresent()) {
            throw new DuplicateMetricDefinitionException(input.name());
        }
        Instant now = TimeProvider.instantNow();
        var definition = new AdditionalMetricDefinition(
            UUID.randomUUID().toString(),
            input.environmentId(),
            field,
            changes.label(),
            changes.description(),
            changes.unit(),
            changes.exposed(),
            now,
            now
        );
        return new Output(repository.create(definition));
    }
}
