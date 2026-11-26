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
package io.gravitee.repository.log.v4.model.guardrail;

import java.util.List;
import lombok.Builder;

/**
 * What one run of a Guardrail decided on a call, as the gateway recorded it. A decision names rules and where they
 * matched. The gateway records no content of the exchange itself; a step's reason is what its guardrail policy wrote, and
 * nothing stops a policy from quoting what it matched.
 *
 * @author GraviteeSource Team
 */
@Builder
public record GuardrailDecisionLog(
    String eventId,
    Long timestamp,
    String apiId,
    String requestId,
    String guardrailId,
    String revision,
    String checkpoint,
    String outcome,
    String enforced,
    String transformationType,
    String verdict,
    List<Step> steps,
    List<Finding> findings,
    Long durationNanos
) {
    /**
     * @param guardrailPolicy the guardrail policy of the Guardrail that found it.
     * @param rule what matched; {@code null} for a guardrail policy making a single check, which names no rule.
     * @param location where in the exchange it matched.
     * @param effect what the guardrail policy concluded: {@code DENY}, {@code TRANSFORM} or {@code ALLOW}.
     */
    public record Finding(String guardrailPolicy, String rule, String location, String effect) {}

    /**
     * One guardrail policy of the sequence, in the order it ran.
     *
     * @param guardrailPolicy the name of the guardrail policy.
     * @param verdict what it concluded, as the policy words it: {@code BLOCK}, {@code CONTINUE}, {@code REDACT}...
     * @param reason why, as the policy gave it; {@code null} when it gave none.
     */
    public record Step(String guardrailPolicy, String verdict, String reason) {}
}
