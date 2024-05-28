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

import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.query_service.ApiCategoryQueryService;
import io.gravitee.apim.core.category.model.Category;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ApiCategoryQueryServiceInMemory implements ApiCategoryQueryService, InMemoryAlternative<Category> {

    final List<Category> storage;

    public ApiCategoryQueryServiceInMemory() {
        storage = new ArrayList<>();
    }

    @Override
    public Collection<String> findApiCategoryKeys(Api api) {
        var categoriesIds = api.getCategories();
        if (categoriesIds == null || categoriesIds.isEmpty()) {
            return Collections.emptySet();
        }

        return storage
            .stream()
            .filter(category -> categoriesIds.contains(category.getId()) || categoriesIds.contains(category.getKey()))
            .map(Category::getKey)
            .collect(Collectors.toSet());
    }

    @Override
    public Collection<Category> findByEnvironmentId(String environmentId) {
        return new HashSet<>(storage);
    }

    @Override
    public void initWith(List<Category> items) {
        storage.clear();
        storage.addAll(items);
    }

    @Override
    public void reset() {
        storage.clear();
    }

    @Override
    public List<Category> storage() {
        return storage;
    }
}
