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
package io.gravitee.apim.infra.query_service.category;

import io.gravitee.apim.core.category.model.ApiCategoryOrder;
import io.gravitee.apim.core.category.query_service.ApiCategoryOrderQueryService;
import io.gravitee.apim.infra.adapter.CategoryAdapter;
import io.gravitee.repository.management.api.ApiCategoryOrderRepository;
import java.util.Set;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class ApiCategoryOrderQueryServiceImpl implements ApiCategoryOrderQueryService {

    private final ApiCategoryOrderRepository apiCategoryOrderRepository;

    public ApiCategoryOrderQueryServiceImpl(@Lazy ApiCategoryOrderRepository apiCategoryOrderRepository) {
        this.apiCategoryOrderRepository = apiCategoryOrderRepository;
    }

    @Override
    public Set<ApiCategoryOrder> findAllByCategoryId(String categoryId) {
        return CategoryAdapter.INSTANCE.toCoreModel(this.apiCategoryOrderRepository.findAllByCategoryId(categoryId));
    }
}
