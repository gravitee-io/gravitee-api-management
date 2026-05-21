package io.gravitee.gamma.module.authz.rest;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

/**
 * JAX-RS root for the authz Gamma plugin.
 *
 * <p>The policy-management UI consumes the canonical {@code /gamma/authz}
 * endpoints exposed by gravitee-gamma-authorization-rest, so this resource
 * intentionally has no subresource locators yet. Follow-up PRs in the stack
 * (entity, schema, actions, scim) introduce locators here.
 */
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthzResource {

    @Context
    private ResourceContext resourceContext;
}
