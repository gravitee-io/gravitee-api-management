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
package io.gravitee.apim.core.membership.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.membership.crud_service.MembershipCrudService;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.apim.core.membership.model.Role;
import io.gravitee.apim.core.membership.query_service.RoleQueryService;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.model.permissions.SystemRole;
import io.gravitee.rest.api.service.common.ReferenceContext;
import io.gravitee.rest.api.service.common.UuidString;
import java.util.Optional;
import lombok.AllArgsConstructor;

@AllArgsConstructor
@DomainService
public class IntegrationPrimaryOwnerDomainService {

    private final MembershipCrudService membershipCrudService;
    private final RoleQueryService roleQueryService;

    public void createIntegrationPrimaryOwnerMembership(String integrationId, PrimaryOwnerEntity primaryOwner, AuditInfo auditInfo) {
        findPrimaryOwnerRole(auditInfo.organizationId())
            .ifPresent(role -> {
                var membership = Membership
                    .builder()
                    .id(UuidString.generateRandom())
                    .referenceId(integrationId)
                    .referenceType(Membership.ReferenceType.INTEGRATION)
                    .roleId(role.getId())
                    .memberId(primaryOwner.id())
                    .memberType(Membership.Type.valueOf(primaryOwner.type().name()))
                    .createdAt(TimeProvider.now())
                    .updatedAt(TimeProvider.now())
                    .build();
                membershipCrudService.create(membership);
            });
    }

    private Optional<Role> findPrimaryOwnerRole(String organizationId) {
        return roleQueryService.findIntegrationRole(
            SystemRole.PRIMARY_OWNER.name(),
            ReferenceContext.builder().referenceType(ReferenceContext.Type.ORGANIZATION).referenceId(organizationId).build()
        );
    }
}
