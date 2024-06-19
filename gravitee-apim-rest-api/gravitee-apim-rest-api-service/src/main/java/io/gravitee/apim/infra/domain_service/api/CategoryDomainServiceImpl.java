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
package io.gravitee.apim.infra.domain_service.api;

import io.gravitee.apim.core.api.domain_service.CategoryDomainService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiCategoryOrderRepository;
import io.gravitee.repository.management.model.ApiCategoryOrder;
import io.gravitee.rest.api.service.converter.CategoryMapper;
import java.util.Collection;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * @author Sergii ILLICHEVSKYI (sergii.illichevskyi at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@Service
public class CategoryDomainServiceImpl implements CategoryDomainService {

    private final CategoryMapper categoryMapper;
    private final ApiCategoryOrderRepository apiCategoryOrderRepository;

    public CategoryDomainServiceImpl(CategoryMapper categoryMapper, @Lazy ApiCategoryOrderRepository apiCategoryOrderRepository) {
        this.categoryMapper = categoryMapper;
        this.apiCategoryOrderRepository = apiCategoryOrderRepository;
    }

    @Override
    public Set<String> toCategoryId(Api api, String environmentId) {
        return categoryMapper.toCategoryId(environmentId, api.getCategories());
    }

    @Override
    public Set<String> toCategoryKey(Api api, String environmentId) {
        return categoryMapper.toCategoryKey(environmentId, api.getCategories());
    }

    @Override
    public void updateOrderCategoriesOfApi(String apiId, Collection<String> categoryIds) {
        try {
            var previousCategories = apiCategoryOrderRepository.findAllByApiId(apiId);
            apiCategoryOrderRepository.delete(apiId, previousCategories.stream().map(ApiCategoryOrder::getCategoryId).toList());

            int index = 0;
            for (String categoryId : categoryIds) {
                ApiCategoryOrder categoryOrder = ApiCategoryOrder.builder().categoryId(categoryId).apiId(apiId).order(index++).build();
                apiCategoryOrderRepository.create(categoryOrder);
            }
        } catch (TechnicalException ex) {
            log.error("Impossible to update order categories of API", ex);
        }
    }
}
