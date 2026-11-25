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
package io.gravitee.repository.elasticsearch.v4.log.adapter.guardian;

import static io.gravitee.repository.elasticsearch.v4.log.adapter.decision.DecisionQueryClauses.addContainsIfAny;
import static io.gravitee.repository.elasticsearch.v4.log.adapter.decision.DecisionQueryClauses.addTerms;
import static io.gravitee.repository.elasticsearch.v4.log.adapter.decision.DecisionQueryClauses.addTermsIfAny;
import static io.gravitee.repository.elasticsearch.v4.log.adapter.decision.DecisionQueryClauses.addTimeRange;

import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.elasticsearch.v4.log.adapter.decision.DecisionQueryClauses;
import io.gravitee.repository.elasticsearch.v4.log.adapter.decision.DecisionReportFields;
import io.gravitee.repository.log.v4.model.guardian.GuardianDecisionLogQuery;

/**
 * @author GraviteeSource Team
 */
public final class SearchGuardianDecisionLogsQueryAdapter {

    /** What the gateway writes as the decision point type of the report of a Guardian run. */
    static final String GUARDIAN = "guardian";

    private SearchGuardianDecisionLogsQueryAdapter() {}

    public static String adapt(QueryContext queryContext, GuardianDecisionLogQuery query) {
        var filters = DecisionQueryClauses.scopedFilters(queryContext, GUARDIAN);
        if (query.getApiIds() != null) {
            addTerms(filters, DecisionReportFields.API_ID, query.getApiIds());
        }
        addTermsIfAny(filters, DecisionReportFields.DECISION_POINT_ID, query.getGuardianIds());
        addTermsIfAny(filters, DecisionReportFields.OUTCOME, query.getOutcomes());
        addTermsIfAny(filters, DecisionReportFields.REQUEST_ID, query.getRequestIds());
        addContainsIfAny(filters, DecisionReportFields.REQUEST_ID, query.getRequestIdContains());
        addTimeRange(filters, query.getFrom(), query.getTo());
        return DecisionQueryClauses.newestFirstPage(filters, query.getPage(), query.getSize());
    }
}
