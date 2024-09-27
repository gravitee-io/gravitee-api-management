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
package io.gravitee.rest.api.service.impl;

import static io.gravitee.repository.management.model.Category.AuditEvent.CATEGORY_CREATED;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.gravitee.common.utils.IdGenerator;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.CategoryRepository;
import io.gravitee.repository.management.model.Category;
import io.gravitee.rest.api.model.CategoryEntity;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.NewCategoryEntity;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.DuplicateCategoryNameException;
import io.gravitee.rest.api.service.exceptions.EnvironmentNotFoundException;
import java.util.Collections;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class CategoryService_CreateTest {

    @InjectMocks
    private CategoryServiceImpl categoryService = new CategoryServiceImpl();

    @Mock
    private CategoryRepository mockCategoryRepository;

    @Mock
    private AuditService mockAuditService;

    @Mock
    private EnvironmentService mockEnvironmentService;

    @Before
    public void setUp() throws Exception {
        GraviteeContext.cleanContext();
    }

    @Test
    public void shouldCreateCategory() throws TechnicalException {
        NewCategoryEntity v1 = new NewCategoryEntity();
        v1.setName("v1");
        when(mockCategoryRepository.create(argThat(cat -> cat.getCreatedAt() != null))).thenReturn(new Category());
        when(mockEnvironmentService.findById("DEFAULT")).thenReturn(new EnvironmentEntity());
        CategoryEntity category = categoryService.create(GraviteeContext.getExecutionContext(), v1);

        assertNotNull("result is null", category);
        verify(mockAuditService, times(1))
            .createAuditLog(eq(GraviteeContext.getExecutionContext()), any(), eq(CATEGORY_CREATED), any(), isNull(), any());
        verify(mockCategoryRepository, times(1)).create(argThat(arg -> arg != null && arg.getName().equals("v1")));
    }

    @Test(expected = EnvironmentNotFoundException.class)
    public void shouldNotCreateCategoryBecauseEnvironmentDoesNotExist() throws TechnicalException {
        when(mockEnvironmentService.findById(any())).thenThrow(EnvironmentNotFoundException.class);

        NewCategoryEntity nv1 = new NewCategoryEntity();
        nv1.setName("v1");
        categoryService.create(GraviteeContext.getExecutionContext(), nv1);
    }

    @Test(expected = DuplicateCategoryNameException.class)
    public void shouldNotCreateExistingCategory() throws TechnicalException {
        Category v1 = new Category();
        NewCategoryEntity nv1 = new NewCategoryEntity();
        v1.setName("v1");
        v1.setKey(IdGenerator.generate(v1.getName()));
        nv1.setName("v1");
        when(mockCategoryRepository.findAllByEnvironment(any())).thenReturn(Collections.singleton(v1));

        try {
            categoryService.create(GraviteeContext.getExecutionContext(), nv1);
        } catch (DuplicateCategoryNameException e) {
            verify(mockCategoryRepository, never()).create(any());
            throw e;
        }
        Assert.fail("should throw DuplicateCategoryNameException");
    }

    @Test(expected = DuplicateCategoryNameException.class)
    public void shouldNotCreateExistingCategoryBecauseSameName() throws TechnicalException {
        Category v1 = new Category();
        NewCategoryEntity nv1 = new NewCategoryEntity();
        v1.setName("A Name With Capital Letters");
        v1.setKey(IdGenerator.generate(v1.getName()));
        nv1.setName("A name with CAPITAL letters");
        when(mockCategoryRepository.findAllByEnvironment(any())).thenReturn(Collections.singleton(v1));

        try {
            categoryService.create(GraviteeContext.getExecutionContext(), nv1);
        } catch (DuplicateCategoryNameException e) {
            verify(mockCategoryRepository, never()).create(any());
            throw e;
        }
        Assert.fail("should throw DuplicateCategoryNameException");
    }
}
