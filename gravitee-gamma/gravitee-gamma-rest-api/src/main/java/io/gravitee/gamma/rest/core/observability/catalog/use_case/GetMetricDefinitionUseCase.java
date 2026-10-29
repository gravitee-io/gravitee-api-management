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
import io.gravitee.gamma.rest.core.observability.catalog.domain_service.MetricDefinitionKeyDomainService;
import io.gravitee.gamma.rest.core.observability.catalog.exception.MetricDefinitionNotFoundException;
import io.gravitee.gamma.rest.core.observability.catalog.model.AdditionalMetricDefinition;
import io.gravitee.gamma.rest.core.observability.catalog.model.AdditionalMetricField;
import io.gravitee.gamma.rest.core.observability.catalog.port.repository.AdditionalMetricDefinitionRepository;
import lombok.AllArgsConstructor;

/**
 * Reads the record on one key of the environment; 404 when the environment holds none.
 *
 * @author GraviteeSource Team
 */
@UseCase
@AllArgsConstructor
public class GetMetricDefinitionUseCase {

    private final AdditionalMetricDefinitionRepository repository;
    private final MetricDefinitionKeyDomainService keys;

    public record Input(String environmentId, String name) {}

    public record Output(AdditionalMetricDefinition definition) {}

    public Output execute(Input input) {
        AdditionalMetricField field = keys.field(input.name());
        return new Output(
            repository
                .findByEnvironmentIdAndRawKey(input.environmentId(), field.rawKey())
                .orElseThrow(() -> new MetricDefinitionNotFoundException(input.name()))
        );
    }
}
