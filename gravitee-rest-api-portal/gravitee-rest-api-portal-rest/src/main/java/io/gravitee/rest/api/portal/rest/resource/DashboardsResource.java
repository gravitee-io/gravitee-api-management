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
package io.gravitee.rest.api.portal.rest.resource;

import static io.gravitee.repository.management.model.DashboardReferenceType.APPLICATION;
import static java.util.stream.Collectors.toList;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.DashboardEntity;
import io.gravitee.rest.api.portal.rest.mapper.DashboardMapper;
import io.gravitee.rest.api.portal.rest.model.Dashboard;
import io.gravitee.rest.api.service.DashboardService;
import java.util.List;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DashboardsResource extends AbstractResource {

    @Inject
    private DashboardService dashboardService;

    @Inject
    private DashboardMapper dashboardMapper;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response list() {
        final List<Dashboard> dashboards = dashboardService
            .findByReferenceType(APPLICATION)
            .stream()
            .filter(DashboardEntity::isEnabled)
            .map(dashboardMapper::convert)
            .collect(toList());
        return Response.ok(dashboards).build();
    }
}
