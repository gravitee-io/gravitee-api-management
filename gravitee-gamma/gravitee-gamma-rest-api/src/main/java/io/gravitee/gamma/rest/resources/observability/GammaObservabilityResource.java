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
package io.gravitee.gamma.rest.resources.observability;

import io.gravitee.gamma.rest.resources.observability.analytics.AnalyticsResource;
import io.gravitee.gamma.rest.resources.observability.dashboards.ObservabilityDashboardsResource;
import io.gravitee.gamma.rest.resources.observability.filters.ObservabilityFiltersResource;
import io.gravitee.gamma.rest.resources.observability.logs.LogsResource;
import io.gravitee.gamma.rest.resources.tracing.TracingResource;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;

/**
 * Groups every resource mounted under the shared {@code /observability/*} namespace (traces,
 * filters, logs, analytics, dashboards), so {@code GammaRootResource} only needs a single locator
 * for the whole Observability perimeter instead of one per sub-domain. Mirrors, at the REST layer,
 * the {@code GammaObservabilityConfiguration} aggregator introduced for Spring wiring.
 *
 * <p>Purely a routing layer — no behavior of its own, each sub-resource is unchanged and still
 * individually registered in {@code GammaModuleApplication}.
 *
 * @author GraviteeSource Team
 */
@Path("/")
public class GammaObservabilityResource {

    @Context
    private ResourceContext resourceContext;

    /**
     * Global trace explorer. See {@link TracingResource} for the contract.
     */
    @Path("/traces")
    public TracingResource getTracingResource() {
        return resourceContext.getResource(TracingResource.class);
    }

    /**
     * Unified observability filter catalog (definition / values / resolve). See
     * {@link ObservabilityFiltersResource} for the contract.
     */
    @Path("/filters")
    public ObservabilityFiltersResource getObservabilityFiltersResource() {
        return resourceContext.getResource(ObservabilityFiltersResource.class);
    }

    /**
     * Environment-wide logs search. See {@link LogsResource} for the contract.
     */
    @Path("/logs")
    public LogsResource getLogsResource() {
        return resourceContext.getResource(LogsResource.class);
    }

    /**
     * Analytics computation endpoints. See {@link AnalyticsResource} for the contract.
     */
    @Path("/analytics")
    public AnalyticsResource getAnalyticsResource() {
        return resourceContext.getResource(AnalyticsResource.class);
    }

    /**
     * Saved-dashboard read endpoints. See {@link ObservabilityDashboardsResource} for the contract.
     */
    @Path("/dashboards")
    public ObservabilityDashboardsResource getObservabilityDashboardsResource() {
        return resourceContext.getResource(ObservabilityDashboardsResource.class);
    }
}
