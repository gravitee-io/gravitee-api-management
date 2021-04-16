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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.analytics.query.Aggregation;
import io.gravitee.rest.api.model.analytics.query.AggregationType;
import io.gravitee.rest.api.model.analytics.query.CountQuery;
import io.gravitee.rest.api.model.analytics.query.DateHistogramQuery;
import io.gravitee.rest.api.model.analytics.query.GroupByQuery;
import io.gravitee.rest.api.model.analytics.query.GroupByQuery.Order;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.Response;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApplicationAnalyticsResourceTest extends AbstractResourceTest {

    @Override
    protected String contextPath() {
        return "applications/";
    }

    private static final String APPLICATION = "my-application";
    private static final String ANALYTICS_ROOT_FIELD = "application";

    @Test
    public void shouldGetDateHistoAnalytics() {
        final Response response = target(APPLICATION)
            .path("analytics")
            .queryParam("from", 0)
            .queryParam("to", 100)
            .queryParam("interval", 10_000)
            .queryParam("query", APPLICATION)
            .queryParam("type", "DATE_HISTO")
            .queryParam("aggs", "AVG:hit")
            .request()
            .get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        ArgumentCaptor<DateHistogramQuery> queryCaptor = ArgumentCaptor.forClass(DateHistogramQuery.class);
        Mockito.verify(analyticsService).execute(queryCaptor.capture());
        final DateHistogramQuery query = queryCaptor.getValue();
        assertEquals(0, query.getFrom());
        assertEquals(100, query.getTo());
        assertEquals(10_000, query.getInterval());
        assertEquals(ANALYTICS_ROOT_FIELD, query.getRootField());
        assertEquals(APPLICATION, query.getRootIdentifier());
        List<Aggregation> aggregations = query.getAggregations();
        assertNotNull(aggregations);
        assertEquals(1, aggregations.size());
        Aggregation agg = aggregations.get(0);
        assertNotNull(agg);
        assertEquals(AggregationType.AVG, agg.type());
        assertEquals("hit", agg.field());
    }

    @Test
    public void shouldGetGroupByAnalytics() {
        final Response response = target(APPLICATION)
            .path("analytics")
            .queryParam("from", 0)
            .queryParam("to", 100)
            .queryParam("interval", 10_000)
            .queryParam("query", APPLICATION)
            .queryParam("field", APPLICATION)
            .queryParam("type", "GROUP_BY")
            .queryParam("ranges", "10:20")
            .queryParam("order", "orderType:orderField")
            .request()
            .get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        ArgumentCaptor<GroupByQuery> queryCaptor = ArgumentCaptor.forClass(GroupByQuery.class);
        Mockito.verify(analyticsService).execute(queryCaptor.capture());
        final GroupByQuery query = queryCaptor.getValue();
        assertEquals(0, query.getFrom());
        assertEquals(100, query.getTo());
        assertEquals(10_000, query.getInterval());
        assertEquals(APPLICATION, query.getQuery());
        assertEquals(APPLICATION, query.getField());
        assertEquals(ANALYTICS_ROOT_FIELD, query.getRootField());
        assertEquals(APPLICATION, query.getRootIdentifier());
        Order order = query.getOrder();
        assertNotNull(order);
        assertEquals("orderType", order.getType());
        assertEquals("orderField", order.getField());
        assertTrue(order.isOrder());
        Map<Double, Double> groups = query.getGroups();
        assertNotNull(groups);
        assertEquals(1, groups.size());
        Double upperRange = groups.get(Double.valueOf(10));
        assertNotNull(upperRange);
        assertEquals(0, upperRange.compareTo(Double.valueOf(20)));
    }

    @Test
    public void shouldGetCountAnalytics() {
        final Response response = target(APPLICATION)
            .path("analytics")
            .queryParam("from", 0)
            .queryParam("to", 100)
            .queryParam("interval", 10_000)
            .queryParam("query", APPLICATION)
            .queryParam("type", "COUNT")
            .request()
            .get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        ArgumentCaptor<CountQuery> queryCaptor = ArgumentCaptor.forClass(CountQuery.class);
        Mockito.verify(analyticsService).execute(queryCaptor.capture());
        final CountQuery query = queryCaptor.getValue();
        assertEquals(0, query.getFrom());
        assertEquals(100, query.getTo());
        assertEquals(10_000, query.getInterval());
        assertEquals(APPLICATION, query.getQuery());
        assertEquals(ANALYTICS_ROOT_FIELD, query.getRootField());
        assertEquals(APPLICATION, query.getRootIdentifier());
    }
}
