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
 * One run of a Guardian on a call, as the gateway reported it. It names guardrails and where they matched, never the
 * content of the exchange. The request log of the same call shares its {@link #requestId()}.
 *
 * @param id the event id of the report, unique per run
 * @param timestamp when the run was reported, in epoch milliseconds
 * @param guardianRevision the deployed revision that ran; {@code null} when the Guardian was not deployed
 * @param checkpoint where the Guardian ran: the phase, then the API type, e.g. {@code request.llm-proxy}
 * @param caller the name of the step that ran the Guardian
 * @param outcome what the guardrails concluded: {@code ALLOW}, {@code DENY}, {@code TRANSFORM} or {@code INDETERMINATE}
 * @param enforced what the gateway did with the call: {@code ALLOW}, {@code DENY} or {@code TRANSFORM}
 * @param verdict the key of a denial, e.g. {@code GUARDRAIL_BLOCKED}
 * @param indeterminateCause why the Guardian could not decide: {@code NOT_READY}, {@code ERROR}, {@code TIMEOUT}, …
 * @param errorType what failed when the Guardian could not decide
 * @param status {@code success} or {@code error}
 * @param reasons one line per guardrail that ran: {@code "<guardrail>: <VERDICT>"}, then {@code " — <reason; …>"} when
 *     it gave reasons
 * @param matchedRules one per finding of a guardrail
 * @param transformationType how the exchange was transformed, e.g. {@code REDACT}
 * @param subjectType {@code user} when the call acts for a user, otherwise {@code agent}
 * @param subjectId the user, else the application, else {@code -1} for an anonymous call
 * @param actorType {@code agent} when an application acts for the user
 * @param actorId the application acting for the user
 * @author GraviteeSource Team
 */
@Builder(toBuilder = true)
public record GuardianDecision(
    String id,
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
    /**
     * A finding of a guardrail.
     *
     * @param effect what the guardrail did: {@code DENY} or {@code TRANSFORM}
     * @param annotations {@code guardrail}: the guardrail name, {@code location}: where in the exchange it matched
     */
    public record MatchedRule(String id, String name, String version, String effect, Map<String, String> annotations) {}
}
