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
 * The authorization outcome carried by a decision row, present only when the search asked for
 * {@code RECORD_TYPE=AUTHZ_DECISION}. Typed rather than folded into {@code additionalMetrics}: that
 * map is the extension bag for keys plugins and custom policies attach at runtime, so a known
 * contract placed there would be invisible to the OpenAPI schema and readable only by convention.
 */
@Builder
public record AuthzDecision(
    String eventId,
    String decision,
    String status,
    String caller,
    String operation,
    String targetPdpId,
    Long policyGeneration,
    List<String> matchedPolicyNames,
    List<String> reasons,
    String subjectType,
    String subjectId,
    String action,
    String resourceType,
    String resourceId,
    String batchId,
    Integer batchIndex,
    Integer batchSize,
    String searchType,
    Integer resultCount,
    Long durationNanos
) {}
