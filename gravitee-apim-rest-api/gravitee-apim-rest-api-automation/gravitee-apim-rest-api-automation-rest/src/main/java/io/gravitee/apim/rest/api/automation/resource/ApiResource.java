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

import static io.gravitee.apim.rest.api.automation.helpers.HRIDHelper.nameToHRID;

import io.gravitee.apim.core.api.model.crd.ApiCRDSpec;
import io.gravitee.apim.core.api.model.crd.IDExportStrategy;
import io.gravitee.apim.core.api.use_case.ExportApiCRDUseCase;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.rest.api.automation.exception.HRIDNotFoundException;
import io.gravitee.apim.rest.api.automation.helpers.HRIDHelper;
import io.gravitee.apim.rest.api.automation.helpers.SharedPolicyGroupIdHelper;
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
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.CustomLog;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
public class ApiResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private ExportApiCRDUseCase exportApiCRDUseCase;

    @Inject
    protected io.gravitee.rest.api.service.v4.ApiService apiServiceV4;

    @Path("subscriptions")
    public ApiSubscriptionsResource getSubscriptionsResource() {
        return resourceContext.getResource(ApiSubscriptionsResource.class);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_DEFINITION, acls = { RolePermissionAction.READ }) })
    public Response getApiByHRID(
        @PathParam("apiHrid") String apiHrid,
        @QueryParam("legacy") boolean legacy,
        @HeaderParam(HRIDHelper.HEADER_X_GRAVITEE_SET_HRID) boolean setHRID
    ) {
        var executionContext = GraviteeContext.getExecutionContext();
        var userDetails = getAuthenticatedUserDetails();

        boolean hridContainsUUID = legacy || setHRID;
        var input = new ExportApiCRDUseCase.Input(
            hridContainsUUID ? apiHrid : IdBuilder.builder(executionContext, apiHrid).buildId(),
            IDExportStrategy.ALL,
            buildAuditInfo(executionContext, userDetails)
        );

        try {
            ApiCRDSpec apiCRDSpec = exportApiCRDUseCase.execute(input).spec();
            if (setHRID) {
                if (apiCRDSpec.getHrid() == null || isUUID(apiCRDSpec.getHrid())) {
                    apiCRDSpec.setHrid(nameToHRID(apiCRDSpec.getName()));
                }
                setPageParentAndGeneralConditionsHRIDs(apiCRDSpec);
            }
            ApiV4Spec apiV4Spec = ApiMapper.INSTANCE.apiCRDSpecToApiV4Spec(ApiCRDMapper.INSTANCE.map(apiCRDSpec));
            SharedPolicyGroupIdHelper.removeSPGID(apiV4Spec, setHRID);
            if (setHRID) {
                // now that hrid are populated from the CRD map that uses names as keys,
                // we need to format them to be compliant with the previous transformation
                formatHrids(apiV4Spec);
            }
            return Response.ok(
                ApiMapper.INSTANCE.apiV4SpecToApiV4State(
                    apiV4Spec,
                    apiCRDSpec.getId(),
                    apiCRDSpec.getCrossId(),
                    executionContext.getOrganizationId(),
                    executionContext.getEnvironmentId()
                )
            ).build();
        } catch (ApiNotFoundException e) {
            log.warn("API not found for HRID: {}, operation: getApiByHRID", apiHrid, e);
            throw new HRIDNotFoundException(apiHrid);
        }
    }

    private void formatHrids(ApiV4Spec apiV4Spec) {
        if (apiV4Spec.getPages() != null) {
            apiV4Spec.getPages().forEach(p -> p.setHrid(nameToHRID(p.getName())));
        }
        if (apiV4Spec.getPlans() != null) {
            apiV4Spec.getPlans().forEach(p -> p.setHrid(nameToHRID(p.getName())));
        }
    }

    @DELETE
    @Permissions({ @Permission(value = RolePermission.API_DEFINITION, acls = RolePermissionAction.DELETE) })
    public Response deleteApi(@PathParam("apiHrid") String apiHrid, @QueryParam("legacy") boolean legacy) {
        var executionContext = GraviteeContext.getExecutionContext();

        try {
            apiServiceV4.delete(
                GraviteeContext.getExecutionContext(),
                legacy ? apiHrid : IdBuilder.builder(executionContext, apiHrid).buildId(),
                true
            );
        } catch (ApiNotFoundException e) {
            log.warn("API not found for HRID: {}, operation: deleteApi", apiHrid, e);
            throw new HRIDNotFoundException(apiHrid);
        }
        return Response.noContent().build();
    }

    private static AuditInfo buildAuditInfo(ExecutionContext executionContext, UserDetails userDetails) {
        return AuditInfo.builder()
            .organizationId(executionContext.getOrganizationId())
            .environmentId(executionContext.getEnvironmentId())
            .actor(
                AuditActor.builder()
                    .userId(userDetails.getUsername())
                    .userSource(userDetails.getSource())
                    .userSourceId(userDetails.getSourceId())
                    .build()
            )
            .build();
    }

    private static void setPageParentAndGeneralConditionsHRIDs(ApiCRDSpec api) {
        Map<String, String> pageIDsToHrid = new HashMap<>();
        api.getPages().forEach((k, v) -> pageIDsToHrid.put(v.getId(), nameToHRID(v.getName())));
        api
            .getPages()
            .values()
            .stream()
            .filter(p -> p.getParentId() != null)
            .forEach(p -> p.setParentHrid(pageIDsToHrid.get(p.getParentId())));
        api
            .getPlans()
            .values()
            .stream()
            .filter(p -> p.getGeneralConditions() != null)
            .forEach(p -> p.setGeneralConditionsHrid(pageIDsToHrid.get(p.getGeneralConditions())));
    }

    public static boolean isUUID(String value) {
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
