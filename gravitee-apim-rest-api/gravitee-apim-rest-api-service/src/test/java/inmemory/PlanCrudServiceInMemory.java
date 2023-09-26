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

import io.gravitee.apim.crud_service.plan.PlanCrudService;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.service.exceptions.PlanNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PlanCrudServiceInMemory implements PlanCrudService, InMemoryCrudService<GenericPlanEntity> {

    private final List<GenericPlanEntity> storage = new ArrayList<>();

    @Override
    public GenericPlanEntity findById(String planId) {
        if (planId == null) {
            throw new TechnicalManagementException("planId should not be null");
        }
        return storage
            .stream()
            .filter(plan -> planId.equals(plan.getId()))
            .findFirst()
            .orElseThrow(() -> new PlanNotFoundException(planId));
    }

    @Override
    public void initWith(List<GenericPlanEntity> items) {
        storage.addAll(items);
    }

    @Override
    public void reset() {
        storage.clear();
    }

    @Override
    public List<GenericPlanEntity> storage() {
        return Collections.unmodifiableList(storage);
    }
}
