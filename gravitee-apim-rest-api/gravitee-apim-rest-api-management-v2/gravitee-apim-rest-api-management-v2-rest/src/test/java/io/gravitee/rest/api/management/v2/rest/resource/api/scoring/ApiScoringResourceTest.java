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

import static assertions.MAPIAssertions.assertThat;
import static io.gravitee.common.http.HttpStatusCode.ACCEPTED_202;
import static io.gravitee.common.http.HttpStatusCode.NOT_FOUND_404;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.mockito.ArgumentMatchers.eq;

import assertions.MAPIAssertions;
import fixtures.core.model.ApiFixtures;
import fixtures.core.model.PageFixtures;
import fixtures.core.model.ScoringReportFixture;
import inmemory.ApiCrudServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.PageCrudServiceInMemory;
import inmemory.ScoringReportQueryServiceInMemory;
import io.gravitee.rest.api.management.v2.rest.model.ApiScoring;
import io.gravitee.rest.api.management.v2.rest.model.ApiScoringAsset;
import io.gravitee.rest.api.management.v2.rest.model.ApiScoringAssetType;
import io.gravitee.rest.api.management.v2.rest.model.ApiScoringDiagnostic;
import io.gravitee.rest.api.management.v2.rest.model.ApiScoringDiagnosticRange;
import io.gravitee.rest.api.management.v2.rest.model.ApiScoringPosition;
import io.gravitee.rest.api.management.v2.rest.model.ApiScoringSeverity;
import io.gravitee.rest.api.management.v2.rest.model.ApiScoringSummary;
import io.gravitee.rest.api.management.v2.rest.model.ApiScoringTriggerResponse;
import io.gravitee.rest.api.management.v2.rest.model.ScoringStatus;
import io.gravitee.rest.api.management.v2.rest.resource.api.ApiResourceTest;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class ApiScoringResourceTest extends ApiResourceTest {

    WebTarget evaluateTarget;
    WebTarget latestReportTarget;

    @Inject
    ApiCrudServiceInMemory apiCrudService;

    @Inject
    ScoringReportQueryServiceInMemory scoringReportQueryService;

    @Inject
    PageCrudServiceInMemory pageCrudService;

    @BeforeEach
    public void setup() {
        evaluateTarget = rootTarget().path("_evaluate");
        latestReportTarget = rootTarget();

        GraviteeContext.setCurrentEnvironment(ENVIRONMENT);
        GraviteeContext.setCurrentOrganization(ORGANIZATION);
    }

    @Override
    @AfterEach
    public void tearDown() {
        super.tearDown();
        GraviteeContext.cleanContext();

        Stream.of(apiCrudService, pageCrudService, scoringReportQueryService).forEach(InMemoryAlternative::reset);
    }

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis/" + API + "/scoring";
    }

    @Nested
    class Evaluate {

        @Test
        void should_return_404_if_not_found() {
            final Response response = evaluateTarget.request().post(null);
            assertThat(response.getStatus()).isEqualTo(NOT_FOUND_404);

            MAPIAssertions
                .assertThat(response)
                .hasStatus(NOT_FOUND_404)
                .asError()
                .hasHttpStatus(NOT_FOUND_404)
                .hasMessage("Api not found.");
        }

        @Test
        void should_return_202_response() {
            apiCrudService.initWith(List.of(ApiFixtures.aFederatedApi().toBuilder().id(API).build()));

            final Response response = evaluateTarget.request().post(null);

            MAPIAssertions
                .assertThat(response)
                .hasStatus(ACCEPTED_202)
                .asEntity(ApiScoringTriggerResponse.class)
                .isEqualTo(ApiScoringTriggerResponse.builder().status(ScoringStatus.PENDING).build());
        }
    }

    @Nested
    class GetLatestReport {

        @Test
        void should_return_empty_response_if_no_report_found() {
            final Response response = latestReportTarget.request().get();

            MAPIAssertions.assertThat(response).hasStatus(OK_200).asEntity(ApiScoring.class).isEqualTo(ApiScoringResource.EMPTY_REPORT);
        }

        @Test
        void should_return_200_response() {
            apiCrudService.initWith(List.of(ApiFixtures.aFederatedApi().toBuilder().id(API).build()));
            pageCrudService.initWith(List.of(PageFixtures.aPage().toBuilder().referenceId(API).build()));
            scoringReportQueryService.initWith(List.of(ScoringReportFixture.aScoringReport().toBuilder().apiId(API).build()));

            final Response response = latestReportTarget.request().get();

            MAPIAssertions
                .assertThat(response)
                .hasStatus(OK_200)
                .asEntity(ApiScoring.class)
                .isEqualTo(
                    ApiScoring
                        .builder()
                        .summary(ApiScoringSummary.builder().all(1).errors(0).hints(0).infos(0).warnings(1).build())
                        .createdAt(Instant.parse("2020-02-03T20:22:02.00Z").atOffset(ZoneOffset.UTC))
                        .assets(
                            List.of(
                                ApiScoringAsset
                                    .builder()
                                    .name("parent")
                                    .type(ApiScoringAssetType.SWAGGER)
                                    .diagnostics(
                                        List.of(
                                            ApiScoringDiagnostic
                                                .builder()
                                                .severity(ApiScoringSeverity.WARN)
                                                .range(
                                                    ApiScoringDiagnosticRange
                                                        .builder()
                                                        .start(ApiScoringPosition.builder().line(17).character(12).build())
                                                        .end(ApiScoringPosition.builder().line(38).character(25).build())
                                                        .build()
                                                )
                                                .rule("operation-operationId")
                                                .message("Operation must have \"operationId\".")
                                                .path("paths./echo.options")
                                                .build()
                                        )
                                    )
                                    .build(),
                                ApiScoringAsset.builder().type(ApiScoringAssetType.GRAVITEE_DEFINITION).diagnostics(List.of()).build()
                            )
                        )
                        .build()
                );
        }
    }
}
