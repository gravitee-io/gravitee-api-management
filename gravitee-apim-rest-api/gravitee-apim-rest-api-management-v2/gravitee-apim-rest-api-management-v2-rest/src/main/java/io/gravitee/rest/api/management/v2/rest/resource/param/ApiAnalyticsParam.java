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
package io.gravitee.rest.api.management.v2.rest.resource.param;

import io.gravitee.apim.core.analytics.query_service.AnalyticsQueryService;
import io.gravitee.apim.core.analytics.use_case.SearchGroupByAnalyticsUseCase;
import io.gravitee.apim.core.analytics.use_case.SearchHistogramAnalyticsUseCase;
import io.gravitee.rest.api.management.v2.rest.model.AnalyticsType;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.QueryParam;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Setter
@Getter
public class ApiAnalyticsParam {

    @QueryParam("from")
    private Long from;

    @QueryParam("to")
    private Long to;

    @QueryParam("interval")
    private Long interval;

    @QueryParam("field")
    private String field;

    @QueryParam("size")
    private Integer size;

    @QueryParam("type")
    private AnalyticsType type;

    @QueryParam("ranges")
    private String ranges;

    @QueryParam("aggregations")
    private String aggregations;

    @QueryParam("order")
    private String order;

    @QueryParam("query")
    private String query;

    public List<Range> getRanges() {
        if (ranges == null || ranges.isEmpty()) {
            return List.of();
        }
        // Split by comma, then parse each "from:to"
        return java.util.Arrays
            .stream(ranges.split(";"))
            .map(String::trim)
            .map(param -> {
                String[] bounds = param.split(":");
                if (bounds.length == 2) {
                    try {
                        int from = Integer.parseInt(bounds[0]);
                        int to = Integer.parseInt(bounds[1]);
                        return new Range(from, to);
                    } catch (NumberFormatException ignored) {
                        log.debug("NumberFormatException ignored in ApiAnalyticsParam");
                    }
                }
                return null;
            })
            .filter(java.util.Objects::nonNull)
            .toList();
    }

    public List<Aggregation> getAggregations() {
        if (aggregations == null || aggregations.isEmpty()) {
            return List.of();
        }
        // Split by comma, then parse each "type:field"
        return java.util.Arrays
            .stream(aggregations.split(","))
            .map(String::trim)
            .map(param -> {
                String[] parts = param.split(":");
                if (parts.length == 2) {
                    return new Aggregation(parts[0], parts[1]);
                }
                return null;
            })
            .filter(java.util.Objects::nonNull)
            .toList();
    }

    public static SearchHistogramAnalyticsUseCase.Input toHistogramInput(String apiId, ApiAnalyticsParam param) {
        var aggregations = param
            .getAggregations()
            .stream()
            .map(a -> {
                if (a.getType() == null) return null;
                try {
                    var type = io.gravitee.apim.core.analytics.model.Aggregation.AggregationType.valueOf(a.getType().toUpperCase());
                    return new io.gravitee.apim.core.analytics.model.Aggregation(a.getField(), type);
                } catch (IllegalArgumentException ex) {
                    throw new BadRequestException("Invalid aggregation type: " + a.getType(), ex);
                }
            })
            .filter(java.util.Objects::nonNull)
            .toList();

        return new SearchHistogramAnalyticsUseCase.Input(apiId, param.getFrom(), param.getTo(), param.getInterval(), aggregations);
    }

    public static SearchGroupByAnalyticsUseCase.Input toGroupByInput(String apiId, ApiAnalyticsParam param) {
        List<AnalyticsQueryService.GroupByQuery.Group> groups = param
            .getRanges()
            .stream()
            .map(r -> new AnalyticsQueryService.GroupByQuery.Group(r.getFrom(), r.getTo()))
            .toList();

        AnalyticsQueryService.GroupByQuery.Order order = null;
        if (param.getOrder() != null) {
            order = AnalyticsQueryService.GroupByQuery.Order.valueOf(param.getOrder());
        }

        return new SearchGroupByAnalyticsUseCase.Input(
            apiId,
            param.getFrom(),
            param.getTo(),
            param.getInterval(),
            param.getField(),
            groups,
            order,
            param.getQuery() // propagate query parameter
        );
    }

    public static io.gravitee.apim.core.analytics.use_case.SearchStatsUseCase.Input toStatsInput(String apiId, ApiAnalyticsParam param) {
        return new io.gravitee.apim.core.analytics.use_case.SearchStatsUseCase.Input(
            apiId,
            param.getFrom(),
            param.getTo(),
            param.getField()
        );
    }

    @Getter
    @Setter
    public static class Range {

        private int from;
        private int to;

        public Range(int from, int to) {
            this.from = from;
            this.to = to;
        }
    }

    @Getter
    @Setter
    public static class Aggregation {

        private String type;
        private String field;

        public Aggregation(String type, String field) {
            this.type = type;
            this.field = field;
        }
    }
}
