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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class CategoryService_CreateTest {

    @InjectMocks
    private CategoryServiceImpl categoryService = new CategoryServiceImpl();

    @Mock
    private CategoryRepository mockCategoryRepository;

    @Mock
    private AuditService mockAuditService;

    @Mock
    private EnvironmentService mockEnvironmentService;

    @BeforeEach
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

        assertNotNull(category, "result is null");
        verify(mockAuditService, times(1)).createAuditLog(
            eq(GraviteeContext.getExecutionContext()),
            argThat(auditLogData -> auditLogData.getEvent().equals(CATEGORY_CREATED) && auditLogData.getOldValue() == null)
        );
        verify(mockCategoryRepository, times(1)).create(argThat(arg -> arg != null && arg.getName().equals("v1")));
    }

    @Test
    public void shouldNotCreateCategoryBecauseEnvironmentDoesNotExist() throws TechnicalException {
        assertThrows(EnvironmentNotFoundException.class, () -> {
            when(mockEnvironmentService.findById(any())).thenThrow(EnvironmentNotFoundException.class);

            NewCategoryEntity nv1 = new NewCategoryEntity();
            nv1.setName("v1");
            categoryService.create(GraviteeContext.getExecutionContext(), nv1);
        });
    }

    @Test
    public void shouldNotCreateExistingCategory() throws TechnicalException {
        assertThrows(DuplicateCategoryNameException.class, () -> {
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
            Assertions.fail("should throw DuplicateCategoryNameException");
        });
    }

    @Test
    public void shouldNotCreateExistingCategoryBecauseSameName() throws TechnicalException {
        assertThrows(DuplicateCategoryNameException.class, () -> {
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
            Assertions.fail("should throw DuplicateCategoryNameException");
        });
    }
}
