/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.service.v4.impl;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.rest.api.model.CategoryEntity;
import io.gravitee.rest.api.service.CategoryService;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.v4.ApiCategoryService;
import java.util.Collection;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
@Slf4j
public class ApiCategoryServiceImpl implements ApiCategoryService {

    private final ApiRepository apiRepository;
    private final CategoryService categoryService;

    public ApiCategoryServiceImpl(final ApiRepository apiRepository, final CategoryService categoryService) {
        this.apiRepository = apiRepository;
        this.categoryService = categoryService;
    }

    @Override
    public Set<CategoryEntity> listCategories(Collection<String> apis, String environment) {
        try {
            ApiCriteria criteria = new ApiCriteria.Builder().ids(apis.toArray(new String[apis.size()])).build();
            Set<String> categoryIds = apiRepository.listCategories(criteria);
            return categoryService.findByIdIn(environment, categoryIds);
        } catch (TechnicalException ex) {
            log.error("An error occurs while trying to list categories for APIs {}", apis, ex);
            throw new TechnicalManagementException("An error occurs while trying to list categories for APIs {}" + apis, ex);
        }
    }
}
