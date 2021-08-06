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
package io.gravitee.rest.api.management.rest.resource;

import static io.gravitee.rest.api.model.permissions.RolePermission.API_QUALITY_RULE;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.READ;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.quality.ApiQualityRuleEntity;
import io.gravitee.rest.api.model.quality.NewApiQualityRuleEntity;
import io.gravitee.rest.api.model.quality.UpdateApiQualityRuleEntity;
import io.gravitee.rest.api.service.ApiQualityRuleService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import java.util.List;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = { "API Quality" })
public class ApiQualityRulesResource extends AbstractResource {

    @Inject
    private ApiQualityRuleService apiQualityRuleService;

    @SuppressWarnings("UnresolvedRestParam")
    @PathParam("api")
    @ApiParam(name = "api", hidden = true)
    private String api;

    @GET
    @ApiOperation(
        value = "List quality rules for an API",
        notes = "User must have the API_QUALITY_RULE[READ] permission to use this service"
    )
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = API_QUALITY_RULE, acls = READ) })
    public List<ApiQualityRuleEntity> getApiQualityRules() {
        return apiQualityRuleService.findByApi(api);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
        value = "Create a new quality rules for an API",
        notes = "User must have the API_QUALITY_RULE[CREATE] permission to use this service"
    )
    @Permissions({ @Permission(value = RolePermission.API_QUALITY_RULE, acls = RolePermissionAction.CREATE) })
    public ApiQualityRuleEntity createApiQualityRule(@Valid @NotNull final NewApiQualityRuleEntity apiQualityRuleEntity) {
        apiQualityRuleEntity.setApi(api);
        return apiQualityRuleService.create(apiQualityRuleEntity);
    }

    @Path("{qualityRule}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
        value = "Update an existing quality rules for an API",
        notes = "User must have the API_QUALITY_RULE[UPDATE] permission to use this service"
    )
    @Permissions({ @Permission(value = RolePermission.API_QUALITY_RULE, acls = RolePermissionAction.UPDATE) })
    public ApiQualityRuleEntity updateApiQualityRule(
        @PathParam("qualityRule") String qualityRule,
        @Valid @NotNull final UpdateApiQualityRuleEntity apiQualityRuleEntity
    ) {
        apiQualityRuleEntity.setApi(api);
        apiQualityRuleEntity.setQualityRule(qualityRule);
        return apiQualityRuleService.update(apiQualityRuleEntity);
    }
}
