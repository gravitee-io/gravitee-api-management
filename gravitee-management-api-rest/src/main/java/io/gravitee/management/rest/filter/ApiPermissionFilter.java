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
import io.gravitee.management.model.MemberEntity;
import io.gravitee.management.model.Visibility;
import io.gravitee.management.model.permissions.ApiPermission;
import io.gravitee.management.model.permissions.PermissionHelper;
import io.gravitee.management.rest.security.ApiPermissionsRequired;
import io.gravitee.management.service.ApiService;
import io.gravitee.management.service.MembershipService;
import io.gravitee.repository.management.model.MembershipReferenceType;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.ext.Provider;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Provider
@ApiPermissionsRequired
@Priority(100)
public class ApiPermissionFilter extends PermissionFilter<ApiPermissionsRequired> {

    @Inject
    private ApiService apiService;

    @Inject
    private MembershipService membershipService;

    public ApiPermissionFilter() {
        super(ApiPermissionsRequired.class);
    }

    @Override
    protected void filter(ApiPermissionsRequired permissionAnnotation, ContainerRequestContext requestContext) {
        String apiId = null;

        List<String> pathParams = requestContext.getUriInfo().getPathParameters().get("api");
        if (pathParams != null) {
            apiId = pathParams.iterator().next();
        } else {
            List<String> queryParams = requestContext.getUriInfo().getQueryParameters().get("api");
            if (queryParams != null) {
                apiId = queryParams.iterator().next();
            }
        }

        // Check that the API exists
        ApiEntity api = apiService.findById(apiId);

        String username = (securityContext.getUserPrincipal() != null) ?
                securityContext.getUserPrincipal().getName() : null;

        logger.debug("Checking permissions for API: [{}] and user: [{}]", api.getId(), username);

        ApiPermission requiredPermission = permissionAnnotation.value();
        if (requiredPermission != ApiPermission.READ || api.getVisibility() != Visibility.PUBLIC) {
            MemberEntity member = membershipService.getMember(MembershipReferenceType.API, api.getId(), username);
            if(!checkMemberPermission(permissionAnnotation, requestContext, member)) {
                if (api.getGroup() != null && api.getGroup().getId() != null) {
                    member = membershipService.getMember(MembershipReferenceType.API_GROUP, api.getGroup().getId(), username);
                    if (!checkMemberPermission(permissionAnnotation, requestContext, member)) {
                        sendSecurityError(requestContext);
                    }
                } else {
                    sendSecurityError(requestContext);
                }
            }
        }
    }

    private boolean checkMemberPermission(ApiPermissionsRequired permissionAnnotation, ContainerRequestContext requestContext, MemberEntity member) {
        if (member == null) {
            return false;
        }
        List<ApiPermission> permissions = PermissionHelper.getApiPermissionsByRole(member.getType());

        if (!permissions.contains(permissionAnnotation.value())) {
            sendSecurityError(requestContext);
            return false;
        }
        return true;
    }
}
