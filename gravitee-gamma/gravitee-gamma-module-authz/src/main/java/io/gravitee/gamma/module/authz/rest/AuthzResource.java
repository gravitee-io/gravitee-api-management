package io.gravitee.gamma.module.authz.rest;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthzResource {

    @Context
    private ResourceContext resourceContext;

    @Path("/environments/{envId}/scim-connectors")
    public ScimConnectorsResource scimConnectors() {
        return resourceContext.getResource(ScimConnectorsResource.class);
    }
}
