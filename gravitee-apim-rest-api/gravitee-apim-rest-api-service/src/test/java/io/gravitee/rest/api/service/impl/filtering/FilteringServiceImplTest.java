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
package io.gravitee.rest.api.service.impl.filtering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.repository.management.api.search.Order;
import io.gravitee.rest.api.idp.api.authentication.UserDetails;
import io.gravitee.rest.api.model.CategoryEntity;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.TopApiEntity;
import io.gravitee.rest.api.model.api.ApiQuery;
import io.gravitee.rest.api.model.subscription.SubscriptionQuery;
import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.SubscriptionService;
import io.gravitee.rest.api.service.TopApiService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.filtering.FilteringService.FilterType;
import io.gravitee.rest.api.service.search.query.SearchSortStrategy;
import io.gravitee.rest.api.service.v4.ApiAuthorizationService;
import io.gravitee.rest.api.service.v4.ApiCategoryService;
import io.gravitee.rest.api.service.v4.ApiSearchService;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class FilteringServiceImplTest {

    private final ExecutionContext executionContext = new ExecutionContext("org", "env");

    @Mock
    SubscriptionService subscriptionService;

    @Mock
    ApiAuthorizationService apiAuthorizationService;

    @Mock
    ApiSearchService apiSearchService;

    @Mock
    ApiCategoryService apiCategoryService;

    @Mock
    ApplicationService applicationService;

    @Mock
    TopApiService topApiService;

    @InjectMocks
    FilteringServiceImpl filteringService;

    @BeforeEach
    void set_up_security_context() {
        SecurityContextHolder.setContext(SecurityContextHolder.createEmptyContext());
    }

    @AfterEach
    void clear_security_context() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = "user")
    void should_return_no_categories_without_accessible_apis(String userId) {
        when(apiAuthorizationService.findAccessibleApiIdsForUser(executionContext, userId)).thenReturn(Set.of());

        Set<CategoryEntity> result = filteringService.listCategories(executionContext, userId, null, null);

        assertThat(result).isEmpty();
        verifyNoInteractions(apiCategoryService, applicationService, subscriptionService);
    }

    @Test
    void should_return_no_categories_when_filter_removes_all_accessible_apis() {
        when(apiAuthorizationService.findAccessibleApiIdsForUser(executionContext, "user")).thenReturn(Set.of("api-1"));
        when(topApiService.findAll(executionContext)).thenReturn(List.of());

        Set<CategoryEntity> result = filteringService.listCategories(executionContext, "user", FilterType.FEATURED, null);

        assertThat(result).isEmpty();
        verifyNoInteractions(apiCategoryService);
    }

    @Test
    void should_return_no_categories_when_excluded_filter_removes_all_accessible_apis() {
        when(apiAuthorizationService.findAccessibleApiIdsForUser(executionContext, "user")).thenReturn(Set.of("api-1"));
        when(topApiService.findAll(executionContext)).thenReturn(List.of(topApi("api-1")));

        Set<CategoryEntity> result = filteringService.listCategories(executionContext, "user", null, FilterType.FEATURED);

        assertThat(result).isEmpty();
        verifyNoInteractions(apiCategoryService);
    }

    @Test
    void should_list_categories_for_filtered_accessible_apis_in_current_environment() {
        when(apiAuthorizationService.findAccessibleApiIdsForUser(executionContext, "user")).thenReturn(Set.of("api-1", "api-2"));
        when(topApiService.findAll(executionContext)).thenReturn(List.of(topApi("api-2"), topApi("inaccessible-api")));
        Set<CategoryEntity> categories = Set.of(new CategoryEntity());
        when(apiCategoryService.listCategories(List.of("api-2"), "env")).thenReturn(categories);

        Set<CategoryEntity> result = filteringService.listCategories(executionContext, "user", FilterType.FEATURED, null);

        assertThat(result).isEqualTo(categories);
        verify(apiCategoryService).listCategories(List.of("api-2"), "env");
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void should_not_search_subscriptions_without_active_applications(boolean excluded) {
        authenticate();
        Set<String> visibleApis = new LinkedHashSet<>(List.of("api-2", "api-1"));
        when(applicationService.findIdsByUser(executionContext, "user")).thenReturn(Set.of());

        Collection<String> result = filteringService.filterApis(
            executionContext,
            visibleApis,
            excluded ? null : FilterType.MINE,
            excluded ? FilterType.MINE : null
        );

        assertThat(result).containsExactlyElementsOf(excluded ? visibleApis : List.of());
        assertThat(visibleApis).containsExactly("api-2", "api-1");
        verifyNoInteractions(subscriptionService);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void should_skip_application_and_subscription_lookup_without_accessible_apis(boolean excluded) {
        authenticate();

        Collection<String> result = filteringService.filterApis(
            executionContext,
            Set.of(),
            excluded ? null : FilterType.MINE,
            excluded ? FilterType.MINE : null
        );

        assertThat(result).isEmpty();
        verifyNoInteractions(applicationService, subscriptionService);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void should_filter_visible_apis_using_only_current_user_applications(boolean excluded) {
        authenticate();
        Set<String> visibleApis = new LinkedHashSet<>(List.of("api-3", "api-2", "api-1"));
        Set<String> applications = Set.of("app-1", "app-2");
        when(applicationService.findIdsByUser(executionContext, "user")).thenReturn(applications);
        when(subscriptionService.search(eq(executionContext), any(SubscriptionQuery.class))).thenReturn(
            List.of(subscription("api-1"), subscription("inaccessible-api"), subscription("api-3"), subscription("api-1"))
        );

        Collection<String> result = filteringService.filterApis(
            executionContext,
            visibleApis,
            excluded ? null : FilterType.MINE,
            excluded ? FilterType.MINE : null
        );

        assertThat(result).containsExactlyElementsOf(excluded ? List.of("api-2") : List.of("api-3", "api-1"));
        assertThat(visibleApis).containsExactly("api-3", "api-2", "api-1");
        ArgumentCaptor<SubscriptionQuery> queryCaptor = ArgumentCaptor.forClass(SubscriptionQuery.class);
        verify(subscriptionService).search(eq(executionContext), queryCaptor.capture());
        assertThat(queryCaptor.getValue().getApplications()).containsExactlyInAnyOrderElementsOf(applications);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void should_handle_active_applications_without_subscriptions(boolean excluded) {
        authenticate();
        Set<String> visibleApis = new LinkedHashSet<>(List.of("api-2", "api-1"));
        when(applicationService.findIdsByUser(executionContext, "user")).thenReturn(Set.of("app-1"));
        when(subscriptionService.search(eq(executionContext), any(SubscriptionQuery.class))).thenReturn(List.of());

        Collection<String> result = filteringService.filterApis(
            executionContext,
            visibleApis,
            excluded ? null : FilterType.MINE,
            excluded ? FilterType.MINE : null
        );

        assertThat(result).containsExactlyElementsOf(excluded ? visibleApis : List.of());
        ArgumentCaptor<SubscriptionQuery> queryCaptor = ArgumentCaptor.forClass(SubscriptionQuery.class);
        verify(subscriptionService).search(eq(executionContext), queryCaptor.capture());
        assertThat(queryCaptor.getValue().getApplications()).containsExactly("app-1");
    }

    @Test
    void should_return_no_mine_categories_without_active_applications() {
        authenticate();
        when(apiAuthorizationService.findAccessibleApiIdsForUser(executionContext, "user")).thenReturn(Set.of("api-1"));
        when(applicationService.findIdsByUser(executionContext, "user")).thenReturn(Set.of());

        Set<CategoryEntity> result = filteringService.listCategories(executionContext, "user", FilterType.MINE, null);

        assertThat(result).isEmpty();
        verifyNoInteractions(subscriptionService, apiCategoryService);
    }

    @Test
    void should_keep_visible_api_scope_for_excluded_mine_categories_without_active_applications() {
        authenticate();
        Set<String> visibleApis = Set.of("api-1", "api-2");
        when(apiAuthorizationService.findAccessibleApiIdsForUser(executionContext, "user")).thenReturn(visibleApis);
        when(applicationService.findIdsByUser(executionContext, "user")).thenReturn(Set.of());
        Set<CategoryEntity> categories = Set.of(new CategoryEntity());
        when(apiCategoryService.listCategories(any(), eq("env"))).thenReturn(categories);

        Set<CategoryEntity> result = filteringService.listCategories(executionContext, "user", null, FilterType.MINE);

        assertThat(result).isEqualTo(categories);
        verify(apiCategoryService).listCategories(argThat(apis -> new HashSet<>(apis).equals(visibleApis)), eq("env"));
        verifyNoInteractions(subscriptionService);
    }

    private void authenticate() {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(new UserDetails("user", "", List.of()), "", List.of())
        );
    }

    private TopApiEntity topApi(String apiId) {
        TopApiEntity topApi = new TopApiEntity();
        topApi.setApi(apiId);
        return topApi;
    }

    private SubscriptionEntity subscription(String apiId) {
        SubscriptionEntity subscription = new SubscriptionEntity();
        subscription.setApi(apiId);
        return subscription;
    }

    @Test
    void getApplicationsOrderByNumberOfSubscriptions_emptyIds_doesNotRunUnfilteredQuery() {
        // An empty application set must not trigger an unfiltered ranking query.
        Collection<String> result = filteringService.getApplicationsOrderByNumberOfSubscriptions(new HashSet<>(), Order.DESC);

        assertThat(result).isEmpty();
        verifyNoInteractions(subscriptionService);
    }

    @Test
    void getApplicationsOrderByNumberOfSubscriptions_nullIds_doesNotRunUnfilteredQuery() {
        Collection<String> result = filteringService.getApplicationsOrderByNumberOfSubscriptions(null, Order.DESC);

        assertThat(result).isEmpty();
        verifyNoInteractions(subscriptionService);
    }

    @Test
    void getApplicationsOrderByNumberOfSubscriptions_withIds_delegatesToRanking() {
        Set<String> ranking = new LinkedHashSet<>(List.of("app-2"));
        when(subscriptionService.findReferenceIdsOrderByNumberOfSubscriptions(any(), eq(Order.DESC))).thenReturn(ranking);

        Collection<String> result = filteringService.getApplicationsOrderByNumberOfSubscriptions(
            new HashSet<>(List.of("app-1", "app-2")),
            Order.DESC
        );

        assertThat(result).contains("app-1", "app-2");
        verify(subscriptionService).findReferenceIdsOrderByNumberOfSubscriptions(any(), eq(Order.DESC));
    }

    @Test
    void searchApis_appliesStableSortStrategy() throws Exception {
        ExecutionContext executionContext = new ExecutionContext("org", "env");
        when(apiAuthorizationService.findAccessibleApiIdsForUser(eq(executionContext), eq("user"), any(ApiQuery.class))).thenReturn(
            Set.of("api-1")
        );

        filteringService.searchApis(executionContext, "user", "term");

        verify(apiSearchService).searchIds(
            eq(executionContext),
            eq("term"),
            any(),
            eq(null),
            any(),
            eq(false),
            eq(SearchSortStrategy.SCORE_WITH_NAME_AND_ID_TIE_BREAKERS)
        );
    }
}
