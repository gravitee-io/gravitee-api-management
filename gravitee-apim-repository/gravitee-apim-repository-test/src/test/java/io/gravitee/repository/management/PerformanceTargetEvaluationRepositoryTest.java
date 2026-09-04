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
package io.gravitee.repository.management;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.common.utils.UUID;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.model.PerformanceTargetEnvironmentSummary;
import io.gravitee.repository.management.model.PerformanceTargetEvaluation;
import io.gravitee.repository.management.model.PerformanceTargetEvaluation.Status;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;

public class PerformanceTargetEvaluationRepositoryTest extends AbstractManagementRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/performancetargetevaluation-tests/";
    }

    // create
    @Test
    public void create_should_store_evaluation_and_unset_previous_latest_of_same_target() throws TechnicalException {
        var evaluation = anEvaluation(UUID.random().toString(), "target1", "api-1", Status.PASS, true);

        var created = performanceTargetEvaluationRepository.create(evaluation);

        assertThat(created).usingRecursiveComparison().isEqualTo(evaluation);
        assertThat(performanceTargetEvaluationRepository.findLatestByReference("my-env", "api-1"))
            .extracting(PerformanceTargetEvaluation::getId)
            .containsExactly(evaluation.getId());
    }

    @Test
    public void create_should_keep_current_latest_when_storing_a_non_latest_evaluation() throws TechnicalException {
        var evaluation = anEvaluation(UUID.random().toString(), "target2", "agent-1", Status.BREACH, false);

        performanceTargetEvaluationRepository.create(evaluation);

        assertThat(performanceTargetEvaluationRepository.findLatestByReference("my-env", "agent-1"))
            .extracting(PerformanceTargetEvaluation::getId)
            .containsExactlyInAnyOrder("eval-2", "eval-3");
    }

    // findLatestByReference
    @Test
    public void findLatestByReference_should_return_latest_evaluation_with_rule_results() throws TechnicalException {
        var found = performanceTargetEvaluationRepository.findLatestByReference("my-env", "api-1");

        assertThat(found)
            .singleElement()
            .usingRecursiveComparison()
            .isEqualTo(
                PerformanceTargetEvaluation.builder()
                    .id("eval-1b")
                    .targetId("target1")
                    .environmentId("my-env")
                    .reference("api-1")
                    .status(Status.BREACH)
                    .rules(
                        List.of(
                            new PerformanceTargetEvaluation.RuleResult(
                                "HTTP_GATEWAY_RESPONSE_TIME",
                                "P95",
                                "LTE",
                                2000,
                                4810.0,
                                2810.0,
                                1.405,
                                63,
                                Status.BREACH
                            ),
                            new PerformanceTargetEvaluation.RuleResult(
                                "HTTP_ERROR_RATE",
                                "PERCENTAGE",
                                "LTE",
                                5,
                                1.6,
                                -3.4,
                                -0.68,
                                91,
                                Status.PASS
                            )
                        )
                    )
                    .windowFrom(new Date(1470157167000L))
                    .windowTo(new Date(1470158067000L))
                    .coveredApiIds(List.of("api-1"))
                    .evaluatedAt(new Date(1470158067000L))
                    .latest(true)
                    .build()
            );
    }

    @Test
    public void findLatestByReference_should_return_one_latest_per_target_of_the_reference() throws TechnicalException {
        var found = performanceTargetEvaluationRepository.findLatestByReference("my-env", "agent-1");

        assertThat(found).extracting(PerformanceTargetEvaluation::getId).containsExactlyInAnyOrder("eval-2", "eval-3");
    }

    @Test
    public void findLatestByReference_should_return_empty_when_unknown() throws TechnicalException {
        assertThat(performanceTargetEvaluationRepository.findLatestByReference("my-env", "unknown")).isEmpty();
    }

    // findLatestByReferences
    @Test
    public void findLatestByReferences_should_return_latest_of_every_reference_in_environment() throws TechnicalException {
        var found = performanceTargetEvaluationRepository.findLatestByReferences("my-env", List.of("api-1", "agent-1", "unknown"));

        assertThat(found).extracting(PerformanceTargetEvaluation::getId).containsExactlyInAnyOrder("eval-1b", "eval-2", "eval-3");
    }

    @Test
    public void findLatestByReferences_should_return_empty_for_no_reference() throws TechnicalException {
        assertThat(performanceTargetEvaluationRepository.findLatestByReferences("my-env", List.of())).isEmpty();
    }

    // findEnvironmentLatest
    @Test
    public void findEnvironmentLatest_should_page_latest_evaluations_most_recent_first() throws TechnicalException {
        var firstPage = performanceTargetEvaluationRepository.findEnvironmentLatest(
            "my-env",
            new PageableBuilder().pageNumber(0).pageSize(2).build()
        );
        var secondPage = performanceTargetEvaluationRepository.findEnvironmentLatest(
            "my-env",
            new PageableBuilder().pageNumber(1).pageSize(2).build()
        );

        assertThat(firstPage.getTotalElements()).isEqualTo(5);
        assertThat(firstPage.getContent()).extracting(PerformanceTargetEvaluation::getId).containsExactly("eval-1b", "eval-del-2");
        assertThat(secondPage.getContent()).extracting(PerformanceTargetEvaluation::getId).containsExactly("eval-del-1", "eval-2");
    }

    @Test
    public void findEnvironmentLatest_should_return_empty_page_for_unknown_environment() throws TechnicalException {
        var page = performanceTargetEvaluationRepository.findEnvironmentLatest(
            "unknown",
            new PageableBuilder().pageNumber(0).pageSize(10).build()
        );

        assertThat(page.getTotalElements()).isZero();
        assertThat(page.getContent()).isEmpty();
    }

    // findByTargetId
    @Test
    public void findByTargetId_should_page_history_most_recent_first() throws TechnicalException {
        var firstPage = performanceTargetEvaluationRepository.findByTargetId(
            "target1",
            new PageableBuilder().pageNumber(0).pageSize(1).build()
        );
        var secondPage = performanceTargetEvaluationRepository.findByTargetId(
            "target1",
            new PageableBuilder().pageNumber(1).pageSize(1).build()
        );

        assertThat(firstPage.getTotalElements()).isEqualTo(2);
        assertThat(firstPage.getContent()).extracting(PerformanceTargetEvaluation::getId).containsExactly("eval-1b");
        assertThat(secondPage.getContent()).extracting(PerformanceTargetEvaluation::getId).containsExactly("eval-1a");
    }

    @Test
    public void findByTargetId_should_return_empty_page_for_unknown_target() throws TechnicalException {
        var page = performanceTargetEvaluationRepository.findByTargetId(
            "unknown",
            new PageableBuilder().pageNumber(0).pageSize(10).build()
        );

        assertThat(page.getTotalElements()).isZero();
        assertThat(page.getContent()).isEmpty();
    }

    // getEnvironmentSummary
    @Test
    public void getEnvironmentSummary_should_count_latest_evaluations_by_status() throws TechnicalException {
        var summary = performanceTargetEvaluationRepository.getEnvironmentSummary("my-env");

        assertThat(summary).isEqualTo(new PerformanceTargetEnvironmentSummary("my-env", 2, 2, 1));
    }

    @Test
    public void getEnvironmentSummary_should_return_zeros_for_unknown_environment() throws TechnicalException {
        var summary = performanceTargetEvaluationRepository.getEnvironmentSummary("unknown");

        assertThat(summary).isEqualTo(new PerformanceTargetEnvironmentSummary("unknown", 0, 0, 0));
    }

    // deleteByReference
    @Test
    public void deleteByReference_should_delete_history_of_every_target_of_the_reference() throws TechnicalException {
        var deleted = performanceTargetEvaluationRepository.deleteByReference("my-env", "ToBeDeleted");

        assertThat(deleted).containsExactlyInAnyOrder("eval-del-1-old", "eval-del-1", "eval-del-2");
        assertThat(performanceTargetEvaluationRepository.findLatestByReference("my-env", "ToBeDeleted")).isEmpty();
        assertThat(performanceTargetEvaluationRepository.getEnvironmentSummary("my-env")).isEqualTo(
            new PerformanceTargetEnvironmentSummary("my-env", 1, 1, 1)
        );
    }

    // deleteByEnvironmentId
    @Test
    public void deleteByEnvironmentId_should_delete_every_evaluation_of_the_environment_only() throws TechnicalException {
        var deleted = performanceTargetEvaluationRepository.deleteByEnvironmentId("my-env");

        assertThat(deleted).hasSize(8).doesNotContain("eval-other-env");
        assertThat(performanceTargetEvaluationRepository.getEnvironmentSummary("my-env")).isEqualTo(
            new PerformanceTargetEnvironmentSummary("my-env", 0, 0, 0)
        );
        assertThat(performanceTargetEvaluationRepository.findLatestByReference("other-env", "agent-1"))
            .extracting(PerformanceTargetEvaluation::getId)
            .containsExactly("eval-other-env");
    }

    // deleteByTargetId
    @Test
    public void deleteByTargetId_should_delete_history_of_target() throws TechnicalException {
        performanceTargetEvaluationRepository.deleteByTargetId("target1");

        assertThat(performanceTargetEvaluationRepository.findLatestByReference("my-env", "api-1")).isEmpty();
        assertThat(
            performanceTargetEvaluationRepository.findEnvironmentLatest("my-env", new PageableBuilder().pageNumber(0).pageSize(10).build())
        )
            .extracting(page -> page.getTotalElements())
            .isEqualTo(4L);
    }

    private static PerformanceTargetEvaluation anEvaluation(String id, String targetId, String reference, Status status, boolean latest) {
        return PerformanceTargetEvaluation.builder()
            .id(id)
            .targetId(targetId)
            .environmentId("my-env")
            .reference(reference)
            .status(status)
            .rules(
                List.of(
                    new PerformanceTargetEvaluation.RuleResult(
                        "HTTP_GATEWAY_RESPONSE_TIME",
                        "P95",
                        "LTE",
                        2000,
                        status == Status.PASS ? 1500.0 : 3000.0,
                        status == Status.PASS ? -500.0 : 1000.0,
                        status == Status.PASS ? -0.25 : 0.5,
                        42,
                        status
                    )
                )
            )
            .windowFrom(new Date(1470158100000L))
            .windowTo(new Date(1470159000000L))
            .coveredApiIds(List.of("api-1"))
            .evaluatedAt(new Date(1470159000000L))
            .latest(latest)
            .build();
    }
}
