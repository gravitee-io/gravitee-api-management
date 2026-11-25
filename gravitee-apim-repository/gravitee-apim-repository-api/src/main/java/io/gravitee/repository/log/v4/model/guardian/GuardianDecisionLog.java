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
package io.gravitee.repository.log.v4.model.guardian;

import java.util.List;
import java.util.Map;
import lombok.Builder;

/**
 * One run of a Guardian, as the gateway reported it: rule names and locations, never the content of the exchange.
 *
 * @param guardianId the decision point id of the report
 * @param guardianRevision the decision point version of the report: the deployed revision that ran, absent when the
 *     Guardian was not deployed
 * @param checkpoint where the Guardian ran, e.g. {@code request.llm-proxy}
 * @param caller the name of the step that ran the Guardian
 * @author GraviteeSource Team
 */
@Builder
public record GuardianDecisionLog(
    String eventId,
    Long timestamp,
    String organizationId,
    String environmentId,
    String gatewayId,
    String apiId,
    String planId,
    String applicationId,
    String guardianId,
    String guardianRevision,
    String checkpoint,
    String caller,
    String requestId,
    String traceId,
    String outcome,
    String enforced,
    String verdict,
    String indeterminateCause,
    String errorType,
    String status,
    List<String> reasons,
    List<MatchedRule> matchedRules,
    Boolean transformed,
    String transformationType,
    Long durationNanos,
    String subjectType,
    String subjectId,
    String actorType,
    String actorId,
    Map<String, Object> additionalMetrics
) {
    /** A finding of a guardrail; for a Guardian, its annotations name the guardrail and the location it matched. */
    public record MatchedRule(String id, String name, String version, String effect, Map<String, String> annotations) {}
}
