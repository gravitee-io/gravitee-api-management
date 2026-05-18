package io.gravitee.gamma.module.authz.rest;

import io.gravitee.gamma.module.authz.entityimport.model.ScimConnectorRequest;
import io.gravitee.gamma.module.authz.entityimport.model.ScimConnectorResponse;
import io.gravitee.gamma.module.authz.entityimport.service.NotFoundException;
import io.gravitee.gamma.module.authz.entityimport.service.ScimConnectorService;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ScimConnectorsResource {

    @Inject
    private ScimConnectorService service;

    @GET
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_AUTHORIZATION, acls = { RolePermissionAction.READ }) })
    public List<ScimConnectorResponse> list(@PathParam("envId") String envId) {
        return service.list(envId);
    }

    @GET
    @Path("/{id}")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_AUTHORIZATION, acls = { RolePermissionAction.READ }) })
    public Response get(@PathParam("envId") String envId, @PathParam("id") String id) {
        return ResponseErrors.call(() ->
            service
                .get(envId, id)
                .map(r -> Response.ok(r).build())
                .orElseThrow(() -> new NotFoundException("Connector not found: " + id))
        );
    }

    @POST
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_AUTHORIZATION, acls = { RolePermissionAction.CREATE }) })
    public Response create(@PathParam("envId") String envId, @Valid @NotNull ScimConnectorRequest request) {
        return ResponseErrors.call(() -> {
            ScimConnectorResponse created = service.create(envId, request);
            return Response.status(Response.Status.CREATED).entity(created).build();
        });
    }

    @PUT
    @Path("/{id}")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_AUTHORIZATION, acls = { RolePermissionAction.UPDATE }) })
    public Response update(@PathParam("envId") String envId, @PathParam("id") String id, @Valid @NotNull ScimConnectorRequest request) {
        return ResponseErrors.call(() -> Response.ok(service.update(envId, id, request)).build());
    }

    @DELETE
    @Path("/{id}")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_AUTHORIZATION, acls = { RolePermissionAction.DELETE }) })
    public Response delete(@PathParam("envId") String envId, @PathParam("id") String id) {
        return ResponseErrors.call(() -> {
            if (!service.delete(envId, id)) {
                throw new NotFoundException("Connector not found: " + id);
            }
            return Response.noContent().build();
        });
    }

    @POST
    @Path("/{id}/sync")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_AUTHORIZATION, acls = { RolePermissionAction.UPDATE }) })
    public Response syncNow(@PathParam("envId") String envId, @PathParam("id") String id) {
        return ResponseErrors.call(() -> Response.ok(service.syncNow(envId, id)).build());
    }
}
