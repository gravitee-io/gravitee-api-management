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
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import fixtures.core.model.ApiFixtures;
import fixtures.core.model.PerformanceTargetFixtures;
import inmemory.InMemoryAlternative;
import inmemory.PerformanceTargetCrudServiceInMemory;
import inmemory.PerformanceTargetEvaluationCrudServiceInMemory;
import io.gravitee.apim.core.performance_target.model.PerformanceTargetEvaluation.Status;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.management.v2.rest.model.CreatePerformanceTarget;
import io.gravitee.rest.api.management.v2.rest.model.Error;
import io.gravitee.rest.api.management.v2.rest.model.LatestPerformanceTargetEvaluationsRequest;
import io.gravitee.rest.api.management.v2.rest.model.PerformanceTarget;
import io.gravitee.rest.api.management.v2.rest.model.PerformanceTargetOperator;
import io.gravitee.rest.api.management.v2.rest.model.PerformanceTargetRule;
import io.gravitee.rest.api.management.v2.rest.model.PerformanceTargetSubject;
import io.gravitee.rest.api.management.v2.rest.model.PerformanceTargetsResponse;
import io.gravitee.rest.api.management.v2.rest.model.PerformanceTargetsSummary;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.UuidString;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.WebTarget;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class EnvironmentPerformanceTargetsResourceTest extends AbstractResourceTest {

    private static final String ENVIRONMENT = "environment-id";
    private static final String A2A_API = "a2a-api";
    private static final String LLM_API = "llm-api";
    private static final Instant NOW = Instant.parse("2020-02-03T20:22:02.00Z");

    WebTarget target;

    @Inject
    PerformanceTargetCrudServiceInMemory performanceTargetCrudService;

    @Inject
    PerformanceTargetEvaluationCrudServiceInMemory performanceTargetEvaluationCrudService;

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/performance-targets";
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
    class Create {

        @Test
        void should_create_target_and_return_it_with_its_location() {
            UuidString.overrideGenerator(() -> "generated-id");
            var request = new CreatePerformanceTarget()
                .subject(new PerformanceTargetSubject().apiIds(List.of(A2A_API)).reference(A2A_API))
                .windowSeconds(900L)
                .intervalSeconds(300L)
                .minSampleSize(20)
                .rules(List.of(aLatencyRule()));

            var response = target.request().post(json(request));

            assertThat(response)
                .hasStatus(HttpStatusCode.CREATED_201)
                .hasHeader("Location", target.path("generated-id").getUri().toString())
                .asEntity(PerformanceTarget.class)
                .isEqualTo(
                    new PerformanceTarget()
                        .id("generated-id")
                        .subject(new PerformanceTargetSubject().apiIds(List.of(A2A_API)).reference(A2A_API))
                        .windowSeconds(900L)
                        .intervalSeconds(300L)
                        .minSampleSize(20)
                        .rules(List.of(aLatencyRule()))
                        .createdAt(NOW.atOffset(ZoneOffset.UTC))
                        .updatedAt(NOW.atOffset(ZoneOffset.UTC))
                );
            assertThat(performanceTargetCrudService.storage())
                .extracting(t -> t.id(), t -> t.environmentId(), t -> t.subject().reference())
                .containsExactly(tuple("generated-id", ENVIRONMENT, A2A_API));
        }

        @Test
        void should_reject_a_rule_the_subject_cannot_be_evaluated_on_with_the_failing_rule_index() {
            var llmCostRule = new PerformanceTargetRule()
                .metric("LLM_PROMPT_TOKEN_TOTAL_COST")
                .measure("AVG")
                .operator(PerformanceTargetOperator.LTE)
                .threshold(0.01);
            var request = new CreatePerformanceTarget()
                .subject(new PerformanceTargetSubject().apiIds(List.of(A2A_API)).reference(A2A_API))
                .rules(List.of(aLatencyRule(), llmCostRule));

            var response = target.request().post(json(request));

            assertThat(response).hasStatus(HttpStatusCode.BAD_REQUEST_400);
            var error = response.readEntity(Error.class);
            assertThat(error.getMessage()).contains("LLM_PROMPT_TOKEN_TOTAL_COST").contains("A2A_PROXY");
            assertThat(error.getParameters()).containsEntry("ruleIndex", "1");
            assertThat(performanceTargetCrudService.storage()).isEmpty();
        }

        @Test
        void should_reject_an_unknown_metric_with_the_failing_rule_index() {
            var request = new CreatePerformanceTarget()
                .subject(new PerformanceTargetSubject().apiIds(List.of(A2A_API)).reference(A2A_API))
                .rules(List.of(aLatencyRule().metric("NOT_A_METRIC")));

            var response = target.request().post(json(request));

            assertThat(response).hasStatus(HttpStatusCode.BAD_REQUEST_400);
            var error = response.readEntity(Error.class);
            assertThat(error.getMessage()).contains("NOT_A_METRIC");
            assertThat(error.getParameters()).containsEntry("ruleIndex", "0");
        }

        @Test
        void should_return_403_without_create_permission() {
            var request = new CreatePerformanceTarget()
                .subject(new PerformanceTargetSubject().apiIds(List.of(A2A_API)).reference(A2A_API))
                .rules(List.of(aLatencyRule()));

            shouldReturn403(RolePermission.ENVIRONMENT_API, ENVIRONMENT, RolePermissionAction.CREATE, () ->
                target.request().post(json(request))
            );
        }
    }

    @Nested
    class GetByReference {

        @Test
        void should_return_the_targets_of_the_reference_in_this_environment() {
            performanceTargetCrudService.initWith(
                List.of(
                    PerformanceTargetFixtures.aTarget("target-1"),
                    PerformanceTargetFixtures.aTarget("target-2"),
                    PerformanceTargetFixtures.aTarget("other-reference")
                        .toBuilder()
                        .subject(new io.gravitee.apim.core.performance_target.model.PerformanceTarget.Subject(List.of(LLM_API), LLM_API))
                        .build(),
                    PerformanceTargetFixtures.aTarget("other-environment").toBuilder().environmentId("other-environment").build()
                )
            );

            var response = target.queryParam("reference", A2A_API).request().get();

            assertThat(response)
                .hasStatus(HttpStatusCode.OK_200)
                .asEntity(PerformanceTargetsResponse.class)
                .extracting(PerformanceTargetsResponse::getData)
                .asInstanceOf(InstanceOfAssertFactories.list(PerformanceTarget.class))
                .extracting(PerformanceTarget::getId)
                .containsExactlyInAnyOrder("target-1", "target-2");
        }

        @Test
        void should_require_a_reference() {
            var response = target.request().get();

            assertThat(response).hasStatus(HttpStatusCode.BAD_REQUEST_400);
        }

        @Test
        void should_return_403_without_read_permission() {
            shouldReturn403(RolePermission.ENVIRONMENT_API, ENVIRONMENT, RolePermissionAction.READ, () ->
                target.queryParam("reference", A2A_API).request().get()
            );
        }
    }

    @Nested
    class LatestEvaluationsByReferences {

        @Test
        void should_return_one_entry_per_reference_with_the_worst_latest_evaluation_or_null() throws Exception {
            performanceTargetEvaluationCrudService.initWith(
                List.of(
                    PerformanceTargetFixtures.anEvaluation("eval-api", "target-api", Status.PASS),
                    PerformanceTargetFixtures.anEvaluation("eval-agent-pass", "target-agent-1", Status.PASS)
                        .toBuilder()
                        .reference("agent-1")
                        .build(),
                    PerformanceTargetFixtures.anEvaluation("eval-agent-not-evaluable", "target-agent-2", Status.NOT_EVALUABLE)
                        .toBuilder()
                        .reference("agent-1")
                        .build(),
                    PerformanceTargetFixtures.anEvaluation("eval-agent-old", "target-agent-1", Status.BREACH)
                        .toBuilder()
                        .reference("agent-1")
                        .latest(false)
                        .build(),
                    PerformanceTargetFixtures.anEvaluation("eval-other-env", "target-other-env", Status.BREACH)
                        .toBuilder()
                        .reference("agent-2")
                        .environmentId("other-environment")
                        .build()
                )
            );
            var request = new LatestPerformanceTargetEvaluationsRequest().references(
                new LinkedHashSet<>(List.of(A2A_API, "agent-1", "agent-2", "unknown"))
            );

            var response = target.path("evaluations/_latest").request().post(json(request));

            assertThat(response).hasStatus(HttpStatusCode.OK_200);
            var data = new ObjectMapper().readTree(response.readEntity(String.class)).get("data");
            assertThat(data.fieldNames()).toIterable().containsExactly(A2A_API, "agent-1", "agent-2", "unknown");
            assertThat(data.get(A2A_API).get("id").asText()).isEqualTo("eval-api");
            assertThat(data.get("agent-1").get("id").asText()).isEqualTo("eval-agent-not-evaluable");
            assertThat(data.get("agent-2").isNull()).isTrue();
            assertThat(data.get("unknown").isNull()).isTrue();
        }

        @Test
        void should_reject_an_empty_reference_list() {
            var response = target.path("evaluations/_latest").request().post(json(new LatestPerformanceTargetEvaluationsRequest()));

            assertThat(response).hasStatus(HttpStatusCode.BAD_REQUEST_400);
        }

        @Test
        void should_return_403_without_read_permission() {
            var request = new LatestPerformanceTargetEvaluationsRequest().references(Set.of(A2A_API));

            shouldReturn403(RolePermission.ENVIRONMENT_API, ENVIRONMENT, RolePermissionAction.READ, () ->
                target.path("evaluations/_latest").request().post(json(request))
            );
        }
    }

    @Nested
    class Summary {

        @Test
        void should_count_the_latest_evaluations_of_the_environment_by_status() {
            performanceTargetEvaluationCrudService.initWith(
                List.of(
                    PerformanceTargetFixtures.anEvaluation("eval-1", "target-1", Status.PASS),
                    PerformanceTargetFixtures.anEvaluation("eval-2", "target-2", Status.PASS),
                    PerformanceTargetFixtures.anEvaluation("eval-3", "target-3", Status.BREACH),
                    PerformanceTargetFixtures.anEvaluation("eval-4", "target-4", Status.NOT_EVALUABLE),
                    PerformanceTargetFixtures.anEvaluation("eval-3-old", "target-3", Status.BREACH).toBuilder().latest(false).build(),
                    PerformanceTargetFixtures.anEvaluation("eval-other-env", "target-5", Status.BREACH)
                        .toBuilder()
                        .environmentId("other-environment")
                        .build()
                )
            );

            var response = target.path("_summary").request().get();

            assertThat(response)
                .hasStatus(HttpStatusCode.OK_200)
                .asEntity(PerformanceTargetsSummary.class)
                .isEqualTo(new PerformanceTargetsSummary().pass(2L).breach(1L).notEvaluable(1L));
        }

        @Test
        void should_return_403_without_read_permission() {
            shouldReturn403(RolePermission.ENVIRONMENT_API, ENVIRONMENT, RolePermissionAction.READ, () ->
                target.path("_summary").request().get()
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
