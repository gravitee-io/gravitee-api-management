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
package io.gravitee.gamma.rest.resources.observability.metricdefinitions;

import io.gravitee.common.http.MediaType;
import io.gravitee.gamma.rest.core.observability.catalog.exception.InvalidMetricDefinitionException;
import io.gravitee.gamma.rest.core.observability.catalog.use_case.DeclareMetricDefinitionUseCase;
import io.gravitee.gamma.rest.core.observability.catalog.use_case.ListMetricDefinitionsUseCase;
import io.gravitee.gamma.rest.resources.observability.metricdefinitions.dto.DeclareMetricDefinitionRequestDto;
import io.gravitee.gamma.rest.resources.observability.metricdefinitions.dto.MetricDefinitionDto;
import io.gravitee.gamma.rest.resources.observability.metricdefinitions.dto.MetricDefinitionsResponseDto;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

/**
 * The environment's additional metrics, mounted under
 * {@code /gamma/organizations/{orgId}/environments/{envId}/observability/metric-definitions} by {@code GammaRootResource}.
 *
 * <h2>Endpoints</h2>
 * <ul>
 *   <li>{@code GET /} — every additional metric of the environment, built-in, declared and discovered.</li>
 *   <li>{@code POST /} — declares a key, named in the body.</li>
 *   <li>{@code GET|PUT|DELETE /record?name=} — see {@link MetricDefinitionResource}.</li>
 * </ul>
 *
 * <p>A key is never a path segment: keys contain {@code /}, and the servlet container refuses an encoded {@code %2F}.
 *
 * <h2>Authorization</h2>
 * Reads need a login only: built-in entries and records hold no traffic data, and discovered entries and
 * {@code lastSeen} are served only to a caller who reads the environment's traffic (see
 * {@code TrafficReadPermissionPort}). Writes need {@code ENVIRONMENT_SETTINGS[UPDATE]}.
 *
 * @author GraviteeSource Team
 */
@Produces(MediaType.APPLICATION_JSON)
public class MetricDefinitionsResource {

    @Inject
    private ListMetricDefinitionsUseCase listUseCase;

    @Inject
    private DeclareMetricDefinitionUseCase declareUseCase;

    @Context
    private ResourceContext resourceContext;

    @GET
    public MetricDefinitionsResponseDto list() {
        var ctx = GraviteeContext.getExecutionContext();
        var output = listUseCase.execute(new ListMetricDefinitionsUseCase.Input(ctx.getOrganizationId(), ctx.getEnvironmentId()));
        return MetricDefinitionsResponseDto.from(output.catalog());
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_SETTINGS, acls = { RolePermissionAction.UPDATE }) })
    public Response declare(@Context UriInfo uriInfo, DeclareMetricDefinitionRequestDto request) {
        if (request == null) {
            throw InvalidMetricDefinitionException.missingBody();
        }
        var output = declareUseCase.execute(
            new DeclareMetricDefinitionUseCase.Input(
                GraviteeContext.getExecutionContext().getEnvironmentId(),
                request.name(),
                request.label(),
                request.description(),
                request.unit(),
                request.exposed()
            )
        );
        var location = uriInfo
            .getAbsolutePathBuilder()
            .path(MetricDefinitionResource.PATH)
            .queryParam("name", "{name}")
            .build(request.name());
        return Response.created(location).entity(MetricDefinitionDto.from(output.definition())).build();
    }

    @Path(MetricDefinitionResource.PATH)
    public MetricDefinitionResource getMetricDefinitionResource() {
        return resourceContext.getResource(MetricDefinitionResource.class);
    }
}
