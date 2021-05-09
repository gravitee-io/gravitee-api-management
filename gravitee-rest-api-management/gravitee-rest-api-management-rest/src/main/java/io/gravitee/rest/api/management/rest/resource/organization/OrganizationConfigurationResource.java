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
package io.gravitee.rest.api.management.rest.resource.organization;

import io.gravitee.rest.api.management.rest.resource.EntrypointsResource;
import io.gravitee.rest.api.management.rest.resource.FlowsResource;
import io.gravitee.rest.api.management.rest.resource.TagsResource;
import io.gravitee.rest.api.management.rest.resource.TenantsResource;
import io.gravitee.rest.api.management.rest.resource.configuration.identity.IdentityProvidersResource;
import io.swagger.annotations.Api;
import javax.ws.rs.Path;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = { "Configuration" })
public class OrganizationConfigurationResource {

    @Context
    private ResourceContext resourceContext;

    @Path("rolescopes")
    public RoleScopesResource getRoleScopesResource() {
        return resourceContext.getResource(RoleScopesResource.class);
    }

    @Path("identities")
    public IdentityProvidersResource getAuthenticationProvidersResource() {
        return resourceContext.getResource(IdentityProvidersResource.class);
    }

    @Path("custom-user-fields")
    public CustomUserFieldsResource getCustomUserFields() {
        return resourceContext.getResource(CustomUserFieldsResource.class);
    }

    @Path("notification-templates")
    public NotificationTemplatesResource getNotificationTemplatesResource() {
        return resourceContext.getResource(NotificationTemplatesResource.class);
    }

    @Path("tags")
    public TagsResource getTagResource() {
        return resourceContext.getResource(TagsResource.class);
    }

    @Path("tenants")
    public TenantsResource getTenantsResource() {
        return resourceContext.getResource(TenantsResource.class);
    }

    @Path("entrypoints")
    public EntrypointsResource getEntryPointsResource() {
        return resourceContext.getResource(EntrypointsResource.class);
    }

    @Path("flows")
    public FlowsResource getFlowsResource() {
        return resourceContext.getResource(FlowsResource.class);
    }
}
