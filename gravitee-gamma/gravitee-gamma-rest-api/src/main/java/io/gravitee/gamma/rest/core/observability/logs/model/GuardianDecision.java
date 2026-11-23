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
package io.gravitee.gamma.rest.core.observability.logs.model;

import java.util.List;
import lombok.Builder;

/**
 * What a Guardian decided on a call, carried by a decision row and present only when the search asked for
 * {@code RECORD_TYPE=GUARDIAN_DECISION}. It names rules and where they matched, never the content of the exchange.
 */
@Builder
public record GuardianDecision(
    String eventId,
    String guardianId,
    String revision,
    String checkpoint,
    String outcome,
    String enforced,
    String transformationType,
    String verdict,
    List<String> reasons,
    List<Finding> findings,
    Long durationNanos
) {
    public record Finding(String guardrail, String rule, String location, String effect) {}
}
