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

import io.gravitee.rest.api.idp.api.authentication.UserDetails;
import io.gravitee.rest.api.model.RatingSummaryEntity;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.TopApiEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.api.ApiLifecycleState;
import io.gravitee.rest.api.model.application.ApplicationListItem;
import io.gravitee.rest.api.model.filtering.FilterableItem;
import io.gravitee.rest.api.model.filtering.FilteredEntities;
import io.gravitee.rest.api.service.filtering.FilteringService;
import io.gravitee.rest.api.service.impl.filtering.FilteringServiceImpl;
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

import java.util.*;

import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class FilteringServiceTest {

    @InjectMocks
    private FilteringServiceImpl filteringService = new FilteringServiceImpl();

    @Mock
    SubscriptionService subscriptionService;

    @Mock
    RatingService ratingService;

    @Mock
    TopApiService topApiService;

    @Mock
    ApplicationService applicationService;

    private static Set<ApiEntity> mockApis;

    @AfterClass
    public static void cleanSecurityContextHolder() {
        // reset authentication to avoid side effect during test executions.
        SecurityContextHolder.setContext(new SecurityContext() {
            @Override
            public Authentication getAuthentication() {
                return null;
            }
            @Override
            public void setAuthentication(Authentication authentication) {
            }
        });
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

        mockApis = new HashSet<>(Arrays.asList(publishedApi5, publishedApi2, publishedApi1, publishedApi3, publishedApi4));
    }

    @Test
    public void shouldNotGetMineApi() {
        FilteredEntities<ApiEntity> apiEntityFilteredEntities = filteringService.filterApis(mockApis, FilteringService.FilterType.MINE, null);

        List<ApiEntity> filteredItems = apiEntityFilteredEntities.getFilteredItems();
        assertEquals(0, filteredItems.size());
    }

    @Test
    public void shouldGetMineApi() {
        final Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(new UserDetails("user", "", emptyList()));
        final SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        ApplicationListItem appA = new ApplicationListItem();
        appA.setId("A");
        ApplicationListItem appB = new ApplicationListItem();
        appB.setId("B");
        ApplicationListItem appC = new ApplicationListItem();
        appC.setId("C");
        doReturn(new HashSet<ApplicationListItem>(Arrays.asList(appC, appB, appA))).when(applicationService)
                .findByUser(any());

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
        doReturn(Arrays.asList(subC8, subA2, subB1, subC4, subA1)).when(subscriptionService).search(any());

        FilteredEntities<ApiEntity> apiEntityFilteredEntities = filteringService.filterApis(mockApis, FilteringService.FilterType.MINE, null);

        List<ApiEntity> filteredItems = apiEntityFilteredEntities.getFilteredItems();
        assertEquals(2, filteredItems.size());
        assertEquals("1", filteredItems.get(0).getId());
        assertEquals("4", filteredItems.get(1).getId());
    }

    @Test
    public void shouldNotGetStarredApi() {
        doReturn(false).when(ratingService).isEnabled();

        FilteredEntities<ApiEntity> apiEntityFilteredEntities = filteringService.filterApis(mockApis, FilteringService.FilterType.STARRED, null);

        List<ApiEntity> filteredItems = apiEntityFilteredEntities.getFilteredItems();
        assertEquals(0, filteredItems.size());
    }

    @Test
    public void shouldGetStarredApi() {
        doReturn(true).when(ratingService).isEnabled();

        RatingSummaryEntity ratingSummary1 = new RatingSummaryEntity();
        ratingSummary1.setApi("1");
        ratingSummary1.setAverageRate(4.5);
        ratingSummary1.setNumberOfRatings(3);
        doReturn(ratingSummary1).when(ratingService).findSummaryByApi("1");

        RatingSummaryEntity ratingSummary3 = new RatingSummaryEntity();
        ratingSummary3.setApi("3");
        ratingSummary3.setAverageRate(5.0);
        ratingSummary3.setNumberOfRatings(10);
        doReturn(ratingSummary3).when(ratingService).findSummaryByApi("3");

        RatingSummaryEntity ratingSummary4 = new RatingSummaryEntity();
        ratingSummary4.setApi("4");
        ratingSummary4.setAverageRate(5.0);
        ratingSummary4.setNumberOfRatings(1);
        doReturn(ratingSummary4).when(ratingService).findSummaryByApi("4");

        RatingSummaryEntity ratingSummary5 = new RatingSummaryEntity();
        ratingSummary5.setApi("5");
        ratingSummary5.setAverageRate(4.5);
        ratingSummary5.setNumberOfRatings(3);
        doReturn(ratingSummary5).when(ratingService).findSummaryByApi("5");

        FilteredEntities<ApiEntity> apiEntityFilteredEntities = filteringService.filterApis(mockApis, FilteringService.FilterType.STARRED, null);

        List<ApiEntity> filteredItems = apiEntityFilteredEntities.getFilteredItems();
        assertEquals(4, filteredItems.size());
        assertEquals("3", filteredItems.get(0).getId());
        assertEquals("4", filteredItems.get(1).getId());
        assertEquals("1", filteredItems.get(2).getId());
        assertEquals("5", filteredItems.get(3).getId());

    }

    @Test
    public void shouldGetTrendingsApi() {

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
        doReturn(Arrays.asList(subC8, subA2, subB1, subC4, subA1)).when(subscriptionService).search(any());

        FilteredEntities<ApiEntity> apiEntityFilteredEntities = filteringService.filterApis(mockApis, FilteringService.FilterType.TRENDINGS, null);

        List<ApiEntity> filteredItems = apiEntityFilteredEntities.getFilteredItems();
        assertEquals(2, filteredItems.size());
        assertEquals("1", filteredItems.get(0).getId());
        assertEquals("4", filteredItems.get(1).getId());
        Map<String, Map<String, Object>> metadata = apiEntityFilteredEntities.getMetadata();
        assertNotNull(metadata);
        assertEquals(1, metadata.size());

        Map<String, Object> subscriptionsMetadata = metadata.get("subscriptions");
        assertNotNull(subscriptionsMetadata);
        assertEquals(2, subscriptionsMetadata.size());
        assertEquals(2L, subscriptionsMetadata.get("1")); // 2 subscriptions for API 1
        assertEquals(1L, subscriptionsMetadata.get("4")); // 1 subscription for API 4
    }

    @Test
    public void shouldGetFeaturedApis() {
        TopApiEntity topApi5 = new TopApiEntity();
        topApi5.setApi("5");
        topApi5.setOrder(2);
        TopApiEntity topApi6 = new TopApiEntity();
        topApi6.setApi("6");
        topApi6.setOrder(1);
        doReturn(Arrays.asList(topApi5, topApi6)).when(topApiService).findAll();

        FilteredEntities<ApiEntity> apiEntityFilteredEntities = filteringService.filterApis(mockApis, FilteringService.FilterType.FEATURED, null);

        List<ApiEntity> filteredItems = apiEntityFilteredEntities.getFilteredItems();
        assertEquals(2, filteredItems.size());
        assertEquals("6", filteredItems.get(0).getId());
        assertEquals("5", filteredItems.get(1).getId());

    }

    @Test
    public void shouldGetApplicationsOrderByNbSubscriptionsDesc() {
        ApplicationListItem applicationListItem1 = new ApplicationListItem();
        applicationListItem1.setId("A");
        applicationListItem1.setName("A");

        ApplicationListItem applicationListItem2 = new ApplicationListItem();
        applicationListItem2.setId("B");
        applicationListItem2.setName("B");

        Set<FilterableItem> mockApplications = new HashSet<>(Arrays.asList(applicationListItem1, applicationListItem2));

        SubscriptionEntity subA1 = new SubscriptionEntity();
        subA1.setApplication("A");
        SubscriptionEntity subA2 = new SubscriptionEntity();
        subA2.setApplication("A");
        SubscriptionEntity subB1 = new SubscriptionEntity();
        subB1.setApplication("B");
        SubscriptionEntity subB2 = new SubscriptionEntity();
        subB2.setApplication("B");
        SubscriptionEntity subB3 = new SubscriptionEntity();
        subB3.setApplication("B");
        doReturn(Arrays.asList(subA1, subA2, subB1, subB2, subB3)).when(subscriptionService).search(any());

        FilteredEntities<FilterableItem> applicationListItemFilteredEntities = filteringService.getEntitiesOrderByNumberOfSubscriptions(mockApplications, false, false);

        List<FilterableItem> filteredItems = applicationListItemFilteredEntities.getFilteredItems();
        assertEquals("B", filteredItems.get(0).getId());
        assertEquals("A", filteredItems.get(1).getId());

        Map<String, Object> subscriptionsMetadata = applicationListItemFilteredEntities.getMetadata().get("subscriptions");
        assertEquals(3L, subscriptionsMetadata.get("B"));
        assertEquals(2L, subscriptionsMetadata.get("A"));
    }

    @Test
    public void shouldGetApplicationsOrderByNbSubscriptionsDescAndName() {
        ApplicationListItem applicationListItem1 = new ApplicationListItem();
        applicationListItem1.setId("A");
        applicationListItem1.setName("A");

        ApplicationListItem applicationListItem2 = new ApplicationListItem();
        applicationListItem2.setId("B");
        applicationListItem2.setName("b");

        ApplicationListItem applicationListItem3 = new ApplicationListItem();
        applicationListItem3.setId("C");
        applicationListItem3.setName("C");

        ApplicationListItem applicationListItem4 = new ApplicationListItem();
        applicationListItem4.setId("D");
        applicationListItem4.setName("d");

        Set<FilterableItem> mockApplications = new HashSet<>(Arrays.asList(applicationListItem1, applicationListItem2, applicationListItem3, applicationListItem4));

        SubscriptionEntity subA1 = new SubscriptionEntity();
        subA1.setApplication("A");
        SubscriptionEntity subA2 = new SubscriptionEntity();
        subA2.setApplication("A");
        SubscriptionEntity subB1 = new SubscriptionEntity();
        subB1.setApplication("B");
        SubscriptionEntity subB2 = new SubscriptionEntity();
        subB2.setApplication("B");
        SubscriptionEntity subB3 = new SubscriptionEntity();
        subB3.setApplication("B");
        SubscriptionEntity subC1 = new SubscriptionEntity();
        subC1.setApplication("C");
        SubscriptionEntity subC2 = new SubscriptionEntity();
        subC2.setApplication("C");
        SubscriptionEntity subC3 = new SubscriptionEntity();
        subC3.setApplication("C");
        SubscriptionEntity subD1 = new SubscriptionEntity();
        subD1.setApplication("D");
        SubscriptionEntity subD2 = new SubscriptionEntity();
        subD2.setApplication("D");
        doReturn(Arrays.asList(subA1, subA2, subB1, subB2, subB3, subC1, subC2, subC3, subD1, subD2)).when(subscriptionService).search(any());

        FilteredEntities<FilterableItem> applicationListItemFilteredEntities = filteringService.getEntitiesOrderByNumberOfSubscriptions(mockApplications, false, false);

        List<FilterableItem> filteredItems = applicationListItemFilteredEntities.getFilteredItems();
        assertEquals("B", filteredItems.get(0).getId());
        assertEquals("C", filteredItems.get(1).getId());
        assertEquals("A", filteredItems.get(2).getId());
        assertEquals("D", filteredItems.get(3).getId());

    }
}
