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
package io.gravitee.rest.api.portal.rest.resource.analytics;

import io.gravitee.rest.api.portal.rest.resource.AbstractResource;
import io.gravitee.rest.api.service.ParameterService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;

/**
 * Entry point for {@code /portal/environments/{envId}/analytics/**} endpoints.
 *
 * <p>Acts as the single gate for the Next Gen Portal analytics feature: before routing to any sub-resource,
 * {@link PortalAnalyticsGate#requireAnalyticsEnabled} is invoked so every analytics endpoint (dashboards today,
 * computation tomorrow) inherits the {@code PORTAL_NEXT_ANALYTICS_ENABLED} check without repeating it.</p>
 *
 * @author GraviteeSource Team
 */
public class PortalAnalyticsResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private ParameterService parameterService;

    @Path("dashboards")
    public PortalAnalyticsDashboardsResource getDashboardsResource() {
        PortalAnalyticsGate.requireAnalyticsEnabled(parameterService);
        return resourceContext.getResource(PortalAnalyticsDashboardsResource.class);
    }
}
