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
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.apim.core.audit.model.event.MembershipAuditEvent;
import io.gravitee.apim.core.membership.crud_service.MembershipCrudService;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.apim.core.membership.model.Role;
import io.gravitee.apim.core.membership.query_service.MembershipQueryService;
import io.gravitee.apim.core.membership.query_service.RoleQueryService;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.model.permissions.SystemRole;
import io.gravitee.rest.api.service.common.ReferenceContext;
import io.gravitee.rest.api.service.common.UuidString;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@DomainService
@RequiredArgsConstructor
public class ApiProductPrimaryOwnerDomainService {

    private final AuditDomainService auditService;
    private final MembershipCrudService membershipCrudService;
    private final MembershipQueryService membershipQueryService;
    private final RoleQueryService roleQueryService;

    public void createApiProductPrimaryOwnerMembership(String apiProductId, PrimaryOwnerEntity primaryOwner, AuditInfo auditInfo) {
        findPrimaryOwnerRole(auditInfo.organizationId()).ifPresent(role -> {
            var membership = Membership.builder()
                .id(UuidString.generateRandom())
                .referenceId(apiProductId)
                .referenceType(Membership.ReferenceType.API_PRODUCT)
                .roleId(role.getId())
                .memberId(primaryOwner.id())
                .memberType(Membership.Type.valueOf(primaryOwner.type().name()))
                .createdAt(TimeProvider.now())
                .updatedAt(TimeProvider.now())
                .build();
            membershipCrudService.create(membership);
            createApiProductAuditLog(membership, auditInfo);
        });
    }

    public void assignPrimaryOwner(String apiProductId, PrimaryOwnerEntity primaryOwner, AuditInfo auditInfo) {
        createApiProductPrimaryOwnerMembership(apiProductId, primaryOwner, auditInfo);
    }

    public PrimaryOwnerEntity getApiProductPrimaryOwner(final String organizationId, String apiProductId) {
        return findPrimaryOwnerRole(organizationId)
            .flatMap(role ->
                membershipQueryService
                    .findByReferenceAndRoleId(Membership.ReferenceType.API_PRODUCT, apiProductId, role.getId())
                    .stream()
                    .findFirst()
                    .map(membership ->
                        PrimaryOwnerEntity.builder()
                            .id(membership.getMemberId())
                            .displayName(membership.getMemberId())
                            .type(PrimaryOwnerEntity.Type.valueOf(membership.getMemberType().name()))
                            .build()
                    )
            )
            .orElseThrow(() -> new RuntimeException("Primary owner not found for API Product: " + apiProductId));
    }

    private Optional<Role> findPrimaryOwnerRole(String organizationId) {
        return roleQueryService.findApiRole(
            SystemRole.PRIMARY_OWNER.name(),
            ReferenceContext.builder().referenceType(ReferenceContext.Type.ORGANIZATION).referenceId(organizationId).build()
        );
    }

    private void createApiProductAuditLog(Membership membership, AuditInfo auditInfo) {
        Map<AuditProperties, String> properties = switch (membership.getMemberType()) {
            case USER -> Map.of(AuditProperties.USER, membership.getMemberId());
            case GROUP -> Map.of(AuditProperties.GROUP, membership.getMemberId());
        };
        auditService.createApiProductAuditLog(
            io.gravitee.apim.core.audit.model.ApiProductAuditLogEntity.builder()
                .organizationId(auditInfo.organizationId())
                .environmentId(auditInfo.environmentId())
                .apiProductId(membership.getReferenceId())
                .event(MembershipAuditEvent.MEMBERSHIP_CREATED)
                .actor(auditInfo.actor())
                .newValue(membership)
                .createdAt(membership.getCreatedAt())
                .properties(properties)
                .build()
        );
    }
}
