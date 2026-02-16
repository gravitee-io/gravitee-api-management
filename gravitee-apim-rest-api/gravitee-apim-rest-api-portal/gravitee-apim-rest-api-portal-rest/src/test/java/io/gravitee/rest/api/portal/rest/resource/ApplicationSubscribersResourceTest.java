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
package io.gravitee.rest.api.portal.rest.resource;

import static io.gravitee.common.http.HttpStatusCode.NOT_FOUND_404;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.SubscriptionStatus;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.analytics.TopHitsAnalytics;
import io.gravitee.rest.api.model.analytics.query.GroupByQuery;
import io.gravitee.rest.api.model.application.ApplicationListItem;
import io.gravitee.rest.api.model.subscription.SubscriptionQuery;
import io.gravitee.rest.api.portal.rest.model.Api;
import io.gravitee.rest.api.portal.rest.model.ApisResponse;
import io.gravitee.rest.api.portal.rest.model.Error;
import io.gravitee.rest.api.portal.rest.model.ErrorResponse;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.core.Response;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Gaurav Sharma (gaurav.sharma at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApplicationSubscribersResourceTest extends AbstractResourceTest {

    private static final String APPLICATION_ID = "my-application";

    @Override
    protected String contextPath() {
        return "applications/";
    }

    @BeforeEach
    public void init() {
        resetAllMocks();
        when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(true);
    }

    private ApplicationListItem buildAppItem(String id, String ownerId) {
        ApplicationListItem app = new ApplicationListItem();
        app.setId(id);
        UserEntity poUser = new UserEntity();
        poUser.setId(ownerId);
        app.setPrimaryOwner(new PrimaryOwnerEntity(poUser));
        return app;
    }

    @Test
    public void shouldReturn404WhenApplicationNotFoundForUser() {
        // applicationService.search returns empty page
        Page<ApplicationListItem> emptyPage = new Page<>(Collections.emptyList(), 1, 1, 0);
        doReturn(emptyPage).when(applicationService).search(eq(GraviteeContext.getExecutionContext()), any(), isNull(), any());

        final Response response = target(APPLICATION_ID).path("subscribers").request().get();
        assertEquals(NOT_FOUND_404, response.getStatus());

        ErrorResponse errorResponse = response.readEntity(ErrorResponse.class);
        List<Error> errors = errorResponse.getErrors();
        assertNotNull(errors);
        assertEquals(1, errors.size());
        assertEquals("errors.application.notFound", errors.get(0).getCode());
    }

    @Test
    public void shouldReturnEmptyWhenNonOwnerAndNoAccessibleApis() {
        ApplicationListItem app = buildAppItem(APPLICATION_ID, "someone-else");
        Page<ApplicationListItem> page = new Page<>(Collections.singletonList(app), 1, 1, 1);
        doReturn(page)
            .when(applicationService)
            .search(eq(GraviteeContext.getExecutionContext()), argThat(q -> q.getIds().contains(APPLICATION_ID)), isNull(), any());

        doReturn(Collections.emptySet()).when(apiAuthorizationService).findAccessibleApiIdsForUser(any(), any());

        final Response response = target(APPLICATION_ID).path("subscribers").request().get();
        assertEquals(OK_200, response.getStatus());
        ApisResponse apisResponse = response.readEntity(ApisResponse.class);
        assertNotNull(apisResponse);
        assertTrue(apisResponse.getData().isEmpty());

        verifyNoInteractions(subscriptionService);
    }

    @Test
    public void shouldFilterByAccessibleApisAndStatusesAndSortByHits() {
        ApplicationListItem app = buildAppItem(APPLICATION_ID, "owner-other");
        Page<ApplicationListItem> page = new Page<>(Collections.singletonList(app), 1, 1, 1);
        doReturn(page)
            .when(applicationService)
            .search(eq(GraviteeContext.getExecutionContext()), argThat(q -> q.getIds().contains(APPLICATION_ID)), isNull(), any());

        Set<String> accessibleApis = new HashSet<>(Arrays.asList("api-1", "api-2"));
        doReturn(accessibleApis)
            .when(apiAuthorizationService)
            .findAccessibleApiIdsForUser(eq(GraviteeContext.getExecutionContext()), anyString());

        SubscriptionEntity s1 = new SubscriptionEntity();
        s1.setApi("api-1");
        s1.setReferenceId("api-1");
        s1.setApplication(APPLICATION_ID);
        s1.setStatus(SubscriptionStatus.ACCEPTED);
        SubscriptionEntity s2 = new SubscriptionEntity();
        s2.setApi("api-2");
        s2.setReferenceId("api-2");
        s2.setApplication(APPLICATION_ID);
        s2.setStatus(SubscriptionStatus.ACCEPTED);
        doReturn(Arrays.asList(s1, s2)).when(subscriptionService).search(eq(GraviteeContext.getExecutionContext()), any());

        TopHitsAnalytics analytics = new TopHitsAnalytics();
        Map<String, Long> values = new HashMap<>();
        values.put("api-1", 5L);
        values.put("api-2", 10L);
        analytics.setValues(values);
        doReturn(analytics).when(analyticsService).execute(eq(GraviteeContext.getExecutionContext()), any(GroupByQuery.class));

        io.gravitee.rest.api.model.v4.api.ApiEntity api1 = new io.gravitee.rest.api.model.v4.api.ApiEntity();
        api1.setId("api-1");
        io.gravitee.rest.api.model.v4.api.ApiEntity api2 = new io.gravitee.rest.api.model.v4.api.ApiEntity();
        api2.setId("api-2");
        doReturn(api1)
            .when(apiSearchService)
            .findGenericById(eq(GraviteeContext.getExecutionContext()), eq("api-1"), eq(false), eq(false), eq(true));
        doReturn(api2)
            .when(apiSearchService)
            .findGenericById(eq(GraviteeContext.getExecutionContext()), eq("api-2"), eq(false), eq(false), eq(true));

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Api mapped1 = new Api().id("api-1").name("API 1").updatedAt(now);
        Api mapped2 = new Api().id("api-2").name("API 2").updatedAt(now);
        doReturn(mapped1).when(apiMapper).convert(eq(GraviteeContext.getExecutionContext()), eq(api1));
        doReturn(mapped2).when(apiMapper).convert(eq(GraviteeContext.getExecutionContext()), eq(api2));

        final Response response = target(APPLICATION_ID)
            .path("subscribers")
            .queryParam("statuses", SubscriptionStatus.ACCEPTED.name())
            .request()
            .get();
        assertEquals(OK_200, response.getStatus());

        ArgumentCaptor<SubscriptionQuery> captor = ArgumentCaptor.forClass(SubscriptionQuery.class);
        verify(subscriptionService).search(eq(GraviteeContext.getExecutionContext()), captor.capture());
        SubscriptionQuery query = captor.getValue();
        assertNotNull(query.getApplications());
        assertTrue(query.getApplications().contains(APPLICATION_ID));
        assertNotNull(query.getStatuses());
        assertTrue(query.getStatuses().contains(SubscriptionStatus.ACCEPTED));
        assertEquals(accessibleApis, query.getApis());

        ApisResponse apisResponse = response.readEntity(ApisResponse.class);
        assertEquals(2, apisResponse.getData().size());
        assertEquals("api-2", apisResponse.getData().get(0).getId());
        assertEquals("api-1", apisResponse.getData().get(1).getId());

        ArgumentCaptor<String> linksUrlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Date> dateCaptor = ArgumentCaptor.forClass(Date.class);
        verify(apiMapper, times(2)).computeApiLinks(linksUrlCaptor.capture(), dateCaptor.capture());
    }

    @Test
    public void shouldSortByNameWhenEqualHitsWithEmptyAnalytics() {
        ApplicationListItem app = buildAppItem(APPLICATION_ID, USER_NAME);
        Page<ApplicationListItem> page = new Page<>(Collections.singletonList(app), 1, 1, 1);
        doReturn(page)
            .when(applicationService)
            .search(eq(GraviteeContext.getExecutionContext()), argThat(q -> q.getIds().contains(APPLICATION_ID)), isNull(), any());

        TopHitsAnalytics emptyAnalytics = new TopHitsAnalytics();
        emptyAnalytics.setValues(new HashMap<>());
        doReturn(emptyAnalytics).when(analyticsService).execute(eq(GraviteeContext.getExecutionContext()), any(GroupByQuery.class));

        SubscriptionEntity s1 = new SubscriptionEntity();
        s1.setApi("api-A");
        s1.setReferenceId("api-A");
        s1.setApplication(APPLICATION_ID);
        SubscriptionEntity s2 = new SubscriptionEntity();
        s2.setApi("api-B");
        s2.setReferenceId("api-B");
        s2.setApplication(APPLICATION_ID);
        doReturn(Arrays.asList(s2, s1)).when(subscriptionService).search(eq(GraviteeContext.getExecutionContext()), any());

        io.gravitee.rest.api.model.v4.api.ApiEntity apiA = new io.gravitee.rest.api.model.v4.api.ApiEntity();
        apiA.setId("api-A");
        io.gravitee.rest.api.model.v4.api.ApiEntity apiB = new io.gravitee.rest.api.model.v4.api.ApiEntity();
        apiB.setId("api-B");
        doReturn(apiA)
            .when(apiSearchService)
            .findGenericById(eq(GraviteeContext.getExecutionContext()), eq("api-A"), eq(false), eq(false), eq(true));
        doReturn(apiB)
            .when(apiSearchService)
            .findGenericById(eq(GraviteeContext.getExecutionContext()), eq("api-B"), eq(false), eq(false), eq(true));

        Api mappedA = new Api().id("api-A").name("AAA");
        Api mappedB = new Api().id("api-B").name("BBB");
        doReturn(mappedA).when(apiMapper).convert(eq(GraviteeContext.getExecutionContext()), eq(apiA));
        doReturn(mappedB).when(apiMapper).convert(eq(GraviteeContext.getExecutionContext()), eq(apiB));

        final Response response = target(APPLICATION_ID).path("subscribers").request().get();
        assertEquals(OK_200, response.getStatus());
        ApisResponse apisResponse = response.readEntity(ApisResponse.class);
        assertEquals(2, apisResponse.getData().size());
        assertEquals("api-B", apisResponse.getData().get(0).getId());
        assertEquals("api-A", apisResponse.getData().get(1).getId());
    }

    @Test
    public void shouldExcludeApiProductSubscriptionsFromSubscribers() {
        ApplicationListItem app = buildAppItem(APPLICATION_ID, USER_NAME);
        Page<ApplicationListItem> page = new Page<>(Collections.singletonList(app), 1, 1, 1);
        doReturn(page)
            .when(applicationService)
            .search(eq(GraviteeContext.getExecutionContext()), argThat(q -> q.getIds().contains(APPLICATION_ID)), isNull(), any());

        TopHitsAnalytics emptyAnalytics = new TopHitsAnalytics();
        emptyAnalytics.setValues(new HashMap<>());
        doReturn(emptyAnalytics).when(analyticsService).execute(eq(GraviteeContext.getExecutionContext()), any(GroupByQuery.class));

        SubscriptionEntity apiSub = new SubscriptionEntity();
        apiSub.setReferenceId("api-only");
        apiSub.setApplication(APPLICATION_ID);
        SubscriptionEntity apiProductSub = new SubscriptionEntity();
        apiProductSub.setReferenceId("api-product-id");
        apiProductSub.setReferenceType("API_PRODUCT");
        apiProductSub.setApplication(APPLICATION_ID);
        doReturn(Arrays.asList(apiSub, apiProductSub)).when(subscriptionService).search(eq(GraviteeContext.getExecutionContext()), any());

        io.gravitee.rest.api.model.v4.api.ApiEntity apiEntity = new io.gravitee.rest.api.model.v4.api.ApiEntity();
        apiEntity.setId("api-only");
        doReturn(apiEntity)
            .when(apiSearchService)
            .findGenericById(eq(GraviteeContext.getExecutionContext()), eq("api-only"), eq(false), eq(false), eq(true));
        Api mappedApi = new Api().id("api-only").name("API Only");
        doReturn(mappedApi).when(apiMapper).convert(eq(GraviteeContext.getExecutionContext()), eq(apiEntity));

        final Response response = target(APPLICATION_ID).path("subscribers").request().get();
        assertEquals(OK_200, response.getStatus());
        ApisResponse apisResponse = response.readEntity(ApisResponse.class);
        assertEquals(1, apisResponse.getData().size());
        assertEquals("api-only", apisResponse.getData().get(0).getId());
    }
}
