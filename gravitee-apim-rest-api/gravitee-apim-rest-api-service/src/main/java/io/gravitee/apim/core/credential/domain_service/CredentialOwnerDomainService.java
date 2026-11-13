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
package io.gravitee.apim.core.credential.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.credential.exception.InvalidCredentialException;
import io.gravitee.apim.core.credential.model.CredentialActor;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.membership.query_service.MembershipQueryService;
import io.gravitee.apim.core.user.crud_service.UserCrudService;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@DomainService
@RequiredArgsConstructor
public class CredentialOwnerDomainService {

    private final UserCrudService userCrudService;
    private final MembershipQueryService membershipQueryService;

    /** @return the owner to give a credential: the actor when no owner is asked for, else a member of the actor's environment */
    public String ownerFor(CredentialActor actor, String requestedOwnerId) {
        if (requestedOwnerId == null || requestedOwnerId.equals(actor.userId())) {
            return actor.userId();
        }
        var isMember = userCrudService
            .findBaseUserById(requestedOwnerId)
            .filter(user -> actor.organizationId().equals(user.getOrganizationId()))
            .isPresent();
        if (!isMember) {
            throw notAMember(requestedOwnerId);
        }
        var hasAccess = membershipQueryService
            .findByMemberIdAndMemberTypeAndReferenceType(requestedOwnerId, Membership.Type.USER, Membership.ReferenceType.ENVIRONMENT)
            .stream()
            .anyMatch(membership -> actor.environmentId().equals(membership.getReferenceId()));
        if (!hasAccess) {
            throw notAMember(requestedOwnerId);
        }
        return requestedOwnerId;
    }

    /** @return the display name of each owner that is still a user of the actor's organization, by user id */
    public Map<String, String> namesOf(CredentialActor actor, Collection<String> ownerIds) {
        var ids = ownerIds.stream().filter(Objects::nonNull).distinct().toList();
        return userCrudService
            .findBaseUsersByIds(ids)
            .stream()
            .filter(user -> actor.organizationId().equals(user.getOrganizationId()) && user.displayName() != null)
            .collect(Collectors.toMap(BaseUserEntity::getId, BaseUserEntity::displayName));
    }

    // One reason for every refusal, so the answer does not tell a caller which user ids exist.
    private static InvalidCredentialException notAMember(String ownerId) {
        return new InvalidCredentialException("Owner " + ownerId + " is not a member of this environment.");
    }
}
