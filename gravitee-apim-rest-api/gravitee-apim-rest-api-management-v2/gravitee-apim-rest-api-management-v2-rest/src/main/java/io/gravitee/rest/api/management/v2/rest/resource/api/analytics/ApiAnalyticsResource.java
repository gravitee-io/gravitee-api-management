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
package io.gravitee.rest.api.management.v2.rest.resource.api.analytics;

import io.gravitee.apim.core.analytics.use_case.SearchRequestsCountAnalyticsUseCase;
import io.gravitee.rest.api.management.v2.rest.mapper.ApiAnalyticsMapper;
import io.gravitee.rest.api.management.v2.rest.model.ApiAnalyticsRequestsCountResponse;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

public class ApiAnalyticsResource extends AbstractResource {

    @PathParam("apiId")
    private String apiId;

    @Inject
    private SearchRequestsCountAnalyticsUseCase searchRequestsCountAnalyticsUseCase;

    @Path("/requests-count")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_ANALYTICS, acls = { RolePermissionAction.READ }) })
    public ApiAnalyticsRequestsCountResponse getApiAnalyticsRequestCount() {
        var request = new SearchRequestsCountAnalyticsUseCase.Input(apiId, GraviteeContext.getCurrentEnvironment());

        return searchRequestsCountAnalyticsUseCase
            .execute(request)
            .requestsCount()
            .map(ApiAnalyticsMapper.INSTANCE::map)
            .orElseThrow(() -> new NotFoundException("No requests count found for api: " + apiId));
    }
}
