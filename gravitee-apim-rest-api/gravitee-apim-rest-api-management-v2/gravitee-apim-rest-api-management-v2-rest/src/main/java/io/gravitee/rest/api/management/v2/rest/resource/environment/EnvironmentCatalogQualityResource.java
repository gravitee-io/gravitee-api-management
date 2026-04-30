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
package io.gravitee.rest.api.management.v2.rest.resource.environment;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v2.rest.model.CatalogQualityApiScore;
import io.gravitee.rest.api.management.v2.rest.model.CatalogQualityScoreRequest;
import io.gravitee.rest.api.management.v2.rest.model.CatalogQualityScoreResponse;
import io.gravitee.rest.api.management.v2.rest.model.CatalogQualitySuggestion;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.catalog.quality.CatalogQualityService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import java.util.List;

public class EnvironmentCatalogQualityResource extends AbstractResource {

    @Inject
    private CatalogQualityService catalogQualityService;

    @GET
    @Path("/apis")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_API, acls = RolePermissionAction.READ) })
    public List<CatalogQualityApiScore> getCatalogQualityScores() {
        var envId = GraviteeContext.getExecutionContext().getEnvironmentId();
        return catalogQualityService.listScores(envId).stream().map(this::toApiScore).toList();
    }

    @POST
    @Path("/apis/{apiId}/suggestions")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_API, acls = RolePermissionAction.READ) })
    public Response generateCatalogQualitySuggestions(@PathParam("apiId") String apiId) {
        var envId = GraviteeContext.getExecutionContext().getEnvironmentId();
        try {
            var row = catalogQualityService.generateSuggestions(apiId, envId);
            return Response.ok(toSuggestion(row)).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND).entity("{\"message\":\"" + e.getMessage() + "\"}").build();
        } catch (IllegalStateException e) {
            var msg = e.getMessage() != null ? e.getMessage() : "";
            if (msg.contains("Ollama") || msg.contains("chat response")) {
                return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity("{\"message\":\"" + msg + "\"}").build();
            }
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"message\":\"" + msg + "\"}").build();
        }
    }

    @POST
    @Path("/score")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_API, acls = RolePermissionAction.READ) })
    public CatalogQualityScoreResponse scoreCatalogQualityText(CatalogQualityScoreRequest request) {
        var result = catalogQualityService.score(
            request.getTitle() != null ? request.getTitle() : "",
            request.getDescription() != null ? request.getDescription() : ""
        );
        return new CatalogQualityScoreResponse()
            .titleScore(result.titleScore())
            .descriptionScore(result.descriptionScore())
            .totalScore(result.totalScore())
            .titleIssues(result.titleIssues())
            .descriptionIssues(result.descriptionIssues());
    }

    private CatalogQualityApiScore toApiScore(CatalogQualityService.CatalogQualityScoreRow row) {
        return new CatalogQualityApiScore()
            .apiId(row.apiId())
            .name(row.name())
            .description(row.description())
            .definitionVersion(row.definitionVersion())
            .apiType(row.apiType())
            .titleScore(row.titleScore())
            .descriptionScore(row.descriptionScore())
            .totalScore(row.totalScore())
            .titleIssues(row.titleIssues())
            .descriptionIssues(row.descriptionIssues());
    }

    private CatalogQualitySuggestion toSuggestion(CatalogQualityService.CatalogQualitySuggestionRow row) {
        return new CatalogQualitySuggestion()
            .apiId(row.apiId())
            .suggestedTitle(row.suggestedTitle())
            .suggestedDescription(row.suggestedDescription())
            .reasoning(row.reasoning());
    }
}
