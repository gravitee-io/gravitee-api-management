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
import io.gravitee.apim.core.shared_policy_group.model.SharedPolicyGroup;
import io.gravitee.apim.core.shared_policy_group.model.SharedPolicyGroupAuditEvent;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.model.EventType;
import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@DomainService
public class DeleteSharedPolicyGroupUseCase {

    private final SharedPolicyGroupCrudService sharedPolicyGroupCrudService;
    private final SharedPolicyGroupHistoryCrudService sharedPolicyGroupHistoryCrudService;
    private final AuditDomainService auditService;
    private final EventCrudService eventCrudService;
    private final EventLatestCrudService eventLatestCrudService;

    public Output execute(Input input) {
        var sharedPolicyGroupToDelete =
            this.sharedPolicyGroupCrudService.getByEnvironmentId(input.auditInfo().environmentId(), input.sharedPolicyGroupId());

        publishUndeployEvent(input, sharedPolicyGroupToDelete);

        this.sharedPolicyGroupCrudService.delete(sharedPolicyGroupToDelete.getId());
        this.sharedPolicyGroupHistoryCrudService.delete(sharedPolicyGroupToDelete.getId());

        createAuditLog(sharedPolicyGroupToDelete, input.auditInfo());
        return new Output();
    }

    @Builder
    public record Input(String sharedPolicyGroupId, AuditInfo auditInfo) {}

    public record Output() {}

    private void createAuditLog(SharedPolicyGroup sharedPolicyGroup, AuditInfo auditInfo) {
        auditService.createEnvironmentAuditLog(
            EnvironmentAuditLogEntity
                .builder()
                .organizationId(auditInfo.organizationId())
                .environmentId(auditInfo.environmentId())
                .event(SharedPolicyGroupAuditEvent.SHARED_POLICY_GROUP_DELETED)
                .actor(auditInfo.actor())
                .oldValue(sharedPolicyGroup)
                .newValue(null)
                .createdAt(TimeProvider.now())
                .properties(Map.of(AuditProperties.SHARED_POLICY_GROUP, sharedPolicyGroup.getId()))
                .build()
        );
    }

    private void publishUndeployEvent(Input input, SharedPolicyGroup sharedPolicyGroup) {
        final Event event = eventCrudService.createEvent(
            input.auditInfo.organizationId(),
            input.auditInfo.environmentId(),
            Set.of(input.auditInfo.environmentId()),
            EventType.UNDEPLOY_SHARED_POLICY_GROUP,
            sharedPolicyGroup.getDescription(),
            Map.ofEntries(
                entry(Event.EventProperties.USER, input.auditInfo.actor().userId()),
                entry(Event.EventProperties.SHARED_POLICY_GROUP_ID, sharedPolicyGroup.getCrossId())
            )
        );

        eventLatestCrudService.createOrPatchLatestEvent(input.auditInfo.organizationId(), sharedPolicyGroup.getId(), event);
    }
}
