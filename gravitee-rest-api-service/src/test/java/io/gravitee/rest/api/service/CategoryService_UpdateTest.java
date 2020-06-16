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

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.CategoryRepository;
import io.gravitee.repository.management.model.Category;
import io.gravitee.rest.api.model.UpdateCategoryEntity;
import io.gravitee.rest.api.model.CategoryEntity;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.exceptions.CategoryNotFoundException;
import io.gravitee.rest.api.service.impl.CategoryServiceImpl;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static io.gravitee.repository.management.model.Category.AuditEvent.CATEGORY_UPDATED;
import static java.util.Collections.singletonList;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class CategoryService_UpdateTest {

    @InjectMocks
    private CategoryServiceImpl categoryService = new CategoryServiceImpl();

    @Mock
    private CategoryRepository mockCategoryRepository;

    @Mock
    private AuditService mockAuditService;

    @Test
    public void shouldNotUpdateUnknownCategory_multi_mode() throws TechnicalException {
        UpdateCategoryEntity mockCategory = mock(UpdateCategoryEntity.class);
        when(mockCategory.getId()).thenReturn("unknown");
        when(mockCategoryRepository.findById("unknown")).thenReturn(Optional.empty());

        List<CategoryEntity> list = categoryService.update(singletonList(mockCategory));

        assertTrue(list.isEmpty());
        verify(mockCategoryRepository, times(1)).findById(any());
        verify(mockCategoryRepository, never()).update(any());
        verify(mockAuditService, never()).createEnvironmentAuditLog(any(), eq(CATEGORY_UPDATED), any(), any(), any());
    }

    @Test(expected = CategoryNotFoundException.class)
    public void shouldNotUpdateUnknownCategory_single_mode() throws TechnicalException {
        UpdateCategoryEntity mockCategory = mock(UpdateCategoryEntity.class);
        when(mockCategoryRepository.findById("unknown")).thenReturn(Optional.empty());

        categoryService.update("unknown", mockCategory);

        verify(mockCategoryRepository, times(1)).findById(any());
        verify(mockCategoryRepository, never()).update(any());
        verify(mockAuditService, never()).createEnvironmentAuditLog(any(), eq(CATEGORY_UPDATED), any(), any(), any());
    }

    @Test
    public void shouldUpdateCategory_multi_mode() throws TechnicalException {
        UpdateCategoryEntity mockCategory = mock(UpdateCategoryEntity.class);
        when(mockCategory.getId()).thenReturn("known");
        when(mockCategory.getName()).thenReturn("Known");
        when(mockCategoryRepository.findById("known")).thenReturn(Optional.of(new Category()));
        Category updatedCategory = mock(Category.class);
        when(updatedCategory.getId()).thenReturn("category-id");
        when(updatedCategory.getName()).thenReturn("category-name");
        when(updatedCategory.getDescription()).thenReturn("category-description");
        when(updatedCategory.getOrder()).thenReturn(1);
        when(updatedCategory.isHidden()).thenReturn(true);
        when(updatedCategory.getUpdatedAt()).thenReturn(new Date(1234567890L));
        when(updatedCategory.getCreatedAt()).thenReturn(new Date(9876543210L));
        when(mockCategoryRepository.update(any())).thenReturn(updatedCategory);

        List<CategoryEntity> list = categoryService.update(singletonList(mockCategory));

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
        verify(mockCategoryRepository, times(1)).findById(any());
        verify(mockCategoryRepository, times(1)).update(any());
        verify(mockAuditService, times(1)).createEnvironmentAuditLog(any(), eq(CATEGORY_UPDATED), any(), any(), any());
    }

    @Test
    public void shouldUpdateCategory_single_mode() throws TechnicalException {
        UpdateCategoryEntity mockCategory = mock(UpdateCategoryEntity.class);
        when(mockCategory.getId()).thenReturn("category-id");
        when(mockCategory.getName()).thenReturn("Category ID");
        when(mockCategoryRepository.findById("category-id")).thenReturn(Optional.of(new Category()));
        Category updatedCategory = mock(Category.class);
        when(updatedCategory.getId()).thenReturn("category-id");
        when(updatedCategory.getName()).thenReturn("category-name");
        when(updatedCategory.getDescription()).thenReturn("category-description");
        when(updatedCategory.getOrder()).thenReturn(1);
        when(updatedCategory.isHidden()).thenReturn(true);
        when(updatedCategory.getUpdatedAt()).thenReturn(new Date(1234567890L));
        when(updatedCategory.getCreatedAt()).thenReturn(new Date(9876543210L));
        when(mockCategoryRepository.update(any())).thenReturn(updatedCategory);

        CategoryEntity category = categoryService.update("category-id", mockCategory);

        assertNotNull(category);
        assertEquals("Id", "category-id", category.getId());
        assertEquals("Name", "category-name", category.getName());
        assertEquals("Description", "category-description", category.getDescription());
        assertEquals("Total APIs", 0, category.getTotalApis());
        assertEquals("Order", 1, category.getOrder());
        assertEquals("Hidden", true, category.isHidden());
        assertEquals("UpdatedAt", new Date(1234567890L), category.getUpdatedAt());
        assertEquals("CreatedAt", new Date(9876543210L), category.getCreatedAt());
        verify(mockCategoryRepository, times(1)).findById(any());
        verify(mockCategoryRepository, times(1)).update(any());
        verify(mockAuditService, times(1)).createEnvironmentAuditLog(any(), eq(CATEGORY_UPDATED), any(), any(), any());
    }
}

