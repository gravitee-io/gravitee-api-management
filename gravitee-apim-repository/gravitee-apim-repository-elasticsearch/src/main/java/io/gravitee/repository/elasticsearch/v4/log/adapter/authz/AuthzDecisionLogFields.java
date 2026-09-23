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

/**
 * Indexed field names of an authz {@code DecisionReport} document, as written by the reporter's
 * FreeMarker templates. Kept in one place so the query and the response adapter cannot drift.
 *
 * @author GraviteeSource Team
 */
final class AuthzDecisionLogFields {

    static final String TIMESTAMP = "@timestamp";
    static final String API_ID = "api-id";
    static final String ORG_ID = "org-id";
    static final String ENV_ID = "env-id";
    static final String GW_ID = "gw-id";
    static final String EVENT_ID = "event-id";
    static final String REQUEST_ID = "request-id";
    static final String STATUS = "status";
    static final String CALLER = "caller";
    static final String REASONS = "reasons";
    static final String SUBJECT_TYPE = "subject-type";
    static final String SUBJECT_ID = "subject-id";
    static final String ACTION = "action";
    static final String RESOURCE_TYPE = "resource-type";
    static final String RESOURCE_ID = "resource-id";
    static final String BATCH_ID = "batch-id";
    static final String DURATION_NANOS = "duration-nanos";
    static final String ERROR_TYPE = "error-type";
    static final String MATCHED_RULE_ID = "id";
    static final String MATCHED_RULE_NAME = "name";
    static final String MATCHED_RULE_VERSION = "version";
    static final String MATCHED_RULE_EFFECT = "effect";
    static final String DECISION_POINT_TYPE = "decision-point-type";
    static final String AUTHZ = "authz";
    static final String PHASE = "phase";
    static final String RESOLVED = "RESOLVED";
    static final String DECISION = "verdict";
    static final String OUTCOME = "outcome";
    static final String ENFORCED = "enforced";
    static final String INDETERMINATE_CAUSE = "indeterminate-cause";
    static final String TARGET_PDP_ID = "decision-point-id";
    static final String POLICY_GENERATION = "decision-point-version";
    static final String MATCHED_RULES = "matched-rules";
    static final String BATCH_INDEX = "additional-metrics.int_authz_batch-index";
    static final String BATCH_SIZE = "additional-metrics.int_authz_batch-size";

    private AuthzDecisionLogFields() {}
}
