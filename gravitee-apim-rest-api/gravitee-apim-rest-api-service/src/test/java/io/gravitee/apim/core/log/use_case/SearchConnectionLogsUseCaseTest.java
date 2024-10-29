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

import static io.gravitee.apim.core.log.use_case.SearchConnectionLogsUseCase.UNKNOWN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import fixtures.core.model.PlanFixtures;
import fixtures.repository.ConnectionLogFixtures;
import inmemory.ApplicationCrudServiceInMemory;
import inmemory.ConnectionLogsCrudServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.PlanCrudServiceInMemory;
import io.gravitee.apim.core.log.use_case.SearchConnectionLogsUseCase.Input;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import io.gravitee.rest.api.model.analytics.SearchLogsFilters;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.model.v4.log.connection.ConnectionLogModel;
import io.gravitee.rest.api.model.v4.plan.BasePlanEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SearchConnectionLogsUseCaseTest {

    private static final String API_ID = "f1608475-dd77-4603-a084-75dd775603e9";
    private static final Plan PLAN_1 = PlanFixtures.aPlanHttpV4().toBuilder().id("plan1").name("1st plan").build();
    private static final Plan PLAN_2 = PlanFixtures.aPlanHttpV4().toBuilder().id("plan2").name("2nd plan").build();
    private static final BaseApplicationEntity APPLICATION_1 = BaseApplicationEntity
        .builder()
        .id("app1")
        .name("an application name")
        .build();
    private static final BaseApplicationEntity APPLICATION_2 = BaseApplicationEntity
        .builder()
        .id("app2")
        .name("another application name")
        .build();
    private static final Long FIRST_FEBRUARY_2020 = Instant.parse("2020-02-01T00:01:00.00Z").toEpochMilli();
    private static final Long SECOND_FEBRUARY_2020 = Instant.parse("2020-02-02T23:59:59.00Z").toEpochMilli();
    private static final Long FOURTH_FEBRUARY_2020 = Instant.parse("2020-02-04T00:01:00.00Z").toEpochMilli();
    private static final Long FIFTH_FEBRUARY_2020 = Instant.parse("2020-02-05T00:01:00.00Z").toEpochMilli();

    ConnectionLogFixtures connectionLogFixtures = new ConnectionLogFixtures(API_ID, APPLICATION_1.getId(), PLAN_1.getId());

    ConnectionLogsCrudServiceInMemory logStorageService = new ConnectionLogsCrudServiceInMemory();
    PlanCrudServiceInMemory planStorageService = new PlanCrudServiceInMemory();
    ApplicationCrudServiceInMemory applicationStorageService = new ApplicationCrudServiceInMemory();

    SearchConnectionLogsUseCase usecase;

    @BeforeEach
    void setUp() {
        usecase = new SearchConnectionLogsUseCase(logStorageService, planStorageService, applicationStorageService);

        planStorageService.initWith(List.of(PLAN_1, PLAN_2));
        applicationStorageService.initWith(List.of(APPLICATION_1, APPLICATION_2));
    }

    @AfterEach
    void tearDown() {
        Stream.of(logStorageService, planStorageService, applicationStorageService).forEach(InMemoryAlternative::reset);

        GraviteeContext.cleanContext();
    }

    @Test
    void should_return_connection_logs_of_an_api() {
        logStorageService.initWithConnectionLogs(
            List.of(
                connectionLogFixtures.aConnectionLog("req1").toBuilder().build(),
                connectionLogFixtures.aConnectionLog().toBuilder().apiId("other-api").planId("other-plan").build()
            )
        );

        var result = usecase.execute(
            GraviteeContext.getExecutionContext(),
            new Input(API_ID, SearchLogsFilters.builder().from(FIRST_FEBRUARY_2020).to(SECOND_FEBRUARY_2020).build())
        );

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(result.total()).isOne();
            soft
                .assertThat(result.data())
                .isEqualTo(
                    List.of(
                        ConnectionLogModel
                            .builder()
                            .requestId("req1")
                            .apiId(API_ID)
                            .application(APPLICATION_1)
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
    void should_return_api_connection_logs_sorted_by_desc_timestamp() {
        logStorageService.initWithConnectionLogs(
            List.of(
                connectionLogFixtures.aConnectionLog("req1").toBuilder().timestamp("2020-02-01T20:00:00.00Z").build(),
                connectionLogFixtures.aConnectionLog("req2").toBuilder().timestamp("2020-02-02T20:00:00.00Z").build(),
                connectionLogFixtures.aConnectionLog("req3").toBuilder().timestamp("2020-02-04T20:00:00.00Z").build()
            )
        );

        var result = usecase.execute(
            GraviteeContext.getExecutionContext(),
            new Input(API_ID, SearchLogsFilters.builder().from(FIRST_FEBRUARY_2020).to(FIFTH_FEBRUARY_2020).build())
        );

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(result.total()).isEqualTo(3);
            soft
                .assertThat(result.data())
                .extracting(ConnectionLogModel::getRequestId, ConnectionLogModel::getTimestamp)
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

        var result = usecase.execute(
            GraviteeContext.getExecutionContext(),
            new Input(
                API_ID,
                SearchLogsFilters.builder().from(FIRST_FEBRUARY_2020).to(SECOND_FEBRUARY_2020).build(),
                new PageableImpl(pageNumber, pageSize)
            )
        );

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(result.total()).isEqualTo(expectedTotal);
            soft.assertThat(result.data()).extracting(ConnectionLogModel::getRequestId).containsExactly("5", "6", "7", "8", "9");
        });
    }

    @Test
    void should_return_api_connection_logs_with_only_plan_id_if_plan_cannot_be_found() {
        var unknownPlan = "unknown";

        logStorageService.initWithConnectionLogs(List.of(connectionLogFixtures.aConnectionLog().toBuilder().planId(unknownPlan).build()));

        var result = usecase.execute(
            GraviteeContext.getExecutionContext(),
            new Input(API_ID, SearchLogsFilters.builder().from(FIRST_FEBRUARY_2020).to(SECOND_FEBRUARY_2020).build())
        );
        assertThat(result.data())
            .extracting(ConnectionLogModel::getPlan)
            .isEqualTo(List.of(BasePlanEntity.builder().id(unknownPlan).name(UNKNOWN).build()));
    }

    @Test
    void should_return_api_connection_logs_with_only_available_info() {
        logStorageService.initWithConnectionLogs(
            List.of(connectionLogFixtures.aConnectionLog().toBuilder().planId(null).clientIdentifier(null).applicationId("1").build())
        );

        var result = usecase.execute(
            GraviteeContext.getExecutionContext(),
            new Input(API_ID, SearchLogsFilters.builder().from(FIRST_FEBRUARY_2020).to(SECOND_FEBRUARY_2020).build())
        );
        assertThat(result.data())
            .extracting(ConnectionLogModel::getPlan)
            .isEqualTo(List.of(BasePlanEntity.builder().id(null).name(UNKNOWN).build()));
    }

    @Test
    void should_return_api_connection_logs_with_only_application_id_if_application_cannot_be_found() {
        var unknownApp = "unknown";

        logStorageService.initWithConnectionLogs(
            List.of(connectionLogFixtures.aConnectionLog().toBuilder().applicationId(unknownApp).build())
        );

        var result = usecase.execute(
            GraviteeContext.getExecutionContext(),
            new Input(API_ID, SearchLogsFilters.builder().from(FIRST_FEBRUARY_2020).to(SECOND_FEBRUARY_2020).build())
        );
        assertThat(result.data())
            .extracting(ConnectionLogModel::getApplication)
            .isEqualTo(List.of(BaseApplicationEntity.builder().id(unknownApp).name(UNKNOWN).build()));
    }

    @Test
    void should_return_api_connection_logs_for_applications() {
        logStorageService.initWithConnectionLogs(
            List.of(
                connectionLogFixtures.aConnectionLog().toBuilder().requestId("req1").applicationId("app1").build(),
                connectionLogFixtures.aConnectionLog().toBuilder().requestId("req2").applicationId("app1").build(),
                connectionLogFixtures.aConnectionLog().toBuilder().requestId("req3").applicationId("app2").build(),
                connectionLogFixtures.aConnectionLog().toBuilder().requestId("req4").applicationId("app3").build(),
                connectionLogFixtures.aConnectionLog().toBuilder().requestId("req5").applicationId("app3").build()
            )
        );

        var result = usecase.execute(
            GraviteeContext.getExecutionContext(),
            new Input(API_ID, SearchLogsFilters.builder().applicationIds(Set.of("app1", "app2")).build())
        );
        assertThat(result.data())
            .extracting(ConnectionLogModel::getRequestId, ConnectionLogModel::getApplication)
            .containsExactlyInAnyOrder(
                tuple("req1", BaseApplicationEntity.builder().id("app1").name("an application name").build()),
                tuple("req2", BaseApplicationEntity.builder().id("app1").name("an application name").build()),
                tuple("req3", BaseApplicationEntity.builder().id("app2").name("another application name").build())
            );
    }

    @Test
    void should_return_api_connection_logs_for_plans() {
        logStorageService.initWithConnectionLogs(
            List.of(
                connectionLogFixtures.aConnectionLog().toBuilder().requestId("req1").planId("plan1").build(),
                connectionLogFixtures.aConnectionLog().toBuilder().requestId("req2").planId("plan1").build(),
                connectionLogFixtures.aConnectionLog().toBuilder().requestId("req3").planId("plan2").build(),
                connectionLogFixtures.aConnectionLog().toBuilder().requestId("req4").planId("plan3").build(),
                connectionLogFixtures.aConnectionLog().toBuilder().requestId("req5").planId("plan3").build()
            )
        );

        var result = usecase.execute(
            GraviteeContext.getExecutionContext(),
            new Input(API_ID, SearchLogsFilters.builder().planIds(Set.of("plan1", "plan2")).build())
        );
        assertThat(result.data())
            .extracting(ConnectionLogModel::getRequestId, ConnectionLogModel::getPlan)
            .containsExactlyInAnyOrder(
                tuple("req1", PlanFixtures.aPlanHttpV4().toBuilder().id(PLAN_1.getId()).name(PLAN_1.getName()).build()),
                tuple("req2", PlanFixtures.aPlanHttpV4().toBuilder().id(PLAN_1.getId()).name(PLAN_1.getName()).build()),
                tuple("req3", PlanFixtures.aPlanHttpV4().toBuilder().id(PLAN_2.getId()).name(PLAN_2.getName()).build())
            );
    }

    @Test
    void should_return_api_connection_logs_filtered_by_timestamp_date_range() {
        logStorageService.initWithConnectionLogs(
            List.of(
                connectionLogFixtures.aConnectionLog("req1").toBuilder().timestamp("2020-02-01T20:00:00.00Z").build(),
                connectionLogFixtures.aConnectionLog("req2").toBuilder().timestamp("2020-02-02T20:00:00.00Z").build(),
                connectionLogFixtures.aConnectionLog("req3").toBuilder().timestamp("2020-02-04T20:00:00.00Z").build()
            )
        );

        var result = usecase.execute(
            GraviteeContext.getExecutionContext(),
            new Input(API_ID, SearchLogsFilters.builder().from(FIRST_FEBRUARY_2020).to(FOURTH_FEBRUARY_2020).build())
        );

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(result.total()).isEqualTo(2);
            soft
                .assertThat(result.data())
                .extracting(ConnectionLogModel::getRequestId, ConnectionLogModel::getTimestamp)
                .containsExactly(tuple("req2", "2020-02-02T20:00:00.00Z"), tuple("req1", "2020-02-01T20:00:00.00Z"));
        });
    }

    @Test
    void should_return_api_connection_logs_from_timestamp() {
        logStorageService.initWithConnectionLogs(
            List.of(
                connectionLogFixtures.aConnectionLog("req1").toBuilder().timestamp("2020-02-01T20:00:00.00Z").build(),
                connectionLogFixtures.aConnectionLog("req2").toBuilder().timestamp("2020-02-02T20:00:00.00Z").build(),
                connectionLogFixtures.aConnectionLog("req3").toBuilder().timestamp("2020-02-04T20:00:00.00Z").build()
            )
        );

        var result = usecase.execute(
            GraviteeContext.getExecutionContext(),
            new Input(API_ID, SearchLogsFilters.builder().from(FOURTH_FEBRUARY_2020).build())
        );

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(result.total()).isOne();
            soft
                .assertThat(result.data())
                .extracting(ConnectionLogModel::getRequestId, ConnectionLogModel::getTimestamp)
                .containsExactly(tuple("req3", "2020-02-04T20:00:00.00Z"));
        });
    }

    @Test
    void should_return_api_connection_logs_to_timestamp() {
        logStorageService.initWithConnectionLogs(
            List.of(
                connectionLogFixtures.aConnectionLog("req1").toBuilder().timestamp("2020-02-01T20:00:00.00Z").build(),
                connectionLogFixtures.aConnectionLog("req2").toBuilder().timestamp("2020-02-02T20:00:00.00Z").build(),
                connectionLogFixtures.aConnectionLog("req3").toBuilder().timestamp("2020-02-04T20:00:00.00Z").build()
            )
        );

        var result = usecase.execute(
            GraviteeContext.getExecutionContext(),
            new Input(API_ID, SearchLogsFilters.builder().to(FOURTH_FEBRUARY_2020).build())
        );

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(result.total()).isEqualTo(2);
            soft
                .assertThat(result.data())
                .extracting(ConnectionLogModel::getRequestId, ConnectionLogModel::getTimestamp)
                .containsExactly(tuple("req2", "2020-02-02T20:00:00.00Z"), tuple("req1", "2020-02-01T20:00:00.00Z"));
        });
    }

    @Test
    void should_return_api_connection_logs_for_entrypoint() {
        logStorageService.initWithConnectionLogs(
            List.of(
                connectionLogFixtures.aConnectionLog("req1").toBuilder().entrypointId("http-get").build(),
                connectionLogFixtures.aConnectionLog("req2").toBuilder().entrypointId("http-post").build()
            )
        );
        var result = usecase.execute(
            GraviteeContext.getExecutionContext(),
            new Input(API_ID, SearchLogsFilters.builder().entrypointIds(Set.of("http-get")).build())
        );

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(result.total()).isEqualTo(1);
            soft.assertThat(result.data()).extracting(ConnectionLogModel::getRequestId).containsExactly("req1");
        });
    }
}
