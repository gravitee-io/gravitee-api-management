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

import io.gravitee.management.model.PolicyConfigurationEntity;
import io.gravitee.management.service.PolicyConfigurationService;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class PoliciesConfigurationResource {

    @PathParam("apiName")
    private String apiName;

    @Inject
    private PolicyConfigurationService policyConfigurationService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<PolicyConfigurationEntity> policies() {
        return policyConfigurationService.getPolicies(apiName);
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response applyPolicyConfigurations(@Valid final List<PolicyConfigurationEntity> policiesConfiguration) {
        policyConfigurationService.updatePolicyConfigurations(apiName, policiesConfiguration);

        return Response.noContent().build();
    }
}
