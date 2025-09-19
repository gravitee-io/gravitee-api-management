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

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.apim.core.audit.model.EnvironmentAuditLogEntity;
import io.gravitee.apim.core.shared_policy_group.crud_service.SharedPolicyGroupCrudService;
import io.gravitee.apim.core.shared_policy_group.domain_service.ValidateUpdateSharedPolicyGroupDomainService;
import io.gravitee.apim.core.shared_policy_group.model.SharedPolicyGroup;
import io.gravitee.apim.core.shared_policy_group.model.SharedPolicyGroupAuditEvent;
import io.gravitee.apim.core.shared_policy_group.model.UpdateSharedPolicyGroup;
import java.util.Map;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@UseCase
public class UpdateSharedPolicyGroupUseCase {

    private final SharedPolicyGroupCrudService sharedPolicyGroupCrudService;
    private final ValidateUpdateSharedPolicyGroupDomainService validateUpdateSharedPolicyGroupDomainService;
    private final AuditDomainService auditService;

    public Output execute(Input input) {
        var existingSharedPolicyGroup = this.sharedPolicyGroupCrudService.getByEnvironmentId(
            input.auditInfo().environmentId(),
            input.sharedPolicyGroupId()
        );

        var sharedPolicyGroupToUpdate = existingSharedPolicyGroup.update(input.sharedPolicyGroupToUpdate());

        validateUpdateSharedPolicyGroupDomainService.validate(sharedPolicyGroupToUpdate, input.auditInfo().environmentId());

        var updatedSharedPolicyGroup = this.sharedPolicyGroupCrudService.update(sharedPolicyGroupToUpdate);
        createAuditLog(existingSharedPolicyGroup, updatedSharedPolicyGroup, input.auditInfo());
        return new Output(updatedSharedPolicyGroup);
    }

    @Builder
    public record Input(String sharedPolicyGroupId, UpdateSharedPolicyGroup sharedPolicyGroupToUpdate, AuditInfo auditInfo) {}

    public record Output(SharedPolicyGroup sharedPolicyGroup) {}

    private void createAuditLog(SharedPolicyGroup oldSharedPolicyGroup, SharedPolicyGroup sharedPolicyGroup, AuditInfo auditInfo) {
        auditService.createEnvironmentAuditLog(
            EnvironmentAuditLogEntity.builder()
                .organizationId(auditInfo.organizationId())
                .environmentId(auditInfo.environmentId())
                .event(SharedPolicyGroupAuditEvent.SHARED_POLICY_GROUP_UPDATED)
                .actor(auditInfo.actor())
                .oldValue(oldSharedPolicyGroup)
                .newValue(sharedPolicyGroup)
                .createdAt(sharedPolicyGroup.getUpdatedAt())
                .properties(Map.of(AuditProperties.SHARED_POLICY_GROUP, sharedPolicyGroup.getId()))
                .build()
        );
    }
}
