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

import io.gravitee.apim.core.category.domain_service.UpdateCategoryApiDomainService;
import io.gravitee.apim.core.category.model.ApiCategoryOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;

public class UpdateCategoryApiDomainServiceInMemory implements UpdateCategoryApiDomainService, InMemoryAlternative<ApiCategoryOrder> {

    private final List<ApiCategoryOrder> storage;

    public UpdateCategoryApiDomainServiceInMemory(CategoryApiCrudServiceInMemory categoryApiCrudService) {
        this.storage = categoryApiCrudService.storage();
    }

    public UpdateCategoryApiDomainServiceInMemory() {
        this.storage = new ArrayList<>();
    }

    @Override
    public void initWith(List<ApiCategoryOrder> items) {
        storage.clear();
        storage.addAll(items);
    }

    @Override
    public void reset() {
        storage.clear();
    }

    @Override
    public List<ApiCategoryOrder> storage() {
        return storage;
    }

    @Override
    public void changeOrder(ApiCategoryOrder apiCategoryOrder, int newOrder) {
        OptionalInt index = this.findIndex(
            this.storage,
            apiCategoryOrder1 ->
                Objects.equals(apiCategoryOrder.getCategoryId(), apiCategoryOrder1.getCategoryId()) &&
                Objects.equals(apiCategoryOrder.getApiId(), apiCategoryOrder1.getApiId())
        );
        if (index.isPresent()) {
            apiCategoryOrder.setOrder(newOrder);
            storage.set(index.getAsInt(), apiCategoryOrder);
            return;
        }
        throw new IllegalStateException("ApiCategoryOrder not found");
    }
}
