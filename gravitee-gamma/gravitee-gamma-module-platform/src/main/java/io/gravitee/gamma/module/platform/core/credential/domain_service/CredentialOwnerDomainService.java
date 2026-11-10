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
package io.gravitee.gamma.module.platform.core.credential.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.user.crud_service.UserCrudService;
import io.gravitee.gamma.module.platform.core.credential.exception.InvalidCredentialException;
import io.gravitee.gamma.module.platform.core.credential.model.CredentialActor;
import lombok.RequiredArgsConstructor;

@DomainService
@RequiredArgsConstructor
public class CredentialOwnerDomainService {

    private final UserCrudService userCrudService;

    /** @return the owner to give a credential: the actor when no owner is asked for, else a user of the actor's organization */
    public String ownerFor(CredentialActor actor, String requestedOwnerId) {
        if (requestedOwnerId == null || requestedOwnerId.equals(actor.userId())) {
            return actor.userId();
        }
        var isMember = userCrudService
            .findBaseUserById(requestedOwnerId)
            .filter(user -> actor.organizationId().equals(user.getOrganizationId()))
            .isPresent();
        if (!isMember) {
            throw new InvalidCredentialException("Owner " + requestedOwnerId + " is not a user of this organization.");
        }
        return requestedOwnerId;
    }
}
