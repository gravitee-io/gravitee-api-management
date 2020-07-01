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
package io.gravitee.management.rest.resource;

import io.gravitee.common.http.MediaType;
import io.gravitee.management.model.permissions.RolePermission;
import io.gravitee.management.model.permissions.RolePermissionAction;
import io.gravitee.management.model.quality.ApiQualityRuleEntity;
import io.gravitee.management.model.quality.NewApiQualityRuleEntity;
import io.gravitee.management.model.quality.UpdateApiQualityRuleEntity;
import io.gravitee.management.rest.security.Permission;
import io.gravitee.management.rest.security.Permissions;
import io.gravitee.management.service.ApiQualityRuleService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import java.util.List;

import static io.gravitee.management.model.permissions.RolePermission.API_QUALITY_RULE;
import static io.gravitee.management.model.permissions.RolePermissionAction.READ;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = {"API Quality"})
public class ApiQualityRulesResource extends AbstractResource {

    @Autowired
    private ApiQualityRuleService apiQualityRuleService;

    @GET
    @ApiOperation(value = "List quality rules for an API",
            notes = "User must have the API_QUALITY_RULE[READ] permission to use this service")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({
            @Permission(value = API_QUALITY_RULE, acls = READ)
    })
    public List<ApiQualityRuleEntity> list(@PathParam("api") String api) {
        return apiQualityRuleService.findByApi(api);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Create a new quality rules for an API",
            notes = "User must have the API_QUALITY_RULE[CREATE] permission to use this service")
    @Permissions({
            @Permission(value = RolePermission.API_QUALITY_RULE, acls = RolePermissionAction.CREATE)
    })
    public ApiQualityRuleEntity create(@PathParam("api") String api, @Valid @NotNull final NewApiQualityRuleEntity apiQualityRuleEntity) {
        apiQualityRuleEntity.setApi(api);
        return apiQualityRuleService.create(apiQualityRuleEntity);
    }

    @Path("{qualityRule}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Update an existing quality rules for an API",
            notes = "User must have the API_QUALITY_RULE[UPDATE] permission to use this service")
    @Permissions({
            @Permission(value = RolePermission.API_QUALITY_RULE, acls = RolePermissionAction.UPDATE)
    })
    public ApiQualityRuleEntity update(@PathParam("api") String api, @PathParam("qualityRule") String qualityRule, @Valid @NotNull final UpdateApiQualityRuleEntity apiQualityRuleEntity) {
        apiQualityRuleEntity.setApi(api);
        apiQualityRuleEntity.setQualityRule(qualityRule);
        return apiQualityRuleService.update(apiQualityRuleEntity);
    }
}
