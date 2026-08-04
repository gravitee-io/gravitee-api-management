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
package io.gravitee.gamma.rest.resources;

import io.gravitee.gamma.rest.resources.observability.GammaObservabilityResource;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;

/**
 * Root REST resource for Gamma.
 */
@Path("/")
public class GammaRootResource {

    @Context
    private ResourceContext resourceContext;

    @Path("/organizations/{orgId}/modules")
    public GammaModulesResource getModulesResourceFromOrganization() {
        return resourceContext.getResource(GammaModulesResource.class);
    }

    @Path("/organizations/{orgId}/environments/{envId}/modules")
    public GammaModulesResource getModulesResourceFromEnvironment() {
        return resourceContext.getResource(GammaModulesResource.class);
    }

    /**
     * Every resource under the shared {@code /observability/*} namespace (traces, filters, logs,
     * analytics, dashboards) — mounted outside the per-module namespace so every gamma module's UI
     * can call them. See {@link GammaObservabilityResource} for the sub-routes.
     */
    @Path("/organizations/{orgId}/environments/{envId}/observability")
    public GammaObservabilityResource getObservabilityResource() {
        return resourceContext.getResource(GammaObservabilityResource.class);
    }
}
