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

import io.gravitee.apim.rest.api.automation.model.DocumentSpec;
import io.gravitee.apim.rest.api.automation.model.DocumentState;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.HRIDToUUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;

public class ApiMockDocumentsResource {

    @Context
    private ResourceContext resourceContext;

    @Path("/{hrid}")
    public ApiMockDocumentResource getMockDocumentResource() {
        return resourceContext.getResource(ApiMockDocumentResource.class);
    }

    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createOrUpdate(@Valid @NotNull DocumentSpec spec, @PathParam("apiHrid") String apiHrid) {
        var executionContext = GraviteeContext.getExecutionContext();
        String id = HRIDToUUID.apiMock().context(executionContext).api(apiHrid).mock(spec.getHrid()).id();
        DocumentState documentState = new DocumentState();
        documentState.setId(id);
        documentState.setHrid(spec.getHrid());
        documentState.setContent("API %s Document %s".formatted(apiHrid, spec.getHrid()));
        documentState.setName(spec.getName());
        documentState.setEnvironmentId(executionContext.getEnvironmentId());
        documentState.setOrganizationId(executionContext.getOrganizationId());
        return Response.ok(MockDocumentStore.put(documentState)).build();
    }
}
