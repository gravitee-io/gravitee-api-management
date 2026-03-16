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
import io.gravitee.apim.core.plan.query_service.PlanSearchQueryService;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.model.v4.plan.PlanQuery;
import io.gravitee.rest.api.model.v4.plan.PlanSecurityType;
import io.gravitee.rest.api.service.exceptions.PlanNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.util.CollectionUtils;

public class PlanSearchQueryServiceInMemory implements PlanSearchQueryService, InMemoryAlternative<Plan> {

    private final List<Plan> storage;

    public PlanSearchQueryServiceInMemory() {
        storage = new ArrayList<>();
    }

    public PlanSearchQueryServiceInMemory(PlanCrudServiceInMemory planCrudServiceInMemory) {
        storage = planCrudServiceInMemory.storage;
    }

    @Override
    public List<Plan> searchPlans(
        String referenceId,
        GenericPlanEntity.ReferenceType referenceType,
        PlanQuery query,
        String authenticatedUser,
        boolean isAdmin
    ) {
        return storage
            .stream()
            .filter(p -> referenceId.equals(p.getReferenceId()) && referenceType.equals(p.getReferenceType()))
            .filter(p -> {
                boolean filtered = true;
                if (query.getName() != null) {
                    filtered = query.getName().equals(p.getName());
                }
                if (filtered && !CollectionUtils.isEmpty(query.getSecurityType())) {
                    if (p.getPlanSecurity() == null || p.getPlanSecurity().getType() == null) {
                        return false;
                    }
                    PlanSecurityType planSecurityType = PlanSecurityType.valueOfLabel(p.getPlanSecurity().getType());
                    filtered = query.getSecurityType().contains(planSecurityType);
                }
                if (filtered && !CollectionUtils.isEmpty(query.getStatus())) {
                    PlanStatus planStatus = PlanStatus.valueOfLabel(p.getPlanStatus().getLabel());
                    filtered = query.getStatus().contains(planStatus);
                }
                if (filtered && query.getMode() != null) {
                    filtered = query.getMode().equals(p.getPlanMode());
                }
                return filtered;
            })
            .toList();
    }

    @Override
    public Plan findByPlanIdAndReferenceIdAndReferenceType(
        String planId,
        String referenceId,
        GenericPlanEntity.ReferenceType referenceType
    ) {
        return storage
            .stream()
            .filter(p -> planId.equals(p.getId()) && referenceId.equals(p.getReferenceId()) && referenceType.equals(p.getReferenceType()))
            .findFirst()
            .orElseThrow(() -> new PlanNotFoundException(planId));
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
