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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.rest.api.model.CategoryEntity;
import io.gravitee.rest.api.service.CategoryService;
import io.gravitee.rest.api.service.exceptions.EndpointNameInvalidException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.v4.ApiCategoryService;
import java.util.List;
import java.util.Set;
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

    @Before
    public void before() {
        apiCategoryService = new ApiCategoryServiceImpl(apiRepository, categoryService);
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
}
