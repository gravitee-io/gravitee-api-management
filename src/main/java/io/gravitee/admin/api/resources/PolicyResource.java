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
package io.gravitee.admin.api.resources;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gateway.core.service.ApiService;
import io.gravitee.model.Api;
import io.gravitee.model.Policy;

/**
 * Defines the REST resources to manage {@code Policy}.
 *
 * @author Azize Elamrani (azize dot elamrani at gmail dot com)
 */
@Component
@Produces(MediaType.APPLICATION_JSON)
@Path("/policies")
public class PolicyResource {

    @Autowired
    private ApiService apiService;

    @GET
    @Path("/{apiName}")
    public Response listAll(@PathParam("apiName") final String apiName) {
        final Api api = apiService.get(apiName);

        final List<Policy> policies = new ArrayList<>();
        Policy policy = new Policy();
        policy.setName("rate-limit");
        policy.setConfiguration("Rate Limiting Policy");
        policies.add(policy);
        policy = new Policy();
        policy.setName("access-control");
        policy.setConfiguration("Access control");
        policies.add(policy);
        policy = new Policy();
        policy.setName("response-time");
        policy.setConfiguration("Response time");
        policies.add(policy);

        if (api.getPolicies() != null) {
            policies.removeAll(api.getPolicies().values());
        }

        return Response.status(HttpStatusCode.OK_200).entity(policies)
            .header("Access-Control-Allow-Origin", "*").build();
    }
}
