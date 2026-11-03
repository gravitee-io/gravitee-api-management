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
package io.gravitee.gamma.rest.core.observability.catalog.inmemory;

import io.gravitee.gamma.rest.core.observability.catalog.exception.DuplicateMetricDefinitionException;
import io.gravitee.gamma.rest.core.observability.catalog.exception.MetricDefinitionNotFoundException;
import io.gravitee.gamma.rest.core.observability.catalog.model.AdditionalMetricDefinition;
import io.gravitee.gamma.rest.core.observability.catalog.port.repository.AdditionalMetricDefinitionRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class InMemoryAdditionalMetricDefinitionRepository implements AdditionalMetricDefinitionRepository {

    private final List<AdditionalMetricDefinition> records = new ArrayList<>();

    private AdditionalMetricDefinition pendingConcurrentCreate;
    private boolean pendingConcurrentDelete;

    public void reset() {
        records.clear();
        pendingConcurrentCreate = null;
        pendingConcurrentDelete = false;
    }

    /** Another write creates {@code definition} right before the next {@link #create}. */
    public void givenConcurrentCreateBeforeNextCreate(AdditionalMetricDefinition definition) {
        pendingConcurrentCreate = definition;
    }

    /** Another write deletes the record right before the next {@link #update}. */
    public void givenConcurrentDeleteBeforeNextUpdate() {
        pendingConcurrentDelete = true;
    }

    public List<AdditionalMetricDefinition> all() {
        return List.copyOf(records);
    }

    @Override
    public List<AdditionalMetricDefinition> findByEnvironmentId(String environmentId) {
        return records
            .stream()
            .filter(record -> Objects.equals(record.environmentId(), environmentId))
            .sorted(Comparator.comparing(record -> record.field().rawKey()))
            .toList();
    }

    @Override
    public Optional<AdditionalMetricDefinition> findByEnvironmentIdAndRawKey(String environmentId, String rawKey) {
        return records
            .stream()
            .filter(record -> Objects.equals(record.environmentId(), environmentId) && record.field().rawKey().equals(rawKey))
            .findFirst();
    }

    @Override
    public AdditionalMetricDefinition create(AdditionalMetricDefinition definition) {
        if (pendingConcurrentCreate != null) {
            records.add(pendingConcurrentCreate);
            pendingConcurrentCreate = null;
        }
        if (findByEnvironmentIdAndRawKey(definition.environmentId(), definition.field().rawKey()).isPresent()) {
            throw new DuplicateMetricDefinitionException(definition.field().name());
        }
        records.add(definition);
        return definition;
    }

    @Override
    public AdditionalMetricDefinition update(AdditionalMetricDefinition definition) {
        if (pendingConcurrentDelete) {
            delete(definition.id());
            pendingConcurrentDelete = false;
        }
        int index = indexOf(definition.id());
        if (index < 0) {
            throw new MetricDefinitionNotFoundException(definition.field().name());
        }
        records.set(index, definition);
        return definition;
    }

    @Override
    public void delete(String id) {
        int index = indexOf(id);
        if (index >= 0) {
            records.remove(index);
        }
    }

    private int indexOf(String id) {
        for (int i = 0; i < records.size(); i++) {
            if (records.get(i).id().equals(id)) {
                return i;
            }
        }
        return -1;
    }
}
