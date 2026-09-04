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
package io.gravitee.apim.core.performance_target.crud_service;

import io.gravitee.apim.core.performance_target.exception.PerformanceTargetNotFoundException;
import io.gravitee.apim.core.performance_target.model.PerformanceTarget;
import java.util.List;
import java.util.Optional;

public interface PerformanceTargetCrudService {
    PerformanceTarget create(PerformanceTarget target);

    Optional<PerformanceTarget> findById(String id);

    /**
     * @throws PerformanceTargetNotFoundException when the target does not exist or belongs to another environment
     */
    default PerformanceTarget get(String environmentId, String id) {
        return findById(id)
            .filter(target -> target.environmentId().equals(environmentId))
            .orElseThrow(() -> new PerformanceTargetNotFoundException(id));
    }

    PerformanceTarget update(PerformanceTarget target);

    void delete(String id);

    /**
     * @return the ids of the deleted targets
     */
    List<String> deleteByReference(String environmentId, String reference);

    /**
     * Removes the api from every target's subject; targets left without ids are kept.
     *
     * @return the ids of the targets that listed the api
     */
    List<String> removeApiId(String apiId);
}
