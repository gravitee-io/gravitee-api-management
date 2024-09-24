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

import io.gravitee.apim.core.scoring.use_case.ImportEnvironmentRulesetUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v2.rest.model.ImportScoringRuleset;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EnvironmentScoringRulesetsResource extends AbstractResource {

    @Context
    protected UriInfo uriInfo;

    @Inject
    private ImportEnvironmentRulesetUseCase importEnvironmentRulesetUseCase;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response importRuleset(@Valid ImportScoringRuleset request) {
        var result = importEnvironmentRulesetUseCase.execute(
            new ImportEnvironmentRulesetUseCase.Input(
                new ImportEnvironmentRulesetUseCase.NewRuleset(request.getName(), request.getDescription(), request.getPayload()),
                getAuditInfo()
            )
        );

        return Response.created(uriInfo.getRequestUri().resolve(result.rulesetId())).build();
    }
}
