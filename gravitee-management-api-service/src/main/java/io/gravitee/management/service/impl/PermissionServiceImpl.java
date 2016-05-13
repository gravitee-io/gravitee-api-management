/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.management.service.impl;

import io.gravitee.management.model.*;
import io.gravitee.management.service.*;
import io.gravitee.management.service.exceptions.ForbiddenAccessException;
import io.gravitee.management.service.exceptions.UnauthorizedAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.security.Principal;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
@Component
public class PermissionServiceImpl extends TransactionalService implements PermissionService {

    /**
     * Logger.
     */
    private final Logger LOGGER = LoggerFactory.getLogger(PermissionServiceImpl.class);

    @Autowired
    private UserService userService;

    @Autowired
    private ApiService apiService;

    @Autowired
    private ApplicationService applicationService;

    @Override
    public void hasPermission(Principal principal, String item, PermissionType permissionType) {
        if (principal != null) {
            final UserEntity user = userService.findByName(principal.getName());
            if (user != null && user.getRoles().contains("ROLE_ADMIN")) {
                LOGGER.debug("User {} has full access because of admin role", principal.getName());
                return;
            }
        }

        if (permissionType == PermissionType.VIEW_API || permissionType == PermissionType.EDIT_API) {
            validateApi(principal, item, permissionType);
        } else if (permissionType == PermissionType.VIEW_APPLICATION || permissionType == PermissionType.EDIT_APPLICATION) {
            validateApplication(principal, item, permissionType);
        }
    }

    private void validateApi(Principal principal, String apiId, PermissionType permissionType) {
        LOGGER.debug("Validate user rights for API: {}", apiId);

        final ApiEntity api = apiService.findById(apiId);
        if (permissionType == PermissionType.VIEW_API) {

            switch (api.getVisibility()) {
                case PRIVATE:
                case RESTRICTED:
                    if (principal == null) {
                        LOGGER.error("Anonymous user does not have rights to view API {}", api);
                        throw new UnauthorizedAccessException();
                    }

                    MemberEntity member = apiService.getMember(apiId, principal.getName());
                    if (member == null) {
                        LOGGER.error("User {} does not have rights to view API {}", principal.getName(), api);
                        throw new ForbiddenAccessException();
                    }
                    break;
            }
        } else if (permissionType == PermissionType.EDIT_API) {
            if (principal == null) {
                LOGGER.error("Anonymous user does not have rights to edit API {}", api);
                throw new UnauthorizedAccessException();
            }

            MemberEntity member = apiService.getMember(apiId, principal.getName());
            if (member == null || member.getType() == MembershipType.USER) {
                LOGGER.error("User {} does not have rights to view API {}", principal.getName(), api);
                throw new ForbiddenAccessException();
            }
        }
    }

    private void validateApplication(Principal principal, String applicationId, PermissionType permissionType) {
        LOGGER.debug("Validate user rights for application: {}", applicationId);

        if (principal == null) {
            LOGGER.error("Anonymous user does not have rights to view application {}", applicationId);
            throw new UnauthorizedAccessException();
        }

        final ApplicationEntity application = applicationService.findById(applicationId);

        MemberEntity member = applicationService.getMember(applicationId, principal.getName());
        if (member == null) {
            LOGGER.error("User {} does not have correct rights to view application {}",
                    principal.getName(), application);
            throw new ForbiddenAccessException();
        } else {
            if (permissionType == PermissionType.EDIT_APPLICATION && member.getType() == MembershipType.USER) {
                LOGGER.error("User {} does not have correct rights to edit application {}",
                        principal.getName(), application);
                throw new ForbiddenAccessException();
            }
        }
    }
}
