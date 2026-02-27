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
package io.gravitee.rest.api.management.v2.rest.resource.logs;

import io.gravitee.apim.core.logs_engine.use_case.GetLogsFilterDefinitionsUseCase;
import io.gravitee.rest.api.management.v2.rest.mapper.LogsDefinitionMapper;
import io.gravitee.rest.api.management.v2.rest.model.logs.engine.LogsFilterSpecsResponse;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * @author GraviteeSource Team
 */
public class LogsDefinitionResource {

    @Inject
    GetLogsFilterDefinitionsUseCase getLogsFilterDefinitions;

    @Path("/filters")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_ANALYTICS, acls = { RolePermissionAction.READ }) })
    public LogsFilterSpecsResponse getFilterDefinitions() {
        return LogsDefinitionMapper.INSTANCE.toFilterSpecsResponse(getLogsFilterDefinitions.execute().specs());
    }
}
