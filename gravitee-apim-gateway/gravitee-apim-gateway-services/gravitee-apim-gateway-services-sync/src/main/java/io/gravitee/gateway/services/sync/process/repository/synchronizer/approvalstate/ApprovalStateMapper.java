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
package io.gravitee.gateway.services.sync.process.repository.synchronizer.approvalstate;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gamma.definition.approval.ApprovalState;
import io.gravitee.gateway.handlers.api.manager.DeployedApprovalState;
import io.gravitee.gateway.handlers.api.manager.DeployedApprovalState.ExecutorLease;
import io.gravitee.gateway.handlers.api.manager.DeployedApprovalState.Grant;
import io.gravitee.gateway.services.sync.process.common.model.SyncAction;
import io.gravitee.repository.management.model.Event;
import io.reactivex.rxjava3.core.Maybe;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

/**
 * Reads the {@link ApprovalState} AIM publishes. A state missing what the gateway needs to answer a held call is
 * skipped, never deployed half-read: the call then waits as if the state had not arrived.
 */
@CustomLog
@RequiredArgsConstructor
public class ApprovalStateMapper {

    private static final String TIMED = "TIMED";

    private final ObjectMapper objectMapper;

    public Maybe<ApprovalStateDeployable> toDeploy(Event event) {
        return Maybe.fromCallable(() -> {
            try {
                ApprovalState wire = objectMapper.readValue(event.getPayload(), ApprovalState.class);
                List<String> invalid = new ArrayList<>();
                requireText(wire.getApprovalRequestId(), "approvalRequestId", invalid);
                requireText(wire.getEnvironmentId(), "environmentId", invalid);
                requireText(wire.getApiId(), "apiId", invalid);
                requireText(wire.getRuleId(), "ruleId", invalid);
                requireText(wire.getStatus(), "status", invalid);
                Long keepUntil = requiredInstant(wire.getKeepUntil(), "keepUntil", invalid);
                Long createdAt = requiredInstant(wire.getCreatedAt(), "createdAt", invalid);
                Long startedAt = optionalInstant(wire.getStartedAt(), "startedAt", invalid);
                Long updatedAt = requiredInstant(wire.getUpdatedAt(), "updatedAt", invalid);
                Long expiresAt = optionalInstant(wire.getExpiresAt(), "expiresAt", invalid);
                Grant grant = grant(wire.getGrant(), invalid);
                ExecutorLease executor = executor(wire.getExecutor(), invalid);
                if (!invalid.isEmpty()) {
                    log.debug("Skipping approval state DEPLOY event [{}] — missing or invalid {}", event.getId(), invalid);
                    return null;
                }
                return ApprovalStateDeployable.builder()
                    .approvalRequestId(wire.getApprovalRequestId())
                    .state(
                        new DeployedApprovalState(
                            wire.getApprovalRequestId(),
                            wire.getAliasIds(),
                            wire.getEnvironmentId(),
                            wire.getApiId(),
                            wire.getRuleId(),
                            wire.getRuleUpdatedAt(),
                            wire.getOpenKey(),
                            wire.getStatus(),
                            expiresAt,
                            wire.getReason(),
                            grant,
                            executor,
                            keepUntil,
                            createdAt,
                            startedAt,
                            updatedAt,
                            wire.getVersion() == null ? 0L : wire.getVersion()
                        )
                    )
                    .syncAction(SyncAction.DEPLOY)
                    .build();
            } catch (Exception e) {
                log.error("Unable to extract approval state from PUBLISH event [{}]", event.getId(), e);
                return null;
            }
        });
    }

    public Maybe<ApprovalStateDeployable> toUndeploy(Event event) {
        return Maybe.fromCallable(() -> {
            try {
                String approvalRequestId = isBlank(event.getPayload())
                    ? null
                    : objectMapper.readValue(event.getPayload(), ApprovalState.class).getApprovalRequestId();
                if (isBlank(approvalRequestId) && event.getProperties() != null) {
                    approvalRequestId = event.getProperties().get(Event.EventProperties.APPROVAL_REQUEST_ID.getValue());
                }
                if (isBlank(approvalRequestId)) {
                    log.debug("Skipping approval state UNDEPLOY event [{}] — missing approvalRequestId", event.getId());
                    return null;
                }
                return ApprovalStateDeployable.builder().approvalRequestId(approvalRequestId).syncAction(SyncAction.UNDEPLOY).build();
            } catch (Exception e) {
                log.error("Unable to extract approval state from UNPUBLISH event [{}]", event.getId(), e);
                return null;
            }
        });
    }

    private static Grant grant(ApprovalState.Grant wire, List<String> invalid) {
        if (wire == null) {
            return null;
        }
        requireText(wire.getScope(), "grant.scope", invalid);
        requireText(wire.getArgumentsDigest(), "grant.argumentsDigest", invalid);
        if (wire.getCaller() == null || isBlank(wire.getCaller().getApplicationId())) {
            invalid.add("grant.caller.applicationId");
        }
        Long coversUntil = TIMED.equals(wire.getScope())
            ? requiredInstant(wire.getCoversUntil(), "grant.coversUntil", invalid)
            : optionalInstant(wire.getCoversUntil(), "grant.coversUntil", invalid);
        return new Grant(
            wire.getScope(),
            coversUntil,
            wire.getCaller() == null ? null : wire.getCaller().getApplicationId(),
            wire.getCaller() == null ? null : wire.getCaller().getSubject(),
            wire.getArgumentsDigest()
        );
    }

    private static ExecutorLease executor(ApprovalState.ExecutorLease wire, List<String> invalid) {
        if (wire == null) {
            return null;
        }
        requireText(wire.getNodeId(), "executor.nodeId", invalid);
        requireText(wire.getLeaseId(), "executor.leaseId", invalid);
        Long leaseUntil = requiredInstant(wire.getLeaseUntil(), "executor.leaseUntil", invalid);
        return leaseUntil == null ? null : new ExecutorLease(wire.getNodeId(), wire.getLeaseId(), leaseUntil);
    }

    private static void requireText(String value, String field, List<String> invalid) {
        if (isBlank(value)) {
            invalid.add(field);
        }
    }

    private static Long requiredInstant(String value, String field, List<String> invalid) {
        if (isBlank(value)) {
            invalid.add(field);
            return null;
        }
        return optionalInstant(value, field, invalid);
    }

    private static Long optionalInstant(String value, String field, List<String> invalid) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return Instant.parse(value).toEpochMilli();
        } catch (DateTimeParseException e) {
            invalid.add(field);
            return null;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
