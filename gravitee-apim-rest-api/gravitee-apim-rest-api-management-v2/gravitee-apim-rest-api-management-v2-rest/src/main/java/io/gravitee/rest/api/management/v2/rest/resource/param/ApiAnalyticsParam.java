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

import static java.util.Optional.ofNullable;

import io.gravitee.apim.core.analytics.query_service.AnalyticsQueryService;
import io.gravitee.apim.core.analytics.use_case.SearchGroupByAnalyticsUseCase;
import io.gravitee.apim.core.analytics.use_case.SearchHistogramAnalyticsUseCase;
import io.gravitee.rest.api.management.v2.rest.model.AnalyticsType;
import io.gravitee.rest.api.management.v2.rest.validation.ApiAnalyticsParamSpecification;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.QueryParam;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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

    @QueryParam("type")
    private String type;

    @QueryParam("ranges")
    private String ranges;

    @QueryParam("aggregations")
    private String aggregations;

    @QueryParam("order")
    private String order;

    @QueryParam("query")
    private String query;

    public AnalyticsType getType() {
        return Arrays
            .stream(AnalyticsType.values())
            .filter(analyticsType -> analyticsType.name().equalsIgnoreCase(type))
            .findFirst()
            .orElse(null);
    }

    public List<Range> getRanges() {
        if (ranges == null || ranges.isEmpty()) {
            return List.of();
        }
        // Split by comma, then parse each "from:to"
        return java.util.Arrays
            .stream(ranges.split("[;,]"))
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
            .stream(aggregations.split("[;,]"))
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

    public SearchHistogramAnalyticsUseCase.Input toHistogramInput(String apiId) {
        ApiAnalyticsParamSpecification.forHistogram().throwIfNotSatisfied(this);
        var aggregations = getAggregations()
            .stream()
            .filter(a -> Objects.nonNull(a.getType()))
            .map(a -> {
                try {
                    var type = io.gravitee.apim.core.analytics.model.Aggregation.AggregationType.valueOf(a.getType().toUpperCase());
                    return new io.gravitee.apim.core.analytics.model.Aggregation(a.getField(), type);
                } catch (IllegalArgumentException ex) {
                    throw new BadRequestException("Invalid aggregation type: " + a.getType(), ex);
                }
            })
            .toList();

        return new SearchHistogramAnalyticsUseCase.Input(
            apiId,
            getFrom(),
            getTo(),
            getInterval(),
            aggregations,
            Optional.ofNullable(getQuery())
        );
    }

    public SearchGroupByAnalyticsUseCase.Input toGroupByInput(String apiId) {
        ApiAnalyticsParamSpecification.forGroupBy().throwIfNotSatisfied(this);
        List<AnalyticsQueryService.GroupByQuery.Group> groups = getRanges()
            .stream()
            .map(r -> new AnalyticsQueryService.GroupByQuery.Group(r.getFrom(), r.getTo()))
            .toList();

        var order = ofNullable(getOrder()).map(AnalyticsQueryService.GroupByQuery.Order::valueOf);

        return new SearchGroupByAnalyticsUseCase.Input(apiId, getFrom(), getTo(), getField(), groups, order, ofNullable(getQuery()));
    }

    public io.gravitee.apim.core.analytics.use_case.SearchStatsUseCase.Input toStatsInput(String apiId) {
        ApiAnalyticsParamSpecification.forStats().throwIfNotSatisfied(this);
        return new io.gravitee.apim.core.analytics.use_case.SearchStatsUseCase.Input(
            apiId,
            getFrom(),
            getTo(),
            getField(),
            ofNullable(getQuery())
        );
    }

    public io.gravitee.apim.core.analytics.use_case.SearchRequestsCountByEventAnalyticsUseCase.Input toRequestsCountInput(String apiId) {
        ApiAnalyticsParamSpecification.forCount().throwIfNotSatisfied(this);
        return new io.gravitee.apim.core.analytics.use_case.SearchRequestsCountByEventAnalyticsUseCase.Input(apiId, getFrom(), getTo());
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
