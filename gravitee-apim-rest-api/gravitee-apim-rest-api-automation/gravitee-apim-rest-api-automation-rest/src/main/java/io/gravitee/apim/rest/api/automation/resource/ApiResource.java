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
package io.gravitee.apim.rest.api.automation.resource;

import io.gravitee.apim.core.api.model.crd.ApiCRDSpec;
import io.gravitee.apim.core.api.query_service.ApiQueryService;
import io.gravitee.apim.core.api.use_case.ExportApiCRDUseCase;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.rest.api.automation.exception.HRIDNotFoundException;
import io.gravitee.apim.rest.api.automation.mapper.ApiMapper;
import io.gravitee.apim.rest.api.automation.model.ApiV4Spec;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.idp.api.authentication.UserDetails;
import io.gravitee.rest.api.management.v2.rest.mapper.ApiCRDMapper;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.IdBuilder;
import io.gravitee.rest.api.service.exceptions.ApiNotFoundException;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiResource extends AbstractResource {

    @Inject
    private ExportApiCRDUseCase exportApiCRDUseCase;

    @Inject
    protected io.gravitee.rest.api.service.v4.ApiService apiServiceV4;

    @PathParam("hrid")
    private String hrid;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_SHARED_POLICY_GROUP, acls = { RolePermissionAction.CREATE }) })
    public Response getApiByHRID() {
        var executionContext = GraviteeContext.getExecutionContext();
        var userDetails = getAuthenticatedUserDetails();

        var input = new ExportApiCRDUseCase.Input(
            IdBuilder.builder(executionContext, hrid).buildId(),
            buildAuditInfo(executionContext, userDetails)
        );

        try {
            ApiCRDSpec apiCRDSpec = exportApiCRDUseCase.execute(input).spec();
            ApiV4Spec apiV4Spec = ApiMapper.INSTANCE.apiCRDSpecToApiV4Spec(ApiCRDMapper.INSTANCE.map(apiCRDSpec));
            return Response
                .ok(
                    ApiMapper.INSTANCE.apiV4SpecToApiV4State(
                        apiV4Spec,
                        apiCRDSpec.getId(),
                        apiCRDSpec.getCrossId(),
                        executionContext.getOrganizationId(),
                        executionContext.getEnvironmentId()
                    )
                )
                .build();
        } catch (ApiNotFoundException e) {
            throw new HRIDNotFoundException(hrid);
        }
    }

    @DELETE
    @Permissions({ @Permission(value = RolePermission.API_DEFINITION, acls = RolePermissionAction.DELETE) })
    public Response deleteApi() {
        var executionContext = GraviteeContext.getExecutionContext();

        try {
            apiServiceV4.delete(GraviteeContext.getExecutionContext(), IdBuilder.builder(executionContext, hrid).buildId(), true);
        } catch (ApiNotFoundException e) {
            throw new HRIDNotFoundException(hrid);
        }
        return Response.noContent().build();
    }

    private static AuditInfo buildAuditInfo(ExecutionContext executionContext, UserDetails userDetails) {
        return AuditInfo
            .builder()
            .organizationId(executionContext.getOrganizationId())
            .environmentId(executionContext.getEnvironmentId())
            .actor(
                AuditActor
                    .builder()
                    .userId(userDetails.getUsername())
                    .userSource(userDetails.getSource())
                    .userSourceId(userDetails.getSourceId())
                    .build()
            )
            .build();
    }
}
