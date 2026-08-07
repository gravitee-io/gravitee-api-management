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

@Builder
public record AuthzDecisionLog(
    String eventId,
    Long timestamp,
    String apiId,
    String organizationId,
    String environmentId,
    String gatewayId,
    String requestId,
    String operation,
    String status,
    String caller,
    String targetPdpId,
    Long policyGeneration,
    String decision,
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
