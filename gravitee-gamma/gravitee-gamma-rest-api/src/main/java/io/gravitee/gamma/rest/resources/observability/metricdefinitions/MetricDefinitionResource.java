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
import io.gravitee.gamma.rest.core.observability.catalog.use_case.DeleteMetricDefinitionUseCase;
import io.gravitee.gamma.rest.core.observability.catalog.use_case.GetMetricDefinitionUseCase;
import io.gravitee.gamma.rest.core.observability.catalog.use_case.SaveMetricDefinitionUseCase;
import io.gravitee.gamma.rest.resources.observability.metricdefinitions.dto.MetricDefinitionDto;
import io.gravitee.gamma.rest.resources.observability.metricdefinitions.dto.SaveMetricDefinitionRequestDto;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

/**
 * The record on one key, named by the {@code name} query parameter: the field's wire name,
 * {@code additional-metrics.<rawKey>}. A record of another environment answers 404 on every verb.
 *
 * <p>PUT writes the record whether or not one exists (201 when it creates it): editing a discovered key is how it
 * becomes declared. Every write needs the same right, so an upsert grants nothing a separate create would not.
 *
 * @author GraviteeSource Team
 */
@Produces(MediaType.APPLICATION_JSON)
public class MetricDefinitionResource {

    static final String PATH = "/record";

    @Inject
    private GetMetricDefinitionUseCase getUseCase;

    @Inject
    private SaveMetricDefinitionUseCase saveUseCase;

    @Inject
    private DeleteMetricDefinitionUseCase deleteUseCase;

    @GET
    public MetricDefinitionDto get(@QueryParam("name") String name) {
        var output = getUseCase.execute(new GetMetricDefinitionUseCase.Input(environmentId(), name));
        return MetricDefinitionDto.from(output.definition());
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_SETTINGS, acls = { RolePermissionAction.UPDATE }) })
    public Response save(@QueryParam("name") String name, SaveMetricDefinitionRequestDto request) {
        if (request == null) {
            throw InvalidMetricDefinitionException.missingBody();
        }
        var output = saveUseCase.execute(
            new SaveMetricDefinitionUseCase.Input(
                environmentId(),
                name,
                request.label(),
                request.description(),
                request.unit(),
                request.exposed()
            )
        );
        return Response.status(output.created() ? Response.Status.CREATED : Response.Status.OK)
            .entity(MetricDefinitionDto.from(output.definition()))
            .build();
    }

    @DELETE
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_SETTINGS, acls = { RolePermissionAction.UPDATE }) })
    public Response delete(@QueryParam("name") String name) {
        deleteUseCase.execute(new DeleteMetricDefinitionUseCase.Input(environmentId(), name));
        return Response.noContent().build();
    }

    private static String environmentId() {
        return GraviteeContext.getExecutionContext().getEnvironmentId();
    }
}
