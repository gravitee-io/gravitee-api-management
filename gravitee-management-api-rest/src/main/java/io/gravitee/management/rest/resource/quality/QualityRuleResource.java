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
package io.gravitee.management.rest.resource.quality;

import io.gravitee.common.http.MediaType;
import io.gravitee.management.model.permissions.RolePermission;
import io.gravitee.management.model.permissions.RolePermissionAction;
import io.gravitee.management.model.quality.QualityRuleEntity;
import io.gravitee.management.model.quality.UpdateQualityRuleEntity;
import io.gravitee.management.rest.resource.AbstractResource;
import io.gravitee.management.rest.security.Permission;
import io.gravitee.management.rest.security.Permissions;
import io.gravitee.management.service.QualityRuleService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;

import static io.gravitee.common.http.MediaType.APPLICATION_JSON;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = {"Configuration"})
public class QualityRuleResource extends AbstractResource {

    @Autowired
    private QualityRuleService qualityRuleService;

    @GET
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Get a quality rule",
            notes = "User must have the MANAGEMENT_QUALITY_RULE[READ] permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Quality rule", response = QualityRuleEntity.class),
            @ApiResponse(code = 404, message = "Quality rule not found"),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.MANAGEMENT_QUALITY_RULE, acls = RolePermissionAction.READ)
    })
    public QualityRuleEntity get(@PathParam("id") String id) {
        return qualityRuleService.findById(id);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Update a quality rule",
            notes = "User must have the MANAGEMENT_QUALITY_RULE[READ] permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Quality rule successfully updated", response = QualityRuleEntity.class),
            @ApiResponse(code = 404, message = "Quality rule not found"),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.MANAGEMENT_QUALITY_RULE, acls = RolePermissionAction.UPDATE)
    })
    public QualityRuleEntity update(@PathParam("id") String id, @Valid @NotNull final UpdateQualityRuleEntity updateQualityRuleEntity) {
        updateQualityRuleEntity.setId(id);
        return qualityRuleService.update(updateQualityRuleEntity);
    }

    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Delete a quality rule",
            notes = "User must have the MANAGEMENT_QUALITY_RULE[READ] permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 201, message = "Quality rule successfully deleted"),
            @ApiResponse(code = 404, message = "Quality rule not found"),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.MANAGEMENT_QUALITY_RULE, acls = RolePermissionAction.DELETE)
    })
    public void delete(@PathParam("id") String id) {
        qualityRuleService.delete(id);
    }
}
