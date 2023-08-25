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
package io.gravitee.rest.api.management.v2.rest.resource.api;

import io.gravitee.rest.api.management.v2.rest.model.ApiLog;
import io.gravitee.rest.api.management.v2.rest.model.ApiLogsResponse;
import io.gravitee.rest.api.management.v2.rest.model.BasePlan;
import io.gravitee.rest.api.management.v2.rest.pagination.PaginationInfo;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.management.v2.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.management.v2.rest.security.Permission;
import io.gravitee.rest.api.management.v2.rest.security.Permissions;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

@Path("/environments/{envId}/apis/{apiId}/logs")
public class ApiLogsResource extends AbstractResource {

    @PathParam("apiId")
    private String apiId;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_LOG, acls = { RolePermissionAction.READ }) })
    public ApiLogsResponse getApiLogs(@BeanParam @Valid PaginationParam paginationParam) {
        return ApiLogsResponse
            .builder()
            .data(
                List.of(
                    ApiLog
                        .builder()
                        .id("log-id")
                        .application(null)
                        .plan(BasePlan.builder().id("id-keyless").name("Keyless").apiId(apiId).build())
                        .status(200)
                        .clientIdentifier("client-id")
                        .requestEnded(true)
                        .requestId("request-id")
                        .transactionId("transaction-id")
                        .timestamp(Instant.parse("2020-01-01T00:00:00.00Z").atOffset(ZoneOffset.UTC))
                        .build()
                )
            )
            .pagination(PaginationInfo.computePaginationInfo(1L, 1, paginationParam))
            .links(computePaginationLinks(1, paginationParam))
            .build();
    }
}
