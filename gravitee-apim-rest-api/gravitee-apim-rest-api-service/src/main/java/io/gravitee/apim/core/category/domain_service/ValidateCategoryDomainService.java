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
package io.gravitee.apim.core.category.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.category.exception.CategoryNotFoundException;
import io.gravitee.apim.core.category.model.Category;
import io.gravitee.apim.core.category.query_service.CategoryQueryService;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@DomainService
@AllArgsConstructor
public class ValidateCategoryDomainService {

    private CategoryQueryService categoryQueryService;

    /**
     * Get the id for a category
     * If no key or id corresponds to categoryIdOrKey, then CategoryNotFoundException thrown.
     *
     * @param categoryIdOrKey -- Either the ID or Key of a Category
     * @param environmentId -- The environmentId of a Category
     * @return categoryId
     */
    public String validateCategoryIdOrKey(String categoryIdOrKey, String environmentId) {
        if (Objects.isNull(categoryIdOrKey)) {
            throw new CategoryNotFoundException(null);
        }

        return this.categoryQueryService.findByIdOrKey(categoryIdOrKey, environmentId)
            .map(Category::getId)
            .orElseThrow(() -> new CategoryNotFoundException(categoryIdOrKey));
    }
}
