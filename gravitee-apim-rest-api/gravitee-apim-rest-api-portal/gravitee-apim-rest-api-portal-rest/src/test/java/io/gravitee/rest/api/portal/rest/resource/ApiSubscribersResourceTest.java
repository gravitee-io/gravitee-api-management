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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.analytics.TopHitsAnalytics;
import io.gravitee.rest.api.model.analytics.query.GroupByQuery;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.application.ApplicationListItem;
import io.gravitee.rest.api.model.common.SortableImpl;
import io.gravitee.rest.api.model.subscription.SubscriptionQuery;
import io.gravitee.rest.api.portal.rest.model.Application;
import io.gravitee.rest.api.portal.rest.model.ApplicationsResponse;
import io.gravitee.rest.api.portal.rest.model.Error;
import io.gravitee.rest.api.portal.rest.model.ErrorResponse;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiSubscribersResourceTest extends AbstractResourceTest {

    private static final String API = "my-api";

    protected String contextPath() {
        return "apis/";
    }

    @BeforeEach
    public void init() throws IOException {
        resetAllMocks();
        when(accessControlService.canAccessApiFromPortal(GraviteeContext.getExecutionContext(), API)).thenReturn(true);
        ApiEntity apiEntity = new ApiEntity();
        apiEntity.setId(API);
        UserEntity user = new UserEntity();
        user.setId(USER_NAME);
        PrimaryOwnerEntity primaryOwner = new PrimaryOwnerEntity(user);
        apiEntity.setPrimaryOwner(primaryOwner);
        when(apiSearchService.findGenericById(GraviteeContext.getExecutionContext(), API)).thenReturn(apiEntity);
    }

    @Test
    public void shouldNotFoundApiWhileGettingApiSubscribers() {
        // init
        when(apiSearchService.findGenericById(GraviteeContext.getExecutionContext(), API)).thenReturn(
            new io.gravitee.rest.api.model.v4.api.ApiEntity()
        );
        // test
        final Response response = target(API).path("metrics").request().get();
        assertEquals(NOT_FOUND_404, response.getStatus());

        ErrorResponse errorResponse = response.readEntity(ErrorResponse.class);
        List<Error> errors = errorResponse.getErrors();
        assertNotNull(errors);
        assertEquals(1, errors.size());
        Error error = errors.get(0);
        assertNotNull(error);
        assertEquals("errors.api.notFound", error.getCode());
        assertEquals("404", error.getStatus());
        assertEquals("Api [" + API + "] cannot be found.", error.getMessage());
    }

    @Test
    public void shouldGetApiSubscribersAsPrimaryOwner() {
        TopHitsAnalytics mockAnalytics = new TopHitsAnalytics();
        Map<String, Long> mockedValues = new HashMap<>();
        mockedValues.put("A", 10L);
        mockedValues.put("B", 20L);
        mockedValues.put("C", 30L);
        mockAnalytics.setValues(mockedValues);
        doReturn(mockAnalytics).when(analyticsService).execute(eq(GraviteeContext.getExecutionContext()), any(GroupByQuery.class));

        SubscriptionEntity subA1 = new SubscriptionEntity();
        subA1.setApplication("A");
        subA1.setApi(API);
        SubscriptionEntity subB1 = new SubscriptionEntity();
        subB1.setApplication("B");
        subB1.setApi(API);
        SubscriptionEntity subC1 = new SubscriptionEntity();
        subC1.setApplication("C");
        subC1.setApi(API);
        doReturn(Arrays.asList(subB1, subC1, subA1)).when(subscriptionService).search(eq(GraviteeContext.getExecutionContext()), any());

        ApplicationListItem appA = new ApplicationListItem();
        appA.setId("A");
        ApplicationListItem appB = new ApplicationListItem();
        appB.setId("B");
        ApplicationListItem appC = new ApplicationListItem();
        appC.setId("C");
        Page<ApplicationListItem> applications = new Page(Arrays.asList(appA, appB, appC), 1, 10, 2);
        doReturn(applications)
            .when(applicationService)
            .search(
                eq(GraviteeContext.getExecutionContext()),
                argThat(q -> q.getIds().containsAll(Arrays.asList("A", "B", "C"))),
                eq(new SortableImpl("name", true)),
                eq(null)
            );
        doReturn(new Application().id("A").name("A"))
            .when(applicationMapper)
            .convert(eq(GraviteeContext.getExecutionContext()), eq(appA), any());

        doReturn(new Application().id("B").name("B"))
            .when(applicationMapper)
            .convert(eq(GraviteeContext.getExecutionContext()), eq(appB), any());

        doReturn(new Application().id("C").name("C"))
            .when(applicationMapper)
            .convert(eq(GraviteeContext.getExecutionContext()), eq(appC), any());

        final Response response = target(API).path("subscribers").request().get();
        assertEquals(OK_200, response.getStatus());

        final ApplicationsResponse applicationsResponse = response.readEntity(ApplicationsResponse.class);
        assertNotNull(applicationsResponse);
        assertEquals(3, applicationsResponse.getData().size());
        assertEquals("C", applicationsResponse.getData().get(0).getId());
        assertEquals("B", applicationsResponse.getData().get(1).getId());
        assertEquals("A", applicationsResponse.getData().get(2).getId());
    }

    @Test
    public void shouldGetApiSubscribersAsPrimaryOwnerAlphaSorted() {
        TopHitsAnalytics mockAnalytics = new TopHitsAnalytics();
        Map<String, Long> mockedValues = new HashMap<>();
        mockedValues.put("A", 0L);
        mockedValues.put("C", 0L);
        mockedValues.put("B", 0L);
        mockAnalytics.setValues(mockedValues);
        doReturn(mockAnalytics).when(analyticsService).execute(eq(GraviteeContext.getExecutionContext()), any(GroupByQuery.class));

        SubscriptionEntity subA1 = new SubscriptionEntity();
        subA1.setApplication("A");
        subA1.setApi(API);
        SubscriptionEntity subB1 = new SubscriptionEntity();
        subB1.setApplication("B");
        subB1.setApi(API);
        SubscriptionEntity subC1 = new SubscriptionEntity();
        subC1.setApplication("C");
        subC1.setApi(API);
        doReturn(Arrays.asList(subB1, subC1, subA1)).when(subscriptionService).search(eq(GraviteeContext.getExecutionContext()), any());

        ApplicationListItem appA = new ApplicationListItem();
        appA.setId("A");
        appA.setName("A");
        ApplicationListItem appB = new ApplicationListItem();
        appB.setId("B");
        appB.setName("B");
        ApplicationListItem appC = new ApplicationListItem();
        appC.setId("C");
        appC.setName("C");
        Page<ApplicationListItem> applications = new Page(Arrays.asList(appA, appB, appC), 1, 10, 2);
        doReturn(applications)
            .when(applicationService)
            .search(
                eq(GraviteeContext.getExecutionContext()),
                argThat(q -> q.getIds().containsAll(Arrays.asList("A", "B", "C"))),
                eq(new SortableImpl("name", true)),
                eq(null)
            );
        doReturn(new Application().id("A").name("A"))
            .when(applicationMapper)
            .convert(eq(GraviteeContext.getExecutionContext()), eq(appA), any());

        doReturn(new Application().id("B").name("B"))
            .when(applicationMapper)
            .convert(eq(GraviteeContext.getExecutionContext()), eq(appB), any());

        doReturn(new Application().id("C").name("C"))
            .when(applicationMapper)
            .convert(eq(GraviteeContext.getExecutionContext()), eq(appC), any());

        final Response response = target(API).path("subscribers").request().get();
        assertEquals(OK_200, response.getStatus());

        final ApplicationsResponse applicationsResponse = response.readEntity(ApplicationsResponse.class);
        assertNotNull(applicationsResponse);
        assertEquals(3, applicationsResponse.getData().size());
        assertEquals("A", applicationsResponse.getData().get(0).getId());
        assertEquals("B", applicationsResponse.getData().get(1).getId());
        assertEquals("C", applicationsResponse.getData().get(2).getId());
    }

    @Test
    public void shouldGetApiSubscribersNotAsPrimaryOwner() {
        ApiEntity apiEntity = new ApiEntity();
        apiEntity.setId(API);
        UserEntity user = new UserEntity();
        user.setId("ANOTHER_NAME");
        PrimaryOwnerEntity primaryOwner = new PrimaryOwnerEntity(user);
        apiEntity.setPrimaryOwner(primaryOwner);
        when(apiSearchService.findGenericById(GraviteeContext.getExecutionContext(), API)).thenReturn(apiEntity);

        TopHitsAnalytics mockAnalytics = new TopHitsAnalytics();
        Map<String, Long> mockedValues = new HashMap<>();
        mockedValues.put("A", 10L);
        mockedValues.put("B", 20L);
        mockedValues.put("C", 30L);
        mockAnalytics.setValues(mockedValues);
        doReturn(mockAnalytics).when(analyticsService).execute(eq(GraviteeContext.getExecutionContext()), any(GroupByQuery.class));

        SubscriptionEntity subA1 = new SubscriptionEntity();
        subA1.setApplication("A");
        subA1.setApi(API);
        SubscriptionEntity subC1 = new SubscriptionEntity();
        subC1.setApplication("C");
        subC1.setApi(API);
        doReturn(Arrays.asList(subA1, subC1)).when(subscriptionService).search(eq(GraviteeContext.getExecutionContext()), any());

        ApplicationListItem appA = new ApplicationListItem();
        appA.setId("A");
        ApplicationListItem appC = new ApplicationListItem();
        appC.setId("C");
        Page<ApplicationListItem> applications = new Page(Arrays.asList(appA, appC), 1, 10, 2);
        doReturn(applications)
            .when(applicationService)
            .search(
                eq(GraviteeContext.getExecutionContext()),
                argThat(q -> q.getIds().containsAll(Arrays.asList("A", "C"))),
                eq(new SortableImpl("name", true)),
                eq(null)
            );
        doReturn(new Application().id("A")).when(applicationMapper).convert(eq(GraviteeContext.getExecutionContext()), eq(appA), any());
        doReturn(new Application().id("C")).when(applicationMapper).convert(eq(GraviteeContext.getExecutionContext()), eq(appC), any());

        ApplicationListItem appLIA = new ApplicationListItem();
        appLIA.setId("A");
        ApplicationListItem appLIC = new ApplicationListItem();
        appLIC.setId("C");
        doReturn(new HashSet<>(Arrays.asList(appLIA, appLIC)))
            .when(applicationService)
            .findByUser(GraviteeContext.getExecutionContext(), USER_NAME);

        final Response response = target(API).path("subscribers").request().get();
        assertEquals(OK_200, response.getStatus());

        ArgumentCaptor<SubscriptionQuery> queryCaptor = ArgumentCaptor.forClass(SubscriptionQuery.class);
        Mockito.verify(subscriptionService).search(eq(GraviteeContext.getExecutionContext()), queryCaptor.capture());
        SubscriptionQuery value = queryCaptor.getValue();
        assertNotNull(value.getApplications());
        assertEquals(2, value.getApplications().size());
        assertTrue(value.getApplications().contains("A"));
        assertTrue(value.getApplications().contains("C"));

        final ApplicationsResponse applicationsResponse = response.readEntity(ApplicationsResponse.class);
        assertNotNull(applicationsResponse);
        assertEquals(2, applicationsResponse.getData().size());
        assertEquals("C", applicationsResponse.getData().get(0).getId());
        assertEquals("A", applicationsResponse.getData().get(1).getId());
    }

    @Test
    public void shouldGetNoSubscribers() {
        doReturn(Collections.emptyList()).when(subscriptionService).search(eq(GraviteeContext.getExecutionContext()), any());

        final Response response = target(API).path("subscribers").request().get();
        assertEquals(OK_200, response.getStatus());

        final ApplicationsResponse applicationsResponse = response.readEntity(ApplicationsResponse.class);
        assertNotNull(applicationsResponse);
        assertEquals(0, applicationsResponse.getData().size());
    }
}
