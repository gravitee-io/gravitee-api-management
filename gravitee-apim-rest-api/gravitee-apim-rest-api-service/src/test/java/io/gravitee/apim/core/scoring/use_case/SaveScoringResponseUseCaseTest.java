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
package io.gravitee.apim.core.scoring.use_case;

import static fixtures.core.model.AsyncJobFixture.aPendingScoringRequestJob;
import static org.assertj.core.api.Assertions.assertThat;

import fixtures.core.model.ScoringReportFixture;
import inmemory.AsyncJobCrudServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.ScoringReportCrudServiceInMemory;
import io.gravitee.apim.core.integration.model.AsyncJob;
import io.gravitee.apim.core.scoring.model.ScoringAssetType;
import io.gravitee.apim.core.scoring.model.ScoringReport;
import io.gravitee.apim.core.scoring.use_case.SaveScoringResponseUseCase.Input;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.service.common.UuidString;
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

class SaveScoringResponseUseCaseTest {

    private static final Instant INSTANT_NOW = Instant.parse("2023-10-22T10:15:30Z");
    private static final String JOB_ID = "job-Id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String API_ID = "api-id";
    private static final String USER_ID = "user-id";
    public static final ScoringReport.Asset ANALYZED_ASSET_1 = new ScoringReport.Asset(
        "page-id",
        ScoringAssetType.SWAGGER,
        List.of(
            new ScoringReport.Diagnostic(
                ScoringReport.Severity.WARN,
                new ScoringReport.Range(new ScoringReport.Position(17, 12), new ScoringReport.Position(38, 25)),
                "operation-operationId",
                "Operation must have \"operationId\".",
                "paths./echo.options"
            )
        )
    );

    ScoringReportCrudServiceInMemory scoringReportCrudService = new ScoringReportCrudServiceInMemory();
    AsyncJobCrudServiceInMemory asyncJobCrudService = new AsyncJobCrudServiceInMemory();

    SaveScoringResponseUseCase useCase;

    @BeforeAll
    static void beforeAll() {
        UuidString.overrideGenerator(() -> "generated-id");
        TimeProvider.overrideClock(Clock.fixed(INSTANT_NOW, ZoneId.systemDefault()));
    }

    @AfterAll
    static void afterAll() {
        UuidString.reset();
        TimeProvider.overrideClock(Clock.systemDefaultZone());
    }

    @BeforeEach
    void setUp() {
        useCase = new SaveScoringResponseUseCase(asyncJobCrudService, scoringReportCrudService);
    }

    @AfterEach
    void tearDown() {
        Stream.of(asyncJobCrudService, scoringReportCrudService).forEach(InMemoryAlternative::reset);
    }

    @Test
    void should_store_scoring_result() {
        // Given
        givenAnAsyncJob(
            aPendingScoringRequestJob().toBuilder().id(JOB_ID).sourceId(API_ID).initiatorId(USER_ID).environmentId(ENVIRONMENT_ID).build()
        );

        // When
        useCase.execute(new Input(JOB_ID, List.of(ANALYZED_ASSET_1))).test().awaitDone(5, TimeUnit.SECONDS).assertComplete();

        // Then
        assertThat(scoringReportCrudService.storage())
            .contains(
                ScoringReport
                    .builder()
                    .id(JOB_ID)
                    .apiId("api-id")
                    .assets(List.of(ANALYZED_ASSET_1))
                    .createdAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                    .build()
            );
    }

    @Test
    void should_complete_scoring_job_when_succeed() {
        // Given
        var job = givenAnAsyncJob(
            aPendingScoringRequestJob().toBuilder().id(JOB_ID).sourceId(API_ID).initiatorId(USER_ID).environmentId(ENVIRONMENT_ID).build()
        );

        // When
        useCase.execute(new Input(JOB_ID, List.of(ANALYZED_ASSET_1))).test().awaitDone(5, TimeUnit.SECONDS).assertComplete();

        // Then
        assertThat(asyncJobCrudService.storage())
            .contains(job.toBuilder().status(AsyncJob.Status.SUCCESS).updatedAt(INSTANT_NOW.atZone(ZoneId.systemDefault())).build());
    }

    @Test
    void should_remove_previous_report_if_exists() {
        // Given
        givenExistingScoringReports(ScoringReportFixture.aScoringReport().toBuilder().apiId(API_ID).build());
        var job = givenAnAsyncJob(
            aPendingScoringRequestJob().toBuilder().id(JOB_ID).sourceId(API_ID).initiatorId(USER_ID).environmentId(ENVIRONMENT_ID).build()
        );

        // When
        useCase.execute(new Input(JOB_ID, List.of(ANALYZED_ASSET_1))).test().awaitDone(5, TimeUnit.SECONDS).assertComplete();

        // Then
        assertThat(scoringReportCrudService.storage())
            .hasSize(1)
            .containsExactly(
                ScoringReport
                    .builder()
                    .id(JOB_ID)
                    .apiId("api-id")
                    .assets(List.of(ANALYZED_ASSET_1))
                    .createdAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                    .build()
            );
    }

    @Test
    void should_do_nothing_when_job_does_not_exists() {
        // Given

        // When
        useCase.execute(new Input(JOB_ID, List.of(ANALYZED_ASSET_1))).test().awaitDone(5, TimeUnit.SECONDS).assertComplete();

        // Then
        assertThat(scoringReportCrudService.storage()).isEmpty();
    }

    private AsyncJob givenAnAsyncJob(AsyncJob job) {
        asyncJobCrudService.initWith(List.of(job));
        return job;
    }

    private void givenExistingScoringReports(ScoringReport... reports) {
        scoringReportCrudService.initWith(List.of(reports));
    }
}
