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
package io.gravitee.rest.api.service;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.CategoryRepository;
import io.gravitee.repository.management.model.Category;
import io.gravitee.rest.api.model.CategoryEntity;
import io.gravitee.rest.api.service.impl.CategoryServiceImpl;
import java.util.Date;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class CategoryService_FindTest {

    @InjectMocks
    private CategoryServiceImpl categoryService = new CategoryServiceImpl();

    @Mock
    private CategoryRepository mockCategoryRepository;

    @Test
    public void shouldDoNothingWithEmptyResult() throws TechnicalException {
        when(mockCategoryRepository.findAllByEnvironment(any())).thenReturn(emptySet());

        List<CategoryEntity> list = categoryService.findAll();

        assertTrue(list.isEmpty());
        verify(mockCategoryRepository, times(1)).findAllByEnvironment(any());
    }

    @Test
    public void shouldFindCategory() throws TechnicalException {
        Category category = mock(Category.class);
        when(category.getId()).thenReturn("category-id");
        when(category.getName()).thenReturn("category-name");
        when(category.getDescription()).thenReturn("category-description");
        when(category.getOrder()).thenReturn(1);
        when(category.isHidden()).thenReturn(true);
        when(category.getUpdatedAt()).thenReturn(new Date(1234567890L));
        when(category.getCreatedAt()).thenReturn(new Date(9876543210L));
        when(mockCategoryRepository.findAllByEnvironment(any())).thenReturn(singleton(category));

        List<CategoryEntity> list = categoryService.findAll();

        assertFalse(list.isEmpty());
        assertEquals("one element", 1, list.size());
        assertEquals("Id", "category-id", list.get(0).getId());
        assertEquals("Name", "category-name", list.get(0).getName());
        assertEquals("Description", "category-description", list.get(0).getDescription());
        assertEquals("Total APIs", 0, list.get(0).getTotalApis());
        assertEquals("Order", 1, list.get(0).getOrder());
        assertEquals("Hidden", true, list.get(0).isHidden());
        assertEquals("UpdatedAt", new Date(1234567890L), list.get(0).getUpdatedAt());
        assertEquals("CreatedAt", new Date(9876543210L), list.get(0).getCreatedAt());
        verify(mockCategoryRepository, times(1)).findAllByEnvironment(any());
    }
}
