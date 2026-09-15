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
package io.gravitee.repository.elasticsearch.v4.log.adapter.decision;

/**
 * Indexed field names of a {@code DecisionEventMetrics} document, as written by the reporter's
 * FreeMarker templates. Kept in one place so the query and the response adapter cannot drift.
 *
 * @author GraviteeSource Team
 */
final class DecisionLogFields {

    static final String TIMESTAMP = "@timestamp";
    static final String GW_ID = "gw-id";
    static final String ORG_ID = "org-id";
    static final String ENV_ID = "env-id";
    static final String API_ID = "api-id";
    static final String PLAN_ID = "plan-id";
    static final String APP_ID = "app-id";
    static final String EVENT_ID = "event-id";
    static final String CASE_ID = "case-id";
    static final String BATCH_ID = "batch-id";
    static final String PHASE = "phase";
    static final String DECISION_POINT_TYPE = "decision-point-type";
    static final String DECISION_POINT_ID = "decision-point-id";
    static final String DECISION_POINT_VERSION = "decision-point-version";
    static final String CHECKPOINT = "checkpoint";
    static final String CALLER = "caller";
    static final String SUBJECT_TYPE = "subject-type";
    static final String SUBJECT_ID = "subject-id";
    static final String ACTOR_TYPE = "actor-type";
    static final String ACTOR_ID = "actor-id";
    static final String ACTION = "action";
    static final String RESOURCE_TYPE = "resource-type";
    static final String RESOURCE_ID = "resource-id";
    static final String ARGS_HASH = "args-hash";
    static final String OUTCOME = "outcome";
    static final String ENFORCED = "enforced";
    static final String VERDICT = "verdict";
    static final String INDETERMINATE_CAUSE = "indeterminate-cause";
    static final String CONFIDENCE = "confidence";
    static final String REASONS = "reasons";
    static final String MATCHED_RULES = "matched-rules";
    static final String MATCHED_RULE_ID = "id";
    static final String MATCHED_RULE_NAME = "name";
    static final String MATCHED_RULE_EFFECT = "effect";
    static final String TRANSFORMED = "transformed";
    static final String TRANSFORMATION_TYPE = "transformation-type";
    static final String REQUIRED_APPROVER = "required-approver";
    static final String DECIDER_TYPE = "decider-type";
    static final String DECIDER_ID = "decider-id";
    static final String CHANNEL = "channel";
    static final String REQUEST_ID = "request-id";
    static final String TRACE_ID = "trace-id";
    static final String CONVERSATION_ID = "conversation-id";
    static final String MISSION_ID = "mission-id";
    static final String STATUS = "status";
    static final String ERROR_TYPE = "error-type";
    static final String DURATION_NANOS = "duration-nanos";
    static final String WAITED_NANOS = "waited-nanos";
    static final String ADDITIONAL_METRICS = "additional-metrics";

    /**
     * The only phase a search reads. A point that holds a call writes once when the hold starts and once
     * when it ends; only the second carries the outcome, so a search that does not pin this counts every
     * settled decision twice. Same reasoning, same value as the aggregate side's
     * {@code HumanApprovalFieldResolver}.
     */
    static final String PHASE_RESOLVED = "RESOLVED";

    private DecisionLogFields() {}
}
