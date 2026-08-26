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

public class AuthzFieldResolver implements FieldResolver {

    private static final String DECISION = "decision";
    private static final String STATUS = "status";
    private static final String STATUS_SUCCESS = "success";
    private static final String API_ID = "api-id";
    private static final String GATEWAY_ID = "gw-id";
    private static final String OPERATION = "operation";
    private static final String CALLER = "caller";
    private static final String SUBJECT_ID = "subject-id";
    private static final String ACTION = "action";
    private static final String RESOURCE_ID = "resource-id";
    private static final String REASONS = "reasons";
    private static final String SEARCH_TYPE = "search-type";
    private static final String DURATION_NANOS = "duration-nanos";

    private static final String DECISION_PERMIT = "PERMIT";
    private static final String DECISION_FORBID = "FORBID";
    private static final String DECISION_NOT_APPLICABLE = "NOT_APPLICABLE";

    @Override
    public String fromMetric(Metric metric) {
        return switch (metric) {
            case AUTHZ_OPERATIONS -> OPERATION;
            case AUTHZ_DECISIONS, AUTHZ_PERMITS, AUTHZ_FORBIDS, AUTHZ_NOT_APPLICABLE -> DECISION;
            case AUTHZ_SEARCHES -> SEARCH_TYPE;
            case AUTHZ_FAILURES -> STATUS;
            case AUTHZ_EVAL_DURATION -> DURATION_NANOS;
            default -> throw new UnsupportedOperationException("AuthzFieldResolver does not support metric " + metric);
        };
    }

    public boolean isDecisionScoped(Metric metric) {
        return switch (metric) {
            case AUTHZ_PERMITS, AUTHZ_FORBIDS, AUTHZ_NOT_APPLICABLE -> true;
            default -> false;
        };
    }

    public String decisionValue(Metric metric) {
        return switch (metric) {
            case AUTHZ_PERMITS -> DECISION_PERMIT;
            case AUTHZ_FORBIDS -> DECISION_FORBID;
            case AUTHZ_NOT_APPLICABLE -> DECISION_NOT_APPLICABLE;
            default -> throw new UnsupportedOperationException("Metric " + metric + " is not decision-scoped");
        };
    }

    public boolean isFailureScoped(Metric metric) {
        return metric == Metric.AUTHZ_FAILURES;
    }

    public String successStatus() {
        return STATUS_SUCCESS;
    }

    @Override
    public String fromFilter(Filter filter) {
        return switch (filter.name()) {
            case Filter.Name.API -> API_ID;
            case Filter.Name.GATEWAY -> GATEWAY_ID;
            case Filter.Name.AUTHZ_DECISION -> DECISION;
            case Filter.Name.AUTHZ_OPERATION -> OPERATION;
            case Filter.Name.AUTHZ_STATUS -> STATUS;
            case Filter.Name.AUTHZ_CALLER -> CALLER;
            case Filter.Name.AUTHZ_SUBJECT_ID -> SUBJECT_ID;
            case Filter.Name.AUTHZ_ACTION -> ACTION;
            case Filter.Name.AUTHZ_RESOURCE_ID -> RESOURCE_ID;
            case Filter.Name.AUTHZ_REASON -> REASONS;
            default -> throw new UnsupportedOperationException("AuthzFieldResolver does not support filter '" + filter.name() + "'");
        };
    }

    @Override
    public String fromFacet(Facet facet) {
        return switch (facet) {
            case API -> API_ID;
            case GATEWAY -> GATEWAY_ID;
            case AUTHZ_DECISION -> DECISION;
            case AUTHZ_OPERATION -> OPERATION;
            case AUTHZ_STATUS -> STATUS;
            case AUTHZ_CALLER -> CALLER;
            case AUTHZ_SUBJECT_ID -> SUBJECT_ID;
            case AUTHZ_ACTION -> ACTION;
            case AUTHZ_RESOURCE_ID -> RESOURCE_ID;
            case AUTHZ_REASON -> REASONS;
            default -> throw new UnsupportedOperationException("AuthzFieldResolver does not support facet '" + facet + "'");
        };
    }
}
