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

import io.gravitee.management.model.ApiEntity;
import io.gravitee.management.model.ApplicationEntity;
import io.gravitee.management.model.Owner;
import io.gravitee.management.service.ApiService;
import io.gravitee.management.service.ApplicationService;
import io.gravitee.management.service.PermissionService;
import io.gravitee.management.service.PermissionType;
import io.gravitee.management.service.exceptions.ForbiddenAccessException;
import io.gravitee.management.service.exceptions.TechnicalManagementException;
import io.gravitee.repository.api.TeamMembershipRepository;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.model.Member;
import io.gravitee.repository.model.TeamRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
@Component
public class PermissionServiceImpl implements PermissionService {

    /**
     * Logger.
     */
    private final Logger LOGGER = LoggerFactory.getLogger(PermissionServiceImpl.class);

    @Autowired
    private TeamMembershipRepository teamMembershipRepository;

    @Autowired
    private ApiService apiService;

    @Autowired
    private ApplicationService applicationService;

    @Override
    public void hasPermission(String username, String entityId, PermissionType permissionType) {
        if (permissionType == PermissionType.VIEW_API || permissionType == PermissionType.EDIT_API) {
            validateApi(username, entityId, permissionType);
        } else if (permissionType == PermissionType.VIEW_APPLICATION || permissionType == PermissionType.EDIT_APPLICATION) {
            validateApplication(username, entityId, permissionType);
        } else if (permissionType == PermissionType.VIEW_TEAM || permissionType == PermissionType.EDIT_TEAM) {
            validateTeam(username, entityId, permissionType);
        }
    }

    private void validateApi(String username, String apiName, PermissionType permissionType) {
        try {
            LOGGER.debug("Validate user rights for API: {}", apiName);
            ApiEntity api = apiService.findByName(apiName).get();

            if (permissionType == PermissionType.VIEW_API) {
                if (api.isPrivate()) {
                    if (api.getOwner().getType() == Owner.OwnerType.Team) {
                        // Check if the user is a member of the team
                        Member member = teamMembershipRepository.getMember(api.getOwner().getLogin(), username);
                        if (member == null) {
                            LOGGER.error("User {} does not have correct rights to view team's API {}", username, apiName);
                            throw new ForbiddenAccessException();
                        }
                    } else {
                        // In case of user owner
                        if (!api.getOwner().getLogin().equalsIgnoreCase(username)) {
                            LOGGER.error("User {} does not have correct rights to view user's API {}", username, apiName);
                            throw new ForbiddenAccessException();
                        }
                    }
                }
            } else {
                if (api.getOwner().getType() == Owner.OwnerType.Team) {
                    // Check if the user is an admin member of the team
                    Member member = teamMembershipRepository.getMember(api.getOwner().getLogin(), username);
                    if (member == null || member.getRole() != TeamRole.ADMIN) {
                        LOGGER.error("User {} does not have correct rights to edit team's API {}", username, apiName);
                        throw new ForbiddenAccessException();
                    }
                } else {
                    // In case of user owner
                    if (!api.getOwner().getLogin().equalsIgnoreCase(username)) {
                        LOGGER.error("User {} does not have correct rights to edit user's API {}", username, apiName);
                        throw new ForbiddenAccessException();
                    }
                }
            }
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to validate user's permissions for API: {}", apiName, ex);
            throw new TechnicalManagementException("An error occurs while trying to validate user's permissions for API: " + apiName, ex);
        }
    }

    private void validateApplication(String username, String applicationName, PermissionType permissionType) {
        try {
            LOGGER.debug("Validate user rights for application: {}", applicationName);

            ApplicationEntity application = applicationService.findByName(applicationName).get();
            if (permissionType == PermissionType.VIEW_API) {
                    if (application.getOwner().getType() == Owner.OwnerType.Team) {
                        // Check if the user is a member of the team
                        Member member = teamMembershipRepository.getMember(application.getOwner().getLogin(), username);
                        if (member == null) {
                            LOGGER.error("User {} does not have correct rights to view team's application {}", username, applicationName);
                            throw new ForbiddenAccessException();
                        }
                    } else {
                        // In case of user owner
                        if (!application.getOwner().getLogin().equalsIgnoreCase(username)) {
                            LOGGER.error("User {} does not have correct rights to view user's application {}", username, applicationName);
                            throw new ForbiddenAccessException();
                        }
                    }
            } else {
                if (application.getOwner().getType() == Owner.OwnerType.Team) {
                    // Check if the user is an admin member of the team
                    Member member = teamMembershipRepository.getMember(application.getOwner().getLogin(), username);
                    if (member == null || member.getRole() != TeamRole.ADMIN) {
                        LOGGER.error("User {} does not have correct rights to edit team's application {}", username, applicationName);
                        throw new ForbiddenAccessException();
                    }
                } else {
                    // In case of user owner
                    if (!application.getOwner().getLogin().equalsIgnoreCase(username)) {
                        LOGGER.error("User {} does not have correct rights to edit user's application {}", username, applicationName);
                        throw new ForbiddenAccessException();
                    }
                }
            }
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to validate user's permissions for application: {}", applicationName, ex);
            throw new TechnicalManagementException("An error occurs while trying to validate user's permissions for application: " + applicationName, ex);
        }
    }

    private void validateTeam(String username, String teamName, PermissionType permissionType) {

    }
}
