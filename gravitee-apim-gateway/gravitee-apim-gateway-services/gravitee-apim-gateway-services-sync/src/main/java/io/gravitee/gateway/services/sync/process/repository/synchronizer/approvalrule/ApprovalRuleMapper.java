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
package io.gravitee.gateway.services.sync.process.repository.synchronizer.approvalrule;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gamma.definition.approval.ApprovalRule;
import io.gravitee.gateway.handlers.api.manager.DeployedApprovalRule.ArgumentCondition;
import io.gravitee.gateway.handlers.api.manager.DeployedApprovalRule.ToolBinding;
import io.gravitee.gateway.services.sync.process.common.model.SyncAction;
import io.gravitee.repository.management.model.Event;
import io.reactivex.rxjava3.core.Maybe;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

@CustomLog
@RequiredArgsConstructor
public class ApprovalRuleMapper {

    private final ObjectMapper objectMapper;

    public Maybe<ApprovalRuleDeployable> toDeploy(Event event) {
        return Maybe.fromCallable(() -> {
            try {
                ApprovalRule wire = objectMapper.readValue(event.getPayload(), ApprovalRule.class);
                Long createdAt = parseInstant(wire.getCreatedAt());
                Long updatedAt = parseInstant(wire.getUpdatedAt());
                List<ApprovalRule.ToolBinding> received = wire.getToolBindings() == null ? List.of() : wire.getToolBindings();
                List<ToolBinding> bindings = validBindings(received);
                List<String> entityIds = validEntityIds(wire.getToolEntityIds());
                List<String> invalid = invalidFields(wire, bindings, entityIds, createdAt, updatedAt);
                if (!invalid.isEmpty()) {
                    log.warn("Skipping approval rule DEPLOY event [{}] — missing or invalid {}", event.getId(), invalid);
                    return null;
                }
                int dropped = received.size() - bindings.size();
                if (dropped > 0) {
                    log.warn("Approval rule [{}] has {} invalid tool bindings; deploying it without them", wire.getId(), dropped);
                }
                return ApprovalRuleDeployable.builder()
                    .approvalRuleId(wire.getId())
                    .environmentId(wire.getEnvironmentId())
                    .toolPattern(isBlank(wire.getToolPattern()) ? null : wire.getToolPattern())
                    .toolBindings(bindings)
                    .toolEntityIds(entityIds)
                    .argumentConditions(conditions(wire.getArgumentConditions()))
                    .priority(wire.getPriority())
                    .createdAt(createdAt)
                    .updatedAt(updatedAt)
                    .syncAction(SyncAction.DEPLOY)
                    .build();
            } catch (Exception e) {
                log.error("Unable to extract approval rule from PUBLISH event [{}]", event.getId(), e);
                return null;
            }
        });
    }

    public Maybe<ApprovalRuleDeployable> toUndeploy(Event event) {
        return Maybe.fromCallable(() -> {
            try {
                ApprovalRule wire = objectMapper.readValue(event.getPayload(), ApprovalRule.class);
                if (isBlank(wire.getId())) {
                    log.debug("Skipping approval rule UNDEPLOY event [{}] — missing id", event.getId());
                    return null;
                }
                return ApprovalRuleDeployable.builder()
                    .approvalRuleId(wire.getId())
                    .environmentId(wire.getEnvironmentId())
                    .syncAction(SyncAction.UNDEPLOY)
                    .build();
            } catch (Exception e) {
                log.error("Unable to extract approval rule from UNPUBLISH event [{}]", event.getId(), e);
                return null;
            }
        });
    }

    // Dropping only the invalid bindings keeps the rest gating; skipping the rule would stop it gating altogether.
    private static List<ToolBinding> validBindings(List<ApprovalRule.ToolBinding> received) {
        return received
            .stream()
            .filter(binding -> binding != null && !isBlank(binding.getApiId()) && !isBlank(binding.getToolName()))
            .map(binding -> new ToolBinding(binding.getApiId(), binding.getToolName()))
            .toList();
    }

    // Same reasoning as the bindings: an entity id that cannot match is dropped, the rest keep gating.
    private static List<String> validEntityIds(List<String> received) {
        return received == null
            ? List.of()
            : received
                .stream()
                .filter(entityId -> !isBlank(entityId))
                .distinct()
                .toList();
    }

    private static List<String> invalidFields(
        ApprovalRule wire,
        List<ToolBinding> bindings,
        List<String> entityIds,
        Long createdAt,
        Long updatedAt
    ) {
        List<String> invalid = new ArrayList<>();
        if (isBlank(wire.getId())) {
            invalid.add("id");
        }
        if (isBlank(wire.getEnvironmentId())) {
            invalid.add("environmentId");
        }
        if (isBlank(wire.getToolPattern()) && bindings.isEmpty() && entityIds.isEmpty()) {
            invalid.add("toolPattern, toolBindings or toolEntityIds");
        }
        if (wire.getPriority() == null) {
            invalid.add("priority");
        }
        if (createdAt == null) {
            invalid.add("createdAt");
        }
        if (updatedAt == null) {
            invalid.add("updatedAt");
        }
        return invalid;
    }

    private static List<ArgumentCondition> conditions(List<ApprovalRule.ArgumentCondition> received) {
        if (received == null) {
            return List.of();
        }
        return received
            .stream()
            .map(condition -> new ArgumentCondition(condition.getPath(), condition.getOperator(), condition.getValue()))
            .toList();
    }

    private static Long parseInstant(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return Instant.parse(value).toEpochMilli();
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
