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
package io.gravitee.gateway.handlers.api.manager;

import io.gravitee.gamma.definition.approval.ApprovalStatus;
import java.util.List;

/**
 * An approval request as the gateway holds it in memory: the published {@code ApprovalState} with its timestamps read
 * once, in epoch milliseconds. It never carries the call's arguments.
 *
 * @param ruleUpdatedAt the {@code updatedAt} of the rule the request was opened under
 * @param expiresAt when a pending request is denied, or {@code null}
 * @param openKey identifies the same call until the request is released, then {@code null}
 * @param status as AIM published it; {@link #approvalStatus()} reads it
 * @param keepUntil after it the state is no longer looked up, and is removed
 * @param createdAt when the call was first held
 * @param startedAt when the call ran, as {@code ApprovalState} defines it, or {@code null}
 * @param version raised by AIM on every change of the request, or {@code 0} when the control plane does not send it
 */
public record DeployedApprovalState(
    String id,
    List<String> aliasIds,
    String environmentId,
    String apiId,
    String ruleId,
    long ruleUpdatedAt,
    String openKey,
    String status,
    Long expiresAt,
    String reason,
    Grant grant,
    ExecutorLease executor,
    long keepUntil,
    long createdAt,
    Long startedAt,
    long updatedAt,
    long version
) {
    public DeployedApprovalState {
        aliasIds = aliasIds == null ? List.of() : List.copyOf(aliasIds);
    }

    /** @return the status, or {@link ApprovalStatus#UNRECOGNIZED} for one this gateway does not know */
    public ApprovalStatus approvalStatus() {
        return ApprovalStatus.parse(status);
    }

    /**
     * @param coversUntil the end of a {@code TIMED} grant, or {@code null} for {@code ONCE}
     * @param subject the end user the grant is for, or {@code null}
     */
    public record Grant(String scope, Long coversUntil, String applicationId, String subject, String argumentsDigest) {}

    public record ExecutorLease(String nodeId, String leaseId, long leaseUntil) {}
}
