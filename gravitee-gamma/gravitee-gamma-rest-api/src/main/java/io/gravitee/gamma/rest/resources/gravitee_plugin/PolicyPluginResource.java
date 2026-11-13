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
package io.gravitee.gamma.rest.resources.gravitee_plugin;

import static java.util.stream.Collectors.toSet;

import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.gamma.rest.core.gravitee_plugin.port.service_provider.PolicyPluginProvider;
import io.gravitee.gamma.rest.core.gravitee_plugin.use_case.ListPolicyPluginsUseCase;
import io.gravitee.gamma.rest.resources.gravitee_plugin.dto.PolicyFlow;
import io.gravitee.gamma.rest.resources.gravitee_plugin.dto.PolicyPluginDto;
import io.gravitee.rest.api.model.v4.policy.ApiProtocolType;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class PolicyPluginResource {

    @Inject
    private ListPolicyPluginsUseCase listPolicyPluginsUseCase;

    @Inject
    private PolicyPluginProvider policyPluginProvider;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<PolicyPluginDto> getPolicies(
        @PathParam("orgId") String organizationId,
        @QueryParam("flow") List<String> flows,
        @QueryParam("phase") String phase
    ) {
        return listPolicyPluginsUseCase
            .execute(new ListPolicyPluginsUseCase.Input(organizationId, toFlows(flows), phase))
            .policies()
            .stream()
            .map(PolicyPluginDto::from)
            .toList();
    }

    @GET
    @Path("/{policyId}")
    @Produces(MediaType.APPLICATION_JSON)
    public PolicyPluginDto getPolicy(@PathParam("orgId") String organizationId, @PathParam("policyId") String policyId) {
        return PolicyPluginDto.from(policyPluginProvider.findPolicyById(organizationId, policyId));
    }

    @GET
    @Path("/{policyId}/schema")
    @Produces(MediaType.APPLICATION_JSON)
    public String getPolicySchema(@PathParam("policyId") String policyId) {
        return policyPluginProvider.getSchema(policyId);
    }

    @GET
    @Path("/{policyId}/documentation")
    @Produces(MediaType.TEXT_PLAIN)
    public String getPolicyDocumentation(@PathParam("policyId") String policyId) {
        return policyPluginProvider.getDocumentation(policyId);
    }

    /** Resolved here rather than by a param converter: Jersey answers 404, not 400, when a query parameter fails to convert. */
    private static Set<ApiProtocolType> toFlows(List<String> names) {
        return names
            .stream()
            .map(name ->
                PolicyFlow.fromName(name).orElseThrow(() ->
                    new ValidationDomainException(
                        "Invalid flow: " +
                            name +
                            ". Expected one of " +
                            Arrays.stream(ApiProtocolType.values()).map(PolicyFlow::nameOf).toList() +
                            "."
                    )
                )
            )
            .collect(toSet());
    }
}
