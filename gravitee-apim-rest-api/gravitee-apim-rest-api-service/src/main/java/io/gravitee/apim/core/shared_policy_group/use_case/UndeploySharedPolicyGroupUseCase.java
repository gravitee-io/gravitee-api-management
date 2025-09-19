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
package io.gravitee.apim.core.shared_policy_group.use_case;

import static java.util.Map.entry;
import static java.util.Objects.requireNonNull;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.apim.core.audit.model.EnvironmentAuditLogEntity;
import io.gravitee.apim.core.event.crud_service.EventCrudService;
import io.gravitee.apim.core.event.crud_service.EventLatestCrudService;
import io.gravitee.apim.core.event.model.Event;
import io.gravitee.apim.core.shared_policy_group.crud_service.SharedPolicyGroupCrudService;
import io.gravitee.apim.core.shared_policy_group.crud_service.SharedPolicyGroupHistoryCrudService;
import io.gravitee.apim.core.shared_policy_group.exception.SharedPolicyGroupNotFoundException;
import io.gravitee.apim.core.shared_policy_group.model.SharedPolicyGroup;
import io.gravitee.apim.core.shared_policy_group.model.SharedPolicyGroupAuditEvent;
import io.gravitee.apim.core.shared_policy_group.query_service.SharedPolicyGroupHistoryQueryService;
import io.gravitee.rest.api.model.EventType;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@DomainService
public class UndeploySharedPolicyGroupUseCase {

    private final EventCrudService eventCrudService;
    private final EventLatestCrudService eventLatestCrudService;
    private final SharedPolicyGroupCrudService sharedPolicyGroupCrudService;
    private final SharedPolicyGroupHistoryCrudService sharedPolicyGroupHistoryCrudService;
    private final SharedPolicyGroupHistoryQueryService sharedPolicyGroupHistoryQueryService;
    private final AuditDomainService auditService;

    public Output execute(Input input) {
        final SharedPolicyGroup sharedPolicyGroup = getLastDeployedSharedPolicyGroup(input);

        sharedPolicyGroup.undeploy();

        publishEvent(input, sharedPolicyGroup.toDefinition(), sharedPolicyGroup);

        updateSharedPolicyGroup(input);
        sharedPolicyGroupHistoryCrudService.create(sharedPolicyGroup);

        createAuditLog(sharedPolicyGroup, sharedPolicyGroup, input.auditInfo);

        return new Output(sharedPolicyGroup);
    }

    private void updateSharedPolicyGroup(Input input) {
        final SharedPolicyGroup existingSharedPolicyGroup = sharedPolicyGroupCrudService.getByEnvironmentId(
            input.environmentId(),
            input.sharedPolicyGroupId()
        );

        existingSharedPolicyGroup.undeploy();

        sharedPolicyGroupCrudService.update(existingSharedPolicyGroup);
    }

    private SharedPolicyGroup getLastDeployedSharedPolicyGroup(Input input) {
        final Optional<SharedPolicyGroup> optional = sharedPolicyGroupHistoryQueryService.getLatestBySharedPolicyGroupId(
            input.environmentId(),
            input.sharedPolicyGroupId()
        );
        if (optional.isEmpty()) {
            throw new SharedPolicyGroupNotFoundException(input.sharedPolicyGroupId());
        }
        return optional.get();
    }

    private void publishEvent(
        Input input,
        io.gravitee.definition.model.v4.sharedpolicygroup.SharedPolicyGroup definition,
        SharedPolicyGroup sharedPolicyGroup
    ) {
        final Event event = eventCrudService.createEvent(
            input.auditInfo().organizationId(),
            input.environmentId,
            Set.of(input.environmentId),
            EventType.UNDEPLOY_SHARED_POLICY_GROUP,
            definition,
            Map.ofEntries(
                entry(Event.EventProperties.USER, input.auditInfo().actor().userId()),
                entry(Event.EventProperties.SHARED_POLICY_GROUP_ID, sharedPolicyGroup.getCrossId())
            )
        );

        eventLatestCrudService.createOrPatchLatestEvent(input.auditInfo.organizationId(), sharedPolicyGroup.getId(), event);
    }

    private void createAuditLog(SharedPolicyGroup oldSharedPolicyGroup, SharedPolicyGroup sharedPolicyGroup, AuditInfo auditInfo) {
        auditService.createEnvironmentAuditLog(
            EnvironmentAuditLogEntity.builder()
                .organizationId(auditInfo.organizationId())
                .environmentId(auditInfo.environmentId())
                .event(SharedPolicyGroupAuditEvent.SHARED_POLICY_GROUP_UNDEPLOYED)
                .actor(auditInfo.actor())
                .oldValue(oldSharedPolicyGroup)
                .newValue(sharedPolicyGroup)
                .createdAt(sharedPolicyGroup.getUpdatedAt())
                .properties(Map.of(AuditProperties.SHARED_POLICY_GROUP, sharedPolicyGroup.getId()))
                .build()
        );
    }

    public record Output(SharedPolicyGroup sharedPolicyGroup) {}

    public record Input(String sharedPolicyGroupId, String environmentId, AuditInfo auditInfo) {
        public Input {
            requireNonNull(sharedPolicyGroupId);
            requireNonNull(environmentId);
            requireNonNull(auditInfo);
        }
    }
}
