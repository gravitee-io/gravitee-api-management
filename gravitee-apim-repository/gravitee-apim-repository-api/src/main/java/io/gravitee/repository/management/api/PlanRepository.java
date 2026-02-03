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
import java.util.Optional;
import java.util.Set;

/**
 * Plan repository API.
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface PlanRepository extends CrudRepository<Plan, String> {
    /**
     * WARNING: this method should only be called by the gatway.
     * Find plans by api ids and environments
     * @param apiIds the list of id of the apis to which to retrieve plans. Cannot NOT be null or empty
     * @param environments the list of environments to which to retrieve plans. Could be null or empty
     *
     * @return the list of plans linked to the specified api ids.
     * @throws TechnicalException
     * @Deprecated since 4.11.0, use {@link #findByReferenceIdsAndReferenceTypeAndEnvironment(List, Plan.PlanReferenceType, Set)}
     */
    default List<Plan> findByApisAndEnvironments(List<String> apiIds, Set<String> environments) throws TechnicalException {
        return findByReferenceIdsAndReferenceTypeAndEnvironment(apiIds, Plan.PlanReferenceType.API, environments);
    }

    /**
     * Returns the list of plans for a given API.
     *
     * @param apiId API identifier.
     *
     * @return Set of plan for the given API.
     * @throws TechnicalException
     * @Deprecated since 4.11.0, use {@link #findByReferenceIdAndReferenceType(String, Plan.PlanReferenceType)}
     */
    default Set<Plan> findByApi(String apiId) throws TechnicalException {
        return findByReferenceIdAndReferenceType(apiId, Plan.PlanReferenceType.API);
    }

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

    boolean exists(String id) throws TechnicalException;

    /**
     * Update order of a plan identified by its id.
     * @param planId the plan id
     * @param order the new order value
     * @throws TechnicalException in case of error
     */
    void updateOrder(String planId, int order) throws TechnicalException;

    /**
     * Update cross ids of plans.
     * @param plans the plans to update.
     */
    void updateCrossIds(List<Plan> plans) throws TechnicalException;

    /**
     * Finds a set of plans by the reference ID and reference type.
     *
     * @param referenceId the ID of the reference to filter the plans. Cannot be null.
     * @param planReferenceType the type of the reference to filter the plans. Cannot be null.
     * @return a set of plans matching the given reference ID and reference type.
     * @throws TechnicalException if an error occurs during the retrieval of the plans.
     */
    Set<Plan> findByReferenceIdAndReferenceType(String referenceId, Plan.PlanReferenceType planReferenceType) throws TechnicalException;

    /**
     * WARNING: this method should only be called by the gatway.
     * Finds a list of plans by their reference IDs, reference type, and associated environments.
     *
     * @param referenceIds The list of reference IDs to filter the plans. Cannot be null or empty.
     * @param planReferenceType The type of reference to filter the plans. Cannot be null.
     * @param environments The set of environments to filter the plans. Cannot be null or empty.
     * @return A list of plans matching the given reference IDs, reference type, and environments.
     * @throws TechnicalException If an error occurs during the retrieval of plans.
     */
    List<Plan> findByReferenceIdsAndReferenceTypeAndEnvironment(
        List<String> referenceIds,
        Plan.PlanReferenceType planReferenceType,
        Set<String> environments
    ) throws TechnicalException;

    /**
     * Finds a plan by its ID, reference ID, and reference type.
     *
     * @param plan the plan ID. Cannot be null.
     * @param referenceId the reference ID. Cannot be null.
     * @param planReferenceType the reference type. Cannot be null.
     * @return an Optional containing the plan if found, or an empty Optional if not found.
     * @throws TechnicalException if an error occurs during the retrieval of the plan.
     */
    Optional<Plan> findByIdAndReferenceIdAndReferenceType(String plan, String referenceId, Plan.PlanReferenceType planReferenceType)
        throws TechnicalException;
}
