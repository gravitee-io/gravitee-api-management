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
package io.gravitee.apim.infra.crud_service.category;

import io.gravitee.apim.core.category.crud_service.CategoryApiCrudService;
import io.gravitee.apim.core.category.exception.ApiAndCategoryNotAssociatedException;
import io.gravitee.apim.core.category.model.ApiCategoryOrder;
import io.gravitee.apim.infra.adapter.CategoryAdapter;
import io.gravitee.repository.management.api.ApiCategoryOrderRepository;
import java.util.Objects;
import java.util.Optional;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class CategoryApiCrudServiceImpl implements CategoryApiCrudService {

    private final ApiCategoryOrderRepository apiCategoryOrderRepository;

    public CategoryApiCrudServiceImpl(@Lazy ApiCategoryOrderRepository apiCategoryOrderRepository) {
        this.apiCategoryOrderRepository = apiCategoryOrderRepository;
    }

    @Override
    public Optional<ApiCategoryOrder> findById(String apiId, String categoryId) {
        if (Objects.isNull(apiId) || Objects.isNull(categoryId)) {
            throw new ApiAndCategoryNotAssociatedException(apiId, categoryId);
        }
        return this.apiCategoryOrderRepository.findById(apiId, categoryId).map(CategoryAdapter.INSTANCE::toCoreModel);
    }

    @Override
    public ApiCategoryOrder get(String apiId, String categoryId) {
        return findById(apiId, categoryId).orElseThrow(() -> new ApiAndCategoryNotAssociatedException(apiId, categoryId));
    }
}
