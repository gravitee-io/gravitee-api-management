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
package io.gravitee.gateway.handlers.api.manager.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gamma.definition.approval.ApprovalState;
import io.gravitee.gateway.api.genericevent.GenericEvent;
import io.gravitee.gateway.handlers.api.manager.DeployedApprovalState;
import io.gravitee.gateway.handlers.api.manager.DeployedApprovalState.ExecutorLease;
import io.gravitee.gateway.handlers.api.manager.DeployedApprovalState.Grant;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;

/**
 * Reads the payload of an {@code approval.state} event into the state the gateway holds. A state missing what the
 * gateway needs to answer a held call is never deployed half-read: the call then waits as if the state had not arrived.
 */
@RequiredArgsConstructor
class ApprovalStateReader {

    private static final String TIMED = "TIMED";

    private final ObjectMapper objectMapper;

    /** @throws UnreadableApprovalEventException when the state cannot be used, or is not the one the event names */
    DeployedApprovalState read(GenericEvent event) {
        if (event.payload() == null) {
            throw new UnreadableApprovalEventException("the event has no payload");
        }
        if (event.expiresAt() == null) {
            // Only the expiry of its event removes a state from the gateway's memory.
            throw new UnreadableApprovalEventException("the event has no expiry");
        }
        ApprovalState wire;
        try {
            wire = objectMapper.readValue(event.payload(), ApprovalState.class);
        } catch (JsonProcessingException e) {
            throw new UnreadableApprovalEventException("the payload is not an approval state", e);
        }
        if (!event.id().equals(wire.getApprovalRequestId()) || !event.environmentId().equals(wire.getEnvironmentId())) {
            throw new UnreadableApprovalEventException("the payload names another request or environment than the event");
        }
        List<String> invalid = new ArrayList<>();
        if (wire.getAliasIds() != null && wire.getAliasIds().contains(null)) {
            invalid.add("aliasIds");
        }
        requireText(wire.getApiId(), "apiId", invalid);
        requireText(wire.getRuleId(), "ruleId", invalid);
        requireText(wire.getStatus(), "status", invalid);
        Long ruleUpdatedAt = requiredInstant(wire.getRuleUpdatedAt(), "ruleUpdatedAt", invalid);
        Long keepUntil = requiredInstant(wire.getKeepUntil(), "keepUntil", invalid);
        Long createdAt = requiredInstant(wire.getCreatedAt(), "createdAt", invalid);
        Long updatedAt = requiredInstant(wire.getUpdatedAt(), "updatedAt", invalid);
        Long startedAt = optionalInstant(wire.getStartedAt(), "startedAt", invalid);
        Long expiresAt = optionalInstant(wire.getExpiresAt(), "expiresAt", invalid);
        Grant grant = grant(wire.getGrant(), invalid);
        ExecutorLease executor = executor(wire.getExecutor(), invalid);
        if (!invalid.isEmpty()) {
            throw new UnreadableApprovalEventException("missing or invalid " + invalid);
        }
        return new DeployedApprovalState(
            wire.getApprovalRequestId(),
            wire.getAliasIds(),
            wire.getEnvironmentId(),
            wire.getApiId(),
            wire.getRuleId(),
            ruleUpdatedAt,
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
        );
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
