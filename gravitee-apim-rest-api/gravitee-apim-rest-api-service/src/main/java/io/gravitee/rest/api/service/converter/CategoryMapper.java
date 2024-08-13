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
package io.gravitee.rest.api.service.converter;

import static java.util.Objects.nonNull;

import io.gravitee.rest.api.model.CategoryEntity;
import io.gravitee.rest.api.service.CategoryService;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.IdGenerator;

/**
 * @author Sergii ILLICHEVSKYI (sergii.illichevskyi at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@Component("Console")
public class CategoryMapper {

    private final CategoryService categoryService;

    public CategoryMapper(@Lazy CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    public Set<String> toCategoryId(String environmentId, Set<String> categories) {
        return toIdentifier(environmentId, categories, CategoryEntity::getId);
    }

    public Set<String> toCategoryKey(String environmentId, Set<String> categories) {
        return toIdentifier(environmentId, categories, CategoryEntity::getKey);
    }

    private Set<String> toIdentifier(
        String environmentId,
        Set<String> oldCategories,
        Function<CategoryEntity, String> identifierExtractor
    ) {
        if (CollectionUtils.isEmpty(oldCategories)) {
            return oldCategories;
        }

        return categoryService
            .findAll(environmentId)
            .stream()
            .filter(Objects::nonNull)
            .filter(dbCategory -> doesContain(oldCategories, dbCategory))
            .map(identifierExtractor)
            .collect(Collectors.toSet());
    }

    private boolean doesContain(Set<String> categories, CategoryEntity category) {
        return doesContain(categories, category, CategoryEntity::getId) || doesContain(categories, category, CategoryEntity::getKey);
    }

    private boolean doesContain(Set<String> categories, CategoryEntity category, Function<CategoryEntity, String> function) {
        return nonNull(function.apply(category)) && categories.contains(function.apply(category));
    }
}
