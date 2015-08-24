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

import io.gravitee.management.model.ApiEntity;
import io.gravitee.management.model.PolicyConfigurationEntity;
import io.gravitee.management.service.ApiService;
import io.gravitee.management.service.PermissionService;
import io.gravitee.management.service.PermissionType;
import io.gravitee.management.service.PolicyConfigurationService;
import io.gravitee.management.service.exceptions.ApiNotFoundException;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class PoliciesConfigurationResource extends AbstractResource {

    @PathParam("apiName")
    private String apiName;

    @Inject
    private PolicyConfigurationService policyConfigurationService;

    @Inject
    private ApiService apiService;

    @Inject
    private PermissionService permissionService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<PolicyConfigurationEntity> policies() {
        // Check that the API exists
        Optional<ApiEntity> api = apiService.findByName(apiName);
        if (! api.isPresent()) {
            throw new ApiNotFoundException(apiName);
        }

        permissionService.hasPermission(getAuthenticatedUser(), apiName, PermissionType.VIEW_API);

        return policyConfigurationService.getPolicies(apiName);
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response applyPolicyConfigurations(@Valid final List<PolicyConfigurationEntity> policiesConfiguration) {
        // Check that the API exists
        Optional<ApiEntity> api = apiService.findByName(apiName);
        if (! api.isPresent()) {
            throw new ApiNotFoundException(apiName);
        }

        permissionService.hasPermission(getAuthenticatedUser(), apiName, PermissionType.EDIT_API);

        policyConfigurationService.updatePolicyConfigurations(apiName, policiesConfiguration);

        return Response.noContent().build();
    }
}
