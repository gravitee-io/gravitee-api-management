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

import io.gravitee.apim.core.exception.DbEntityNotFoundException;
import io.gravitee.apim.core.promotion.crud_service.PromotionCrudService;
import io.gravitee.apim.core.promotion.model.Promotion;
import io.gravitee.rest.api.service.exceptions.PlanNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.OptionalInt;

public class PromotionCrudServiceInMemory implements PromotionCrudService, InMemoryAlternative<Promotion> {

    final List<Promotion> storage = new ArrayList<>();

    @Override
    public void initWith(List<Promotion> items) {
        storage.addAll(items);
    }

    @Override
    public void reset() {
        storage.clear();
    }

    @Override
    public List<Promotion> storage() {
        return Collections.unmodifiableList(storage);
    }

    @Override
    public Promotion create(Promotion promotion) {
        storage.add(promotion);
        return promotion;
    }

    @Override
    public Promotion update(Promotion promotion) {
        OptionalInt index = this.findIndex(this.storage, saved -> saved.getId().equals(promotion.getId()));
        if (index.isPresent()) {
            storage.set(index.getAsInt(), promotion);
            return promotion;
        }

        throw new IllegalStateException("Promotion not found");
    }

    @Override
    public Promotion getById(String id) {
        if (id == null) {
            throw new TechnicalManagementException("Promotion id should not be null");
        }
        return storage
            .stream()
            .filter(plan -> id.equals(plan.getId()))
            .findFirst()
            .orElseThrow(() -> new DbEntityNotFoundException(io.gravitee.repository.management.model.Promotion.class, id));
    }
}
