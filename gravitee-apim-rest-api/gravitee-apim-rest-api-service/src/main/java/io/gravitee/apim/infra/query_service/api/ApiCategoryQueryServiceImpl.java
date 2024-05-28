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
package io.gravitee.apim.infra.query_service.api;

import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.query_service.ApiCategoryQueryService;
import io.gravitee.apim.core.category.model.Category;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.CategoryRepository;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ApiCategoryQueryServiceImpl implements ApiCategoryQueryService {

    private final CategoryRepository categoryRepository;

    public ApiCategoryQueryServiceImpl(@Lazy CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public Collection<String> findApiCategoryKeys(Api api) {
        try {
            var categoriesIds = api.getCategories();
            if (categoriesIds == null || categoriesIds.isEmpty()) {
                return Collections.emptySet();
            }

            // Uncomment when https://gravitee.atlassian.net/browse/APIM-4437 is fixed
            //            return categoryRepository
            //                .findByEnvironmentIdAndIdIn(api.getEnvironmentId(), categoriesIds)
            //                .stream()
            //                .map(io.gravitee.repository.management.model.Category::getKey)
            //                .collect(Collectors.toSet());

            return categoryRepository
                .findAllByEnvironment(api.getEnvironmentId())
                .stream()
                .filter(category -> categoriesIds.contains(category.getId()) || categoriesIds.contains(category.getKey()))
                .map(io.gravitee.repository.management.model.Category::getKey)
                .collect(Collectors.toSet());
        } catch (TechnicalException e) {
            throw new TechnicalDomainException("An error occurs while trying to find Categories for the api: " + api.getId(), e);
        }
    }

    @Override
    public Collection<Category> findByEnvironmentId(String environmentId) {
        try {
            return categoryRepository
                .findAllByEnvironment(environmentId)
                .stream()
                .map(category -> Category.builder().key(category.getKey()).name(category.getName()).id(category.getId()).build())
                .toList();
        } catch (TechnicalException e) {
            throw new TechnicalDomainException("Cannot find categories for environment " + environmentId, e);
        }
    }
}
