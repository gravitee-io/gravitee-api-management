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
package io.gravitee.apim.core.log.model;

import java.util.List;
import java.util.Map;
import lombok.Builder;

/**
 * One record written by a decision point — a guardian agent, a human approver, an external authority.
 *
 * <p>One record for every family, deliberately: the {@code decisions} data stream is shared, so a reader
 * that asked for one family gets documents of exactly one shape, and a field the family does not write is
 * {@code null}. A model per family would have to be chosen before the documents come back, which is what
 * a shared index rules out, and it would drop whatever a point this version never heard of wrote.
 *
 * <p>{@code outcome} is what the point concluded and may be {@code INDETERMINATE} or {@code PENDING};
 * {@code enforced} is what the system did and is never unknown. A guardian that timed out and failed open
 * reads {@code INDETERMINATE}/{@code ALLOW} — keeping only one of the two makes it indistinguishable from
 * a clean approval.
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
    /** A rule, policy or detector the point matched on, kept whole: the effect of a rule is not always the outcome of the decision. */
    public record MatchedRule(String id, String name, String effect) {}
}
