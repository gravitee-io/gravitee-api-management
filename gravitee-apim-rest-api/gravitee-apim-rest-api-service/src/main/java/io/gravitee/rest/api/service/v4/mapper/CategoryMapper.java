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
package io.gravitee.rest.api.service.v4.mapper;

import io.gravitee.rest.api.model.CategoryEntity;
import io.gravitee.rest.api.service.CategoryService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component("CategoryMapperV4")
@Slf4j
public class CategoryMapper {

    private final CategoryService categoryService;

    public CategoryMapper(@Lazy final CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    public Set<String> toIdentifier(
        final ExecutionContext executionContext,
        final Set<String> apiCategories,
        List<CategoryEntity> categories
    ) {
        if (apiCategories != null) {
            if (categories == null) {
                categories = categoryService.findAll(executionContext.getEnvironmentId());
            }
            final Set<String> newApiCategories = new HashSet<>(apiCategories.size());
            for (final String apiView : apiCategories) {
                categories
                    .stream()
                    .filter(categoryEntity -> apiView.equals(categoryEntity.getId()) || apiView.equals(categoryEntity.getKey()))
                    .forEach(categoryEntity -> newApiCategories.add(categoryEntity.getKey()));
            }
            return newApiCategories;
        }
        return null;
    }
}
