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
package io.gravitee.apim.core.user.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.user.crud_service.UserCrudService;
import io.gravitee.rest.api.service.exceptions.UserNotFoundException;
import lombok.RequiredArgsConstructor;

@DomainService
@RequiredArgsConstructor
public class ValidateUserInOrganizationDomainService {

    private final UserCrudService userCrudService;

    /**
     * Ensures the user belongs to the given organization, so a lookup cannot reach a user from another
     * organization. The organization id is compared case-insensitively, as the v1 {@code UserService} does.
     *
     * @param userId the user whose details are requested
     * @param organizationId the organization the request is scoped to
     * @throws UserNotFoundException when the user does not exist or belongs to another organization
     */
    public void validate(String userId, String organizationId) {
        userCrudService
            .findBaseUserById(userId)
            .filter(user -> organizationId.equalsIgnoreCase(user.getOrganizationId()))
            .orElseThrow(() -> new UserNotFoundException(userId));
    }
}
