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

import static io.gravitee.gateway.handlers.api.manager.DeployedApprovalRule.ArgumentCondition;
import static io.gravitee.gateway.handlers.api.manager.DeployedApprovalRule.ToolBinding;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gamma.definition.approval.ApprovalRule;
import io.gravitee.gateway.api.genericevent.GenericEvent;
import io.gravitee.gateway.handlers.api.manager.DeployedApprovalRule;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;

/** Reads the payload of an {@code approval.rule} event into the rule this gateway evaluates. */
@RequiredArgsConstructor
class ApprovalRuleReader {

    private final ObjectMapper objectMapper;

    /** @throws UnreadableApprovalEventException when the rule cannot be evaluated, or is not the one the event names */
    DeployedApprovalRule read(GenericEvent event) {
        if (event.payload() == null) {
            throw new UnreadableApprovalEventException("the event has no payload");
        }
        ApprovalRule wire;
        try {
            wire = objectMapper.readValue(event.payload(), ApprovalRule.class);
        } catch (JsonProcessingException e) {
            throw new UnreadableApprovalEventException("the payload is not an approval rule", e);
        }
        if (!event.id().equals(wire.getId()) || !event.environmentId().equals(wire.getEnvironmentId())) {
            throw new UnreadableApprovalEventException("the payload names another rule or environment than the event");
        }
        // Dropping only the invalid bindings and entity ids keeps the rest of the rule gating; refusing the rule would stop all of it.
        List<ToolBinding> bindings = validBindings(wire.getToolBindings());
        List<String> entityIds = validEntityIds(wire.getToolEntityIds());
        Long createdAt = parseInstant(wire.getCreatedAt());
        Long updatedAt = parseInstant(wire.getUpdatedAt());
        List<String> invalid = invalidFields(wire, bindings, entityIds, createdAt, updatedAt);
        if (!invalid.isEmpty()) {
            throw new UnreadableApprovalEventException("missing or invalid " + invalid);
        }
        return new DeployedApprovalRule(
            wire.getId(),
            wire.getEnvironmentId(),
            event.organizationId(),
            isBlank(wire.getToolPattern()) ? null : wire.getToolPattern(),
            bindings,
            entityIds,
            conditions(wire.getArgumentConditions()),
            wire.getPriority(),
            createdAt,
            updatedAt
        );
    }

    private static List<ToolBinding> validBindings(List<ApprovalRule.ToolBinding> received) {
        return received == null
            ? List.of()
            : received
                .stream()
                .filter(binding -> binding != null && !isBlank(binding.getApiId()) && !isBlank(binding.getToolName()))
                .map(binding -> new ToolBinding(binding.getApiId(), binding.getToolName()))
                .toList();
    }

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
        if (isBlank(wire.getToolPattern()) && bindings.isEmpty() && entityIds.isEmpty()) {
            invalid.add("toolPattern, toolBindings or toolEntityIds");
        }
        if (
            wire.getArgumentConditions() != null &&
            wire
                .getArgumentConditions()
                .stream()
                .anyMatch(condition -> condition == null || isBlank(condition.getPath()) || condition.getOperator() == null)
        ) {
            invalid.add("argumentConditions");
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
        return received == null
            ? List.of()
            : received
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
