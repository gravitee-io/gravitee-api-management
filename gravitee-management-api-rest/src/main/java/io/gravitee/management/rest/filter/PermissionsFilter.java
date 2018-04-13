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

import io.gravitee.management.model.ApiEntity;
import io.gravitee.management.model.ApplicationEntity;
import io.gravitee.management.model.GroupEntity;
import io.gravitee.management.model.RoleEntity;
import io.gravitee.management.model.permissions.SystemRole;
import io.gravitee.management.rest.resource.AbstractResource;
import io.gravitee.management.rest.security.Permission;
import io.gravitee.management.rest.security.Permissions;
import io.gravitee.management.service.*;
import io.gravitee.management.service.exceptions.ForbiddenAccessException;
import io.gravitee.management.service.exceptions.UnauthorizedAccessException;
import io.gravitee.repository.management.model.MembershipDefaultReferenceId;
import io.gravitee.repository.management.model.MembershipReferenceType;
import io.gravitee.repository.management.model.RoleScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.Map;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Provider
@Priority(100)
public class PermissionsFilter implements ContainerRequestFilter {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Context
    protected ResourceInfo resourceInfo;

    @Inject
    private SecurityContext securityContext;

    @Inject
    private MembershipService membershipService;

    @Inject
    private ApplicationService applicationService;

    @Inject
    private ApiService apiService;

    @Inject
    private RoleService roleService;

    @Inject
    private GroupService groupService;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if (securityContext.isUserInRole(SystemRole.ADMIN.name()) ||
                securityContext.isUserInRole(AbstractResource.MANAGEMENT_ADMIN) ||
                securityContext.isUserInRole(AbstractResource.PORTAL_ADMIN)) {
            logger.debug("User [{}] has full access because of its ADMIN role",
                    securityContext.getUserPrincipal().getName());
            return;
        }

        filter(getRequiredPermission(), requestContext);
    }

    protected void filter(Permissions permissions, ContainerRequestContext requestContext) {
        if (permissions != null && permissions.value().length > 0) {
            Principal principal = securityContext.getUserPrincipal();
            if (principal != null) {
                String username = principal.getName();
                for (Permission permission : permissions.value()) {
                    RoleEntity role;
                    Map<String, char[]> memberPermissions;
                    switch (permission.value().getScope()) {
                        case MANAGEMENT:
                            role = membershipService.getRole(MembershipReferenceType.MANAGEMENT, MembershipDefaultReferenceId.DEFAULT.name(), username, RoleScope.MANAGEMENT);
                            if (roleService.hasPermission(role.getPermissions(), permission.value().getPermission(), permission.acls())) {
                                return;
                            }
                            break;
                        case PORTAL:
                            role = membershipService.getRole(MembershipReferenceType.PORTAL, MembershipDefaultReferenceId.DEFAULT.name(), username, RoleScope.PORTAL);
                            if (roleService.hasPermission(role.getPermissions(), permission.value().getPermission(), permission.acls())) {
                                return;
                            }
                            break;
                        case APPLICATION:
                            ApplicationEntity application = getApplication(requestContext);
                            memberPermissions = membershipService.getMemberPermissions(application, username);
                            if (roleService.hasPermission(memberPermissions, permission.value().getPermission(), permission.acls())) {
                                return;
                            }
                            break;
                        case API:
                            ApiEntity api = getApi(requestContext);
                            memberPermissions = membershipService.getMemberPermissions(api, username);
                            if (roleService.hasPermission(memberPermissions, permission.value().getPermission(), permission.acls())) {
                                return;
                            }
                            break;
                        case GROUP:
                            GroupEntity group = getGroup(requestContext);
                            memberPermissions = membershipService.getMemberPermissions(group, username);
                            if (roleService.hasPermission(memberPermissions, permission.value().getPermission(), permission.acls())) {
                                return;
                            }
                            break;
                        default:
                            sendSecurityError();
                    }
                }
            }
            sendSecurityError();
        }
    }

    private ApiEntity getApi(ContainerRequestContext requestContext) {
        String apiId = getId("api", requestContext);
        if (apiId == null) {
            return null;
        }
        return apiService.findById(apiId);
    }

    private GroupEntity getGroup(ContainerRequestContext requestContext) {
        String groupId = getId("group", requestContext);
        if (groupId == null) {
            return null;
        }
        return groupService.findById(groupId);
    }

    private ApplicationEntity getApplication(ContainerRequestContext requestContext) {
        String applicationId = getId("application", requestContext);
        if (applicationId == null) {
            return null;
        }
        return applicationService.findById(applicationId);
    }

    private String getId(String key, ContainerRequestContext requestContext) {
        List<String> pathParams = requestContext.getUriInfo().getPathParameters().get(key);
        if (pathParams != null) {
            return pathParams.iterator().next();
        } else {
            List<String> queryParams = requestContext.getUriInfo().getQueryParameters().get(key);
            if (queryParams != null) {
                return queryParams.iterator().next();
            }
        }
        return null;
    }

    private void sendSecurityError() {
        Principal principal = securityContext.getUserPrincipal();
        if (principal != null) {
            throw new ForbiddenAccessException();
        } else {
            throw new UnauthorizedAccessException();
        }
    }

    private Permissions getRequiredPermission() {
        Permissions permission = resourceInfo.getResourceMethod().getDeclaredAnnotation(Permissions.class);

        if (permission == null) {
            return resourceInfo.getResourceClass().getDeclaredAnnotation(Permissions.class);
        }

        return permission;
    }
}
