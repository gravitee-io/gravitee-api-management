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
import lombok.Builder;

/**
 * What one run of a Guardrail decided on a call. A decision names rules and where they matched. The gateway records no
 * content of the exchange itself; a step's reason is what its guardrail policy wrote.
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
    public record Finding(String guardrailPolicy, String rule, String location, String effect) {}

    /** One guardrail policy of the sequence, in the order it ran: what it concluded, and why when it said. */
    public record Step(String guardrailPolicy, String verdict, String reason) {}
}
