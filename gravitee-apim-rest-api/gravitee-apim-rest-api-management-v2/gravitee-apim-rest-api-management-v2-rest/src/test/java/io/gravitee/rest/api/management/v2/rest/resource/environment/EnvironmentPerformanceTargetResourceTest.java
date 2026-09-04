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

import static assertions.MAPIAssertions.assertThat;
import static jakarta.ws.rs.client.Entity.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import fixtures.core.model.ApiFixtures;
import fixtures.core.model.PerformanceTargetFixtures;
import inmemory.InMemoryAlternative;
import inmemory.PerformanceTargetCrudServiceInMemory;
import inmemory.PerformanceTargetEvaluationCrudServiceInMemory;
import io.gravitee.apim.core.performance_target.model.PerformanceTargetEvaluation;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.management.v2.rest.model.Error;
import io.gravitee.rest.api.management.v2.rest.model.PerformanceTarget;
import io.gravitee.rest.api.management.v2.rest.model.PerformanceTargetDeviation;
import io.gravitee.rest.api.management.v2.rest.model.PerformanceTargetEvaluationStatus;
import io.gravitee.rest.api.management.v2.rest.model.PerformanceTargetEvaluationsResponse;
import io.gravitee.rest.api.management.v2.rest.model.PerformanceTargetOperator;
import io.gravitee.rest.api.management.v2.rest.model.PerformanceTargetRule;
import io.gravitee.rest.api.management.v2.rest.model.PerformanceTargetRuleResult;
import io.gravitee.rest.api.management.v2.rest.model.PerformanceTargetSubject;
import io.gravitee.rest.api.management.v2.rest.model.UpdatePerformanceTarget;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.UuidString;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.WebTarget;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class EnvironmentPerformanceTargetResourceTest extends AbstractResourceTest {

    private static final String ENVIRONMENT = "environment-id";
    private static final String A2A_API = "a2a-api";
    private static final String LLM_API = "llm-api";
    private static final String TARGET_ID = "target-id";
    private static final Instant NOW = Instant.parse("2021-06-01T10:00:00.00Z");
    private static final Instant CREATED_AT = Instant.parse("2020-02-03T20:22:02.00Z");

    WebTarget target;

    @Inject
    PerformanceTargetCrudServiceInMemory performanceTargetCrudService;

    @Inject
    PerformanceTargetEvaluationCrudServiceInMemory performanceTargetEvaluationCrudService;

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/performance-targets/" + TARGET_ID;
    }

    @BeforeEach
    void setup() {
        target = rootTarget();

        var environmentEntity = EnvironmentEntity.builder().id(ENVIRONMENT).organizationId(ORGANIZATION).build();
        when(environmentService.findById(ENVIRONMENT)).thenReturn(environmentEntity);
        when(environmentService.findByOrgAndIdOrHrid(ORGANIZATION, ENVIRONMENT)).thenReturn(environmentEntity);

        GraviteeContext.setCurrentEnvironment(ENVIRONMENT);
        GraviteeContext.setCurrentOrganization(ORGANIZATION);

        apiCrudService.initWith(
            List.of(
                ApiFixtures.anA2AProxyApiV4().toBuilder().id(A2A_API).build(),
                ApiFixtures.aLLMProxyApiV4().toBuilder().id(LLM_API).build()
            )
        );
        TimeProvider.overrideClock(Clock.fixed(NOW, ZoneId.systemDefault()));
    }

    @AfterEach
    @Override
    public void tearDown() {
        super.tearDown();
        UuidString.reset();
        TimeProvider.reset();
        GraviteeContext.cleanContext();
        Stream.of(apiCrudService, performanceTargetCrudService, performanceTargetEvaluationCrudService).forEach(InMemoryAlternative::reset);
    }

    @Nested
    class Get {

        @Test
        void should_return_the_target() {
            performanceTargetCrudService.initWith(List.of(PerformanceTargetFixtures.aTarget(TARGET_ID)));

            var response = target.request().get();

            assertThat(response)
                .hasStatus(HttpStatusCode.OK_200)
                .asEntity(PerformanceTarget.class)
                .isEqualTo(
                    new PerformanceTarget()
                        .id(TARGET_ID)
                        .subject(new PerformanceTargetSubject().apiIds(List.of(A2A_API)).reference(A2A_API))
                        .windowSeconds(900L)
                        .intervalSeconds(300L)
                        .minSampleSize(20)
                        .rules(List.of(aLatencyRule()))
                        .createdAt(CREATED_AT.atOffset(ZoneOffset.UTC))
                        .updatedAt(CREATED_AT.atOffset(ZoneOffset.UTC))
                );
        }

        @Test
        void should_return_404_when_the_target_does_not_exist() {
            var response = target.request().get();

            assertThat(response).hasStatus(HttpStatusCode.NOT_FOUND_404);
        }

        @Test
        void should_return_404_when_the_target_belongs_to_another_environment() {
            performanceTargetCrudService.initWith(
                List.of(PerformanceTargetFixtures.aTarget(TARGET_ID).toBuilder().environmentId("other-environment").build())
            );

            var response = target.request().get();

            assertThat(response).hasStatus(HttpStatusCode.NOT_FOUND_404);
        }

        @Test
        void should_return_403_without_read_permission() {
            performanceTargetCrudService.initWith(List.of(PerformanceTargetFixtures.aTarget(TARGET_ID)));

            shouldReturn403(RolePermission.ENVIRONMENT_API, ENVIRONMENT, RolePermissionAction.READ, () -> target.request().get());
        }
    }

    @Nested
    class Update {

        @Test
        void should_replace_subject_schedule_and_rules_and_keep_identity() {
            performanceTargetCrudService.initWith(List.of(PerformanceTargetFixtures.aTarget(TARGET_ID)));
            var request = new UpdatePerformanceTarget()
                .subject(new PerformanceTargetSubject().apiIds(List.of(A2A_API, LLM_API)).reference("agent-42"))
                .windowSeconds(3600L)
                .intervalSeconds(600L)
                .minSampleSize(5)
                .rules(List.of(aLatencyRule().threshold(1500.0)));

            var response = target.request().put(json(request));

            assertThat(response)
                .hasStatus(HttpStatusCode.OK_200)
                .asEntity(PerformanceTarget.class)
                .isEqualTo(
                    new PerformanceTarget()
                        .id(TARGET_ID)
                        .subject(new PerformanceTargetSubject().apiIds(List.of(A2A_API, LLM_API)).reference("agent-42"))
                        .windowSeconds(3600L)
                        .intervalSeconds(600L)
                        .minSampleSize(5)
                        .rules(List.of(aLatencyRule().threshold(1500.0)))
                        .createdAt(CREATED_AT.atOffset(ZoneOffset.UTC))
                        .updatedAt(NOW.atOffset(ZoneOffset.UTC))
                );
            assertThat(performanceTargetCrudService.storage())
                .singleElement()
                .satisfies(stored -> {
                    assertThat(stored.environmentId()).isEqualTo(ENVIRONMENT);
                    assertThat(stored.subject().reference()).isEqualTo("agent-42");
                });
        }

        @Test
        void should_reject_an_invalid_rule_with_the_failing_rule_index_and_keep_the_target() {
            performanceTargetCrudService.initWith(List.of(PerformanceTargetFixtures.aTarget(TARGET_ID)));
            var request = new UpdatePerformanceTarget()
                .subject(new PerformanceTargetSubject().apiIds(List.of(A2A_API)).reference(A2A_API))
                .rules(List.of(aLatencyRule().measure("PERCENTAGE")));

            var response = target.request().put(json(request));

            assertThat(response).hasStatus(HttpStatusCode.BAD_REQUEST_400);
            assertThat(response.readEntity(Error.class).getParameters()).containsEntry("ruleIndex", "0");
            assertThat(performanceTargetCrudService.storage()).containsExactly(PerformanceTargetFixtures.aTarget(TARGET_ID));
        }

        @Test
        void should_return_404_when_the_target_does_not_exist() {
            var request = new UpdatePerformanceTarget()
                .subject(new PerformanceTargetSubject().apiIds(List.of(A2A_API)).reference(A2A_API))
                .rules(List.of(aLatencyRule()));

            var response = target.request().put(json(request));

            assertThat(response).hasStatus(HttpStatusCode.NOT_FOUND_404);
        }

        @Test
        void should_return_403_without_update_permission() {
            var request = new UpdatePerformanceTarget()
                .subject(new PerformanceTargetSubject().apiIds(List.of(A2A_API)).reference(A2A_API))
                .rules(List.of(aLatencyRule()));

            shouldReturn403(RolePermission.ENVIRONMENT_API, ENVIRONMENT, RolePermissionAction.UPDATE, () ->
                target.request().put(json(request))
            );
        }
    }

    @Nested
    class Delete {

        @Test
        void should_delete_the_target_and_its_evaluations() {
            performanceTargetCrudService.initWith(
                List.of(PerformanceTargetFixtures.aTarget(TARGET_ID), PerformanceTargetFixtures.aTarget("other-target"))
            );
            performanceTargetEvaluationCrudService.initWith(
                List.of(
                    PerformanceTargetFixtures.anEvaluation("eval-1", TARGET_ID, PerformanceTargetEvaluation.Status.PASS),
                    PerformanceTargetFixtures.anEvaluation("eval-2", TARGET_ID, PerformanceTargetEvaluation.Status.BREACH),
                    PerformanceTargetFixtures.anEvaluation("eval-other", "other-target", PerformanceTargetEvaluation.Status.PASS)
                )
            );

            var response = target.request().delete();

            assertThat(response).hasStatus(HttpStatusCode.NO_CONTENT_204);
            assertThat(performanceTargetCrudService.storage())
                .extracting(t -> t.id())
                .containsExactly("other-target");
            assertThat(performanceTargetEvaluationCrudService.storage())
                .extracting(e -> e.id())
                .containsExactly("eval-other");
        }

        @Test
        void should_return_404_when_the_target_does_not_exist() {
            var response = target.request().delete();

            assertThat(response).hasStatus(HttpStatusCode.NOT_FOUND_404);
        }

        @Test
        void should_return_403_without_delete_permission() {
            performanceTargetCrudService.initWith(List.of(PerformanceTargetFixtures.aTarget(TARGET_ID)));

            shouldReturn403(RolePermission.ENVIRONMENT_API, ENVIRONMENT, RolePermissionAction.DELETE, () -> target.request().delete());
        }
    }

    @Nested
    class Evaluations {

        private static final Instant T1 = Instant.parse("2021-06-01T09:00:00.00Z");
        private static final Instant T2 = Instant.parse("2021-06-01T09:05:00.00Z");
        private static final Instant T3 = Instant.parse("2021-06-01T09:10:00.00Z");

        @BeforeEach
        void givenTargetWithHistory() {
            performanceTargetCrudService.initWith(List.of(PerformanceTargetFixtures.aTarget(TARGET_ID)));
            performanceTargetEvaluationCrudService.initWith(
                List.of(
                    PerformanceTargetFixtures.anEvaluation("eval-1", TARGET_ID, PerformanceTargetEvaluation.Status.PASS, T1)
                        .toBuilder()
                        .latest(false)
                        .build(),
                    PerformanceTargetFixtures.anEvaluation("eval-2", TARGET_ID, PerformanceTargetEvaluation.Status.PASS, T2)
                        .toBuilder()
                        .latest(false)
                        .build(),
                    PerformanceTargetFixtures.anEvaluation("eval-3", TARGET_ID, PerformanceTargetEvaluation.Status.BREACH, T3),
                    PerformanceTargetFixtures.anEvaluation("eval-other", "other-target", PerformanceTargetEvaluation.Status.PASS, T3)
                )
            );
        }

        @Test
        void should_return_the_latest_evaluation_with_its_rule_results() {
            var response = target.path("evaluations/latest").request().get();

            assertThat(response)
                .hasStatus(HttpStatusCode.OK_200)
                .asEntity(io.gravitee.rest.api.management.v2.rest.model.PerformanceTargetEvaluation.class)
                .isEqualTo(
                    new io.gravitee.rest.api.management.v2.rest.model.PerformanceTargetEvaluation()
                        .id("eval-3")
                        .targetId(TARGET_ID)
                        .reference(A2A_API)
                        .status(PerformanceTargetEvaluationStatus.BREACH)
                        .rules(
                            List.of(
                                new PerformanceTargetRuleResult()
                                    .metric("HTTP_GATEWAY_RESPONSE_TIME")
                                    .measure("P95")
                                    .operator(PerformanceTargetOperator.LTE)
                                    .threshold(2000.0)
                                    .observed(4810.0)
                                    .deviation(new PerformanceTargetDeviation().absolute(2810.0).ratio(1.405))
                                    .sampleCount(63L)
                                    .status(PerformanceTargetEvaluationStatus.BREACH)
                            )
                        )
                        .windowFrom(T3.minus(Duration.ofMinutes(15)).atOffset(ZoneOffset.UTC))
                        .windowTo(T3.atOffset(ZoneOffset.UTC))
                        .coveredApiIds(List.of(A2A_API))
                        .evaluatedAt(T3.atOffset(ZoneOffset.UTC))
                );
        }

        @Test
        void should_return_404_when_the_target_has_not_been_evaluated_yet() {
            performanceTargetEvaluationCrudService.reset();

            var response = target.path("evaluations/latest").request().get();

            assertThat(response).hasStatus(HttpStatusCode.NOT_FOUND_404);
        }

        @Test
        void should_return_404_for_the_latest_evaluation_of_an_unknown_target() {
            performanceTargetCrudService.reset();

            var response = target.path("evaluations/latest").request().get();

            assertThat(response).hasStatus(HttpStatusCode.NOT_FOUND_404);
        }

        @Test
        void should_page_the_history_most_recent_first() {
            var response = target.path("evaluations").queryParam("page", 1).queryParam("perPage", 2).request().get();

            assertThat(response).hasStatus(HttpStatusCode.OK_200);
            var body = response.readEntity(PerformanceTargetEvaluationsResponse.class);
            assertThat(body.getData())
                .extracting(io.gravitee.rest.api.management.v2.rest.model.PerformanceTargetEvaluation::getId)
                .containsExactly("eval-3", "eval-2");
            assertThat(body.getPagination().getTotalCount()).isEqualTo(3L);
            assertThat(body.getPagination().getPage()).isEqualTo(1);
            assertThat(body.getPagination().getPerPage()).isEqualTo(2);
            assertThat(body.getPagination().getPageCount()).isEqualTo(2);
            assertThat(body.getPagination().getPageItemsCount()).isEqualTo(2);
        }

        @Test
        void should_return_404_for_the_history_of_an_unknown_target() {
            performanceTargetCrudService.reset();

            var response = target.path("evaluations").request().get();

            assertThat(response).hasStatus(HttpStatusCode.NOT_FOUND_404);
        }

        @Test
        void should_return_403_without_read_permission() {
            shouldReturn403(RolePermission.ENVIRONMENT_API, ENVIRONMENT, RolePermissionAction.READ, () ->
                target.path("evaluations").request().get()
            );
        }
    }

    @Nested
    class EvaluateNow {

        @BeforeEach
        void givenTarget() {
            performanceTargetCrudService.initWith(List.of(PerformanceTargetFixtures.aTarget(TARGET_ID)));
        }

        @Test
        void should_evaluate_store_as_latest_and_return_the_evaluation() {
            UuidString.overrideGenerator(() -> "evaluation-id");

            var response = target.path("_evaluate").request().post(null);

            assertThat(response).hasStatus(HttpStatusCode.OK_200);
            var evaluation = response.readEntity(io.gravitee.rest.api.management.v2.rest.model.PerformanceTargetEvaluation.class);
            assertThat(evaluation.getId()).isEqualTo("evaluation-id");
            assertThat(evaluation.getTargetId()).isEqualTo(TARGET_ID);
            assertThat(evaluation.getStatus()).isEqualTo(PerformanceTargetEvaluationStatus.PASS);
            assertThat(evaluation.getWindowFrom()).isEqualTo(NOW.minus(Duration.ofMinutes(15)).atOffset(ZoneOffset.UTC));
            assertThat(evaluation.getEvaluatedAt()).isEqualTo(NOW.atOffset(ZoneOffset.UTC));
            assertThat(performanceTargetEvaluationCrudService.storage())
                .singleElement()
                .satisfies(stored -> {
                    assertThat(stored.id()).isEqualTo("evaluation-id");
                    assertThat(stored.latest()).isTrue();
                });
        }

        @Test
        void should_refuse_with_429_and_retry_after_when_evaluated_less_than_30_seconds_ago() {
            performanceTargetEvaluationCrudService.initWith(
                List.of(
                    PerformanceTargetFixtures.anEvaluation(
                        "recent",
                        TARGET_ID,
                        PerformanceTargetEvaluation.Status.PASS,
                        NOW.minusSeconds(10)
                    )
                )
            );

            var response = target.path("_evaluate").request().post(null);

            assertThat(response).hasStatus(HttpStatusCode.TOO_MANY_REQUESTS_429).hasHeader("Retry-After", "20");
            assertThat(response.readEntity(Error.class).getMessage()).contains(TARGET_ID);
            assertThat(performanceTargetEvaluationCrudService.storage())
                .extracting(e -> e.id())
                .containsExactly("recent");
        }

        @Test
        void should_evaluate_again_once_30_seconds_have_passed_and_replace_the_latest() {
            performanceTargetEvaluationCrudService.initWith(
                List.of(
                    PerformanceTargetFixtures.anEvaluation(
                        "previous",
                        TARGET_ID,
                        PerformanceTargetEvaluation.Status.BREACH,
                        NOW.minusSeconds(30)
                    )
                )
            );

            var response = target.path("_evaluate").request().post(null);

            assertThat(response).hasStatus(HttpStatusCode.OK_200);
            assertThat(performanceTargetEvaluationCrudService.storage())
                .filteredOn(PerformanceTargetEvaluation::latest)
                .singleElement()
                .satisfies(latest -> assertThat(latest.evaluatedAt()).isEqualTo(NOW));
        }

        @Test
        void should_return_404_when_the_target_does_not_exist() {
            performanceTargetCrudService.reset();

            var response = target.path("_evaluate").request().post(null);

            assertThat(response).hasStatus(HttpStatusCode.NOT_FOUND_404);
        }

        @Test
        void should_return_403_without_update_permission() {
            shouldReturn403(RolePermission.ENVIRONMENT_API, ENVIRONMENT, RolePermissionAction.UPDATE, () ->
                target.path("_evaluate").request().post(null)
            );
        }
    }

    private static PerformanceTargetRule aLatencyRule() {
        return new PerformanceTargetRule()
            .metric("HTTP_GATEWAY_RESPONSE_TIME")
            .measure("P95")
            .operator(PerformanceTargetOperator.LTE)
            .threshold(2000.0);
    }
}
