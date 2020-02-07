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
import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.model.NewApplicationEntity;
import io.gravitee.rest.api.model.application.ApplicationSettings;
import io.gravitee.rest.api.model.application.OAuthClientSettings;
import io.gravitee.rest.api.model.application.SimpleApplicationSettings;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.portal.rest.mapper.ApplicationMapper;
import io.gravitee.rest.api.portal.rest.model.Application;
import io.gravitee.rest.api.portal.rest.model.ApplicationInput;
import io.gravitee.rest.api.portal.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.portal.rest.security.Permission;
import io.gravitee.rest.api.portal.rest.security.Permissions;
import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.notification.ApplicationHook;
import io.gravitee.rest.api.service.notification.Hook;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApplicationsResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private ApplicationService applicationService;

    @Inject
    private ApplicationMapper applicationMapper;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({
            @Permission(value = RolePermission.ENVIRONMENT_APPLICATION, acls = RolePermissionAction.CREATE),
    })
    public Response createApplication(@Valid @NotNull(message = "Input must not be null.") ApplicationInput applicationInput) {
        NewApplicationEntity newApplicationEntity = new NewApplicationEntity();
        newApplicationEntity.setDescription(applicationInput.getDescription());
        newApplicationEntity.setGroups(applicationInput.getGroups() != null ? new HashSet<>(applicationInput.getGroups()) : new HashSet<>());
        newApplicationEntity.setName(applicationInput.getName());
        newApplicationEntity.setPicture(applicationInput.getPicture());

        final io.gravitee.rest.api.portal.rest.model.ApplicationSettings settings = applicationInput.getSettings();
        ApplicationSettings newApplicationEntitySettings = new ApplicationSettings();

        if (settings == null || (settings.getApp() == null && settings.getOauth() == null)) {
            newApplicationEntity.setSettings(newApplicationEntitySettings);
        } else {
            final io.gravitee.rest.api.portal.rest.model.SimpleApplicationSettings simpleAppInput = settings.getApp();
            if (simpleAppInput != null) {
                SimpleApplicationSettings sas = new SimpleApplicationSettings();
                sas.setClientId(simpleAppInput.getClientId());
                sas.setType(simpleAppInput.getType());

                newApplicationEntitySettings.setApp(sas);
            }

            final io.gravitee.rest.api.portal.rest.model.OAuthClientSettings oauthAppInput = settings.getOauth();
            if (oauthAppInput != null) {
                OAuthClientSettings ocs = new OAuthClientSettings();
                ocs.setApplicationType(oauthAppInput.getApplicationType());
                ocs.setClientId(oauthAppInput.getClientId());
                ocs.setClientSecret(oauthAppInput.getClientSecret());
                ocs.setClientUri(oauthAppInput.getClientUri());
                ocs.setClientId(oauthAppInput.getClientId());
                ocs.setLogoUri(oauthAppInput.getLogoUri());
                ocs.setGrantTypes(oauthAppInput.getGrantTypes());
                ocs.setRedirectUris(oauthAppInput.getRedirectUris());
                ocs.setRenewClientSecretSupported(oauthAppInput.getRenewClientSecretSupported().booleanValue());
                ocs.setResponseTypes(oauthAppInput.getResponseTypes());

                newApplicationEntitySettings.setoAuthClient(ocs);
            }
        }
        newApplicationEntity.setSettings(newApplicationEntitySettings);

        ApplicationEntity createdApplicationEntity = applicationService.create(newApplicationEntity, getAuthenticatedUser());

        return Response
                .status(Response.Status.CREATED)
                .entity(applicationMapper.convert(createdApplicationEntity, uriInfo))
                .build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({
            @Permission(value = RolePermission.ENVIRONMENT_APPLICATION, acls = RolePermissionAction.READ),
            @Permission(value = RolePermission.ENVIRONMENT_APPLICATION, acls = RolePermissionAction.READ)
    })
    public Response getApplications(@BeanParam PaginationParam paginationParam,
                                    @QueryParam("forSubscription") final boolean forSubscription) {

        Stream<Application> applicationStream = applicationService.findByUser(getAuthenticatedUser())
                .stream()
                .map(application ->applicationMapper.convert(application, uriInfo))
                .map(this::addApplicationLinks);

        if (forSubscription) {
            applicationStream = applicationStream.filter((app) -> this.hasPermission(RolePermission.APPLICATION_SUBSCRIPTION, app.getId(), RolePermissionAction.CREATE));
        }

        List<Application> applicationsList = applicationStream.collect(Collectors.toList());
        return createListResponse(applicationsList, paginationParam);
    }

    private Application addApplicationLinks(Application application) {
        String basePath = uriInfo.getAbsolutePathBuilder().path(application.getId()).build().toString();
        return application.links(applicationMapper.computeApplicationLinks(basePath, application.getUpdatedAt()));
    }

    @GET
    @Path("/hooks")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getHooks() {
        return Response.ok(Arrays.stream(ApplicationHook.values()).toArray(Hook[]::new)).build();
    }

    @Path("{applicationId}")
    public ApplicationResource getApplicationResource() {
        return resourceContext.getResource(ApplicationResource.class);
    }
}
