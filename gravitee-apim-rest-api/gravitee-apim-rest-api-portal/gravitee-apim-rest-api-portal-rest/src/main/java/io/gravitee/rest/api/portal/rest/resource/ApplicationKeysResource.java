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
package io.gravitee.rest.api.portal.rest.resource;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.ApiKeyEntity;
import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.portal.rest.mapper.KeyMapper;
import io.gravitee.rest.api.portal.rest.model.Key;
import io.gravitee.rest.api.portal.rest.security.Permission;
import io.gravitee.rest.api.portal.rest.security.Permissions;
import io.gravitee.rest.api.service.ApiKeyService;
import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApplicationKeysResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private ApplicationService applicationService;

    @Inject
    private ApiKeyService apiKeyService;

    @Inject
    private KeyMapper keyMapper;

    @SuppressWarnings("UnresolvedRestParam")
    @PathParam("applicationId")
    private String applicationId;

    @POST
    @Path("/_renew")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.APPLICATION_SUBSCRIPTION, acls = RolePermissionAction.UPDATE) })
    public Response renewSharedKey() {
        ApplicationEntity applicationEntity = applicationService.findById(GraviteeContext.getExecutionContext(), applicationId);
        final Key createdKey = keyMapper.convert(apiKeyService.renew(GraviteeContext.getExecutionContext(), applicationEntity));
        return Response.status(Response.Status.CREATED).entity(createdKey).build();
    }

    @POST
    @Path("/{apiKey}/_revoke")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.APPLICATION_SUBSCRIPTION, acls = RolePermissionAction.UPDATE) })
    public Response revokeKeySubscription(@PathParam("apiKey") String apiKey) {
        ApplicationEntity applicationEntity = applicationService.findById(GraviteeContext.getExecutionContext(), applicationId);
        ApiKeyEntity apiKeyEntity = apiKeyService.findById(GraviteeContext.getExecutionContext(), apiKey);
        if (!apiKeyEntity.getApplication().equals(applicationEntity)) {
            return Response.status(Response.Status.BAD_REQUEST).entity("'keyId' parameter does not correspond to the application").build();
        }
        apiKeyService.revoke(GraviteeContext.getExecutionContext(), apiKeyEntity, true);

        return Response.noContent().build();
    }
}
