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
package io.gravitee.gateway.services.sync.process.distributed.mapper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gateway.handlers.api.manager.DeployedApprovalRule.ArgumentCondition;
import io.gravitee.gateway.handlers.api.manager.DeployedApprovalRule.ToolBinding;
import io.gravitee.gateway.services.sync.process.common.model.SyncAction;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.approvalrule.ApprovalRuleDeployable;
import io.gravitee.repository.distributedsync.model.DistributedEvent;
import io.gravitee.repository.distributedsync.model.DistributedEventType;
import io.reactivex.rxjava3.core.Maybe;
import java.util.Date;
import java.util.List;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

/**
 * Maps approval rules to and from distributed events, so secondary nodes receive the approval rules the primary node deployed.
 *
 * <p>The payload carries the organization the primary node resolved, because a secondary node cannot look it up and the
 * licence check needs it. An undeploy carries no payload: the approval rule id is enough. The payload records ignore
 * unknown fields, so a secondary node keeps reading what a newer primary node writes.
 */
@RequiredArgsConstructor
@CustomLog
public class ApprovalRuleMapper {

    private final ObjectMapper objectMapper;

    public Maybe<ApprovalRuleDeployable> to(final DistributedEvent event) {
        return Maybe.fromCallable(() -> {
            try {
                SyncAction syncAction = SyncActionMapper.to(event.getSyncAction());
                if (syncAction == SyncAction.UNDEPLOY) {
                    return ApprovalRuleDeployable.builder().approvalRuleId(event.getId()).syncAction(SyncAction.UNDEPLOY).build();
                }
                Payload payload = objectMapper.readValue(event.getPayload(), Payload.class);
                List<Binding> received = payload.toolBindings() == null ? List.of() : payload.toolBindings();
                List<ToolBinding> bindings = validBindings(received);
                List<String> entityIds = validEntityIds(payload.toolEntityIds());
                if (isIncomplete(payload, bindings, entityIds)) {
                    log.warn(
                        "Skipping distributed approval rule event [{}] — missing id, environmentId, organizationId, or all of toolPattern, a valid toolBinding and a valid toolEntityId",
                        event.getId()
                    );
                    return null;
                }
                int dropped = received.size() - bindings.size();
                if (dropped > 0) {
                    log.warn("Approval rule [{}] has {} invalid tool bindings; deploying it without them", payload.id(), dropped);
                }
                return ApprovalRuleDeployable.builder()
                    .approvalRuleId(payload.id())
                    .environmentId(payload.environmentId())
                    .organizationId(payload.organizationId())
                    .toolPattern(isBlank(payload.toolPattern()) ? null : payload.toolPattern())
                    .toolBindings(bindings)
                    .toolEntityIds(entityIds)
                    .argumentConditions(conditions(payload.argumentConditions()))
                    .priority(payload.priority())
                    .createdAt(payload.createdAt())
                    .updatedAt(payload.updatedAt())
                    .syncAction(syncAction)
                    .build();
            } catch (Exception e) {
                log.warn("Error while reading approval rule from distributed event [{}]", event.getId(), e);
                return null;
            }
        });
    }

    public Maybe<DistributedEvent> to(final ApprovalRuleDeployable deployable) {
        return Maybe.fromCallable(() -> {
            try {
                DistributedEvent.DistributedEventBuilder builder = DistributedEvent.builder()
                    .id(deployable.approvalRuleId())
                    .type(DistributedEventType.APPROVAL_RULE)
                    .syncAction(SyncActionMapper.to(deployable.syncAction()))
                    .updatedAt(new Date());
                if (deployable.syncAction() == SyncAction.DEPLOY) {
                    builder.payload(
                        objectMapper.writeValueAsString(
                            new Payload(
                                deployable.approvalRuleId(),
                                deployable.environmentId(),
                                deployable.organizationId(),
                                deployable.toolPattern(),
                                deployable.toolBindings() == null
                                    ? List.of()
                                    : deployable
                                        .toolBindings()
                                        .stream()
                                        .map(binding -> new Binding(binding.apiId(), binding.toolName()))
                                        .toList(),
                                deployable.toolEntityIds() == null ? List.of() : deployable.toolEntityIds(),
                                deployable
                                    .argumentConditions()
                                    .stream()
                                    .map(condition -> new Condition(condition.path(), condition.operator(), condition.value()))
                                    .toList(),
                                deployable.priority(),
                                deployable.createdAt(),
                                deployable.updatedAt()
                            )
                        )
                    );
                }
                return builder.build();
            } catch (Exception e) {
                log.warn("Error while building distributed event from approval rule [{}]", deployable.approvalRuleId(), e);
                return null;
            }
        });
    }

    // Dropping only the invalid bindings keeps the rest gating; skipping the rule would stop it gating altogether.
    private static List<ToolBinding> validBindings(List<Binding> received) {
        return received
            .stream()
            .filter(binding -> binding != null && !isBlank(binding.apiId()) && !isBlank(binding.toolName()))
            .map(binding -> new ToolBinding(binding.apiId(), binding.toolName()))
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

    private static boolean isIncomplete(Payload payload, List<ToolBinding> bindings, List<String> entityIds) {
        return (
            isBlank(payload.id()) ||
            isBlank(payload.environmentId()) ||
            isBlank(payload.organizationId()) ||
            (isBlank(payload.toolPattern()) && bindings.isEmpty() && entityIds.isEmpty())
        );
    }

    private static List<ArgumentCondition> conditions(List<Condition> received) {
        if (received == null) {
            return List.of();
        }
        return received
            .stream()
            .map(condition -> new ArgumentCondition(condition.path(), condition.operator(), condition.value()))
            .toList();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Payload(
        String id,
        String environmentId,
        String organizationId,
        String toolPattern,
        List<Binding> toolBindings,
        List<String> toolEntityIds,
        List<Condition> argumentConditions,
        int priority,
        long createdAt,
        long updatedAt
    ) {}

    // The deployed condition holds its compiled expression, which has no place on the wire.
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Condition(String path, String operator, String value) {}

    // Kept apart from the deployed binding so the wire format does not follow changes to the gateway model.
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Binding(String apiId, String toolName) {}
}
