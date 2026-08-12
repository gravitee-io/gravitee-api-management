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
package io.gravitee.rest.api.service.cockpit.command.handler;

import static fixtures.core.model.AsyncJobFixture.aPendingScoringRequestJob;
import static org.assertj.core.api.Assertions.assertThat;

import fixtures.core.model.ScoringReportFixture;
import inmemory.AsyncJobCrudServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.ScoringReportCrudServiceInMemory;
import io.gravitee.apim.core.async_job.model.AsyncJob;
import io.gravitee.apim.core.scoring.domain_service.ScoreComputingDomainService;
import io.gravitee.apim.core.scoring.model.ScoringReport;
import io.gravitee.apim.core.scoring.use_case.SaveScoringResponseUseCase;
import io.gravitee.cockpit.api.command.v1.scoring.response.ScoringResponseCommand;
import io.gravitee.cockpit.api.command.v1.scoring.response.ScoringResponseCommandPayload;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.scoring.api.model.ScoringResult;
import io.gravitee.scoring.api.model.asset.AssetAnalyzed;
import io.gravitee.scoring.api.model.asset.AssetType;
import io.gravitee.scoring.api.model.asset.ContentType;
import io.gravitee.scoring.api.model.diagnostic.AssetDiagnostic;
import io.gravitee.scoring.api.model.diagnostic.Diagnostic;
import io.gravitee.scoring.api.model.diagnostic.Position;
import io.gravitee.scoring.api.model.diagnostic.Range;
import io.gravitee.scoring.api.model.diagnostic.Severity;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ScoringResponseCommandHandlerTest {

    private static final Instant INSTANT_NOW = Instant.parse("2023-10-22T10:15:30Z");
    private static final String JOB_ID = "job-id";
    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String API_ID = "api-id";
    private static final String USER_ID = "user-id";

    ScoringReportCrudServiceInMemory scoringReportCrudService = new ScoringReportCrudServiceInMemory();
    AsyncJobCrudServiceInMemory asyncJobCrudService = new AsyncJobCrudServiceInMemory();

    ScoringResponseCommandHandler handler;

    @BeforeAll
    static void beforeAll() {
        TimeProvider.overrideClock(Clock.fixed(INSTANT_NOW, ZoneId.systemDefault()));
    }

    @AfterAll
    static void afterAll() {
        TimeProvider.overrideClock(Clock.systemDefaultZone());
    }

    @BeforeEach
    void setUp() {
        handler = new ScoringResponseCommandHandler(
            new SaveScoringResponseUseCase(asyncJobCrudService, scoringReportCrudService, new ScoreComputingDomainService())
        );
    }

    @AfterEach
    void tearDown() {
        Stream.of(asyncJobCrudService, scoringReportCrudService).forEach(InMemoryAlternative::reset);
    }

    @Test
    void should_store_the_report_and_complete_the_job_when_scoring_succeeded() {
        // Given
        givenAPendingJob();

        // When
        handler.handle(aCommand(new ScoringResult(List.of(anAssetDiagnostic())))).test().awaitDone(5, TimeUnit.SECONDS).assertComplete();

        // Then
        assertThat(asyncJobCrudService.storage()).singleElement().extracting(AsyncJob::getStatus).isEqualTo(AsyncJob.Status.SUCCESS);
        assertThat(scoringReportCrudService.storage()).singleElement().extracting(ScoringReport::apiId).isEqualTo(API_ID);
    }

    @Test
    void should_flag_the_job_in_error_and_keep_the_previous_report_when_scoring_failed() {
        // Given a previously scored API, and a provider failure reported by Cockpit
        var previousReport = ScoringReportFixture.aScoringReport().toBuilder().apiId(API_ID).build();
        scoringReportCrudService.initWith(List.of(previousReport));
        givenAPendingJob();

        // When
        handler
            .handle(aCommand(new ScoringResult(List.of(), false, "Error while scoring with provider spectral")))
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertComplete();

        // Then the failure is surfaced on the job instead of being recorded as an empty successful report
        assertThat(asyncJobCrudService.storage())
            .singleElement()
            .satisfies(job -> {
                assertThat(job.getStatus()).isEqualTo(AsyncJob.Status.ERROR);
                assertThat(job.getErrorMessage()).isEqualTo("Error while scoring with provider spectral");
            });
        assertThat(scoringReportCrudService.storage()).containsExactly(previousReport);
    }

    private void givenAPendingJob() {
        asyncJobCrudService.initWith(
            List.of(
                aPendingScoringRequestJob()
                    .toBuilder()
                    .id(JOB_ID)
                    .sourceId(API_ID)
                    .initiatorId(USER_ID)
                    .environmentId(ENVIRONMENT_ID)
                    .build()
            )
        );
    }

    private static ScoringResponseCommand aCommand(ScoringResult result) {
        return new ScoringResponseCommand(new ScoringResponseCommandPayload(JOB_ID, ORGANIZATION_ID, ENVIRONMENT_ID, result));
    }

    private static AssetDiagnostic anAssetDiagnostic() {
        return new AssetDiagnostic(
            new AssetAnalyzed("page-id", AssetType.OPEN_API, "swagger.json", ContentType.JSON),
            List.of(
                new Diagnostic(
                    new Range(new Position(1, 0), new Position(1, 5)),
                    Severity.WARN,
                    "operation-operationId",
                    "Operation must have \"operationId\".",
                    "swagger.json",
                    "paths./echo.options"
                )
            ),
            List.of()
        );
    }
}
