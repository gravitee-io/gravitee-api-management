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
package io.gravitee.management.rest.filter;

import io.gravitee.management.model.ApplicationEntity;
import io.gravitee.management.model.MemberEntity;
import io.gravitee.management.model.permissions.ApplicationPermission;
import io.gravitee.management.model.permissions.PermissionHelper;
import io.gravitee.management.rest.security.ApplicationPermissionsRequired;
import io.gravitee.management.service.ApplicationService;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.ext.Provider;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Provider
@ApplicationPermissionsRequired
@Priority(100)
public class ApplicationPermissionFilter extends PermissionFilter<ApplicationPermissionsRequired> {

    @Inject
    private ApplicationService applicationService;

    public ApplicationPermissionFilter() {
        super(ApplicationPermissionsRequired.class);
    }

    @Override
    protected void filter(ApplicationPermissionsRequired permissionAnnotation, ContainerRequestContext requestContext) {
        String applicationId = requestContext.getUriInfo().getPathParameters().get("application").iterator().next();

        // Check that the application exists
        ApplicationEntity application = applicationService.findById(applicationId);

        String username = (securityContext.getUserPrincipal() != null) ?
                securityContext.getUserPrincipal().getName() : null;

        logger.debug("Checking permissions for application: [{}] and user: [{}]", application.getId(), username);

        Set<MemberEntity> members = applicationService.getMembers(application.getId(), null);
        Optional<MemberEntity> optMember =
                members.stream().filter(memberEntity -> memberEntity.getUsername().equalsIgnoreCase(username)).findFirst();

        if (optMember.isPresent()) {
            List<ApplicationPermission> permissions = PermissionHelper.getApplicationPermissionsByRole(optMember.get().getType());

            if (!permissions.contains(permissionAnnotation.value())) {
                sendSecurityError(requestContext);
            }
        } else {
            sendSecurityError(requestContext);
        }
    }
}
