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
package io.gravitee.repository.management.api;

import io.gravitee.repository.exceptions.DuplicateKeyException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.model.GammaMetricDefinition;
import java.util.List;
import java.util.Optional;

public interface GammaMetricDefinitionRepository extends CrudRepository<GammaMetricDefinition, String> {
    /** Ordered by {@code rawKey}. */
    List<GammaMetricDefinition> findByEnvironmentId(String environmentId) throws TechnicalException;

    Optional<GammaMetricDefinition> findByEnvironmentIdAndRawKey(String environmentId, String rawKey) throws TechnicalException;

    /**
     * Fails with a {@link DuplicateKeyException} when the environment already holds a record on the same
     * {@code rawKey}: the store enforces the uniqueness, so two concurrent creations cannot both succeed.
     */
    @Override
    GammaMetricDefinition create(GammaMetricDefinition item) throws TechnicalException;

    void deleteByEnvironmentId(String environmentId) throws TechnicalException;
}
