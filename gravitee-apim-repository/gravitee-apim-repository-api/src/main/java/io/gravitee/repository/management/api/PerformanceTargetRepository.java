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

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.model.PerformanceTarget;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface PerformanceTargetRepository {
    PerformanceTarget create(PerformanceTarget target) throws TechnicalException;

    Optional<PerformanceTarget> findById(String id) throws TechnicalException;

    PerformanceTarget update(PerformanceTarget target) throws TechnicalException;

    void delete(String id) throws TechnicalException;

    /**
     * @return every target of every environment, the scheduler's view of what is to be evaluated
     */
    Set<PerformanceTarget> findAll() throws TechnicalException;

    List<PerformanceTarget> findByReference(String environmentId, String reference) throws TechnicalException;

    /**
     * @return the ids of the deleted targets
     */
    List<String> deleteByReference(String environmentId, String reference) throws TechnicalException;

    /**
     * @return the ids of the deleted targets
     */
    List<String> deleteByEnvironmentId(String environmentId) throws TechnicalException;

    /**
     * Removes {@code apiId} from the api ids of every target that lists it. Targets are kept even when no id is left.
     *
     * @return the ids of the targets that listed the api
     */
    List<String> removeApiId(String apiId) throws TechnicalException;
}
