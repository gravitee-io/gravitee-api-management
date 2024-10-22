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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchException;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import inmemory.CategoryServiceInMemory;
import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiCategoryOrderRepository;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.ApiFieldFilter;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.ApiCategoryOrder;
import io.gravitee.repository.management.model.Category;
import io.gravitee.rest.api.model.CategoryEntity;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.v4.ApiNotificationService;
import io.gravitee.rest.api.service.v4.exception.ApiNotInCategoryException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
public class ApiCategoryServiceImplTest {

    private static final String API_ID = "api-id";
    private static final String CATEGORY_ID = "cat-id";

    @InjectMocks
    private ApiCategoryServiceImpl apiCategoryService;

    @Mock
    private ApiRepository apiRepository;

    @Mock
    private ApiCategoryOrderRepository apiCategoryOrderRepository;

    private CategoryServiceInMemory categoryService;

    @Mock
    private ApiNotificationService apiNotificationService;

    @Mock
    private AuditService auditService;

    @Mock
    private MembershipService membershipService;

    @Mock
    private RoleService roleService;

    @BeforeEach
    public void before() {
        categoryService = new CategoryServiceInMemory();
        apiCategoryService =
            new ApiCategoryServiceImpl(
                apiRepository,
                apiCategoryOrderRepository,
                categoryService,
                apiNotificationService,
                auditService,
                membershipService,
                roleService
            );
    }

    @Nested
    public class ListCategories {

        @Test
        public void shouldReturnCategories() throws TechnicalException {
            String category1 = "category1";
            Set<String> categories = Set.of(category1);
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
            var ex = catchException(() -> apiCategoryService.listCategories(List.of("api1"), "DEFAULT"));
            assertThat(ex).isInstanceOf(TechnicalManagementException.class);
        }
    }

    @Nested
    class DeleteCategoryFromApis {

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

            assertThat(firstOrphan.getCategories()).isEmpty();
            assertThat(secondOrphan.getCategories()).hasSize(1);

            verify(apiCategoryOrderRepository, times(1)).delete(eq(firstOrphan.getId()), eq(categoryId));
            verify(apiCategoryOrderRepository, times(1)).delete(eq(secondOrphan.getId()), eq(categoryId));
        }

        @Test
        public void shouldDeleteApiCategoryReferences() throws TechnicalException {
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
            verify(apiRepository, times(1)).update(firstOrphan);
            verify(apiNotificationService, times(1)).triggerUpdateNotification(GraviteeContext.getExecutionContext(), firstOrphan);
            verify(apiNotificationService, times(1)).triggerUpdateNotification(GraviteeContext.getExecutionContext(), secondOrphan);
            verify(auditService, times(1))
                .createApiAuditLog(
                    eq(GraviteeContext.getExecutionContext()),
                    eq(firstOrphan.getId()),
                    eq(Collections.emptyMap()),
                    eq(Api.AuditEvent.API_UPDATED),
                    any(),
                    any(),
                    any()
                );
            verify(auditService, times(1))
                .createApiAuditLog(
                    eq(GraviteeContext.getExecutionContext()),
                    eq(secondOrphan.getId()),
                    eq(Collections.emptyMap()),
                    eq(Api.AuditEvent.API_UPDATED),
                    any(),
                    any(),
                    any()
                );

            assertThat(firstOrphan.getCategories()).isEmpty();
            assertThat(secondOrphan.getCategories()).hasSize(1);
        }
    }

    @Nested
    class CountApisPublishedGroupedByCategoriesForUser {

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
            var counts = apiCategoryService.countApisPublishedGroupedByCategoriesForUser("user-1");

            expectedCounts.forEach((catId, expectedCount) -> assertThat(counts.applyAsLong(catId)).isEqualTo(expectedCount));
            assertThat(counts.applyAsLong("unexistingCategory")).isZero();
        }
    }

    @Nested
    class AddApiToCategories {

        @Test
        void shouldAddApiIfNoApiCategoryOrdersExist() throws Exception {
            when(apiCategoryOrderRepository.findAllByCategoryId(eq(CATEGORY_ID))).thenReturn(Set.of());
            apiCategoryService.addApiToCategories(API_ID, Set.of(CATEGORY_ID));
            verify(apiCategoryOrderRepository, times(1))
                .create(eq(ApiCategoryOrder.builder().order(0).apiId(API_ID).categoryId(CATEGORY_ID).build()));
        }

        @Test
        void shouldAddApiToEndIfApiCategoryOrdersExist() throws Exception {
            when(apiCategoryOrderRepository.findAllByCategoryId(eq(CATEGORY_ID)))
                .thenReturn(
                    Set.of(
                        ApiCategoryOrder.builder().categoryId(CATEGORY_ID).apiId("api1").order(0).build(),
                        ApiCategoryOrder.builder().categoryId(CATEGORY_ID).order(1).apiId("api2").build()
                    )
                );
            apiCategoryService.addApiToCategories(API_ID, Set.of(CATEGORY_ID));
            verify(apiCategoryOrderRepository, times(1))
                .create(eq(ApiCategoryOrder.builder().order(2).apiId(API_ID).categoryId(CATEGORY_ID).build()));
        }

        @Test
        void shouldNotAddApiToCategoryIfAlreadyExists() throws Exception {
            when(apiCategoryOrderRepository.findAllByCategoryId(eq(CATEGORY_ID)))
                .thenReturn(Set.of(ApiCategoryOrder.builder().categoryId(CATEGORY_ID).apiId(API_ID).order(0).build()));
            apiCategoryService.addApiToCategories(API_ID, Set.of(CATEGORY_ID));
            verify(apiCategoryOrderRepository, never()).create(any());
        }

        @Test
        void shouldAddApiToMultipleCategories() throws Exception {
            var OTHER_CATEGORY = "other-category";
            when(apiCategoryOrderRepository.findAllByCategoryId(eq(CATEGORY_ID)))
                .thenReturn(Set.of(ApiCategoryOrder.builder().categoryId(CATEGORY_ID).apiId("api1").order(0).build()));
            when(apiCategoryOrderRepository.findAllByCategoryId(eq(OTHER_CATEGORY)))
                .thenReturn(Set.of(ApiCategoryOrder.builder().categoryId(OTHER_CATEGORY).apiId("api1").order(0).build()));
            apiCategoryService.addApiToCategories(API_ID, Set.of(CATEGORY_ID, OTHER_CATEGORY));
            verify(apiCategoryOrderRepository, times(1))
                .create(eq(ApiCategoryOrder.builder().order(1).apiId(API_ID).categoryId(CATEGORY_ID).build()));
            verify(apiCategoryOrderRepository, times(1))
                .create(eq(ApiCategoryOrder.builder().order(1).apiId(API_ID).categoryId(OTHER_CATEGORY).build()));
        }

        @Test
        void shouldDoNothingIfCategoryIdsEmpty() throws Exception {
            apiCategoryService.addApiToCategories(API_ID, Set.of());
            verify(apiCategoryOrderRepository, never()).findAllByCategoryId(any());
            verify(apiCategoryOrderRepository, never()).create(any());
        }
    }

    @Nested
    class ChangeApiOrderInCategory {

        @Test
        void shouldMoveApiCategoryDownMultiplePlaces() throws Exception {
            when(apiCategoryOrderRepository.findAllByCategoryId(eq(CATEGORY_ID)))
                .thenReturn(
                    Set.of(
                        ApiCategoryOrder.builder().order(0).apiId(API_ID).categoryId(CATEGORY_ID).build(),
                        ApiCategoryOrder.builder().order(1).apiId("api2").categoryId(CATEGORY_ID).build(),
                        ApiCategoryOrder.builder().order(2).apiId("api3").categoryId(CATEGORY_ID).build()
                    )
                );
            apiCategoryService.changeApiOrderInCategory(API_ID, CATEGORY_ID, 2);
            verify(apiCategoryOrderRepository, times(1))
                .update(eq(ApiCategoryOrder.builder().order(2).apiId(API_ID).categoryId(CATEGORY_ID).build()));
            verify(apiCategoryOrderRepository, times(1))
                .update(eq(ApiCategoryOrder.builder().order(0).apiId("api2").categoryId(CATEGORY_ID).build()));
            verify(apiCategoryOrderRepository, times(1))
                .update(eq(ApiCategoryOrder.builder().order(1).apiId("api3").categoryId(CATEGORY_ID).build()));
        }

        @Test
        void shouldMoveApiCategoryUpMultiplePlaces() throws Exception {
            when(apiCategoryOrderRepository.findAllByCategoryId(eq(CATEGORY_ID)))
                .thenReturn(
                    Set.of(
                        ApiCategoryOrder.builder().order(0).apiId("api2").categoryId(CATEGORY_ID).build(),
                        ApiCategoryOrder.builder().order(1).apiId("api3").categoryId(CATEGORY_ID).build(),
                        ApiCategoryOrder.builder().order(2).apiId(API_ID).categoryId(CATEGORY_ID).build()
                    )
                );
            apiCategoryService.changeApiOrderInCategory(API_ID, CATEGORY_ID, 0);
            verify(apiCategoryOrderRepository, times(1))
                .update(eq(ApiCategoryOrder.builder().order(0).apiId(API_ID).categoryId(CATEGORY_ID).build()));
            verify(apiCategoryOrderRepository, times(1))
                .update(eq(ApiCategoryOrder.builder().order(1).apiId("api2").categoryId(CATEGORY_ID).build()));
            verify(apiCategoryOrderRepository, times(1))
                .update(eq(ApiCategoryOrder.builder().order(2).apiId("api3").categoryId(CATEGORY_ID).build()));
        }

        @Test
        void shouldMoveApiCategoryDownByOnePlace() throws Exception {
            when(apiCategoryOrderRepository.findAllByCategoryId(eq(CATEGORY_ID)))
                .thenReturn(
                    Set.of(
                        ApiCategoryOrder.builder().order(0).apiId(API_ID).categoryId(CATEGORY_ID).build(),
                        ApiCategoryOrder.builder().order(1).apiId("api2").categoryId(CATEGORY_ID).build(),
                        ApiCategoryOrder.builder().order(2).apiId("api3").categoryId(CATEGORY_ID).build()
                    )
                );
            apiCategoryService.changeApiOrderInCategory(API_ID, CATEGORY_ID, 1);

            verify(apiCategoryOrderRepository, times(2)).update(any());

            verify(apiCategoryOrderRepository, times(1))
                .update(eq(ApiCategoryOrder.builder().order(1).apiId(API_ID).categoryId(CATEGORY_ID).build()));
            verify(apiCategoryOrderRepository, times(1))
                .update(eq(ApiCategoryOrder.builder().order(0).apiId("api2").categoryId(CATEGORY_ID).build()));
        }

        @Test
        void shouldMoveApiCategoryUpOnePlace() throws Exception {
            when(apiCategoryOrderRepository.findAllByCategoryId(eq(CATEGORY_ID)))
                .thenReturn(
                    Set.of(
                        ApiCategoryOrder.builder().order(0).apiId("api2").categoryId(CATEGORY_ID).build(),
                        ApiCategoryOrder.builder().order(1).apiId("api3").categoryId(CATEGORY_ID).build(),
                        ApiCategoryOrder.builder().order(2).apiId(API_ID).categoryId(CATEGORY_ID).build()
                    )
                );
            apiCategoryService.changeApiOrderInCategory(API_ID, CATEGORY_ID, 1);

            verify(apiCategoryOrderRepository, times(2)).update(any());

            verify(apiCategoryOrderRepository, times(1))
                .update(eq(ApiCategoryOrder.builder().order(1).apiId(API_ID).categoryId(CATEGORY_ID).build()));
            verify(apiCategoryOrderRepository, times(1))
                .update(eq(ApiCategoryOrder.builder().order(2).apiId("api3").categoryId(CATEGORY_ID).build()));
        }

        @Test
        void shouldDoNothingIfNewOrderIsTheSame() throws Exception {
            when(apiCategoryOrderRepository.findAllByCategoryId(eq(CATEGORY_ID)))
                .thenReturn(
                    Set.of(
                        ApiCategoryOrder.builder().order(0).apiId("api2").categoryId(CATEGORY_ID).build(),
                        ApiCategoryOrder.builder().order(1).apiId("api3").categoryId(CATEGORY_ID).build(),
                        ApiCategoryOrder.builder().order(2).apiId(API_ID).categoryId(CATEGORY_ID).build()
                    )
                );
            apiCategoryService.changeApiOrderInCategory(API_ID, CATEGORY_ID, 2);
            verify(apiCategoryOrderRepository, never()).update(any());
        }

        @Test
        void shouldNotAllowApiToChangePositionIfNotAttachedToCategory() {
            when(apiCategoryOrderRepository.findAllByCategoryId(eq(CATEGORY_ID))).thenReturn(Set.of());
            assertThrows(ApiNotInCategoryException.class, () -> apiCategoryService.changeApiOrderInCategory(API_ID, CATEGORY_ID, 0));
        }
    }

    @Nested
    class UpdateApiCategories {

        @Test
        void shouldAddApiToCategories() throws Exception {
            when(apiCategoryOrderRepository.findAllByApiId(eq(API_ID))).thenReturn(Set.of());
            when(apiCategoryOrderRepository.findAllByCategoryId(eq(CATEGORY_ID)))
                .thenReturn(Set.of(ApiCategoryOrder.builder().categoryId(CATEGORY_ID).apiId("api2").order(0).build()));

            apiCategoryService.updateApiCategories(API_ID, Set.of(CATEGORY_ID));
            verify(apiCategoryOrderRepository, times(1))
                .create(eq(ApiCategoryOrder.builder().categoryId(CATEGORY_ID).apiId(API_ID).order(1).build()));
        }

        @Test
        void shouldRemoveApiFromCategories() throws Exception {
            var OTHER_CATEGORY = "other-cat";
            var OTHER_API = "other-api";
            when(apiCategoryOrderRepository.findAllByApiId(eq(API_ID)))
                .thenReturn(
                    Set.of(
                        ApiCategoryOrder.builder().categoryId(CATEGORY_ID).apiId(API_ID).order(0).build(),
                        ApiCategoryOrder.builder().categoryId(OTHER_CATEGORY).apiId(API_ID).order(0).build()
                    )
                );

            when(apiCategoryOrderRepository.findAllByCategoryId(eq(OTHER_CATEGORY)))
                .thenReturn(
                    Set.of(
                        ApiCategoryOrder.builder().categoryId(OTHER_CATEGORY).apiId(API_ID).order(0).build(),
                        ApiCategoryOrder.builder().categoryId(OTHER_CATEGORY).apiId(OTHER_API).order(1).build()
                    )
                );

            apiCategoryService.updateApiCategories(API_ID, Set.of(CATEGORY_ID));
            verify(apiCategoryOrderRepository, never()).findAllByCategoryId(eq(CATEGORY_ID));

            verify(apiCategoryOrderRepository, times(1)).delete(anyString(), anyString());
            verify(apiCategoryOrderRepository, times(1)).delete(eq(API_ID), eq(OTHER_CATEGORY));

            verify(apiCategoryOrderRepository, times(1)).update(any());
            verify(apiCategoryOrderRepository, times(1))
                .update(eq(ApiCategoryOrder.builder().order(0).apiId(OTHER_API).categoryId(OTHER_CATEGORY).build()));
        }

        @Test
        void shouldAddAndRemoveApiFromCategories() throws Exception {
            var OTHER_CATEGORY = "other-cat";
            var YET_ANOTHER_CATEGORY = "yet-another-cat";
            var OTHER_API = "other-api";
            when(apiCategoryOrderRepository.findAllByApiId(eq(API_ID)))
                .thenReturn(
                    Set.of(
                        ApiCategoryOrder.builder().categoryId(CATEGORY_ID).apiId(API_ID).order(0).build(),
                        ApiCategoryOrder.builder().categoryId(OTHER_CATEGORY).apiId(API_ID).order(0).build()
                    )
                );

            when(apiCategoryOrderRepository.findAllByCategoryId(eq(OTHER_CATEGORY)))
                .thenReturn(
                    Set.of(
                        ApiCategoryOrder.builder().categoryId(OTHER_CATEGORY).apiId(API_ID).order(0).build(),
                        ApiCategoryOrder.builder().categoryId(OTHER_CATEGORY).apiId(OTHER_API).order(1).build()
                    )
                );

            when(apiCategoryOrderRepository.findAllByCategoryId(eq(YET_ANOTHER_CATEGORY))).thenReturn(Set.of());

            apiCategoryService.updateApiCategories(API_ID, Set.of(CATEGORY_ID, YET_ANOTHER_CATEGORY));
            verify(apiCategoryOrderRepository, never()).findAllByCategoryId(eq(CATEGORY_ID));

            verify(apiCategoryOrderRepository, times(1)).delete(anyString(), anyString());
            verify(apiCategoryOrderRepository, times(1)).delete(eq(API_ID), eq(OTHER_CATEGORY));

            verify(apiCategoryOrderRepository, times(1)).update(any());
            verify(apiCategoryOrderRepository, times(1))
                .update(eq(ApiCategoryOrder.builder().order(0).apiId(OTHER_API).categoryId(OTHER_CATEGORY).build()));

            verify(apiCategoryOrderRepository, times(1)).create(any());
            verify(apiCategoryOrderRepository, times(1))
                .create(eq(ApiCategoryOrder.builder().order(0).apiId(API_ID).categoryId(YET_ANOTHER_CATEGORY).build()));
        }

        @Test
        void shouldDoNothingIfCategoryIdsAreNull() throws Exception {
            apiCategoryService.updateApiCategories(API_ID, null);
            verify(apiCategoryOrderRepository, never()).update(any());
            verify(apiCategoryOrderRepository, never()).create(any());
            verify(apiCategoryOrderRepository, never()).delete(anyString(), anyString());
        }
    }

    @Nested
    class DeleteApiFromCategories {

        @Test
        void shouldRemoveApiFromCategories() throws Exception {
            var OTHER_API = "other-api";
            when(apiCategoryOrderRepository.findAllByApiId(eq(API_ID)))
                .thenReturn(Set.of(ApiCategoryOrder.builder().categoryId(CATEGORY_ID).apiId(API_ID).order(0).build()));

            when(apiCategoryOrderRepository.findAllByCategoryId(eq(CATEGORY_ID)))
                .thenReturn(
                    Set.of(
                        ApiCategoryOrder.builder().categoryId(CATEGORY_ID).apiId(API_ID).order(0).build(),
                        ApiCategoryOrder.builder().categoryId(CATEGORY_ID).apiId(OTHER_API).order(1).build()
                    )
                );

            apiCategoryService.deleteApiFromCategories(API_ID);

            verify(apiCategoryOrderRepository, times(1)).delete(anyString(), anyString());
            verify(apiCategoryOrderRepository, times(1)).delete(eq(API_ID), eq(CATEGORY_ID));

            verify(apiCategoryOrderRepository, times(1)).update(any());
            verify(apiCategoryOrderRepository, times(1))
                .update(eq(ApiCategoryOrder.builder().order(0).apiId(OTHER_API).categoryId(CATEGORY_ID).build()));
        }

        @Test
        void shouldDoNothingIfApiHadNoCategories() throws Exception {
            when(apiCategoryOrderRepository.findAllByApiId(eq(API_ID))).thenReturn(Set.of());
            apiCategoryService.deleteApiFromCategories(API_ID);
            verify(apiCategoryOrderRepository, never()).delete(anyString(), anyString());
            verify(apiCategoryOrderRepository, never()).update(any());
        }
    }
}
