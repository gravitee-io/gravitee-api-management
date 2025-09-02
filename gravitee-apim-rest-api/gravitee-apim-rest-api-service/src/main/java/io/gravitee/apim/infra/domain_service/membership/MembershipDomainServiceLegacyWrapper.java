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
package io.gravitee.apim.infra.domain_service.membership;

import io.gravitee.apim.core.membership.domain_service.MembershipDomainService;
import io.gravitee.apim.core.membership.model.TransferOwnership;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.TransferOwnershipNotAllowedException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class MembershipDomainServiceLegacyWrapper implements MembershipDomainService {

    private final MembershipService legacyService;
    private final RoleService roleService;

    @Override
    public Map<String, char[]> getUserMemberPermissions(
        ExecutionContext executionContext,
        MembershipReferenceType referenceType,
        String referenceId,
        String userId
    ) {
        return legacyService.getUserMemberPermissions(executionContext, referenceType, referenceId, userId);
    }

    @Override
    public void transferOwnership(TransferOwnership transferOwnership, RoleScope roleScope, String itemId) {
        validateTransferOwnership(transferOwnership);
        List<RoleEntity> newRoles = new ArrayList<>();
        roleService
            .findByScopeAndName(
                roleScope,
                transferOwnership.getCurrentPrimaryOwnerNewRole(),
                GraviteeContext.getExecutionContext().getOrganizationId()
            )
            .ifPresent(newRoles::add);
        MembershipService.MembershipMember membershipMember = new MembershipService.MembershipMember(
            transferOwnership.getNewPrimaryOwnerId(),
            transferOwnership.getUserReference(),
            MembershipMemberType.USER
        );
        legacyService.transferOwnership(
            GraviteeContext.getExecutionContext(),
            MembershipReferenceType.valueOf(roleScope.name()),
            roleScope,
            itemId,
            membershipMember,
            newRoles
        );
    }

    private void validateTransferOwnership(TransferOwnership transferOwnership) {
        if ("PRIMARY_OWNER".equals(transferOwnership.getCurrentPrimaryOwnerNewRole())) {
            throw new TransferOwnershipNotAllowedException(transferOwnership.getCurrentPrimaryOwnerNewRole());
        }
    }
}
