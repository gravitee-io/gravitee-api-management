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

import javax.ws.rs.Path;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;

import io.gravitee.rest.api.management.rest.resource.auth.OAuth2AuthenticationResource;
import io.gravitee.rest.api.management.rest.resource.search.SearchResource;
import io.swagger.annotations.Api;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Path("/{envId}")
@Api
public class EnvironmentResource extends AbstractResource {
    
    @Context
    private ResourceContext resourceContext;
    
    @Path("alerts")
    public AlertsResource getAlertsResource() {
        return resourceContext.getResource(AlertsResource.class);
    }
    
    @Path("apis")
    public ApisResource getApisResource() {
        return resourceContext.getResource(ApisResource.class);
    }
    
    @Path("applications")
    public ApplicationsResource getApplicationsResource() {
        return resourceContext.getResource(ApplicationsResource.class);
    }
    
    @Path("configuration")
    public ConfigurationResource getConfigurationResource() {
        return resourceContext.getResource(ConfigurationResource.class);
    }
    
    @Path("user")
    public CurrentUserResource getCurrentUserResource() {
        return resourceContext.getResource(CurrentUserResource.class);
    }
    
    @Path("subscriptions")
    public SubscriptionsResource getSubscriptionsResource() {
        return resourceContext.getResource(SubscriptionsResource.class);
    }
    
    @Path("audit")
    public AuditResource getAuditResource() {
        return resourceContext.getResource(AuditResource.class);
    }
    
    @Path("portal")
    public PortalResource getPortalResource() {
        return resourceContext.getResource(PortalResource.class);
    }
    
    // Dynamic authentication provider endpoints
    @Path("auth/oauth2/{identity}")
    public OAuth2AuthenticationResource getOAuth2AuthenticationResource() {
        return resourceContext.getResource(OAuth2AuthenticationResource.class);
    }
    
    @Path("users")
    public UsersResource getUsersResource() {
        return resourceContext.getResource(UsersResource.class);
    }
    
    @Path("search")
    public SearchResource getSearchResource() {
        return resourceContext.getResource(SearchResource.class);
    }
    
    @Path("fetchers")
    public FetchersResource getFetchersResource() {
        return resourceContext.getResource(FetchersResource.class);
    }
    
    @Path("policies")
    public PoliciesResource getPoliciesResource() {
        return resourceContext.getResource(PoliciesResource.class);
    }
    
    @Path("resources")
    public ResourcesResource getResourcesResource() {
        return resourceContext.getResource(ResourcesResource.class);
    }
    
    @Path("services-discovery")
    public ServicesDiscoveryResource getServicesDiscoveryResource() {
        return resourceContext.getResource(ServicesDiscoveryResource.class);
    }
    
    @Path("instances")
    public InstancesResource getInstancesResource() {
        return resourceContext.getResource(InstancesResource.class);
    }
    
    @Path("platform")
    public PlatformResource getPlatformResource() {
        return resourceContext.getResource(PlatformResource.class);
    }
    
    @Path("messages")
    public MessagesResource getMessagesResource() {
        return resourceContext.getResource(MessagesResource.class);
    }
    
    @Path("tickets")
    public PlatformTicketsResource getPlatformTicketsResource() {
        return resourceContext.getResource(PlatformTicketsResource.class);
    }
    
    @Path("entrypoints")
    public PortalEntryPointsResource getPortalEntryPointsResource() {
        return resourceContext.getResource(PortalEntryPointsResource.class);
    }
}
