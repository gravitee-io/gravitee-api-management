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
package io.gravitee.repository.log.v4.model.decision;

import java.util.List;
import java.util.Map;
import lombok.Builder;

/**
 * One record written by a decision point — a guardian agent, a human approver, an external authority —
 * as stored in the {@code decisions} data stream. A mirror of the reporter's {@code DecisionEventMetrics}:
 * the field names differ only by the kebab-case the index uses.
 *
 * <p><strong>One record, every family.</strong> The data stream is shared, and so is this record. A
 * family that does not write a field leaves it {@code null} — {@code requiredApprover} on a guardian
 * verdict, {@code confidence} on a human approval. Splitting the record per family would buy nothing:
 * the reportable itself is one flat class, the index is one mapping, and a reader filtering on
 * {@code decisionPointType} already knows which half of the record to look at. A per-family model would
 * have to be picked before the documents come back, which is exactly what a shared index prevents.
 *
 * <p>Two fields carry the result and they are not interchangeable. {@code outcome} is what the point
 * concluded and may be {@code INDETERMINATE} or {@code PENDING}; {@code enforced} is what the system did
 * and is never unknown. A guardian that times out and fails open reads {@code INDETERMINATE}/{@code ALLOW},
 * which is indistinguishable from a clean approval if only one of the two is kept.
 *
 * <p>The closed vocabularies ({@code phase}, {@code outcome}, {@code enforced}, {@code status},
 * {@code indeterminateCause}, {@code transformationType}) stay {@link String} rather than becoming enums.
 * They are read back out of an index that older gateways wrote and newer ones will extend, and a value
 * this version does not know must reach the caller, not throw on the way out.
 *
 * @author GraviteeSource Team
 */
@Builder
public record DecisionLog(
    Long timestamp,
    String gatewayId,
    String organizationId,
    String environmentId,
    String apiId,
    String planId,
    String applicationId,
    String eventId,
    String caseId,
    String batchId,
    String phase,
    String decisionPointType,
    String decisionPointId,
    String decisionPointVersion,
    String checkpoint,
    String caller,
    String subjectType,
    String subjectId,
    String actorType,
    String actorId,
    String action,
    String resourceType,
    String resourceId,
    String argsHash,
    String outcome,
    String enforced,
    String verdict,
    String indeterminateCause,
    Double confidence,
    List<String> reasons,
    List<MatchedRule> matchedRules,
    Boolean transformed,
    String transformationType,
    String requiredApprover,
    String deciderType,
    String deciderId,
    String channel,
    String requestId,
    String traceId,
    String conversationId,
    String missionId,
    String status,
    String errorType,
    Long durationNanos,
    Long waitedNanos,
    Map<String, Object> additionalMetrics
) {
    /**
     * A rule, policy or detector the point matched on. Kept whole rather than flattened to a list of
     * names: which rule fired and with what effect is the answer an auditor is after, and the effect of
     * a rule is not always the outcome of the decision.
     */
    public record MatchedRule(String id, String name, String effect) {}
}
