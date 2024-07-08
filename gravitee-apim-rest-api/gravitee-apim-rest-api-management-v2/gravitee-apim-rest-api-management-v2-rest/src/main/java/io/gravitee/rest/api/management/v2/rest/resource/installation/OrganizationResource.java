/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.management.v2.rest.resource.installation;

import io.gravitee.common.http.MediaType;
import io.gravitee.node.api.license.License;
import io.gravitee.node.api.license.LicenseManager;
import io.gravitee.rest.api.management.v2.rest.mapper.GraviteeLicenseMapper;
import io.gravitee.rest.api.management.v2.rest.mapper.OrganizationMapper;
import io.gravitee.rest.api.management.v2.rest.model.GraviteeLicense;
import io.gravitee.rest.api.management.v2.rest.model.Organization;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.management.v2.rest.resource.plugin.ApiServicesResource;
import io.gravitee.rest.api.management.v2.rest.resource.plugin.EndpointsResource;
import io.gravitee.rest.api.management.v2.rest.resource.plugin.EntrypointsResource;
import io.gravitee.rest.api.management.v2.rest.resource.plugin.PoliciesResource;
import io.gravitee.rest.api.management.v2.rest.resource.plugin.ResourcesResource;
import io.gravitee.rest.api.management.v2.rest.resource.ui.ManagementUIResource;
import io.gravitee.rest.api.model.v4.license.GraviteeLicenseEntity;
import io.gravitee.rest.api.service.OrganizationService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import java.util.Objects;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Path("/organizations/{orgId}")
public class OrganizationResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private OrganizationService organizationService;

    @Inject
    private LicenseManager licenseManager;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Organization getOrganizationById(@PathParam("orgId") String orgId) {
        return OrganizationMapper.INSTANCE.map(organizationService.findById(orgId));
    }

    @GET
    @Path("/license")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public GraviteeLicense getOrganizationLicense(@PathParam("orgId") String orgId) {
        // Throw error if organization does not exist
        organizationService.findById(orgId);

        final License license = licenseManager.getOrganizationLicenseOrPlatform(orgId);

        return GraviteeLicenseMapper.INSTANCE.map(
            GraviteeLicenseEntity
                .builder()
                .tier(license.getTier())
                .packs(license.getPacks())
                .features(license.getFeatures())
                .expiresAt(license.getExpirationDate())
                .isExpired(license.isExpired())
                .scope(license.getReferenceType())
                .build()
        );
    }

    @Path("/environments")
    public EnvironmentsResource getEnvironmentsResource() {
        return resourceContext.getResource(EnvironmentsResource.class);
    }

    @Path("/plugins/endpoints")
    public EndpointsResource getEndpointsResource() {
        return resourceContext.getResource(EndpointsResource.class);
    }

    @Path("/plugins/entrypoints")
    public EntrypointsResource getOrganizationEntrypointsResource() {
        return resourceContext.getResource(EntrypointsResource.class);
    }

    @Path("/plugins/api-services")
    public ApiServicesResource getApiServicesResource() {
        return resourceContext.getResource(ApiServicesResource.class);
    }

    @Path("/plugins/policies")
    public PoliciesResource getOrganizationPoliciesResource() {
        return resourceContext.getResource(PoliciesResource.class);
    }

    @Path("/plugins/resources")
    public ResourcesResource getOrganizationResourcesResource() {
        return resourceContext.getResource(ResourcesResource.class);
    }

    @Path("ui")
    public ManagementUIResource getManagementUIResource() {
        return resourceContext.getResource(ManagementUIResource.class);
    }
}
