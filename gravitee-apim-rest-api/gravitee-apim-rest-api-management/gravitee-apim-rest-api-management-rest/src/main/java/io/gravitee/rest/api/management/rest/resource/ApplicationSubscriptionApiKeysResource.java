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
package io.gravitee.rest.api.management.rest.resource;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.model.ApiKeyEntity;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.ApiKeyService;
import io.gravitee.rest.api.service.SubscriptionService;
import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.SubscriptionService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ApplicationNotFoundException;
import io.gravitee.rest.api.service.exceptions.InvalidApplicationApiKeyModeException;
import io.swagger.annotations.*;
import java.net.URI;
import java.util.List;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

/**
 * @author GraviteeSource Team
 */
@Api(tags = { "API Keys" })
public class ApplicationSubscriptionApiKeysResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private ApiKeyService apiKeyService;

    @Inject
    private SubscriptionService subscriptionService;

    @Inject
    private ApplicationService applicationService;

    @SuppressWarnings("UnresolvedRestParam")
    @PathParam("application")
    @ApiParam(name = "application", hidden = true)
    private String application;

    @SuppressWarnings("UnresolvedRestParam")
    @PathParam("subscription")
    @ApiParam(name = "subscription", hidden = true)
    private String subscription;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "List all API Keys for a subscription", notes = "User must have the READ permission to use this service")
    @ApiResponses(
        {
            @ApiResponse(
                code = 200,
                message = "List of API Keys for a subscription",
                response = ApiKeyEntity.class,
                responseContainer = "Set"
            ),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    @Permissions({ @Permission(value = RolePermission.APPLICATION_SUBSCRIPTION, acls = RolePermissionAction.READ) })
    public List<ApiKeyEntity> getApiKeysForApplicationSubscription() {
        return apiKeyService.findBySubscription(subscription);
    }

    @POST
    @Path("/_renew")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Renew an API key", notes = "User must have the MANAGE_API_KEYS permission to use this service")
    @ApiResponses(
        {
            @ApiResponse(code = 201, message = "A new API Key", response = ApiKeyEntity.class),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    @Permissions({ @Permission(value = RolePermission.APPLICATION_SUBSCRIPTION, acls = RolePermissionAction.UPDATE) })
    public Response renewApiKeyForApplicationSubscription() {
        checkApplicationApiKeyModeAllowed(application);
        SubscriptionEntity subscriptionEntity = subscriptionService.findById(subscription);
        ApiKeyEntity apiKeyEntity = apiKeyService.renew(subscriptionEntity);
        URI location = URI.create(uriInfo.getPath().replace("_renew", apiKeyEntity.getId()));
        return Response.created(location).entity(apiKeyEntity).build();
    }

    @Path("{apikey}")
    public ApplicationSubscriptionApiKeyResource getApplicationSubscriptionApiKeyResource() {
        checkApplicationApiKeyModeAllowed(application);
        return resourceContext.getResource(ApplicationSubscriptionApiKeyResource.class);
    }

    private void checkApplicationApiKeyModeAllowed(String applicationId) {
        ApplicationEntity applicationEntity = applicationService.findById(GraviteeContext.getCurrentEnvironment(), applicationId);
        if (applicationEntity == null) {
            throw new ApplicationNotFoundException(applicationId);
        }
        if (applicationEntity.hasApiKeySharedMode()) {
            throw new InvalidApplicationApiKeyModeException("Can't access API key by application subscription cause it's a shared API Key");
        }
    }
}
