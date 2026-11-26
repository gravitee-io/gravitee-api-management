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
package io.gravitee.gamma.rest.core.observability.catalog.port.repository;

import io.gravitee.gamma.rest.core.observability.catalog.exception.DuplicateMetricDefinitionException;
import io.gravitee.gamma.rest.core.observability.catalog.exception.MetricDefinitionNotFoundException;
import io.gravitee.gamma.rest.core.observability.catalog.model.AdditionalMetricDefinition;
import io.gravitee.gamma.rest.core.observability.catalog.model.AdditionalMetricField;
import java.util.List;
import java.util.Optional;

/**
 * Port onto the environment's metric definition records. Every read is scoped to one environment, so a record of
 * another environment is never reachable above the adapter.
 *
 * @author GraviteeSource Team
 */
public interface AdditionalMetricDefinitionRepository {
    /** In no order callers rely on: each store sorts by its own collation, so the catalog orders the records itself. */
    List<AdditionalMetricDefinition> findByEnvironmentId(String environmentId);

    /** The record on a field, whatever its root: a custom key and an additional key of the same spelling are two keys. */
    Optional<AdditionalMetricDefinition> findByEnvironmentIdAndField(String environmentId, AdditionalMetricField field);

    /**
     * Refuses a second record on the same key of the same environment, even a concurrent one, with a
     * {@link DuplicateMetricDefinitionException}.
     */
    AdditionalMetricDefinition create(AdditionalMetricDefinition definition);

    /** Fails with a {@link MetricDefinitionNotFoundException} when the record was deleted meanwhile. */
    AdditionalMetricDefinition update(AdditionalMetricDefinition definition);

    /** Takes a bare id: callers resolve the record through {@link #findByEnvironmentIdAndField} first. */
    void delete(String id);
}
