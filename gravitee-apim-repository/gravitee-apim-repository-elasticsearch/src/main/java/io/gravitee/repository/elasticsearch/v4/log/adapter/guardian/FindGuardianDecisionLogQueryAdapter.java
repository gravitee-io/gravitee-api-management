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

import static io.gravitee.repository.elasticsearch.v4.log.adapter.decision.DecisionQueryClauses.addTerm;

import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.elasticsearch.v4.log.adapter.decision.DecisionQueryClauses;
import io.gravitee.repository.elasticsearch.v4.log.adapter.decision.DecisionReportFields;

/**
 * Reads one decision of a Guardian by its event id, within the organization and the environment of the caller.
 *
 * @author GraviteeSource Team
 */
public final class FindGuardianDecisionLogQueryAdapter {

    private FindGuardianDecisionLogQueryAdapter() {}

    public static String adapt(QueryContext queryContext, String eventId) {
        var filters = DecisionQueryClauses.scopedFilters(queryContext, SearchGuardianDecisionLogsQueryAdapter.GUARDIAN);
        addTerm(filters, DecisionReportFields.EVENT_ID, eventId);
        return DecisionQueryClauses.singleHit(filters);
    }
}
