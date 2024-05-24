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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import inmemory.CategoryServiceInMemory;
import io.gravitee.rest.api.model.CategoryEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.converter.CategoryMapper;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Sergii ILLICHEVSKYI (sergii.illichevskyi at graviteesource.com)
 * @author GraviteeSource Team
 */
public class CategoryMapperTest {

    private CategoryMapper categoryMapper;

    private CategoryServiceInMemory categoryService;

    private final String categoryId1 = "categoryId1";
    private final String categoryKey1 = "categoryKey1";
    private final String categoryId2 = "categoryId2";
    private final String categoryKey2 = "categoryKey2";
    private final CategoryEntity categoryEntity1 = new CategoryEntity();
    private final CategoryEntity categoryEntity2 = new CategoryEntity();
    private List<CategoryEntity> categories;

    @BeforeEach
    public void setUp() {
        categoryService = new CategoryServiceInMemory();
        categoryMapper = new CategoryMapper(categoryService);
        categoryEntity1.setId(categoryId1);
        categoryEntity1.setKey(categoryKey1);
        categoryEntity2.setId(categoryId2);
        categoryEntity2.setKey(categoryKey2);
        categories = List.of(categoryEntity1, categoryEntity2);
    }

    @Test
    public void shouldUpdateApiCategoriesToIdsWhenApiAndCategoriesKeyAndId() {
        Set<String> inputCategories = Set.of(categoryId1, categoryKey2);
        categoryService.initWith(categories);

        var result = categoryMapper.toCategoryId(GraviteeContext.getExecutionContext().getEnvironmentId(), inputCategories);

        assertThat(result).isNotNull();
        assertThat(result).containsExactlyInAnyOrder(categoryId1, categoryId2);
        assertThat(result).doesNotContain(categoryKey1, categoryKey2);
    }

    @Test
    public void shouldUpdateApiCategoriesToIdsWhenApiAndCategoriesIds() {
        Set<String> inputCategories = Set.of(categoryId1, categoryId2);

        categoryService.initWith(categories);

        var result = categoryMapper.toCategoryId(GraviteeContext.getExecutionContext().getEnvironmentId(), inputCategories);

        assertThat(result).isNotNull();
        assertThat(result).containsExactlyInAnyOrder(categoryId1, categoryId2);
        assertThat(result).doesNotContain(categoryKey1, categoryKey2);
    }

    @Test
    public void shouldUpdateApiCategoriesToIdsWhenApiAndCategoriesKeys() {
        Set<String> inputCategories = Set.of(categoryKey1, categoryKey2);

        categoryService.initWith(categories);

        var actualCategories = categoryMapper.toCategoryId(GraviteeContext.getExecutionContext().getEnvironmentId(), inputCategories);

        assertThat(actualCategories).isNotNull();
        assertThat(actualCategories).containsExactlyInAnyOrder(categoryId1, categoryId2);
        assertThat(actualCategories).doesNotContain(categoryKey1, categoryKey2);
    }

    @Test
    public void shouldUpdateApiCategoriesToKeysWhenApiAndCategoriesKeyAndId() {
        Set<String> inputCategories = Set.of(categoryId1, categoryKey2);
        categoryService.initWith(categories);

        var result = categoryMapper.toCategoryKey(GraviteeContext.getExecutionContext().getEnvironmentId(), inputCategories);

        assertThat(result).isNotNull();
        assertThat(result).containsExactlyInAnyOrder(categoryKey1, categoryKey2);
        assertThat(result).doesNotContain(categoryId1, categoryId2);
    }

    @Test
    public void shouldUpdateApiCategoriesToKeysWhenApiAndCategoriesIds() {
        Set<String> inputCategories = Set.of(categoryId1, categoryId2);

        categoryService.initWith(categories);

        var result = categoryMapper.toCategoryKey(GraviteeContext.getExecutionContext().getEnvironmentId(), inputCategories);

        assertThat(result).isNotNull();
        assertThat(result).containsExactlyInAnyOrder(categoryKey1, categoryKey2);
        assertThat(result).doesNotContain(categoryId1, categoryId2);
    }

    @Test
    public void shouldUpdateApiCategoriesToKeysWhenApiAndCategoriesKeys() {
        Set<String> inputCategories = Set.of(categoryKey1, categoryKey2);

        categoryService.initWith(categories);

        var result = categoryMapper.toCategoryKey(GraviteeContext.getExecutionContext().getEnvironmentId(), inputCategories);

        assertThat(result).isNotNull();
        assertThat(result).containsExactlyInAnyOrder(categoryKey1, categoryKey2);
        assertThat(result).doesNotContain(categoryId1, categoryId2);
    }

    @Test
    public void shouldNotUpdateApiCategoriesToIdsWhenCategoriesAreNull() {
        var actualCategories = categoryMapper.toCategoryId(GraviteeContext.getExecutionContext().getEnvironmentId(), null);
        assertThat(actualCategories).isNull();
    }

    @Test
    public void shouldReturnEmptySetWhenCategoriesAreEmpty() {
        Set<String> apiCategories = new HashSet<>();

        Set<String> result = categoryMapper.toCategoryId(GraviteeContext.getExecutionContext().getEnvironmentId(), apiCategories);
        assertEquals(apiCategories, result);
    }

    @Test
    public void shouldReturnEmptySetWhenCategoriesAreNull() {
        var actual = categoryMapper.toCategoryId(GraviteeContext.getExecutionContext().getEnvironmentId(), null);
        assertThat(actual).isNull();
    }

    @Test
    public void shouldNotUpdateApiCategoriesToKeysWhenCategoriesAreNull() {
        var actualCategories = categoryMapper.toCategoryKey(GraviteeContext.getExecutionContext().getEnvironmentId(), null);
        assertThat(actualCategories).isNull();
    }

    @Test
    public void shouldReturnEmptyKeysWhenCategoriesAreEmpty() {
        Set<String> apiCategories = new HashSet<>();

        Set<String> result = categoryMapper.toCategoryKey(GraviteeContext.getExecutionContext().getEnvironmentId(), apiCategories);
        assertEquals(apiCategories, result);
    }

    @Test
    public void shouldReturnEmptyKeysWhenCategoriesAreNull() {
        var actual = categoryMapper.toCategoryKey(GraviteeContext.getExecutionContext().getEnvironmentId(), null);
        assertThat(actual).isNull();
    }
}
