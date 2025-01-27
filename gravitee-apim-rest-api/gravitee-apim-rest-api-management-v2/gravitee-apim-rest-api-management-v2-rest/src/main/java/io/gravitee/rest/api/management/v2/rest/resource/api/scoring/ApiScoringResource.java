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
package io.gravitee.rest.api.management.v2.rest.resource.api.scoring;

import io.gravitee.apim.core.scoring.use_case.GetLatestReportUseCase;
import io.gravitee.apim.core.scoring.use_case.ScoreApiRequestUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v2.rest.mapper.ScoringReportMapper;
import io.gravitee.rest.api.management.v2.rest.model.ApiScoringTriggerResponse;
import io.gravitee.rest.api.management.v2.rest.model.ScoringStatus;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.Response;

public class ApiScoringResource extends AbstractResource {

    @Inject
    private ScoreApiRequestUseCase scoreApiRequestUseCase;

    @Inject
    private GetLatestReportUseCase getLatestReportUseCase;

    @PathParam("apiId")
    private String apiId;

    @POST
    @Path("/_evaluate")
    @Produces(MediaType.APPLICATION_JSON)
    public void scoreAPI(@Suspended final AsyncResponse response) {
        scoreApiRequestUseCase
            .execute(new ScoreApiRequestUseCase.Input(apiId, getAuditInfo()))
            .subscribe(
                () -> response.resume(Response.accepted(ApiScoringTriggerResponse.builder().status(ScoringStatus.PENDING).build()).build()),
                response::resume
            );
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getApiScoring() {
        var report = getLatestReportUseCase.execute(new GetLatestReportUseCase.Input(apiId)).report();
        if (report == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(ScoringReportMapper.INSTANCE.map(report)).build();
    }
}
