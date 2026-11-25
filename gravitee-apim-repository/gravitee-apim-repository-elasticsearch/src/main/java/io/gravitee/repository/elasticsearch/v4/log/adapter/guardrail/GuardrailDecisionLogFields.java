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
package io.gravitee.repository.elasticsearch.v4.log.adapter.guardrail;

/**
 * Indexed field names of a Guardrail {@code DecisionReport} document, as written by the reporter's FreeMarker
 * templates. Kept in one place so the query and the source mapper cannot drift.
 *
 * @author GraviteeSource Team
 */
final class GuardrailDecisionLogFields {

    static final String TIMESTAMP = "@timestamp";
    static final String API_ID = "api-id";
    static final String ORG_ID = "org-id";
    static final String ENV_ID = "env-id";
    static final String EVENT_ID = "event-id";
    static final String REQUEST_ID = "request-id";

    static final String DECISION_POINT_TYPE = "decision-point-type";
    static final String GUARDRAIL = "guardrail";
    static final String PHASE = "phase";
    static final String RESOLVED = "RESOLVED";

    static final String GUARDRAIL_ID = "decision-point-id";
    static final String REVISION = "decision-point-version";
    static final String CHECKPOINT = "checkpoint";
    static final String OUTCOME = "outcome";
    static final String ENFORCED = "enforced";
    static final String TRANSFORMATION_TYPE = "transformation-type";
    static final String VERDICT = "verdict";
    static final String REASONS = "reasons";
    static final String DURATION_NANOS = "duration-nanos";

    static final String MATCHED_RULES = "matched-rules";
    static final String MATCHED_RULE_NAME = "name";
    static final String MATCHED_RULE_EFFECT = "effect";
    static final String MATCHED_RULE_ANNOTATIONS = "annotations";
    static final String ANNOTATION_GUARDRAIL = "guardrail";
    static final String ANNOTATION_LOCATION = "location";

    private GuardrailDecisionLogFields() {}
}
