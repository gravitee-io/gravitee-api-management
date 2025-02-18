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
package io.gravitee.apim.core.log.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import fixtures.core.model.PlanFixtures;
import fixtures.repository.ConnectionLogFixtures;
import inmemory.ApiCrudServiceInMemory;
import inmemory.ApplicationCrudServiceInMemory;
import inmemory.ConnectionLogsCrudServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.PlanCrudServiceInMemory;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.log.domain_service.ConnectionLogMetadataDomainService;
import io.gravitee.apim.core.log.model.ConnectionLog;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import io.gravitee.rest.api.model.analytics.SearchLogsFilters;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SearchApplicationConnectionLogsUseCaseTest {

    private static final String API_ID = "api-1";
    private static final String APPLICATION_ID = "app1";
    private static final Plan PLAN_1 = PlanFixtures.aPlanHttpV4().toBuilder().id("plan1").name("1st plan").build();
    private static final Plan PLAN_2 = PlanFixtures.aPlanHttpV4().toBuilder().id("plan2").name("2nd plan").build();
    private static final BaseApplicationEntity APPLICATION_1 = BaseApplicationEntity
        .builder()
        .id(APPLICATION_ID)
        .name("an application name")
        .build();

    private static final Api API_1 = Api.builder().id("api-1").name("api-name").version("1.1").build();
    private static final Api API_2 = Api.builder().id("api-2").name("api-name-2").version("2.2").build();

    private static final Long FIRST_FEBRUARY_2020 = Instant.parse("2020-02-01T00:01:00.00Z").toEpochMilli();
    private static final Long SECOND_FEBRUARY_2020 = Instant.parse("2020-02-02T23:59:59.00Z").toEpochMilli();
    private static final Long FOURTH_FEBRUARY_2020 = Instant.parse("2020-02-04T00:01:00.00Z").toEpochMilli();
    private static final Long FIFTH_FEBRUARY_2020 = Instant.parse("2020-02-05T00:01:00.00Z").toEpochMilli();

    ConnectionLogFixtures connectionLogFixtures = new ConnectionLogFixtures(API_1.getId(), APPLICATION_ID, PLAN_1.getId());

    ConnectionLogsCrudServiceInMemory logStorageService = new ConnectionLogsCrudServiceInMemory();
    PlanCrudServiceInMemory planStorageService = new PlanCrudServiceInMemory();
    ApplicationCrudServiceInMemory applicationStorageService = new ApplicationCrudServiceInMemory();
    ApiCrudServiceInMemory apiCrudServiceInMemory = new ApiCrudServiceInMemory();

    SearchApplicationConnectionLogsUseCase cut;

    @BeforeEach
    void setUp() {
        cut =
            new SearchApplicationConnectionLogsUseCase(
                logStorageService,
                applicationStorageService,
                planStorageService,
                apiCrudServiceInMemory,
                new ConnectionLogMetadataDomainService()
            );

        planStorageService.initWith(List.of(PLAN_1, PLAN_2));
        applicationStorageService.initWith(List.of(APPLICATION_1));
        apiCrudServiceInMemory.initWith(List.of(API_1, API_2));
    }

    @AfterEach
    void tearDown() {
        Stream.of(logStorageService, planStorageService, applicationStorageService).forEach(InMemoryAlternative::reset);

        GraviteeContext.cleanContext();
    }

    @Test
    void should_return_connection_logs_of_an_application() {
        logStorageService.initWithConnectionLogs(
            List.of(
                connectionLogFixtures.aConnectionLog("req1"),
                connectionLogFixtures
                    .aConnectionLog()
                    .toBuilder()
                    .apiId("other-api")
                    .planId("other-plan")
                    .applicationId("other-application")
                    .build()
            )
        );

        var result = cut.execute(
            GraviteeContext.getExecutionContext(),
            new SearchApplicationConnectionLogsUseCase.Input(
                APPLICATION_ID,
                SearchLogsFilters.builder().from(FIRST_FEBRUARY_2020).to(SECOND_FEBRUARY_2020).build(),
                Optional.empty()
            )
        );

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(result.total()).isOne();
            soft
                .assertThat(result.data())
                .isEqualTo(
                    List.of(
                        ConnectionLog
                            .builder()
                            .requestId("req1")
                            .apiId(API_ID)
                            .api(API_1)
                            .applicationId(APPLICATION_ID)
                            .plan(PLAN_1)
                            .timestamp("2020-02-01T20:00:00.00Z")
                            .requestEnded(true)
                            .method(HttpMethod.GET)
                            .clientIdentifier("client-identifier")
                            .transactionId("transaction-id")
                            .status(200)
                            .build()
                    )
                );
        });
    }

    @Test
    void should_return_application_connection_logs_sorted_by_desc_timestamp() {
        logStorageService.initWithConnectionLogs(
            List.of(
                connectionLogFixtures.aConnectionLog("req1").toBuilder().timestamp("2020-02-01T20:00:00.00Z").build(),
                connectionLogFixtures.aConnectionLog("req2").toBuilder().timestamp("2020-02-02T20:00:00.00Z").build(),
                connectionLogFixtures.aConnectionLog("req3").toBuilder().timestamp("2020-02-04T20:00:00.00Z").build()
            )
        );

        var result = cut.execute(
            GraviteeContext.getExecutionContext(),
            new SearchApplicationConnectionLogsUseCase.Input(
                APPLICATION_ID,
                SearchLogsFilters.builder().from(FIRST_FEBRUARY_2020).to(FIFTH_FEBRUARY_2020).build()
            )
        );

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(result.total()).isEqualTo(3);
            soft
                .assertThat(result.data())
                .extracting(ConnectionLog::getRequestId, ConnectionLog::getTimestamp)
                .containsExactly(
                    tuple("req3", "2020-02-04T20:00:00.00Z"),
                    tuple("req2", "2020-02-02T20:00:00.00Z"),
                    tuple("req1", "2020-02-01T20:00:00.00Z")
                );
        });
    }

    @Test
    void should_return_the_page_requested() {
        var expectedTotal = 15;
        var pageNumber = 2;
        var pageSize = 5;
        logStorageService.initWithConnectionLogs(
            IntStream.range(0, expectedTotal).mapToObj(i -> connectionLogFixtures.aConnectionLog(String.valueOf(i))).toList()
        );

        var result = cut.execute(
            GraviteeContext.getExecutionContext(),
            new SearchApplicationConnectionLogsUseCase.Input(
                APPLICATION_ID,
                SearchLogsFilters.builder().from(FIRST_FEBRUARY_2020).to(SECOND_FEBRUARY_2020).build(),
                new PageableImpl(pageNumber, pageSize)
            )
        );

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(result.total()).isEqualTo(expectedTotal);
            soft.assertThat(result.data()).extracting(ConnectionLog::getRequestId).containsExactly("5", "6", "7", "8", "9");
        });
    }

    @Test
    void should_return_application_connection_logs_with_only_plan_id_if_plan_cannot_be_found() {
        var unknownPlan = "unknown";

        logStorageService.initWithConnectionLogs(List.of(connectionLogFixtures.aConnectionLog().toBuilder().planId(unknownPlan).build()));

        var result = cut.execute(
            GraviteeContext.getExecutionContext(),
            new SearchApplicationConnectionLogsUseCase.Input(
                APPLICATION_ID,
                SearchLogsFilters.builder().from(FIRST_FEBRUARY_2020).to(SECOND_FEBRUARY_2020).build()
            )
        );
        assertThat(result.data()).extracting(ConnectionLog::getPlan).isEqualTo(List.of(Plan.builder().id("1").build()));
    }

    @Test
    void should_return_application_connection_logs_with_only_available_info() {
        logStorageService.initWithConnectionLogs(
            List.of(connectionLogFixtures.aConnectionLog().toBuilder().planId(null).clientIdentifier(null).build())
        );

        var result = cut.execute(
            GraviteeContext.getExecutionContext(),
            new SearchApplicationConnectionLogsUseCase.Input(
                APPLICATION_ID,
                SearchLogsFilters.builder().from(FIRST_FEBRUARY_2020).to(SECOND_FEBRUARY_2020).build()
            )
        );
        assertThat(result.data()).extracting(ConnectionLog::getPlan).isEqualTo(List.of(Plan.builder().id("1").build()));
    }

    @Test
    void should_return_application_connection_logs_with_only_api_id_if_application_cannot_be_found() {
        var unknownApi = "unknown";

        logStorageService.initWithConnectionLogs(List.of(connectionLogFixtures.aConnectionLog().toBuilder().apiId(unknownApi).build()));

        var result = cut.execute(
            GraviteeContext.getExecutionContext(),
            new SearchApplicationConnectionLogsUseCase.Input(
                APPLICATION_ID,
                SearchLogsFilters.builder().from(FIRST_FEBRUARY_2020).to(SECOND_FEBRUARY_2020).build()
            )
        );
        assertThat(result.data()).extracting(ConnectionLog::getApi).isEqualTo(List.of(Api.builder().id("1").build()));
    }

    @Test
    void should_return_application_connection_logs_for_apis() {
        logStorageService.initWithConnectionLogs(
            List.of(
                connectionLogFixtures.aConnectionLog().toBuilder().requestId("req1").apiId("api-1").build(),
                connectionLogFixtures.aConnectionLog().toBuilder().requestId("req2").apiId("api-1").build(),
                connectionLogFixtures.aConnectionLog().toBuilder().requestId("req3").apiId("api-2").build(),
                connectionLogFixtures.aConnectionLog().toBuilder().requestId("req4").apiId("already-deleted").build()
            )
        );

        var result = cut.execute(
            GraviteeContext.getExecutionContext(),
            new SearchApplicationConnectionLogsUseCase.Input(
                APPLICATION_ID,
                SearchLogsFilters.builder().apiIds(Set.of("api-1", "api-2")).build()
            )
        );
        assertThat(result.data())
            .extracting(ConnectionLog::getRequestId, ConnectionLog::getApi)
            .containsExactlyInAnyOrder(tuple("req1", API_1), tuple("req2", API_1), tuple("req3", API_2));
    }

    @Test
    void should_return_application_connection_logs_for_plans() {
        logStorageService.initWithConnectionLogs(
            List.of(
                connectionLogFixtures.aConnectionLog().toBuilder().requestId("req1").planId("plan1").build(),
                connectionLogFixtures.aConnectionLog().toBuilder().requestId("req2").planId("plan1").build(),
                connectionLogFixtures.aConnectionLog().toBuilder().requestId("req3").planId("plan2").build(),
                connectionLogFixtures.aConnectionLog().toBuilder().requestId("req4").planId("plan3").build(),
                connectionLogFixtures.aConnectionLog().toBuilder().requestId("req5").planId("plan3").build()
            )
        );

        var result = cut.execute(
            GraviteeContext.getExecutionContext(),
            new SearchApplicationConnectionLogsUseCase.Input(
                APPLICATION_ID,
                SearchLogsFilters.builder().planIds(Set.of("plan1", "plan2")).build()
            )
        );
        assertThat(result.data())
            .extracting(ConnectionLog::getRequestId, ConnectionLog::getPlan)
            .containsExactlyInAnyOrder(tuple("req1", PLAN_1), tuple("req2", PLAN_1), tuple("req3", PLAN_2));
    }

    @Test
    void should_return_application_connection_logs_filtered_by_timestamp_date_range() {
        logStorageService.initWithConnectionLogs(
            List.of(
                connectionLogFixtures.aConnectionLog("req1").toBuilder().timestamp("2020-02-01T20:00:00.00Z").build(),
                connectionLogFixtures.aConnectionLog("req2").toBuilder().timestamp("2020-02-02T20:00:00.00Z").build(),
                connectionLogFixtures.aConnectionLog("req3").toBuilder().timestamp("2020-02-04T20:00:00.00Z").build()
            )
        );

        var result = cut.execute(
            GraviteeContext.getExecutionContext(),
            new SearchApplicationConnectionLogsUseCase.Input(
                APPLICATION_ID,
                SearchLogsFilters.builder().from(FIRST_FEBRUARY_2020).to(FOURTH_FEBRUARY_2020).build(),
                Optional.empty()
            )
        );

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(result.total()).isEqualTo(2);
            soft
                .assertThat(result.data())
                .extracting(ConnectionLog::getRequestId, ConnectionLog::getTimestamp)
                .containsExactly(tuple("req2", "2020-02-02T20:00:00.00Z"), tuple("req1", "2020-02-01T20:00:00.00Z"));
        });
    }

    @Test
    void should_return_application_connection_logs_from_timestamp() {
        logStorageService.initWithConnectionLogs(
            List.of(
                connectionLogFixtures.aConnectionLog("req1").toBuilder().timestamp("2020-02-01T20:00:00.00Z").build(),
                connectionLogFixtures.aConnectionLog("req2").toBuilder().timestamp("2020-02-02T20:00:00.00Z").build(),
                connectionLogFixtures.aConnectionLog("req3").toBuilder().timestamp("2020-02-04T20:00:00.00Z").build()
            )
        );

        var result = cut.execute(
            GraviteeContext.getExecutionContext(),
            new SearchApplicationConnectionLogsUseCase.Input(
                APPLICATION_ID,
                SearchLogsFilters.builder().from(FOURTH_FEBRUARY_2020).build(),
                Optional.empty()
            )
        );

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(result.total()).isOne();
            soft
                .assertThat(result.data())
                .extracting(ConnectionLog::getRequestId, ConnectionLog::getTimestamp)
                .containsExactly(tuple("req3", "2020-02-04T20:00:00.00Z"));
        });
    }

    @Test
    void should_return_application_connection_logs_to_timestamp() {
        logStorageService.initWithConnectionLogs(
            List.of(
                connectionLogFixtures.aConnectionLog("req1").toBuilder().timestamp("2020-02-01T20:00:00.00Z").build(),
                connectionLogFixtures.aConnectionLog("req2").toBuilder().timestamp("2020-02-02T20:00:00.00Z").build(),
                connectionLogFixtures.aConnectionLog("req3").toBuilder().timestamp("2020-02-04T20:00:00.00Z").build()
            )
        );

        var result = cut.execute(
            GraviteeContext.getExecutionContext(),
            new SearchApplicationConnectionLogsUseCase.Input(
                APPLICATION_ID,
                SearchLogsFilters.builder().to(FOURTH_FEBRUARY_2020).build(),
                Optional.empty()
            )
        );

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(result.total()).isEqualTo(2);
            soft
                .assertThat(result.data())
                .extracting(ConnectionLog::getRequestId, ConnectionLog::getTimestamp)
                .containsExactly(tuple("req2", "2020-02-02T20:00:00.00Z"), tuple("req1", "2020-02-01T20:00:00.00Z"));
        });
    }

    @Test
    void should_return_application_connection_logs_for_entrypoint() {
        logStorageService.initWithConnectionLogs(
            List.of(
                connectionLogFixtures.aConnectionLog("req1").toBuilder().entrypointId("http-get").build(),
                connectionLogFixtures.aConnectionLog("req2").toBuilder().entrypointId("http-post").build()
            )
        );
        var result = cut.execute(
            GraviteeContext.getExecutionContext(),
            new SearchApplicationConnectionLogsUseCase.Input(
                APPLICATION_ID,
                SearchLogsFilters.builder().entrypointIds(Set.of("http-get")).build(),
                Optional.empty()
            )
        );

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(result.total()).isEqualTo(1);
            soft.assertThat(result.data()).extracting(ConnectionLog::getRequestId).containsExactly("req1");
        });
    }

    @Test
    void should_return_logs_with_metadata_with_api_and_plan_found() {
        logStorageService.initWithConnectionLogs(
            List.of(connectionLogFixtures.aConnectionLog().toBuilder().requestId("req1").apiId("api-1").planId("plan1").build())
        );

        var result = cut.execute(
            GraviteeContext.getExecutionContext(),
            new SearchApplicationConnectionLogsUseCase.Input(
                APPLICATION_ID,
                SearchLogsFilters.builder().apiIds(Set.of("api-1")).planIds(Set.of("plan1")).build()
            )
        );
        assertThat(result.data())
            .extracting(ConnectionLog::getRequestId, ConnectionLog::getApi, ConnectionLog::getPlan)
            .containsExactlyInAnyOrder(tuple("req1", API_1, PLAN_1));
        assertThat(result.metadata())
            .isEqualTo(
                Map.of(
                    API_1.getId(),
                    Map.of("name", API_1.getName(), "version", API_1.getVersion()),
                    PLAN_1.getId(),
                    Map.of("name", PLAN_1.getName())
                )
            );
    }

    @Test
    void should_return_logs_with_metadata_with_api_and_plan_not_found() {
        String apiNotFoundId = "not-found";
        String planNotFoundId = "missing";
        logStorageService.initWithConnectionLogs(
            List.of(
                connectionLogFixtures.aConnectionLog().toBuilder().requestId("req1").apiId(apiNotFoundId).planId(planNotFoundId).build()
            )
        );

        var result = cut.execute(
            GraviteeContext.getExecutionContext(),
            new SearchApplicationConnectionLogsUseCase.Input(APPLICATION_ID, SearchLogsFilters.builder().build())
        );
        assertThat(result.data())
            .extracting(ConnectionLog::getRequestId, ConnectionLog::getApi, ConnectionLog::getPlan)
            .containsExactlyInAnyOrder(tuple("req1", Api.builder().id("1").build(), Plan.builder().id("1").build()));
        assertThat(result.metadata())
            .isEqualTo(
                Map.of(
                    apiNotFoundId,
                    Map.of("name", "Unknown API (not found)", "unknown", "true"),
                    planNotFoundId,
                    Map.of("name", "Unknown plan", "unknown", "true")
                )
            );
    }
}
