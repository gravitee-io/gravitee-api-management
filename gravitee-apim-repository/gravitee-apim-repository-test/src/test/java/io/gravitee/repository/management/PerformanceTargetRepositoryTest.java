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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.common.utils.UUID;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.model.PerformanceTarget;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;

public class PerformanceTargetRepositoryTest extends AbstractManagementRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/performancetarget-tests/";
    }

    // create
    @Test
    public void create_should_create_target_with_rules_and_api_ids() throws TechnicalException {
        var target = aTarget(UUID.random().toString(), new Date());

        var created = performanceTargetRepository.create(target);

        assertThat(created).usingRecursiveComparison().ignoringCollectionOrderInFields("apiIds").isEqualTo(target);
    }

    @Test
    public void create_should_create_target_without_api_ids() throws TechnicalException {
        var target = aTarget(UUID.random().toString(), new Date()).toBuilder().apiIds(List.of()).build();

        var created = performanceTargetRepository.create(target);

        assertThat(created).usingRecursiveComparison().ignoringCollectionOrderInFields("apiIds").isEqualTo(target);
    }

    // findById
    @Test
    public void findById_should_return_target_with_rules_filters_and_api_ids() throws TechnicalException {
        var found = performanceTargetRepository.findById("target1");

        assertThat(found)
            .isPresent()
            .get()
            .usingRecursiveComparison()
            .isEqualTo(
                PerformanceTarget.builder()
                    .id("target1")
                    .environmentId("my-env")
                    .reference("api-1")
                    .apiIds(List.of("api-1"))
                    .windowSeconds(900)
                    .intervalSeconds(300)
                    .minSampleSize(20)
                    .rules(
                        List.of(
                            new PerformanceTarget.Rule("HTTP_GATEWAY_RESPONSE_TIME", "P95", "LTE", 2000, List.of(), List.of()),
                            new PerformanceTarget.Rule(
                                "HTTP_ERROR_RATE",
                                "PERCENTAGE",
                                "LTE",
                                5,
                                List.of("MCP_PROXY"),
                                List.of(new PerformanceTarget.Filter("MCP_PROXY_TOOL", "EQ", "search"))
                            )
                        )
                    )
                    .createdAt(new Date(1470157767000L))
                    .updatedAt(new Date(1470157767000L))
                    .build()
            );
    }

    @Test
    public void findById_should_return_empty_when_not_found() throws TechnicalException {
        assertThat(performanceTargetRepository.findById("unknown")).isEmpty();
    }

    // update
    @Test
    public void update_should_replace_rules_and_api_ids() throws TechnicalException {
        var updated = performanceTargetRepository
            .findById("target1")
            .orElseThrow()
            .toBuilder()
            .apiIds(List.of("api-1", "api-9"))
            .minSampleSize(30)
            .rules(List.of(new PerformanceTarget.Rule("HTTP_ERRORS", "COUNT", "LT", 10, List.of(), List.of())))
            .updatedAt(new Date(1712660289000L))
            .build();

        var result = performanceTargetRepository.update(updated);

        assertThat(result).usingRecursiveComparison().ignoringCollectionOrderInFields("apiIds").isEqualTo(updated);
        assertThat(performanceTargetRepository.findById("target1"))
            .get()
            .usingRecursiveComparison()
            .ignoringCollectionOrderInFields("apiIds")
            .isEqualTo(updated);
    }

    @Test
    public void update_should_throw_when_target_not_found() {
        var unknown = aTarget("unknown", new Date());

        assertThatThrownBy(() -> performanceTargetRepository.update(unknown)).isInstanceOf(Exception.class);
    }

    // delete
    @Test
    public void delete_should_delete_target() throws TechnicalException {
        performanceTargetRepository.delete("to-delete");

        assertThat(performanceTargetRepository.findById("to-delete")).isEmpty();
        assertThat(performanceTargetRepository.removeApiId("api-to-delete")).isEmpty();
    }

    // findByReference
    @Test
    public void findByReference_should_return_targets_of_reference_in_environment_only() throws TechnicalException {
        var found = performanceTargetRepository.findByReference("my-env", "agent-1");

        assertThat(found).extracting(PerformanceTarget::getId).containsExactlyInAnyOrder("target2", "target3");
        assertThat(found).allSatisfy(target -> assertThat(target.getApiIds()).isNotEmpty());
    }

    @Test
    public void findByReference_should_return_empty_when_unknown() throws TechnicalException {
        assertThat(performanceTargetRepository.findByReference("my-env", "unknown")).isEmpty();
    }

    // deleteByReference
    @Test
    public void deleteByReference_should_delete_targets_and_return_their_ids() throws TechnicalException {
        var deleted = performanceTargetRepository.deleteByReference("my-env", "ToBeDeleted");

        assertThat(deleted).containsExactlyInAnyOrder("86adbd41-8a3f-45f3-b1ca-f980042db19d", "b8c2ef52-ebb5-4d0f-be1e-a41cc5579fc8");
        assertThat(performanceTargetRepository.findByReference("my-env", "ToBeDeleted")).isEmpty();
        assertThat(performanceTargetRepository.removeApiId("api-3")).isEmpty();
    }

    // removeApiId
    @Test
    public void removeApiId_should_remove_id_from_every_target_and_keep_targets_left_without_ids() throws TechnicalException {
        var touched = performanceTargetRepository.removeApiId("api-shared");

        assertThat(touched).containsExactlyInAnyOrder("86adbd41-8a3f-45f3-b1ca-f980042db19d", "b8c2ef52-ebb5-4d0f-be1e-a41cc5579fc8");
        assertThat(performanceTargetRepository.findById("86adbd41-8a3f-45f3-b1ca-f980042db19d"))
            .get()
            .extracting(PerformanceTarget::getApiIds)
            .isEqualTo(List.of());
        assertThat(performanceTargetRepository.findById("b8c2ef52-ebb5-4d0f-be1e-a41cc5579fc8"))
            .get()
            .extracting(PerformanceTarget::getApiIds)
            .isEqualTo(List.of("api-3"));
    }

    @Test
    public void removeApiId_should_return_empty_when_no_target_uses_the_api() throws TechnicalException {
        assertThat(performanceTargetRepository.removeApiId("unused-api")).isEmpty();
        assertThat(performanceTargetRepository.findById("target1"))
            .get()
            .extracting(PerformanceTarget::getApiIds)
            .isEqualTo(List.of("api-1"));
    }

    private static PerformanceTarget aTarget(String id, Date date) {
        return PerformanceTarget.builder()
            .id(id)
            .environmentId("my-env")
            .reference("agent-42")
            .apiIds(List.of("api-a2a", "api-llm"))
            .windowSeconds(3600)
            .intervalSeconds(300)
            .minSampleSize(20)
            .rules(
                List.of(
                    new PerformanceTarget.Rule("HTTP_GATEWAY_RESPONSE_TIME", "P95", "LTE", 2000, List.of("A2A_PROXY"), List.of()),
                    new PerformanceTarget.Rule(
                        "LLM_PROMPT_TOKEN_TOTAL_COST",
                        "AVG",
                        "LTE",
                        0.01,
                        List.of("LLM_PROXY"),
                        List.of(new PerformanceTarget.Filter("LLM_PROXY_MODEL", "IN", List.of("gpt-4o", "gpt-4o-mini")))
                    )
                )
            )
            .createdAt(date)
            .updatedAt(date)
            .build();
    }
}
