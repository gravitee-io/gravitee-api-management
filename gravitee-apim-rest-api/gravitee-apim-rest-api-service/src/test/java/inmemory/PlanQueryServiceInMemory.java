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
package inmemory;

import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.plan.query_service.PlanQueryService;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class PlanQueryServiceInMemory implements PlanQueryService, InMemoryAlternative<Plan> {

    private final List<Plan> storage;

    public PlanQueryServiceInMemory() {
        storage = new ArrayList<>();
    }

    public PlanQueryServiceInMemory(PlanCrudServiceInMemory planCrudServiceInMemory) {
        storage = planCrudServiceInMemory.storage;
    }

    @Override
    public List<Plan> findAllByApiIdAndGeneralConditionsAndIsActive(String apiId, DefinitionVersion definitionVersion, String pageId) {
        return storage
            .stream()
            .filter(
                plan ->
                    Objects.equals(apiId, plan.getReferenceId()) &&
                    Objects.equals(pageId, plan.getGeneralConditions()) &&
                    !(PlanStatus.STAGING.equals(plan.getPlanStatus()) || PlanStatus.CLOSED.equals(plan.getPlanStatus()))
            )
            .toList();
    }

    @Override
    public List<Plan> findAllByApiId(String apiId) {
        return findAllByReferenceIdAndReferenceType(apiId, GenericPlanEntity.ReferenceType.API);
    }

    @Override
    public List<Plan> findAllByApiIds(Set<String> apiIds, Set<String> environmentIds) {
        return findAllByReferenceIdsAndEnvironments(apiIds, environmentIds, GenericPlanEntity.ReferenceType.API);
    }

    @Override
    public List<Plan> findAllByReferenceIdAndReferenceType(String referenceId, GenericPlanEntity.ReferenceType referenceType) {
        Objects.requireNonNull(referenceId, "referenceId must not be null");
        Objects.requireNonNull(referenceType, "referenceType must not be null");
        return storage
            .stream()
            .filter(plan -> referenceId.equals(plan.getReferenceId()) && referenceType.equals(plan.getReferenceType()))
            .map(p -> (Plan) p)
            .toList();
    }

    @Override
    public List<Plan> findAllByReferenceIdsAndEnvironments(
        Set<String> referenceIds,
        Set<String> environmentIds,
        GenericPlanEntity.ReferenceType referenceType
    ) {
        Objects.requireNonNull(referenceType, "referenceType must not be null");
        if (referenceIds == null || referenceIds.isEmpty() || environmentIds == null || environmentIds.isEmpty()) {
            return List.of();
        }
        return storage
            .stream()
            .filter(
                plan ->
                    referenceIds.contains(plan.getReferenceId()) &&
                    referenceType.equals(plan.getReferenceType()) &&
                    (plan.getEnvironmentId() == null || environmentIds.contains(plan.getEnvironmentId()))
            )
            .map(p -> (Plan) p)
            .toList();
    }

    @Override
    public void initWith(List<Plan> items) {
        storage.addAll(items.stream().map(Plan::copy).toList());
    }

    @Override
    public void reset() {
        storage.clear();
    }

    @Override
    public List<Plan> storage() {
        return Collections.unmodifiableList(storage);
    }
}
