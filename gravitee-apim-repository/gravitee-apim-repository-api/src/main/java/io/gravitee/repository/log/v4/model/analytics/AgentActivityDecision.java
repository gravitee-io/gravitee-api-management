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
package io.gravitee.repository.log.v4.model.analytics;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * One decision on the unified {@code decisions} stream — authz, human-approval, or guardian.
 *
 * <p>HITL writes two phases (REQUESTED then RESOLVED); the feed resolves them into one intervention
 * so the UI does not double-count.
 */
@Data
@Builder
public class AgentActivityDecision {

    /** Gateway request-id that joins this decision to its hop. */
    String requestId;

    /** {@code authz}, {@code human-approval}, or {@code guardian}. */
    String decisionPointType;

    /** HITL phase — REQUESTED or RESOLVED. Used to collapse pairs in the adapter. */
    String phase;

    /**
     * Gateway outcome — ALLOW, DENY, TRANSFORM, INDETERMINATE, PENDING.
     * For HITL this is the RESOLVED phase outcome; REQUESTED alone maps to {@code waiting}.
     */
    String outcome;

    /** Enforced action — ALLOW, DENY, TRANSFORM, SUSPEND. */
    String enforced;

    /** Who or what decided — a rule name, a guardian id, a person's id. */
    String decider;

    /** Verbatim reason from the decision, when present. */
    String reason;

    /** Epoch millis of the decision. */
    long timestamp;

    /** HITL approval id ({@code case-id}) when present, else event id — for Govern deep links. */
    String decisionId;

    /** Key-value pairs the decision recorded (guardian findings, policy rule metadata). */
    List<RecordField> record;

    @Data
    @Builder
    public static class RecordField {

        String label;
        String value;
    }
}
