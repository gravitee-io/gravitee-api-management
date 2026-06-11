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

import io.gravitee.gamma.rest.resources.observability.filters.ObservabilityFiltersResource;
import io.gravitee.gamma.rest.resources.observability.logs.LogsResource;
import io.gravitee.gamma.rest.resources.tracing.TracingResource;
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
     * Global trace explorer mounted outside the per-module namespace so every gamma module's UI can call
     * it with its own {@code module} query parameter. See {@link TracingResource} for the contract.
     */
    @Path("/organizations/{orgId}/environments/{envId}/observability/traces")
    public TracingResource getTracingResource() {
        return resourceContext.getResource(TracingResource.class);
    }

    /**
     * Unified observability filter catalog (definition / values / resolve) shared by every gamma
     * module's logs and analytics UI. Mounted outside the per-module namespace; relevance is carried
     * by the {@code signal} / {@code apiType} discovery axes rather than a module perimeter. See
     * {@link ObservabilityFiltersResource} for the contract.
     */
    @Path("/organizations/{orgId}/environments/{envId}/observability/filters")
    public ObservabilityFiltersResource getObservabilityFiltersResource() {
        return resourceContext.getResource(ObservabilityFiltersResource.class);
    }

    /**
     * Environment-wide logs search (light rows, v4-only). Server-side RBAC scoping — no mandatory
     * {@code apiId}. See {@link LogsResource} for the contract.
     */
    @Path("/organizations/{orgId}/environments/{envId}/observability/logs")
    public LogsResource getLogsResource() {
        return resourceContext.getResource(LogsResource.class);
    }
}
