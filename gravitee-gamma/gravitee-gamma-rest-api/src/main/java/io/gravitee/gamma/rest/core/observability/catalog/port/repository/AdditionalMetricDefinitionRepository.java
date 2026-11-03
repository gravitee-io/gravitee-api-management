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
import java.util.List;
import java.util.Optional;

/**
 * Port onto the environment's metric definition records. Every read is scoped to one environment, so a record of
 * another environment is never reachable above the adapter.
 *
 * @author GraviteeSource Team
 */
public interface AdditionalMetricDefinitionRepository {
    /** Ordered by key. */
    List<AdditionalMetricDefinition> findByEnvironmentId(String environmentId);

    Optional<AdditionalMetricDefinition> findByEnvironmentIdAndRawKey(String environmentId, String rawKey);

    /**
     * Refuses a second record on the same key of the same environment, even a concurrent one, with a
     * {@link DuplicateMetricDefinitionException}.
     */
    AdditionalMetricDefinition create(AdditionalMetricDefinition definition);

    /** Fails with a {@link MetricDefinitionNotFoundException} when the record was deleted meanwhile. */
    AdditionalMetricDefinition update(AdditionalMetricDefinition definition);

    /** Takes a bare id: callers resolve the record through {@link #findByEnvironmentIdAndRawKey} first. */
    void delete(String id);
}
