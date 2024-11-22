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
package io.gravitee.rest.api.management.v2.rest.resource.environment;

import io.gravitee.apim.core.scoring.use_case.DeleteEnvironmentFunctionUseCase;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EnvironmentScoringFunctionResource extends AbstractResource {

    @PathParam("functionId")
    String functionId;

    @Inject
    private DeleteEnvironmentFunctionUseCase deleteEnvironmentFunctionUseCase;

    @DELETE
    public Response deleteFunction() {
        deleteEnvironmentFunctionUseCase.execute(new DeleteEnvironmentFunctionUseCase.Input(functionId, getAuditInfo()));
        return Response.noContent().build();
    }
}
