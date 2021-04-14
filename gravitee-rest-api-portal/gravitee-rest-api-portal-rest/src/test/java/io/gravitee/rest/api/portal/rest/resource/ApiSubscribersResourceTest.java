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
package io.gravitee.rest.api.portal.rest.resource;

import static io.gravitee.common.http.HttpStatusCode.NOT_FOUND_404;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;

import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.analytics.TopHitsAnalytics;
import io.gravitee.rest.api.model.analytics.query.GroupByQuery;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.application.ApplicationListItem;
import io.gravitee.rest.api.model.subscription.SubscriptionQuery;
import io.gravitee.rest.api.portal.rest.model.Application;
import io.gravitee.rest.api.portal.rest.model.ApplicationsResponse;
import io.gravitee.rest.api.portal.rest.model.Error;
import io.gravitee.rest.api.portal.rest.model.ErrorResponse;
import java.io.IOException;
import java.util.*;
import javax.ws.rs.core.Response;
import org.junit.Before;
import org.junit.Test;
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

    @Before
    public void init() throws IOException {
        resetAllMocks();

        ApiEntity mockApi = new ApiEntity();
        mockApi.setId(API);
        UserEntity user = new UserEntity();
        user.setId(USER_NAME);
        PrimaryOwnerEntity primaryOwner = new PrimaryOwnerEntity(user);
        mockApi.setPrimaryOwner(primaryOwner);
        Set<ApiEntity> mockApis = new HashSet<>(Arrays.asList(mockApi));
        doReturn(mockApis).when(apiService).findPublishedByUser(any());
    }

    @Test
    public void shouldNotFoundApiWhileGettingApiSubscribers() {
        // init
        ApiEntity userApi = new ApiEntity();
        userApi.setId("1");
        Set<ApiEntity> mockApis = new HashSet<>(Arrays.asList(userApi));
        doReturn(mockApis).when(apiService).findPublishedByUser(any());

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
        assertEquals("Api [" + API + "] can not be found.", error.getMessage());
    }

    @Test
    public void shouldGetApiSubscribersAsPrimaryOwner() {
        TopHitsAnalytics mockAnalytics = new TopHitsAnalytics();
        Map<String, Long> mockedValues = new HashMap<>();
        mockedValues.put("A", 10L);
        mockedValues.put("B", 20L);
        mockedValues.put("C", 30L);
        mockAnalytics.setValues(mockedValues);
        doReturn(mockAnalytics).when(analyticsService).execute(any(GroupByQuery.class));

        SubscriptionEntity subA1 = new SubscriptionEntity();
        subA1.setApplication("A");
        subA1.setApi(API);
        SubscriptionEntity subB1 = new SubscriptionEntity();
        subB1.setApplication("B");
        subB1.setApi(API);
        SubscriptionEntity subC1 = new SubscriptionEntity();
        subC1.setApplication("C");
        subC1.setApi(API);
        doReturn(Arrays.asList(subB1, subC1, subA1)).when(subscriptionService).search(any());

        ApplicationEntity appA = new ApplicationEntity();
        appA.setId("A");
        doReturn(appA).when(applicationService).findById("A");
        doReturn(new Application().id("A")).when(applicationMapper).convert(eq(appA), any());

        ApplicationEntity appB = new ApplicationEntity();
        appB.setId("B");
        doReturn(appB).when(applicationService).findById("B");
        doReturn(new Application().id("B")).when(applicationMapper).convert(eq(appB), any());

        ApplicationEntity appC = new ApplicationEntity();
        appC.setId("C");
        doReturn(appC).when(applicationService).findById("C");
        doReturn(new Application().id("C")).when(applicationMapper).convert(eq(appC), any());

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
        doReturn(mockAnalytics).when(analyticsService).execute(any(GroupByQuery.class));

        SubscriptionEntity subA1 = new SubscriptionEntity();
        subA1.setApplication("A");
        subA1.setApi(API);
        SubscriptionEntity subB1 = new SubscriptionEntity();
        subB1.setApplication("B");
        subB1.setApi(API);
        SubscriptionEntity subC1 = new SubscriptionEntity();
        subC1.setApplication("C");
        subC1.setApi(API);
        doReturn(Arrays.asList(subB1, subC1, subA1)).when(subscriptionService).search(any());

        ApplicationEntity appA = new ApplicationEntity();
        appA.setId("A");
        appA.setName("A");
        doReturn(appA).when(applicationService).findById("A");
        doReturn(new Application().id("A").name("A")).when(applicationMapper).convert(eq(appA), any());

        ApplicationEntity appB = new ApplicationEntity();
        appB.setId("B");
        appB.setName("B");
        doReturn(appB).when(applicationService).findById("B");
        doReturn(new Application().id("B").name("B")).when(applicationMapper).convert(eq(appB), any());

        ApplicationEntity appC = new ApplicationEntity();
        appC.setId("C");
        appC.setName("C");
        doReturn(appC).when(applicationService).findById("C");
        doReturn(new Application().id("C").name("C")).when(applicationMapper).convert(eq(appC), any());

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
        ApiEntity mockApi = new ApiEntity();
        mockApi.setId(API);
        UserEntity user = new UserEntity();
        user.setId("ANOTHER_NAME");
        PrimaryOwnerEntity primaryOwner = new PrimaryOwnerEntity(user);
        mockApi.setPrimaryOwner(primaryOwner);
        Set<ApiEntity> mockApis = new HashSet<>(Arrays.asList(mockApi));
        doReturn(mockApis).when(apiService).findPublishedByUser(any());

        TopHitsAnalytics mockAnalytics = new TopHitsAnalytics();
        Map<String, Long> mockedValues = new HashMap<>();
        mockedValues.put("A", 10L);
        mockedValues.put("B", 20L);
        mockedValues.put("C", 30L);
        mockAnalytics.setValues(mockedValues);
        doReturn(mockAnalytics).when(analyticsService).execute(any(GroupByQuery.class));

        SubscriptionEntity subA1 = new SubscriptionEntity();
        subA1.setApplication("A");
        subA1.setApi(API);
        SubscriptionEntity subC1 = new SubscriptionEntity();
        subC1.setApplication("C");
        subC1.setApi(API);
        doReturn(Arrays.asList(subA1, subC1)).when(subscriptionService).search(any());

        ApplicationEntity appA = new ApplicationEntity();
        appA.setId("A");
        doReturn(appA).when(applicationService).findById("A");
        doReturn(new Application().id("A")).when(applicationMapper).convert(eq(appA), any());

        ApplicationEntity appC = new ApplicationEntity();
        appC.setId("C");
        doReturn(appC).when(applicationService).findById("C");
        doReturn(new Application().id("C")).when(applicationMapper).convert(eq(appC), any());

        ApplicationListItem appLIA = new ApplicationListItem();
        appLIA.setId("A");
        ApplicationListItem appLIC = new ApplicationListItem();
        appLIC.setId("C");
        doReturn(new HashSet<>(Arrays.asList(appLIA, appLIC))).when(applicationService).findByUser(USER_NAME);

        final Response response = target(API).path("subscribers").request().get();
        assertEquals(OK_200, response.getStatus());

        ArgumentCaptor<SubscriptionQuery> queryCaptor = ArgumentCaptor.forClass(SubscriptionQuery.class);
        Mockito.verify(subscriptionService).search(queryCaptor.capture());
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
        doReturn(Collections.emptyList()).when(subscriptionService).search(any());

        final Response response = target(API).path("subscribers").request().get();
        assertEquals(OK_200, response.getStatus());

        final ApplicationsResponse applicationsResponse = response.readEntity(ApplicationsResponse.class);
        assertNotNull(applicationsResponse);
        assertEquals(0, applicationsResponse.getData().size());
    }
}
