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

import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.analytics.query.StatsAnalytics;
import io.gravitee.rest.api.model.analytics.query.StatsQuery;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.portal.rest.model.ApiMetrics;
import io.gravitee.rest.api.portal.rest.model.Error;
import io.gravitee.rest.api.portal.rest.model.ErrorResponse;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;

import java.io.IOException;
import java.util.*;

import static io.gravitee.common.http.HttpStatusCode.NOT_FOUND_404;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiMetricsResourceTest extends AbstractResourceTest {

    private static final String API = "my-api";
    private static final Float API_NB_HITS = Float.valueOf(150000);
    private static final Double API_HEALTHCHECK_RATIO = 92.9999991199;
    private static final Number EXPECTED_API_HEALTHCHECK_RATIO = 0.9299;

    protected String contextPath() {
        return "apis/";
    }

    @Before
    public void init() throws IOException {
        resetAllMocks();

        ApiEntity mockApi = new ApiEntity();
        mockApi.setId(API);
        doReturn(mockApi).when(apiService).findById(API);

        Set<ApiEntity> mockApis = new HashSet<>(Arrays.asList(mockApi));
        doReturn(mockApis).when(apiService).findPublishedByUser(any(), argThat(q -> singletonList(API).equals(q.getIds())));
    }

    @Test
    public void shouldNotFoundApiWhileGettingApiMetrics() {
        // init
        ApiEntity userApi = new ApiEntity();
        userApi.setId("1");
        doReturn(emptySet()).when(apiService).findPublishedByUser(any(), argThat(q -> singletonList(API).equals(q.getIds())));

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
    public void shouldGetApiMetrics() {
        StatsAnalytics mockAnalytics = new StatsAnalytics();
        mockAnalytics.setCount(API_NB_HITS);
        doReturn(mockAnalytics).when(analyticsService).execute(any(StatsQuery.class));

        SubscriptionEntity subA1 = new SubscriptionEntity();
        subA1.setApplication("A");
        subA1.setApi(API);
        SubscriptionEntity subA2 = new SubscriptionEntity();
        subA2.setApplication("A");
        subA2.setApi("2");
        SubscriptionEntity subB1 = new SubscriptionEntity();
        subB1.setApplication("B");
        subB1.setApi(API);
        SubscriptionEntity subC4 = new SubscriptionEntity();
        subC4.setApplication("C");
        subC4.setApi("4");
        SubscriptionEntity subC8 = new SubscriptionEntity();
        subC8.setApplication("C");
        subC8.setApi("8");
        doReturn(Arrays.asList(subC8, subA2, subB1, subC4, subA1)).when(subscriptionService).search(any());

        io.gravitee.rest.api.model.healthcheck.ApiMetrics<Number> mockMetrics = new io.gravitee.rest.api.model.healthcheck.ApiMetrics<>();
        Map<String, Double> globalMetrics = new HashMap<>();
        globalMetrics.put("1w", API_HEALTHCHECK_RATIO);
        mockMetrics.setGlobal(globalMetrics);
        doReturn(mockMetrics).when(healthCheckService).getAvailability(any(), any());

        final Response response = target(API).path("metrics").request().get();
        assertEquals(OK_200, response.getStatus());

        final ApiMetrics apiMetrics = response.readEntity(ApiMetrics.class);
        assertNotNull(apiMetrics);
        assertEquals(Double.valueOf(API_NB_HITS.doubleValue()), apiMetrics.getHits());
        assertEquals(EXPECTED_API_HEALTHCHECK_RATIO, apiMetrics.getHealth());
        assertEquals(2, apiMetrics.getSubscribers());
    }

    @Test
    public void shouldGetEmptyApiMetrics() {
        // Case 1
        doReturn(null).when(analyticsService).execute(any(StatsQuery.class));
        doReturn(null).when(subscriptionService).search(any());
        doReturn(null).when(healthCheckService).getAvailability(any(), any());

        Response response = target(API).path("metrics").request().get();
        assertEquals(OK_200, response.getStatus());

        ApiMetrics apiMetrics = response.readEntity(ApiMetrics.class);
        assertNotNull(apiMetrics);
        assertNull(apiMetrics.getHits());
        assertNull(apiMetrics.getHealth());
        assertNull(apiMetrics.getSubscribers());
        
        // Case 2
        doReturn(null).when(analyticsService).execute(any(StatsQuery.class));
        doReturn(Collections.emptyList()).when(subscriptionService).search(any());
        doReturn(new io.gravitee.rest.api.model.healthcheck.ApiMetrics<Number>()).when(healthCheckService).getAvailability(any(), any());

        response = target(API).path("metrics").request().get();
        assertEquals(OK_200, response.getStatus());

        apiMetrics = response.readEntity(ApiMetrics.class);
        assertNotNull(apiMetrics);
        assertNull(apiMetrics.getHits());
        assertNull(apiMetrics.getHealth());
        assertNull(apiMetrics.getSubscribers());
        
        // Case 3
        doReturn(null).when(analyticsService).execute(any(StatsQuery.class));
        doReturn(null).when(subscriptionService).search(any());
        io.gravitee.rest.api.model.healthcheck.ApiMetrics<Number> mockedMetrics = new io.gravitee.rest.api.model.healthcheck.ApiMetrics<>();
        mockedMetrics.setGlobal(Collections.singletonMap("1w", Double.NaN));
        doReturn(mockedMetrics).when(healthCheckService).getAvailability(any(), any());

        response = target(API).path("metrics").request().get();
        assertEquals(OK_200, response.getStatus());

        apiMetrics = response.readEntity(ApiMetrics.class);
        assertNotNull(apiMetrics);
        assertNull(apiMetrics.getHits());
        assertNull(apiMetrics.getHealth());
        assertNull(apiMetrics.getSubscribers());
    }
}
