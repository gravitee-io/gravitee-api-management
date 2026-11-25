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
package io.gravitee.repository.elasticsearch.v4.log.adapter.authz;

import io.gravitee.repository.elasticsearch.v4.log.adapter.decision.DecisionReportFields;

/**
 * Indexed field names of an authz {@code DecisionReport} document, as written by the reporter's
 * FreeMarker templates, under the names the authz model gives them. Kept in one place so the query
 * and the response adapter cannot drift.
 *
 * @author GraviteeSource Team
 */
public final class AuthzDecisionLogFields {

    static final String TIMESTAMP = DecisionReportFields.TIMESTAMP;
    static final String API_ID = DecisionReportFields.API_ID;
    static final String ORG_ID = DecisionReportFields.ORG_ID;
    static final String ENV_ID = DecisionReportFields.ENV_ID;
    static final String GW_ID = DecisionReportFields.GW_ID;
    static final String EVENT_ID = DecisionReportFields.EVENT_ID;
    static final String REQUEST_ID = DecisionReportFields.REQUEST_ID;
    static final String STATUS = DecisionReportFields.STATUS;
    static final String CALLER = DecisionReportFields.CALLER;
    static final String REASONS = DecisionReportFields.REASONS;
    static final String SUBJECT_TYPE = DecisionReportFields.SUBJECT_TYPE;
    static final String SUBJECT_ID = DecisionReportFields.SUBJECT_ID;
    static final String ACTION = DecisionReportFields.ACTION;
    static final String RESOURCE_TYPE = DecisionReportFields.RESOURCE_TYPE;
    static final String RESOURCE_ID = DecisionReportFields.RESOURCE_ID;
    static final String BATCH_ID = DecisionReportFields.BATCH_ID;
    static final String DURATION_NANOS = DecisionReportFields.DURATION_NANOS;
    static final String ERROR_TYPE = DecisionReportFields.ERROR_TYPE;
    static final String MATCHED_RULE_ID = DecisionReportFields.MATCHED_RULE_ID;
    static final String MATCHED_RULE_NAME = DecisionReportFields.MATCHED_RULE_NAME;
    static final String MATCHED_RULE_VERSION = DecisionReportFields.MATCHED_RULE_VERSION;
    static final String MATCHED_RULE_EFFECT = DecisionReportFields.MATCHED_RULE_EFFECT;
    static final String AUTHZ = "authz";
    static final String PHASE = DecisionReportFields.PHASE;
    static final String RESOLVED = "RESOLVED";
    static final String DECISION = DecisionReportFields.VERDICT;
    static final String OUTCOME = DecisionReportFields.OUTCOME;
    static final String ENFORCED = DecisionReportFields.ENFORCED;
    static final String INDETERMINATE_CAUSE = DecisionReportFields.INDETERMINATE_CAUSE;
    static final String TARGET_PDP_ID = DecisionReportFields.DECISION_POINT_ID;
    static final String POLICY_GENERATION = DecisionReportFields.DECISION_POINT_VERSION;
    static final String MATCHED_RULES = DecisionReportFields.MATCHED_RULES;
    static final String ADDITIONAL_METRICS = DecisionReportFields.ADDITIONAL_METRICS;
    public static final String BATCH_INDEX = ADDITIONAL_METRICS + ".int_authz_batch-index";
    public static final String BATCH_SIZE = ADDITIONAL_METRICS + ".int_authz_batch-size";

    private AuthzDecisionLogFields() {}
}
