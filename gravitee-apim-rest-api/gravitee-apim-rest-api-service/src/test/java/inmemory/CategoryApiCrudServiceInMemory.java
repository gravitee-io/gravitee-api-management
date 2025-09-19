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

import io.gravitee.apim.core.category.crud_service.CategoryApiCrudService;
import io.gravitee.apim.core.category.exception.ApiAndCategoryNotAssociatedException;
import io.gravitee.apim.core.category.model.ApiCategoryOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class CategoryApiCrudServiceInMemory implements CategoryApiCrudService, InMemoryAlternative<ApiCategoryOrder> {

    private final List<ApiCategoryOrder> storage = new ArrayList<>();

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
    public Optional<ApiCategoryOrder> findById(String apiId, String categoryId) {
        return storage
            .stream()
            .filter(
                apiCategoryOrder ->
                    Objects.equals(apiId, apiCategoryOrder.getApiId()) && Objects.equals(categoryId, apiCategoryOrder.getCategoryId())
            )
            .findFirst();
    }

    @Override
    public ApiCategoryOrder get(String apiId, String categoryId) {
        return this.findById(apiId, categoryId).orElseThrow(() -> new ApiAndCategoryNotAssociatedException(apiId, categoryId));
    }
}
