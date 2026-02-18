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
package io.gravitee.apim.core.plan.query_service;

import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.rest.api.model.v4.plan.PlanQuery;
import java.util.List;

public interface PlanSearchQueryService {
    /**
     * Search plans for a given reference, applying query filters.
     *
     * @param referenceId        the reference id (e.g. API id or API Product id)
     * @param referenceType      the reference type (e.g. "API", "API_PRODUCT")
     * @param query              optional filters (name, securityType, status, mode)
     * @param authenticatedUser current user id (for group-based visibility when implemented)
     * @param isAdmin            whether the user is admin (admin bypasses group check in REST API plan search)
     * @return list of plans matching the reference and filters
     */
    List<Plan> searchPlans(String referenceId, String referenceType, PlanQuery query, String authenticatedUser, boolean isAdmin);

    /**
     * Find a plan by its id, reference id and reference type, or throw if not found.
     *
     * @param planId        the plan id
     * @param referenceId   the reference id (e.g. API id or API Product id)
     * @param referenceType the reference type (e.g. "API", "API_PRODUCT")
     * @return the plan
     * @throws io.gravitee.rest.api.service.exceptions.PlanNotFoundException if plan not found or does not belong to reference
     */
    Plan findByPlanIdAndReferenceIdAndReferenceType(String planId, String referenceId, String referenceType);
}
