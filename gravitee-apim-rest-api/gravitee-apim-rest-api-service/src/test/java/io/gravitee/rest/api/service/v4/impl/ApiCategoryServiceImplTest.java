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
package io.gravitee.rest.api.service.v4.impl;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import inmemory.CategoryServiceInMemory;
import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.ApiFieldFilter;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Category;
import io.gravitee.rest.api.model.CategoryEntity;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.v4.ApiCategoryService;
import io.gravitee.rest.api.service.v4.ApiNotificationService;
import java.util.*;
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

    private CategoryServiceInMemory categoryService;

    @Mock
    private ApiNotificationService apiNotificationService;

    @Mock
    private AuditService auditService;

    @Mock
    private MembershipService membershipService;

    @Mock
    private RoleService roleService;

    @Before
    public void before() {
        categoryService = new CategoryServiceInMemory();
        apiCategoryService =
            new ApiCategoryServiceImpl(
                apiRepository,
                categoryService,
                apiNotificationService,
                auditService,
                membershipService,
                roleService
            );
    }

    @Test
    public void shouldReturnCategories() throws TechnicalException {
        String category1 = "category1";
        Map<String, Integer> categories = Map.of(category1, 1);
        when(apiRepository.listCategories(any())).thenReturn(categories);
        CategoryEntity categoryEntity = new CategoryEntity();
        categoryEntity.setId(category1);
        categoryService.initWith(List.of(categoryEntity));

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

    @Test
    public void shouldRetrieveUserApiCountsGroupedByCategory() {
        final Category category1 = new Category();
        category1.setId("category-1");

        final Category category2 = new Category();
        category2.setId("category-2");

        final Category category3 = new Category();
        category3.setId("category-3");

        final Api api1 = new Api();
        api1.setId("api-1");
        api1.setCategories(Set.of(category1.getId()));

        final Api api2 = new Api();
        api2.setId("api-2");
        api2.setCategories(Set.of(category1.getId(), category2.getId()));

        final Api api3 = new Api();
        api3.setId("api-3");
        api3.setCategories(Set.of(category1.getId(), category2.getId(), category3.getId()));

        final Api api4 = new Api();
        api4.setId("api-4");

        when(apiRepository.searchIds(anyList(), isA(Pageable.class), eq(null)))
            .thenReturn(new Page<>(Arrays.asList(api1.getId(), api2.getId(), api3.getId(), api4.getId()), 0, 4, 4));
        when(apiRepository.search(any(ApiCriteria.class), isNull(), any(ApiFieldFilter.class)))
            .thenReturn(Stream.of(api1, api2, api3, api4));

        Map<String, Long> expectedCounts = Map.of(category1.getId(), 3L, category2.getId(), 2L, category3.getId(), 1L);
        Map<String, Long> counts = apiCategoryService.countApisPublishedGroupedByCategoriesForUser("user-1");

        assertEquals(expectedCounts, counts);
    }
}
