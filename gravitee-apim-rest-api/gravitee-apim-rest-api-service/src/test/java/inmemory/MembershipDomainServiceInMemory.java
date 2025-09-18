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
package inmemory;

import io.gravitee.apim.core.member.domain_service.MemberDomainService;
import io.gravitee.apim.core.membership.domain_service.MembershipDomainService;
import io.gravitee.apim.core.membership.model.TransferOwnership;
import io.gravitee.rest.api.model.MemberEntity;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MembershipDomainServiceInMemory extends AbstractServiceInMemory<MemberEntity> implements MembershipDomainService {

    @Override
    public Map<String, char[]> getUserMemberPermissions(
        ExecutionContext executionContext,
        MembershipReferenceType referenceType,
        String referenceId,
        String userId
    ) {
        return storage
            .stream()
            .filter(memberEntity -> memberEntity.getReferenceType().equals(referenceType))
            .filter(memberEntity -> memberEntity.getReferenceId().equals(referenceId))
            .filter(memberEntity -> memberEntity.getType().equals(MembershipMemberType.USER))
            .filter(memberEntity -> memberEntity.getId().equals(userId))
            .findFirst()
            .map(MemberEntity::getPermissions)
            .orElse(null);
    }

    @Override
    public void transferOwnership(TransferOwnership transferOwnership, RoleScope roleScope, String itemId) {
        MemberEntity currentPrimaryOwnerMember = storage
            .stream()
            .filter(memberEntity -> memberEntity.getReferenceType().equals(MembershipReferenceType.valueOf(roleScope.name())))
            .filter(memberEntity -> memberEntity.getReferenceId().equals(itemId))
            .filter(memberEntity ->
                memberEntity
                    .getRoles()
                    .stream()
                    .anyMatch(role -> role.getScope().equals(roleScope) && role.getName().equals("PRIMARY_OWNER"))
            )
            .findAny()
            .orElseThrow();
        RoleEntity currentPrimaryOwnerRole = currentPrimaryOwnerMember
            .getRoles()
            .stream()
            .filter(role -> role.getScope().equals(roleScope))
            .findFirst()
            .orElseThrow();
        currentPrimaryOwnerRole.setName(transferOwnership.getCurrentPrimaryOwnerNewRole());
        Optional<MemberEntity> newPrimaryOwnerMember = storage
            .stream()
            .filter(memberEntity -> memberEntity.getReferenceType().equals(MembershipReferenceType.valueOf(roleScope.name())))
            .filter(memberEntity -> memberEntity.getReferenceId().equals(itemId))
            .filter(memberEntity -> memberEntity.getId().equals(transferOwnership.getNewPrimaryOwnerId()))
            .findAny();
        if (newPrimaryOwnerMember.isPresent()) {
            RoleEntity newPrimaryOwnerRole = newPrimaryOwnerMember
                .get()
                .getRoles()
                .stream()
                .filter(role -> role.getScope().equals(roleScope))
                .findFirst()
                .orElseThrow();
            newPrimaryOwnerRole.setName("PRIMARY_OWNER");
        } else {
            MemberEntity newPrimaryOwner = new MemberEntity();
            newPrimaryOwner.setId(transferOwnership.getNewPrimaryOwnerId());
            newPrimaryOwner.setReferenceType(MembershipReferenceType.valueOf(roleScope.name()));
            newPrimaryOwner.setReferenceId(itemId);
            newPrimaryOwner.setRoles(List.of(RoleEntity.builder().name("PRIMARY_OWNER").build()));
            storage.add(newPrimaryOwner);
        }
    }
}
