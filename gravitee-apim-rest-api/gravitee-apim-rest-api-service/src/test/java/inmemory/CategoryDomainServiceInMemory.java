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

import io.gravitee.apim.core.api.domain_service.CategoryDomainService;
import io.gravitee.apim.core.api.model.Api;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.util.CollectionUtils;

/**
 * In-memory implementation of CategoryDomainService for tests.
 * Uses CategoryQueryServiceInMemory for resolveToCategoryIds.
 */
public class CategoryDomainServiceInMemory implements CategoryDomainService {

    private final CategoryQueryServiceInMemory categoryQueryService;

    public CategoryDomainServiceInMemory() {
        this(new CategoryQueryServiceInMemory());
    }

    public CategoryDomainServiceInMemory(CategoryQueryServiceInMemory categoryQueryService) {
        this.categoryQueryService = categoryQueryService;
    }

    @Override
    public Set<String> toCategoryId(Api api, String environmentId) {
        return resolveToCategoryIds(environmentId, api.getCategories());
    }

    @Override
    public Set<String> toCategoryKey(Api api, String environmentId) {
        if (api.getCategories() == null || api.getCategories().isEmpty()) {
            return api.getCategories();
        }
        return api
            .getCategories()
            .stream()
            .map(idOrKey -> categoryQueryService.findByIdOrKey(idOrKey, environmentId))
            .filter(java.util.Optional::isPresent)
            .map(opt -> opt.get().getKey())
            .filter(key -> key != null)
            .collect(Collectors.toCollection(HashSet::new));
    }

    @Override
    public Set<String> resolveToCategoryIds(String environmentId, Set<String> categoryIdsOrKeys) {
        if (CollectionUtils.isEmpty(categoryIdsOrKeys)) {
            return categoryIdsOrKeys;
        }
        return categoryIdsOrKeys
            .stream()
            .map(idOrKey -> categoryQueryService.findByIdOrKey(idOrKey, environmentId))
            .filter(java.util.Optional::isPresent)
            .map(opt -> opt.get().getId())
            .collect(Collectors.toCollection(HashSet::new));
    }

    @Override
    public void updateOrderCategoriesOfApi(String apiId, Collection<String> categoryIds) {
        // No-op for in-memory tests; ApiCategoryOrderRepository is used elsewhere
    }
}
