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
import io.gravitee.apim.core.audit.model.ApiAuditLogEntity;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.event.MembershipAuditEvent;
import io.gravitee.apim.core.membership.crud_service.MembershipCrudService;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.membership.query_service.MembershipQueryService;
import io.gravitee.common.utils.TimeProvider;
import java.util.Collections;

@DomainService
public class DeleteMembershipDomainService {

    MembershipQueryService membershipQueryService;
    MembershipCrudService membershipCrudService;
    AuditDomainService auditDomainService;

    public DeleteMembershipDomainService(
        MembershipQueryService membershipQueryService,
        MembershipCrudService membershipCrudService,
        AuditDomainService auditDomainService
    ) {
        this.membershipQueryService = membershipQueryService;
        this.membershipCrudService = membershipCrudService;
        this.auditDomainService = auditDomainService;
    }

    public void deleteApiMemberships(String apiId, AuditInfo auditInfo) {
        var memberships = membershipQueryService.findByReference(Membership.ReferenceType.API, apiId);
        memberships.forEach(membership -> {
            membershipCrudService.delete(membership.getId());
            createAuditLog(auditInfo, membership, apiId);
        });
    }

    public void deleteIntegrationMemberships(String integrationId) {
        var memberships = membershipQueryService.findByReference(Membership.ReferenceType.INTEGRATION, integrationId);
        memberships.forEach(membership -> {
            membershipCrudService.delete(membership.getId());
        });
    }

    private void createAuditLog(AuditInfo auditInfo, Membership membership, String apiId) {
        auditDomainService.createApiAuditLog(
            ApiAuditLogEntity
                .builder()
                .organizationId(auditInfo.organizationId())
                .environmentId(auditInfo.environmentId())
                .apiId(apiId)
                .event(MembershipAuditEvent.MEMBERSHIP_DELETED)
                .actor(auditInfo.actor())
                .oldValue(membership)
                .createdAt(TimeProvider.now())
                .properties(Collections.emptyMap())
                .build()
        );
    }
}
