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
package io.gravitee.management.rest.resource;

import io.gravitee.common.http.MediaType;
import io.gravitee.management.model.permissions.RolePermission;
import io.gravitee.management.model.permissions.RolePermissionAction;
import io.gravitee.management.model.analytics.Analytics;
import io.gravitee.management.model.analytics.query.AggregationType;
import io.gravitee.management.model.analytics.query.CountQuery;
import io.gravitee.management.model.analytics.query.DateHistogramQuery;
import io.gravitee.management.model.analytics.query.GroupByQuery;
import io.gravitee.management.rest.resource.param.Aggregation;
import io.gravitee.management.rest.resource.param.AnalyticsParam;
import io.gravitee.management.rest.resource.param.Range;
import io.gravitee.management.rest.security.Permission;
import io.gravitee.management.rest.security.Permissions;
import io.gravitee.management.service.AnalyticsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.inject.Inject;
import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = {"Application"})
public class ApplicationAnalyticsResource extends AbstractResource {

    @Inject
    private AnalyticsService analyticsService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get Application analytics")
    @Permissions({
            @Permission(value = RolePermission.APPLICATION_ANALYTICS, acls = RolePermissionAction.READ)
    })
    public Response hits(
            @PathParam("application") String application,
            @BeanParam AnalyticsParam analyticsParam) {
        analyticsParam.validate();

        Analytics analytics = null;

        switch(analyticsParam.getTypeParam().getValue()) {
            case DATE_HISTO:
                analytics = executeDateHisto(application, analyticsParam);
                break;
            case GROUP_BY:
                analytics = executeGroupBy(application, analyticsParam);
                break;
            case COUNT:
                analytics = executeCount(application, analyticsParam);
                break;
        }

        return Response.ok(analytics).build();
    }

    private Analytics executeCount(String application, AnalyticsParam analyticsParam) {
        CountQuery query = new CountQuery();
        query.setFrom(analyticsParam.getFrom());
        query.setTo(analyticsParam.getTo());
        query.setInterval(analyticsParam.getInterval());
        query.setQuery(analyticsParam.getQuery());
        query.setRootField("application");
        query.setRootIdentifier(application);
        return analyticsService.execute(query);
    }

    private Analytics executeDateHisto(String application, AnalyticsParam analyticsParam) {
        DateHistogramQuery query = new DateHistogramQuery();
        query.setFrom(analyticsParam.getFrom());
        query.setTo(analyticsParam.getTo());
        query.setInterval(analyticsParam.getInterval());
        query.setQuery(analyticsParam.getQuery());
        query.setRootField("application");
        query.setRootIdentifier(application);
        List<Aggregation> aggregations = analyticsParam.getAggregations();
        if (aggregations != null) {
            List<io.gravitee.management.model.analytics.query.Aggregation> aggregationList = aggregations
                    .stream()
                    .map((Function<Aggregation, io.gravitee.management.model.analytics.query.Aggregation>) aggregation -> new io.gravitee.management.model.analytics.query.Aggregation() {
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
        query.setRootField("application");
        query.setRootIdentifier(application);

        if (analyticsParam.getOrder() != null) {
            GroupByQuery.Order order = new GroupByQuery.Order();
            order.setField(analyticsParam.getOrder().getField());
            order.setType(analyticsParam.getOrder().getType());
            order.setOrder(analyticsParam.getOrder().isOrder());
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
