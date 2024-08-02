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
import io.gravitee.repository.management.model.Plan;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Plan repository API.
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface PlanRepository extends CrudRepository<Plan, String> {
    /**
     * Find plans by api ids and environments
     * @param apiIds the list of id of the apis to which to retrieve plans. Cannot NOT be null or empty
     * @param environments the list of environments to which to retrieve plans. Could be null or empty
     *
     * @return the list of plans linked to the specified api ids.
     * @throws TechnicalException
     */
    List<Plan> findByApisAndEnvironments(List<String> apiIds, Set<String> environments) throws TechnicalException;

    /**
     * Returns the list of plans for a given API.
     *
     * @param apiId API identifier.
     *
     * @return List of plan for the given API.
     * @throws TechnicalException
     */
    Set<Plan> findByApi(String apiId) throws TechnicalException;

    /**
     * Returns the list of plans matching the list of plan IDs.
     *
     * @param ids A set of plan ids.
     *
     * @return The set of plans matching the given IDs.
     * @throws TechnicalException
     */
    Set<Plan> findByIdIn(Collection<String> ids) throws TechnicalException;

    /**
     * Delete plans for the environment
     * @param environmentId The environment ID
     * @return List of deleted plans IDs for the environment
     * @throws TechnicalException
     */
    List<String> deleteByEnvironmentId(String environmentId) throws TechnicalException;
}
