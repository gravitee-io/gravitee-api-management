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

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.analytics.Analytics;
import io.gravitee.rest.api.model.analytics.HistogramAnalytics;
import io.gravitee.rest.api.model.analytics.HitsAnalytics;
import io.gravitee.rest.api.model.analytics.TopHitsAnalytics;
import io.gravitee.rest.api.model.analytics.query.AggregationType;
import io.gravitee.rest.api.model.analytics.query.CountQuery;
import io.gravitee.rest.api.model.analytics.query.DateHistogramQuery;
import io.gravitee.rest.api.model.analytics.query.GroupByQuery;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.portal.rest.mapper.AnalyticsMapper;
import io.gravitee.rest.api.portal.rest.resource.param.Aggregation;
import io.gravitee.rest.api.portal.rest.resource.param.AnalyticsParam;
import io.gravitee.rest.api.portal.rest.resource.param.Range;
import io.gravitee.rest.api.portal.rest.security.Permission;
import io.gravitee.rest.api.portal.rest.security.Permissions;
import io.gravitee.rest.api.service.AnalyticsService;

/**
 * @author Florent CHAMFROY (forent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApplicationAnalyticsResource extends AbstractResource {

    private static final String ANALYTICS_ROOT_FIELD = "application";

    @Inject
    private AnalyticsService analyticsService;
    
    @Inject
    private AnalyticsMapper analyticsMapper;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({
            @Permission(value = RolePermission.APPLICATION_ANALYTICS, acls = RolePermissionAction.READ)
    })
    public Response hits(
            @PathParam("applicationId") String applicationId,
            @BeanParam AnalyticsParam analyticsParam) {
        analyticsParam.validate();

        Object analytics = null;
        switch(analyticsParam.getTypeParam().getValue()) {
            case DATE_HISTO:
                analytics = analyticsMapper.convert((HistogramAnalytics) executeDateHisto(applicationId, analyticsParam));
                break;
            case GROUP_BY:
                analytics = analyticsMapper.convert((TopHitsAnalytics) executeGroupBy(applicationId, analyticsParam));
                break;
            case COUNT:
                analytics = analyticsMapper.convert((HitsAnalytics) executeCount(applicationId, analyticsParam));
                break;
            default:
                break;
        }

        return Response
                .ok(analytics)
                .build();
    }

    private Analytics executeCount(String application, AnalyticsParam analyticsParam) {
        CountQuery query = new CountQuery();
        query.setFrom(analyticsParam.getFrom());
        query.setTo(analyticsParam.getTo());
        query.setInterval(analyticsParam.getInterval());
        query.setQuery(analyticsParam.getQuery());
        query.setRootField(ANALYTICS_ROOT_FIELD);
        query.setRootIdentifier(application);
        return analyticsService.execute(query);
    }

    private Analytics executeDateHisto(String application, AnalyticsParam analyticsParam) {
        DateHistogramQuery query = new DateHistogramQuery();
        query.setFrom(analyticsParam.getFrom());
        query.setTo(analyticsParam.getTo());
        query.setInterval(analyticsParam.getInterval());
        query.setQuery(analyticsParam.getQuery());
        query.setRootField(ANALYTICS_ROOT_FIELD);
        query.setRootIdentifier(application);
        List<Aggregation> aggregations = analyticsParam.getAggregations();
        if (aggregations != null) {
            List<io.gravitee.rest.api.model.analytics.query.Aggregation> aggregationList = aggregations
                    .stream()
                    .map((Function<Aggregation, io.gravitee.rest.api.model.analytics.query.Aggregation>) aggregation -> new io.gravitee.rest.api.model.analytics.query.Aggregation() {
                        @Override
                        public AggregationType type() {
                            return AggregationType.valueOf(aggregation.getType().name().toUpperCase());
                        }

                        @Override
                        public String field() {
                            return aggregation.getField();
                        }
                    }).collect(Collectors.toList());

            query.setAggregations(aggregationList);
        }
        return analyticsService.execute(query);
    }

    private Analytics executeGroupBy(String application, AnalyticsParam analyticsParam) {
        GroupByQuery query = new GroupByQuery();
        query.setFrom(analyticsParam.getFrom());
        query.setTo(analyticsParam.getTo());
        query.setInterval(analyticsParam.getInterval());
        query.setQuery(analyticsParam.getQuery());
        query.setField(analyticsParam.getField());
        query.setRootField(ANALYTICS_ROOT_FIELD);
        query.setRootIdentifier(application);

        if (analyticsParam.getOrder() != null) {
            GroupByQuery.Order order = new GroupByQuery.Order();
            order.setField(analyticsParam.getOrder().getField());
            order.setType(analyticsParam.getOrder().getType());
            order.setOrder(analyticsParam.getOrder().isSorted());
            query.setOrder(order);
        }

        List<Range> ranges = analyticsParam.getRanges();
        if (ranges != null) {
            Map<Double, Double> rangeMap = ranges.stream().collect(
                    Collectors.toMap(Range::getFrom, Range::getTo));

            query.setGroups(rangeMap);
        }
        return analyticsService.execute(query);
    }
}
