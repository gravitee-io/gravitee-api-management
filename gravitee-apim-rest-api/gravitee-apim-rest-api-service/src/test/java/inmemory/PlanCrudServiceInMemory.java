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

import io.gravitee.apim.core.plan.crud_service.PlanCrudService;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.rest.api.service.exceptions.PlanNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

public class PlanCrudServiceInMemory implements PlanCrudService, InMemoryAlternative<Plan> {

    final List<Plan> storage = new ArrayList<>();

    @Override
    public Plan getById(String planId) {
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
    public Optional<Plan> findById(String planId) {
        return storage.stream().filter(plan -> planId.equals(plan.getId())).findFirst();
    }

    @Override
    public Plan create(Plan plan) {
        storage.add(plan);
        return plan;
    }

    @Override
    public Plan update(Plan entity) {
        OptionalInt index = this.findIndex(this.storage, plan -> plan.getId().equals(entity.getId()));
        if (index.isPresent()) {
            storage.set(index.getAsInt(), entity);
            return entity;
        }

        throw new IllegalStateException("Plan not found");
    }

    @Override
    public void delete(String planId) {
        OptionalInt index = this.findIndex(this.storage, plan -> plan.getId().equals(planId));
        if (index.isPresent()) {
            storage.remove(index.getAsInt());
        }
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
