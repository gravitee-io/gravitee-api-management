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

import io.gravitee.repository.management.model.KafkaPortRange;
import java.time.Instant;
import java.util.List;
import org.junit.Test;

public class KafkaPortRangeRepositoryTest extends AbstractManagementRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/kafkaportrange-tests/";
    }

    @Test
    public void shouldFindById() throws Exception {
        var row = kafkaPortRangeRepository.findById("plan-default-1");

        assertThat(row).hasValueSatisfying(r -> {
            assertThat(r.getPlanId()).isEqualTo("plan-default-1");
            assertThat(r.getApiId()).isEqualTo("api-default-1");
            assertThat(r.getEnvironmentId()).isEqualTo("DEFAULT");
            assertThat(r.getBootstrapPort()).isEqualTo(9092);
            assertThat(r.getRangeStart()).isEqualTo(9100);
            assertThat(r.getRangeEnd()).isEqualTo(9105);
        });
    }

    @Test
    public void shouldReturnEmptyForUnknownId() throws Exception {
        assertThat(kafkaPortRangeRepository.findById("unknown")).isEmpty();
    }

    @Test
    public void shouldCreate() throws Exception {
        var row = KafkaPortRange.builder()
            .planId("new-plan")
            .apiId("new-api")
            .environmentId("DEFAULT")
            .bootstrapPort(9700)
            .rangeStart(9710)
            .rangeEnd(9712)
            .createdAt(Instant.parse("2026-05-01T00:00:00Z"))
            .updatedAt(Instant.parse("2026-05-01T00:00:00Z"))
            .build();

        kafkaPortRangeRepository.create(row);

        assertThat(kafkaPortRangeRepository.findById("new-plan")).hasValueSatisfying(saved -> {
            assertThat(saved.getApiId()).isEqualTo("new-api");
            assertThat(saved.getBootstrapPort()).isEqualTo(9700);
            assertThat(saved.getRangeStart()).isEqualTo(9710);
            assertThat(saved.getRangeEnd()).isEqualTo(9712);
        });
    }

    @Test
    public void shouldUpdate() throws Exception {
        var existing = kafkaPortRangeRepository.findById("plan-default-2").orElseThrow();
        existing.setBootstrapPort(9293);
        existing.setRangeStart(9310);
        existing.setRangeEnd(9315);
        existing.setUpdatedAt(Instant.parse("2026-05-02T00:00:00Z"));

        kafkaPortRangeRepository.update(existing);

        assertThat(kafkaPortRangeRepository.findById("plan-default-2")).hasValueSatisfying(updated -> {
            assertThat(updated.getBootstrapPort()).isEqualTo(9293);
            assertThat(updated.getRangeStart()).isEqualTo(9310);
            assertThat(updated.getRangeEnd()).isEqualTo(9315);
        });
    }

    @Test
    public void shouldDelete() throws Exception {
        assertThat(kafkaPortRangeRepository.findById("plan-default-1")).isPresent();
        kafkaPortRangeRepository.delete("plan-default-1");
        assertThat(kafkaPortRangeRepository.findById("plan-default-1")).isEmpty();
    }

    @Test
    public void shouldDeleteByApiId() throws Exception {
        kafkaPortRangeRepository.deleteByApiId("api-multi-plan");

        assertThat(kafkaPortRangeRepository.findById("plan-same-api-a")).isEmpty();
        assertThat(kafkaPortRangeRepository.findById("plan-same-api-b")).isEmpty();
        // Other APIs in the same env are untouched.
        assertThat(kafkaPortRangeRepository.findById("plan-default-1")).isPresent();
    }

    @Test
    public void shouldDeleteByEnvironmentId() throws Exception {
        kafkaPortRangeRepository.deleteByEnvironmentId("DEFAULT");

        assertThat(kafkaPortRangeRepository.findById("plan-default-1")).isEmpty();
        assertThat(kafkaPortRangeRepository.findById("plan-default-2")).isEmpty();
        assertThat(kafkaPortRangeRepository.findById("plan-same-api-a")).isEmpty();
        // Other environments are untouched.
        assertThat(kafkaPortRangeRepository.findById("plan-other-env")).isPresent();
    }

    // ---- findConflicting / findConflictingForUpdate ----

    @Test
    public void findConflicting_should_detect_broker_range_overlap() throws Exception {
        // Candidate range [9103-9108] overlaps with plan-default-1 [9100-9105]
        List<KafkaPortRange> conflicts = kafkaPortRangeRepository.findConflicting("DEFAULT", 9999, 9103, 9108, null);
        assertThat(conflicts).extracting(KafkaPortRange::getPlanId).contains("plan-default-1");
    }

    @Test
    public void findConflicting_should_detect_new_bootstrap_inside_existing_range() throws Exception {
        // Candidate bootstrap 9102 lands inside plan-default-1's [9100-9105] range
        List<KafkaPortRange> conflicts = kafkaPortRangeRepository.findConflicting("DEFAULT", 9102, 9400, 9402, null);
        assertThat(conflicts).extracting(KafkaPortRange::getPlanId).contains("plan-default-1");
    }

    @Test
    public void findConflicting_should_detect_existing_bootstrap_inside_new_range() throws Exception {
        // Candidate range [9090-9095] swallows plan-default-1's bootstrap 9092
        List<KafkaPortRange> conflicts = kafkaPortRangeRepository.findConflicting("DEFAULT", 9999, 9090, 9095, null);
        assertThat(conflicts).extracting(KafkaPortRange::getPlanId).contains("plan-default-1");
    }

    @Test
    public void findConflicting_should_detect_bootstrap_collision() throws Exception {
        // Same bootstrap as plan-default-1, no range overlap
        List<KafkaPortRange> conflicts = kafkaPortRangeRepository.findConflicting("DEFAULT", 9092, 9400, 9402, null);
        assertThat(conflicts).extracting(KafkaPortRange::getPlanId).contains("plan-default-1");
    }

    @Test
    public void findConflicting_should_return_empty_when_no_overlap() throws Exception {
        List<KafkaPortRange> conflicts = kafkaPortRangeRepository.findConflicting("DEFAULT", 19999, 19000, 19002, null);
        assertThat(conflicts).isEmpty();
    }

    @Test
    public void findConflicting_should_scope_by_environment() throws Exception {
        // plan-other-env has the same bootstrap+range as plan-default-1 but in OTHER_ENV
        List<KafkaPortRange> conflicts = kafkaPortRangeRepository.findConflicting("OTHER_ENV", 9092, 9100, 9105, null);

        assertThat(conflicts).extracting(KafkaPortRange::getPlanId).contains("plan-other-env").doesNotContain("plan-default-1");
    }

    @Test
    public void findConflicting_should_exclude_plan_id() throws Exception {
        // Querying plan-default-1's own allocation while excluding it returns no self-conflict
        List<KafkaPortRange> conflicts = kafkaPortRangeRepository.findConflicting("DEFAULT", 9092, 9100, 9105, "plan-default-1");

        assertThat(conflicts).extracting(KafkaPortRange::getPlanId).doesNotContain("plan-default-1");
    }

    @Test
    public void findConflictingForUpdate_should_behave_like_findConflicting() throws Exception {
        // Same overlap as the broker-range case but via the locking variant.
        List<KafkaPortRange> conflicts = kafkaPortRangeRepository.findConflictingForUpdate("DEFAULT", 9999, 9103, 9108, null);
        assertThat(conflicts).extracting(KafkaPortRange::getPlanId).contains("plan-default-1");
    }
}
