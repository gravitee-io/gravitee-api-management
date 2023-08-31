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
package io.gravitee.rest.api.usecase.log;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import fixtures.repository.ConnectionLogFixtures;
import inmemory.ApplicationStorageServiceInMemory;
import inmemory.InMemoryStorageService;
import inmemory.LogStorageServiceInMemory;
import inmemory.PlanStorageServiceInMemory;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.model.v4.log.ConnectionLogModel;
import io.gravitee.rest.api.model.v4.plan.BasePlanEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.usecase.log.SearchConnectionLogUsecase.Request;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SearchConnectionLogUsecaseTest {

    private static final String API_ID = "f1608475-dd77-4603-a084-75dd775603e9";
    private static final BasePlanEntity PLAN = BasePlanEntity.builder().id("plan1").name("1st plan").build();
    private static final BaseApplicationEntity APPLICATION = BaseApplicationEntity.builder().id("app1").name("an application name").build();
    private static final String USER_ID = "userId";

    ConnectionLogFixtures connectionLogFixtures = new ConnectionLogFixtures(API_ID, APPLICATION.getId(), PLAN.getId());

    LogStorageServiceInMemory logStorageService = new LogStorageServiceInMemory();
    PlanStorageServiceInMemory planStorageService = new PlanStorageServiceInMemory();
    ApplicationStorageServiceInMemory applicationStorageService = new ApplicationStorageServiceInMemory();

    SearchConnectionLogUsecase usecase;

    @BeforeEach
    void setUp() {
        usecase = new SearchConnectionLogUsecase(logStorageService, planStorageService, applicationStorageService);

        planStorageService.initWith(List.of(PLAN));
        applicationStorageService.initWith(List.of(APPLICATION));
    }

    @AfterEach
    void tearDown() {
        Stream.of(logStorageService, planStorageService, applicationStorageService).forEach(InMemoryStorageService::reset);

        GraviteeContext.cleanContext();
    }

    @Test
    void should_return_connection_logs_of_an_api() {
        logStorageService.initWith(
            List.of(
                connectionLogFixtures.aConnectionLog("req1").toBuilder().build(),
                connectionLogFixtures.aConnectionLog().toBuilder().apiId("other-api").planId("other-plan").build()
            )
        );

        var result = usecase.execute(GraviteeContext.getExecutionContext(), new Request(API_ID, USER_ID));

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
                            .application(APPLICATION)
                            .plan(PLAN)
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
        logStorageService.initWith(
            List.of(
                connectionLogFixtures.aConnectionLog("req1").toBuilder().timestamp("2020-02-01T20:00:00.00Z").build(),
                connectionLogFixtures.aConnectionLog("req2").toBuilder().timestamp("2020-02-02T20:00:00.00Z").build(),
                connectionLogFixtures.aConnectionLog("req3").toBuilder().timestamp("2020-02-04T20:00:00.00Z").build()
            )
        );

        var result = usecase.execute(GraviteeContext.getExecutionContext(), new Request(API_ID, USER_ID));

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
        logStorageService.initWith(
            IntStream.range(0, expectedTotal).mapToObj(i -> connectionLogFixtures.aConnectionLog(String.valueOf(i))).toList()
        );

        var result = usecase.execute(
            GraviteeContext.getExecutionContext(),
            new Request(API_ID, USER_ID, new PageableImpl(pageNumber, pageSize))
        );

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(result.total()).isEqualTo(expectedTotal);
            soft.assertThat(result.data()).extracting(ConnectionLogModel::getRequestId).containsExactly("5", "6", "7", "8", "9");
        });
    }

    @Test
    void should_return_api_connection_logs_with_only_plan_id_if_plan_cannot_be_found() {
        var unknownPlan = "unknown";

        logStorageService.initWith(List.of(connectionLogFixtures.aConnectionLog().toBuilder().planId(unknownPlan).build()));

        var result = usecase.execute(GraviteeContext.getExecutionContext(), new Request(API_ID, USER_ID));
        assertThat(result.data())
            .extracting(ConnectionLogModel::getPlan)
            .isEqualTo(List.of(BasePlanEntity.builder().id(unknownPlan).name("Unknown plan").build()));
    }

    @Test
    void should_return_api_connection_logs_with_only_application_id_if_application_cannot_be_found() {
        var unknownApp = "unknown";

        logStorageService.initWith(List.of(connectionLogFixtures.aConnectionLog().toBuilder().applicationId(unknownApp).build()));

        var result = usecase.execute(GraviteeContext.getExecutionContext(), new Request(API_ID, USER_ID));
        assertThat(result.data())
            .extracting(ConnectionLogModel::getApplication)
            .isEqualTo(List.of(BaseApplicationEntity.builder().id(unknownApp).name("Unknown application").build()));
    }
}
