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

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.category.model.ApiCategoryOrder;
import io.gravitee.apim.core.category.use_case.GetCategoryApisUseCase;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.search.Order;
import io.gravitee.rest.api.idp.api.authentication.UserDetails;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.SubscriptionStatus;
import io.gravitee.rest.api.model.TopApiEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.api.ApiLifecycleState;
import io.gravitee.rest.api.model.api.ApiQuery;
import io.gravitee.rest.api.model.subscription.SubscriptionQuery;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.filtering.FilteringService;
import io.gravitee.rest.api.service.impl.filtering.FilteringServiceImpl;
import io.gravitee.rest.api.service.v4.ApiAuthorizationService;
import io.gravitee.rest.api.service.v4.ApiSearchService;
import java.util.*;
import org.jetbrains.annotations.NotNull;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class FilteringServiceTest {

    private static Set<String> mockApis;

    @Mock
    SubscriptionService subscriptionService;

    @Mock
    RatingService ratingService;

    @Mock
    TopApiService topApiService;

    @Mock
    ApiSearchService apiSearchService;

    @Mock
    ApiAuthorizationService apiAuthorizationService;

    @Mock
    ApplicationService applicationService;

    @Mock
    GetCategoryApisUseCase getCategoryApisUseCase;

    @InjectMocks
    private FilteringServiceImpl filteringService = new FilteringServiceImpl();

    @AfterClass
    public static void cleanSecurityContextHolder() {
        // reset authentication to avoid side effect during test executions.
        SecurityContextHolder.setContext(
            new SecurityContext() {
                @Override
                public Authentication getAuthentication() {
                    return null;
                }

                @Override
                public void setAuthentication(Authentication authentication) {}
            }
        );
    }

    @BeforeClass
    public static void initAllTest() {
        ApiEntity publishedApi1 = new ApiEntity();
        publishedApi1.setLifecycleState(ApiLifecycleState.PUBLISHED);
        publishedApi1.setName("1");
        publishedApi1.setId("1");

        ApiEntity unpublishedApi = new ApiEntity();
        unpublishedApi.setLifecycleState(ApiLifecycleState.UNPUBLISHED);
        unpublishedApi.setName("2");
        unpublishedApi.setId("2");

        ApiEntity publishedApi2 = new ApiEntity();
        publishedApi2.setLifecycleState(ApiLifecycleState.PUBLISHED);
        publishedApi2.setName("3");
        publishedApi2.setId("3");

        ApiEntity publishedApi3 = new ApiEntity();
        publishedApi3.setLifecycleState(ApiLifecycleState.PUBLISHED);
        publishedApi3.setName("4");
        publishedApi3.setId("4");

        ApiEntity publishedApi4 = new ApiEntity();
        publishedApi4.setLifecycleState(ApiLifecycleState.PUBLISHED);
        publishedApi4.setName("5");
        publishedApi4.setId("5");

        ApiEntity publishedApi5 = new ApiEntity();
        publishedApi5.setLifecycleState(ApiLifecycleState.PUBLISHED);
        publishedApi5.setName("6");
        publishedApi5.setId("6");

        mockApis =
            new HashSet<>(
                Arrays.asList(
                    publishedApi5.getId(),
                    publishedApi2.getId(),
                    publishedApi1.getId(),
                    publishedApi3.getId(),
                    publishedApi4.getId()
                )
            );
    }

    @Test
    public void shouldNotApplyAnyFilterIfEmptyApiList() {
        Set<String> apis = emptySet();
        Collection<String> filteredList = filteringService.filterApis(
            GraviteeContext.getExecutionContext(),
            apis,
            FilteringService.FilterType.TRENDINGS,
            null
        );
        assertSame(apis, filteredList);

        verifyNoInteractions(subscriptionService);
    }

    @Test
    public void shouldNotApplyAnyFilterIfNoFilter() {
        Collection<String> filteredList = filteringService.filterApis(GraviteeContext.getExecutionContext(), mockApis, null, null);
        assertSame(mockApis, filteredList);
    }

    @Test
    public void shouldNotApplyAnyFilterIfAllFilter() {
        Collection<String> filteredList = filteringService.filterApis(
            GraviteeContext.getExecutionContext(),
            mockApis,
            FilteringService.FilterType.ALL,
            null
        );
        assertSame(mockApis, filteredList);
    }

    @Test
    public void shouldNotGetMineApi() {
        Collection<String> apiEntityFilteredEntities = filteringService.filterApis(
            GraviteeContext.getExecutionContext(),
            mockApis,
            FilteringService.FilterType.MINE,
            null
        );

        Collection<String> filteredItems = apiEntityFilteredEntities;
        assertEquals(0, filteredItems.size());
    }

    @Test
    public void shouldGetMineApi() {
        final Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(new UserDetails("user", "", emptyList()));
        final SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        doReturn(new HashSet<>(Arrays.asList("C", "B", "A")))
            .when(applicationService)
            .findIdsByUser(eq(GraviteeContext.getExecutionContext()), any());

        SubscriptionEntity subA1 = new SubscriptionEntity();
        subA1.setApplication("A");
        subA1.setApi("1");
        SubscriptionEntity subA2 = new SubscriptionEntity();
        subA2.setApplication("A");
        subA2.setApi("2");
        SubscriptionEntity subB1 = new SubscriptionEntity();
        subB1.setApplication("B");
        subB1.setApi("1");
        SubscriptionEntity subC4 = new SubscriptionEntity();
        subC4.setApplication("C");
        subC4.setApi("4");
        SubscriptionEntity subC8 = new SubscriptionEntity();
        subC8.setApplication("C");
        subC8.setApi("8");
        doReturn(Arrays.asList(subC8, subA2, subB1, subC4, subA1))
            .when(subscriptionService)
            .search(eq(GraviteeContext.getExecutionContext()), any());

        Collection<String> filteredItems = filteringService.filterApis(
            GraviteeContext.getExecutionContext(),
            mockApis,
            FilteringService.FilterType.MINE,
            null
        );

        assertEquals(2, filteredItems.size());
        assertEquals(Arrays.asList("1", "4"), filteredItems);
    }

    @Test
    public void shouldNotGetStarredApi() {
        doReturn(false).when(ratingService).isEnabled(GraviteeContext.getExecutionContext());

        Collection<String> filteredItems = filteringService.filterApis(
            GraviteeContext.getExecutionContext(),
            mockApis,
            FilteringService.FilterType.STARRED,
            null
        );

        assertEquals(0, filteredItems.size());
    }

    @Test
    public void shouldGetStarredApi() {
        Set<String> apis = new LinkedHashSet<>(Arrays.asList("5", "4", "2", "1"));
        doReturn(true).when(ratingService).isEnabled(GraviteeContext.getExecutionContext());
        doReturn(new LinkedHashSet<>(Arrays.asList("3", "4", "1", "5")))
            .when(ratingService)
            .findReferenceIdsOrderByRate(GraviteeContext.getExecutionContext(), apis);

        Collection<String> filteredItems = filteringService.filterApis(
            GraviteeContext.getExecutionContext(),
            apis,
            FilteringService.FilterType.STARRED,
            null
        );

        assertEquals(5, filteredItems.size());
        assertEquals(new LinkedHashSet<>(Arrays.asList("3", "4", "1", "5", "2")), filteredItems);
    }

    @Test
    public void shouldGetStarredApiExcluded() {
        Set<String> apis = new LinkedHashSet<>(Arrays.asList("5", "4", "2", "1"));
        doReturn(true).when(ratingService).isEnabled(GraviteeContext.getExecutionContext());
        doReturn(new LinkedHashSet<>(Arrays.asList("3", "4", "1", "5")))
            .when(ratingService)
            .findReferenceIdsOrderByRate(GraviteeContext.getExecutionContext(), apis);

        Collection<String> filteredItems = filteringService.filterApis(
            GraviteeContext.getExecutionContext(),
            apis,
            null,
            FilteringService.FilterType.STARRED
        );

        assertEquals(1, filteredItems.size());
        assertEquals(new LinkedHashSet<>(Arrays.asList("2")), filteredItems);
    }

    @Test
    public void shouldGetTrendingsApi() {
        Set<String> apis = new LinkedHashSet<>(Arrays.asList("5", "4", "2", "1"));

        SubscriptionQuery subscriptionQuery = new SubscriptionQuery();
        subscriptionQuery.setStatuses(Arrays.asList(SubscriptionStatus.ACCEPTED, SubscriptionStatus.PAUSED));
        subscriptionQuery.setApis(apis);
        doReturn(new LinkedHashSet<>(Arrays.asList("1", "4")))
            .when(subscriptionService)
            .findReferenceIdsOrderByNumberOfSubscriptions(subscriptionQuery, Order.DESC);

        Collection<String> filteredItems = filteringService.filterApis(
            GraviteeContext.getExecutionContext(),
            apis,
            FilteringService.FilterType.TRENDINGS,
            null
        );

        assertEquals(4, filteredItems.size());
        assertEquals(new LinkedHashSet<>(Arrays.asList("1", "4", "5", "2")), filteredItems);
    }

    @Test
    public void shouldGetTrendingsApiExcluded() {
        Set<String> apis = new LinkedHashSet<>(Arrays.asList("5", "4", "2", "1"));

        SubscriptionQuery subscriptionQuery = new SubscriptionQuery();
        subscriptionQuery.setStatuses(Arrays.asList(SubscriptionStatus.ACCEPTED, SubscriptionStatus.PAUSED));
        subscriptionQuery.setApis(apis);
        doReturn(new LinkedHashSet<>(Arrays.asList("1", "4")))
            .when(subscriptionService)
            .findReferenceIdsOrderByNumberOfSubscriptions(subscriptionQuery, Order.DESC);

        Collection<String> filteredItems = filteringService.filterApis(
            GraviteeContext.getExecutionContext(),
            apis,
            null,
            FilteringService.FilterType.TRENDINGS
        );

        assertEquals(2, filteredItems.size());
        assertEquals(new LinkedHashSet<>(Arrays.asList("5", "2")), filteredItems);
    }

    @Test
    public void shouldGetFeaturedApis() {
        TopApiEntity topApi5 = new TopApiEntity();
        topApi5.setApi("5");
        topApi5.setOrder(2);
        TopApiEntity topApi6 = new TopApiEntity();
        topApi6.setApi("6");
        topApi6.setOrder(1);
        doReturn(Arrays.asList(topApi5, topApi6)).when(topApiService).findAll(GraviteeContext.getExecutionContext());

        Collection<String> filteredItems = filteringService.filterApis(
            GraviteeContext.getExecutionContext(),
            mockApis,
            FilteringService.FilterType.FEATURED,
            null
        );

        assertEquals(2, filteredItems.size());
        assertEquals(Arrays.asList("6", "5"), filteredItems);
    }

    @Test
    public void shouldGetFeaturedApisExcluded() {
        TopApiEntity topApi5 = new TopApiEntity();
        topApi5.setApi("5");
        topApi5.setOrder(2);
        TopApiEntity topApi6 = new TopApiEntity();
        topApi6.setApi("6");
        topApi6.setOrder(1);
        doReturn(Arrays.asList(topApi5, topApi6)).when(topApiService).findAll(GraviteeContext.getExecutionContext());

        Collection<String> filteredItems = filteringService.filterApis(
            GraviteeContext.getExecutionContext(),
            mockApis,
            null,
            FilteringService.FilterType.FEATURED
        );

        assertEquals(3, filteredItems.size());
        assertEquals(Arrays.asList("1", "3", "4"), filteredItems);
    }

    @Test
    public void shouldSearchApis() throws TechnicalException {
        String aQuery = "a Query";

        doReturn(Set.of("api-#1", "api-#2", "api-#3"))
            .when(apiAuthorizationService)
            .findAccessibleApiIdsForUser(eq(GraviteeContext.getExecutionContext()), eq("user-#1"), nullable(ApiQuery.class));
        doReturn(Set.of("api-#3"))
            .when(apiSearchService)
            .searchIds(
                eq(GraviteeContext.getExecutionContext()),
                eq(aQuery),
                argThat(map -> ((List<String>) map.get("api")).containsAll(Set.of("api-#1", "api-#2", "api-#3"))),
                isNull()
            );

        Collection<String> searchItems = filteringService.searchApis(GraviteeContext.getExecutionContext(), "user-#1", aQuery);

        assertThat(searchItems).singleElement().isEqualTo("api-#3");
    }

    @Test
    public void shouldSearchApisWithCategory() throws TechnicalException {
        String aQuery = "a Query";
        String category = "category-key";

        var apiQuery = new ApiQuery();
        apiQuery.setCategory(category);

        doReturn(
            new GetCategoryApisUseCase.Output(
                List.of(resultForApi("api-#1", 1, category), resultForApi("api-#3", 2, category), resultForApi("api-#2", 3, category))
            )
        )
            .when(getCategoryApisUseCase)
            .execute(
                argThat(input -> input.categoryIdOrKey().equals(category) && input.onlyPublishedApiLifecycleState() && !input.isAdmin())
            );
        doReturn(Set.of("api-#3", "api-#1", "api-#2"))
            .when(apiSearchService)
            .searchIds(
                eq(GraviteeContext.getExecutionContext()),
                eq(aQuery),
                eq(Map.of("api", List.of("api-#1", "api-#3", "api-#2"))),
                isNull()
            );

        Collection<String> searchItems = filteringService.searchApisWithCategory(
            GraviteeContext.getExecutionContext(),
            "user-#1",
            aQuery,
            category
        );

        assertThat(searchItems).containsExactly("api-#1", "api-#3", "api-#2");
    }

    private static GetCategoryApisUseCase.@NotNull Result resultForApi(String apiId, int order, String category) {
        return new GetCategoryApisUseCase.Result(
            ApiCategoryOrder.builder().apiId(apiId).categoryId(category).order(order).build(),
            Api.builder().id(apiId).categories(Set.of(category)).build()
        );
    }
}
