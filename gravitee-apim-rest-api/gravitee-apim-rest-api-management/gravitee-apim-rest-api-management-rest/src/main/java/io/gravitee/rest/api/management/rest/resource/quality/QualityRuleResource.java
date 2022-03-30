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
package io.gravitee.rest.api.management.rest.resource.quality;

import static io.gravitee.common.http.MediaType.APPLICATION_JSON;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.rest.resource.AbstractResource;
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.quality.QualityRuleEntity;
import io.gravitee.rest.api.model.quality.UpdateQualityRuleEntity;
import io.gravitee.rest.api.service.QualityRuleService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Tag(name = "Configuration")
public class QualityRuleResource extends AbstractResource {

    @Inject
    private QualityRuleService qualityRuleService;

    @PathParam("id")
    @Parameter(name = "id", required = true)
    private String id;

    @GET
    @Produces(APPLICATION_JSON)
    @Operation(
        summary = "Get a quality rule",
        description = "User must have the MANAGEMENT_QUALITY_RULE[READ] permission to use this service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Quality rule",
        content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = QualityRuleEntity.class))
    )
    @ApiResponse(responseCode = "404", description = "Quality rule not found")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_QUALITY_RULE, acls = RolePermissionAction.READ) })
    public QualityRuleEntity getQualityRule() {
        return qualityRuleService.findById(id);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Update a quality rule",
        description = "User must have the MANAGEMENT_QUALITY_RULE[READ] permission to use this service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Quality rule successfully updated",
        content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = QualityRuleEntity.class))
    )
    @ApiResponse(responseCode = "404", description = "Quality rule not found")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_QUALITY_RULE, acls = RolePermissionAction.UPDATE) })
    public QualityRuleEntity updateQualityRule(@Valid @NotNull final UpdateQualityRuleEntity updateQualityRuleEntity) {
        updateQualityRuleEntity.setId(id);
        return qualityRuleService.update(GraviteeContext.getExecutionContext(), updateQualityRuleEntity);
    }

    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Delete a quality rule",
        description = "User must have the MANAGEMENT_QUALITY_RULE[READ] permission to use this service"
    )
    @ApiResponse(responseCode = "201", description = "Quality rule successfully deleted")
    @ApiResponse(responseCode = "404", description = "Quality rule not found")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_QUALITY_RULE, acls = RolePermissionAction.DELETE) })
    public void deleteQualityRule() {
        qualityRuleService.delete(GraviteeContext.getExecutionContext(), id);
    }
}
