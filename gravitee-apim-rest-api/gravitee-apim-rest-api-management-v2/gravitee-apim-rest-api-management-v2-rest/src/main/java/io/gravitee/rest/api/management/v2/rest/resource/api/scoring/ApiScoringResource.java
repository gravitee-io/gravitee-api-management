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

import io.gravitee.apim.core.scoring.use_case.ScoreApiRequestUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v2.rest.model.ApiScoring;
import io.gravitee.rest.api.management.v2.rest.model.ApiScoringAsset;
import io.gravitee.rest.api.management.v2.rest.model.ApiScoringAssetType;
import io.gravitee.rest.api.management.v2.rest.model.ApiScoringDiagnostic;
import io.gravitee.rest.api.management.v2.rest.model.ApiScoringDiagnosticRange;
import io.gravitee.rest.api.management.v2.rest.model.ApiScoringPosition;
import io.gravitee.rest.api.management.v2.rest.model.ApiScoringSeverity;
import io.gravitee.rest.api.management.v2.rest.model.ApiScoringTriggerResponse;
import io.gravitee.rest.api.management.v2.rest.model.ScoringStatus;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.Response;
import java.util.List;

public class ApiScoringResource extends AbstractResource {

    @Inject
    private ScoreApiRequestUseCase scoreApiRequestUseCase;

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
    public ApiScoring getApiScoring() {
        return ApiScoring
            .builder()
            .all(3)
            .errors(0)
            .hints(1)
            .infos(0)
            .warnings(2)
            .assets(
                List.of(
                    ApiScoringAsset
                        .builder()
                        .name("Echo-oas.json")
                        .type(ApiScoringAssetType.SWAGGER)
                        .diagnostics(
                            List.of(
                                ApiScoringDiagnostic
                                    .builder()
                                    .range(
                                        ApiScoringDiagnosticRange
                                            .builder()
                                            .start(ApiScoringPosition.builder().line(9).character(9).build())
                                            .end(ApiScoringPosition.builder().line(5).character(38).build())
                                            .build()
                                    )
                                    .severity(ApiScoringSeverity.WARN)
                                    .rule("info-contact")
                                    .message("Info object must have \"contact\" object.")
                                    .path("info")
                                    .build(),
                                ApiScoringDiagnostic
                                    .builder()
                                    .range(
                                        ApiScoringDiagnosticRange
                                            .builder()
                                            .start(ApiScoringPosition.builder().line(17).character(12).build())
                                            .end(ApiScoringPosition.builder().line(38).character(25).build())
                                            .build()
                                    )
                                    .severity(ApiScoringSeverity.WARN)
                                    .rule("operation-description")
                                    .message("Operation \"description\" must be present and non-empty string.")
                                    .path("paths./echo.get")
                                    .build(),
                                ApiScoringDiagnostic
                                    .builder()
                                    .range(
                                        ApiScoringDiagnosticRange
                                            .builder()
                                            .start(ApiScoringPosition.builder().line(17).character(12).build())
                                            .end(ApiScoringPosition.builder().line(38).character(25).build())
                                            .build()
                                    )
                                    .severity(ApiScoringSeverity.HINT)
                                    .rule("operation-tags")
                                    .message("Operation must have non-empty \"tags\" array.")
                                    .path("paths./echo.get")
                                    .build()
                            )
                        )
                        .build()
                )
            )
            .build();
    }
}
