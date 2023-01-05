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
package io.gravitee.rest.api.service.v4.impl;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.ApiFieldFilter;
import io.gravitee.repository.management.model.Api;
import io.gravitee.rest.api.model.CategoryEntity;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.CategoryService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.v4.ApiCategoryService;
import io.gravitee.rest.api.service.v4.ApiNotificationService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiCategoryServiceImplTest {

    private ApiCategoryService apiCategoryService;

    @Mock
    private ApiRepository apiRepository;

    @Mock
    private CategoryService categoryService;

    @Mock
    private ApiNotificationService apiNotificationService;

    @Mock
    private AuditService auditService;

    @Before
    public void before() {
        apiCategoryService = new ApiCategoryServiceImpl(apiRepository, categoryService, apiNotificationService, auditService);
    }

    @Test
    public void shouldReturnCategories() throws TechnicalException {
        Set<String> categories = Set.of("category1");
        when(apiRepository.listCategories(any())).thenReturn(categories);
        CategoryEntity categoryEntity = new CategoryEntity();
        categoryEntity.setId("category1");
        when(categoryService.findByIdIn("DEFAULT", categories)).thenReturn(Set.of(categoryEntity));
        Set<CategoryEntity> categoryEntities = apiCategoryService.listCategories(List.of("api1"), "DEFAULT");
        assertThat(categoryEntities).isNotNull();
        assertThat(categoryEntities.size()).isEqualTo(1);
        assertThat(categoryEntities.contains(categoryEntity)).isTrue();
    }

    @Test
    public void shouldThrownManagementExceptionWhenTechnicalExceptionOccurred() throws TechnicalException {
        when(apiRepository.listCategories(any())).thenThrow(new TechnicalException());
        assertThatExceptionOfType(TechnicalManagementException.class)
            .isThrownBy(() -> apiCategoryService.listCategories(List.of("api1"), "DEFAULT"));
    }

    @Test
    public void shouldDeleteCategoryFromApis() throws TechnicalException {
        final String categoryId = UuidString.generateRandom();

        Api firstOrphan = new Api();
        firstOrphan.setId(UuidString.generateRandom());
        firstOrphan.setCategories(new HashSet<>(Set.of(categoryId)));

        Api secondOrphan = new Api();
        secondOrphan.setId(UuidString.generateRandom());
        secondOrphan.setCategories(new HashSet<>(Set.of(UuidString.generateRandom(), categoryId)));

        when(apiRepository.search(new ApiCriteria.Builder().category(categoryId).build(), null, ApiFieldFilter.allFields()))
            .thenReturn(Stream.of(firstOrphan, secondOrphan));
        apiCategoryService.deleteCategoryFromAPIs(GraviteeContext.getExecutionContext(), categoryId);

        verify(apiRepository, times(1)).update(firstOrphan);
        verify(apiRepository, times(1)).update(secondOrphan);

        assertEquals(0, firstOrphan.getCategories().size());
        assertEquals(1, secondOrphan.getCategories().size());
    }
}
