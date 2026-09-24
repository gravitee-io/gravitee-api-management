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
package io.gravitee.repository.elasticsearch.v4.analytics.engine.adapter;

import io.gravitee.repository.analytics.engine.api.metric.Metric;
import io.gravitee.repository.analytics.engine.api.query.Facet;
import io.gravitee.repository.analytics.engine.api.query.Filter;
import io.gravitee.repository.elasticsearch.v4.analytics.engine.adapter.api.FieldResolver;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class AuthzTrafficFieldResolver implements FieldResolver {

    public static final String OPERATION_FIELD = "additional-metrics.keyword_authz_operation";
    private static final String OPERATION_SEARCH = "search";

    private static final String API_ID = "api-id";
    private static final String GATEWAY_ID = "gateway";
    private static final String SEARCH_TYPE = "additional-metrics.keyword_authz_search-type";
    private static final String SUBJECT_TYPE = "additional-metrics.keyword_authz_subject-type";
    private static final String SUBJECT_ID = "additional-metrics.keyword_authz_subject-id";
    private static final String ACTION = "additional-metrics.keyword_authz_action";
    private static final String RESOURCE_TYPE = "additional-metrics.keyword_authz_resource-type";
    private static final String RESOURCE_ID = "additional-metrics.keyword_authz_resource-id";

    private static final Set<Filter.Name> SEARCH_ONLY_FILTERS = Set.of(
        Filter.Name.AUTHZ_SUBJECT_ID,
        Filter.Name.AUTHZ_ACTION,
        Filter.Name.AUTHZ_RESOURCE_ID
    );

    public record ScopeTerm(String field, String value) {}

    @Override
    public String fromMetric(Metric metric) {
        return switch (metric) {
            case AUTHZ_OPERATIONS, AUTHZ_SEARCHES -> OPERATION_FIELD;
            default -> throw new UnsupportedOperationException("AuthzTrafficFieldResolver does not support metric " + metric);
        };
    }

    public Optional<ScopeTerm> scopeTerm(Metric metric) {
        return switch (metric) {
            case AUTHZ_SEARCHES -> Optional.of(new ScopeTerm(OPERATION_FIELD, OPERATION_SEARCH));
            default -> Optional.empty();
        };
    }

    public boolean countsNothing(Metric metric, List<Filter> filters) {
        return metric == Metric.AUTHZ_OPERATIONS && filters.stream().anyMatch(filter -> SEARCH_ONLY_FILTERS.contains(filter.name()));
    }

    @Override
    public String fromFilter(Filter filter) {
        return switch (filter.name()) {
            case Filter.Name.API -> API_ID;
            case Filter.Name.GATEWAY -> GATEWAY_ID;
            case Filter.Name.AUTHZ_OPERATION -> OPERATION_FIELD;
            case Filter.Name.AUTHZ_SEARCH_TYPE -> SEARCH_TYPE;
            case Filter.Name.AUTHZ_SUBJECT_ID -> SUBJECT_ID;
            case Filter.Name.AUTHZ_ACTION -> ACTION;
            case Filter.Name.AUTHZ_RESOURCE_ID -> RESOURCE_ID;
            default -> throw new UnsupportedOperationException("AuthzTrafficFieldResolver does not support filter '" + filter.name() + "'");
        };
    }

    @Override
    public String entityTypeFromFilter(Filter filter) {
        return switch (filter.name()) {
            case Filter.Name.AUTHZ_SUBJECT_ID -> SUBJECT_TYPE;
            case Filter.Name.AUTHZ_RESOURCE_ID -> RESOURCE_TYPE;
            default -> FieldResolver.super.entityTypeFromFilter(filter);
        };
    }

    @Override
    public String fromFacet(Facet facet) {
        return switch (facet) {
            case API -> API_ID;
            case GATEWAY -> GATEWAY_ID;
            case AUTHZ_OPERATION -> OPERATION_FIELD;
            case AUTHZ_SEARCH_TYPE -> SEARCH_TYPE;
            case AUTHZ_SUBJECT_ID -> SUBJECT_ID;
            case AUTHZ_ACTION -> ACTION;
            case AUTHZ_RESOURCE_ID -> RESOURCE_ID;
            default -> throw new UnsupportedOperationException("AuthzTrafficFieldResolver does not support facet '" + facet + "'");
        };
    }
}
