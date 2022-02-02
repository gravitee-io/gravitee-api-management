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
package io.gravitee.rest.api.service.impl;

import static io.gravitee.repository.management.model.Category.AuditEvent.CATEGORY_DELETED;
import static io.gravitee.repository.management.model.Category.AuditEvent.CATEGORY_UPDATED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.CategoryRepository;
import io.gravitee.repository.management.model.Category;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.impl.CategoryServiceImpl;
import java.util.Optional;
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
public class CategoryService_DeleteTest {

    @InjectMocks
    private CategoryServiceImpl categoryService = new CategoryServiceImpl();

    @Mock
    private CategoryRepository mockCategoryRepository;

    @Mock
    private AuditService mockAuditService;

    @Mock
    private ApiService mockApiService;

    @Test
    public void shouldNotDeleteUnknownCategory() throws TechnicalException {
        when(mockCategoryRepository.findById("unknown")).thenReturn(Optional.empty());

        categoryService.delete("unknown");

        verify(mockCategoryRepository, times(1)).findById(any());
        verify(mockCategoryRepository, never()).delete(any());
        verify(mockAuditService, never()).createEnvironmentAuditLog(any(), any(), eq(CATEGORY_UPDATED), any(), any(), any());
        verify(mockApiService, never()).deleteCategoryFromAPIs(eq("unknown"));
    }

    @Test
    public void shouldDeleteCategory() throws TechnicalException {
        Category categoryToDelete = new Category();
        categoryToDelete.setId("known");
        categoryToDelete.setEnvironmentId("DEFAULT");
        when(mockCategoryRepository.findById("known")).thenReturn(Optional.of(categoryToDelete));

        categoryService.delete("known");

        verify(mockCategoryRepository, times(1)).findById("known");
        verify(mockCategoryRepository, times(1)).delete("known");
        verify(mockAuditService, times(1)).createEnvironmentAuditLog(eq("DEFAULT"), any(), eq(CATEGORY_DELETED), any(), any(), any());
        verify(mockApiService, times(1)).deleteCategoryFromAPIs(eq("known"));
    }
}
