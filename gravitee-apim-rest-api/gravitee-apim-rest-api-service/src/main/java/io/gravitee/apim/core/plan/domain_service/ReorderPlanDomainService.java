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
package io.gravitee.apim.core.plan.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.plan.crud_service.PlanCrudService;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.plan.query_service.PlanQueryService;
import java.util.Comparator;
import java.util.Objects;

@DomainService
public class ReorderPlanDomainService {

    private final PlanQueryService planQueryService;
    private final PlanCrudService planCrudService;

    public ReorderPlanDomainService(PlanQueryService planQueryService, PlanCrudService planCrudService) {
        this.planQueryService = planQueryService;
        this.planCrudService = planCrudService;
    }

    /**
     * Reorder existing plans when update the order of an existing plan.
     *
     * @param toUpdate The updated plan
     *
     * @return The updated plan
     */
    public Plan reorderAfterUpdate(Plan toUpdate) {
        var toReorder = planQueryService
            .findAllByApiId(toUpdate.getReferenceId())
            .stream()
            .filter(p -> p.isPublished() && !Objects.equals(p.getId(), toUpdate.getId()))
            .sorted(Comparator.comparingInt(Plan::getOrder))
            .toList();

        if (toUpdate.getOrder() < 1) {
            toUpdate.setOrder(1);
        } else if (toUpdate.getOrder() > toReorder.size() + 1) {
            toUpdate.setOrder(toReorder.size() + 1);
        }

        for (int i = 0; i < toReorder.size(); i++) {
            int newOrder = (i + 1) < toUpdate.getOrder() ? (i + 1) : (i + 2);
            var planToReorder = toReorder.get(i);
            if (planToReorder.getOrder() != newOrder) {
                planCrudService.update(planToReorder.toBuilder().order(newOrder).build());
            }
        }

        return planCrudService.update(toUpdate);
    }

    public Plan reorderAfterUpdateForApiProduct(Plan toUpdate) {
        var toReorder = planQueryService
            .findAllForApiProduct(toUpdate.getReferenceId())
            .stream()
            .filter(p -> p.isPublished() && !Objects.equals(p.getId(), toUpdate.getId()))
            .sorted(Comparator.comparingInt(Plan::getOrder))
            .toList();
        if (toUpdate.getOrder() < 1) {
            toUpdate.setOrder(1);
        } else if (toUpdate.getOrder() > toReorder.size() + 1) {
            toUpdate.setOrder(toReorder.size() + 1);
        }

        for (int i = 0; i < toReorder.size(); i++) {
            int newOrder = (i + 1) < toUpdate.getOrder() ? (i + 1) : (i + 2);
            var planToReorder = toReorder.get(i);
            if (planToReorder.getOrder() != newOrder) {
                planCrudService.update(planToReorder.toBuilder().order(newOrder).build());
            }
        }

        return planCrudService.update(toUpdate);
    }

    /**
     * Refresh the order of existing plans when plans have been deleted.
     *
     * @param apiId The API ID
     */
    public void refreshOrderAfterDelete(String apiId) {
        var toReorder = planQueryService
            .findAllByApiId(apiId)
            .stream()
            .filter(Plan::isPublished)
            .sorted(Comparator.comparingInt(Plan::getOrder))
            .toList();

        for (int i = 0; i < toReorder.size(); i++) {
            int expectedOrder = i + 1;
            var planToReorder = toReorder.get(i);
            if (planToReorder.getOrder() != expectedOrder) {
                planCrudService.update(planToReorder.toBuilder().order(expectedOrder).build());
            }
        }
    }
}
