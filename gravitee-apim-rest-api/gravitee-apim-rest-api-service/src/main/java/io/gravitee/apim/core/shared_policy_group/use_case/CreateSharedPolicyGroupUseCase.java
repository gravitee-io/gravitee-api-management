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
import io.gravitee.apim.core.shared_policy_group.domain_service.ValidateCreateSharedPolicyGroupDomainService;
import io.gravitee.apim.core.shared_policy_group.model.CreateSharedPolicyGroup;
import io.gravitee.apim.core.shared_policy_group.model.SharedPolicyGroup;
import io.gravitee.apim.core.shared_policy_group.model.SharedPolicyGroupAuditEvent;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.service.common.UuidString;
import java.util.Map;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@UseCase
public class CreateSharedPolicyGroupUseCase {

    private final SharedPolicyGroupCrudService sharedPolicyGroupCrudService;
    private final ValidateCreateSharedPolicyGroupDomainService validateCreateSharedPolicyGroup;
    private final AuditDomainService auditService;

    public Output execute(Input input) {
        var sharedPolicyGroupToCreate = SharedPolicyGroup
            .from(input.sharedPolicyGroupToCreate())
            .toBuilder()
            .id(UuidString.generateRandom())
            .environmentId(input.auditInfo().environmentId())
            .organizationId(input.auditInfo().organizationId())
            .lifecycleState(SharedPolicyGroup.SharedPolicyGroupLifecycleState.UNDEPLOYED)
            .version(0)
            .createdAt(TimeProvider.now())
            .updatedAt(TimeProvider.now())
            .build();

        validateCreateSharedPolicyGroup.validate(sharedPolicyGroupToCreate, input.auditInfo().environmentId());

        var createdSharedPolicyGroup = this.sharedPolicyGroupCrudService.create(sharedPolicyGroupToCreate);

        createAuditLog(createdSharedPolicyGroup, input.auditInfo());

        return new Output(createdSharedPolicyGroup);
    }

    @Builder
    public record Input(CreateSharedPolicyGroup sharedPolicyGroupToCreate, AuditInfo auditInfo) {}

    public record Output(SharedPolicyGroup sharedPolicyGroup) {}

    private void createAuditLog(SharedPolicyGroup sharedPolicyGroup, AuditInfo auditInfo) {
        auditService.createEnvironmentAuditLog(
            EnvironmentAuditLogEntity
                .builder()
                .organizationId(auditInfo.organizationId())
                .environmentId(auditInfo.environmentId())
                .event(SharedPolicyGroupAuditEvent.SHARED_POLICY_GROUP_CREATED)
                .actor(auditInfo.actor())
                .newValue(sharedPolicyGroup)
                .createdAt(sharedPolicyGroup.getCreatedAt())
                .properties(Map.of(AuditProperties.SHARED_POLICY_GROUP, sharedPolicyGroup.getId()))
                .build()
        );
    }
}
