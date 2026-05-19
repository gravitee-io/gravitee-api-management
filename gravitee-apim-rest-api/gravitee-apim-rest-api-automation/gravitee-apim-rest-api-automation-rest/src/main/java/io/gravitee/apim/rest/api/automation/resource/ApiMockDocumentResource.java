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

import io.gravitee.apim.rest.api.automation.model.DocumentState;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.HRIDToUUID;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

public class ApiMockDocumentResource extends AbstractResource{

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response get(@PathParam("apiHrid") String apiHrid, @PathParam("hrid") String hrid) {
        var executionContext = GraviteeContext.getExecutionContext();
        String id = HRIDToUUID.apiMock().context(executionContext).api(apiHrid).mock(hrid).id();
        DocumentState document = MockDocumentStore.get(id);
        if (document == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(document).build();
    }

    @DELETE
    public Response delete(@PathParam("apiHrid") String apiHrid,@PathParam("hrid") String hrid) {
        var executionContext = GraviteeContext.getExecutionContext();
        String id = HRIDToUUID.apiMock().context(executionContext).api(apiHrid).mock(hrid).id();
        var removed = MockDocumentStore.remove(id);
        if (removed == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.noContent().build();
    }
}
