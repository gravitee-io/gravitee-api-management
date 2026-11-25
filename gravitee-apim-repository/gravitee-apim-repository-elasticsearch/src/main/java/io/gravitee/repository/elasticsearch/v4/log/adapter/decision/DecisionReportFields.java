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
 * Indexed field names of a {@code DecisionReport} document, whatever its decision point, as written by the reporter's
 * FreeMarker templates ({@code base-decision-report.ftl} and {@code decision-report-fields.ftl}).
 *
 * @author GraviteeSource Team
 */
public final class DecisionReportFields {

    public static final String TIMESTAMP = "@timestamp";
    public static final String GW_ID = "gw-id";
    public static final String ORG_ID = "org-id";
    public static final String ENV_ID = "env-id";
    public static final String API_ID = "api-id";
    public static final String PLAN_ID = "plan-id";
    public static final String APP_ID = "app-id";
    public static final String EVENT_ID = "event-id";
    public static final String BATCH_ID = "batch-id";
    public static final String PHASE = "phase";
    public static final String DECISION_POINT_TYPE = "decision-point-type";
    public static final String DECISION_POINT_ID = "decision-point-id";
    public static final String DECISION_POINT_VERSION = "decision-point-version";
    public static final String CHECKPOINT = "checkpoint";
    public static final String CALLER = "caller";
    public static final String SUBJECT_TYPE = "subject-type";
    public static final String SUBJECT_ID = "subject-id";
    public static final String ACTOR_TYPE = "actor-type";
    public static final String ACTOR_ID = "actor-id";
    public static final String ACTION = "action";
    public static final String RESOURCE_TYPE = "resource-type";
    public static final String RESOURCE_ID = "resource-id";
    public static final String OUTCOME = "outcome";
    public static final String ENFORCED = "enforced";
    public static final String VERDICT = "verdict";
    public static final String INDETERMINATE_CAUSE = "indeterminate-cause";
    public static final String REASONS = "reasons";
    public static final String MATCHED_RULES = "matched-rules";
    public static final String MATCHED_RULE_ID = "id";
    public static final String MATCHED_RULE_NAME = "name";
    public static final String MATCHED_RULE_VERSION = "version";
    public static final String MATCHED_RULE_EFFECT = "effect";
    public static final String MATCHED_RULE_ANNOTATIONS = "annotations";
    public static final String TRANSFORMED = "transformed";
    public static final String TRANSFORMATION_TYPE = "transformation-type";
    public static final String REQUEST_ID = "request-id";
    public static final String TRACE_ID = "trace-id";
    public static final String STATUS = "status";
    public static final String ERROR_TYPE = "error-type";
    public static final String DURATION_NANOS = "duration-nanos";
    public static final String ADDITIONAL_METRICS = "additional-metrics";

    private DecisionReportFields() {}
}
