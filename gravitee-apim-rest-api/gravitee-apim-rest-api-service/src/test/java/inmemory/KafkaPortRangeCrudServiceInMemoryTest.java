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
package inmemory;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.core.plan.model.KafkaPortRange;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class KafkaPortRangeCrudServiceInMemoryTest {

    private KafkaPortRangeCrudServiceInMemory cut;

    @BeforeEach
    void setUp() {
        cut = new KafkaPortRangeCrudServiceInMemory();
    }

    private static KafkaPortRange range(String planId, int bootstrap, int rangeStart, int rangeEnd) {
        return KafkaPortRange.builder()
            .planId(planId)
            .apiId("api-1")
            .environmentId("env-1")
            .bootstrapPort(bootstrap)
            .rangeStart(rangeStart)
            .rangeEnd(rangeEnd)
            .build();
    }

    @Nested
    class FindConflicting {

        @Test
        void should_detect_broker_range_overlap() {
            cut.create(range("existing", 9092, 9100, 9105));

            var conflicts = cut.findConflicting("env-1", 9192, 9103, 9108, null);

            assertThat(conflicts).extracting(KafkaPortRange::getPlanId).containsExactly("existing");
        }

        @Test
        void should_detect_new_bootstrap_inside_existing_broker_range() {
            cut.create(range("existing", 9092, 9100, 9110));

            var conflicts = cut.findConflicting("env-1", 9105, 9200, 9202, null);

            assertThat(conflicts).extracting(KafkaPortRange::getPlanId).containsExactly("existing");
        }

        @Test
        void should_detect_existing_bootstrap_inside_new_broker_range() {
            cut.create(range("existing", 9105, 9200, 9210));

            var conflicts = cut.findConflicting("env-1", 9092, 9100, 9110, null);

            assertThat(conflicts).extracting(KafkaPortRange::getPlanId).containsExactly("existing");
        }

        @Test
        void should_detect_bootstrap_port_collision() {
            cut.create(range("existing", 9092, 9200, 9202));

            var conflicts = cut.findConflicting("env-1", 9092, 9300, 9302, null);

            assertThat(conflicts).extracting(KafkaPortRange::getPlanId).containsExactly("existing");
        }

        @Test
        void should_not_detect_conflict_on_non_overlapping_ranges() {
            cut.create(range("existing", 9092, 9100, 9102));

            var conflicts = cut.findConflicting("env-1", 9192, 9200, 9202, null);

            assertThat(conflicts).isEmpty();
        }

        @Test
        void should_exclude_plan_id_from_conflict_check() {
            cut.create(range("plan-1", 9092, 9100, 9102));

            var conflicts = cut.findConflicting("env-1", 9092, 9100, 9102, "plan-1");

            assertThat(conflicts).isEmpty();
        }

        @Test
        void should_scope_by_environment_id() {
            cut.create(range("other-env-plan", 9092, 9100, 9102).toBuilder().environmentId("env-2").build());

            var conflicts = cut.findConflicting("env-1", 9092, 9100, 9102, null);

            assertThat(conflicts).isEmpty();
        }

        @Test
        void should_return_multiple_conflicts() {
            cut.create(range("plan-a", 9092, 9100, 9102));
            cut.create(range("plan-b", 9092, 9200, 9202)); // bootstrap collision
            cut.create(range("plan-c", 9192, 9101, 9103)); // range overlap with new

            var conflicts = cut.findConflicting("env-1", 9092, 9100, 9105, null);

            assertThat(conflicts).hasSize(3);
        }
    }

    @Nested
    class DeleteByApiId {

        @Test
        void should_remove_all_rows_for_api() {
            cut.create(range("plan-1", 9092, 9100, 9102));
            cut.create(range("plan-2", 9192, 9200, 9202));
            cut.create(range("plan-3", 9292, 9300, 9302).toBuilder().apiId("api-2").build());

            cut.deleteByApiId("api-1");

            assertThat(cut.storage()).extracting(KafkaPortRange::getPlanId).containsExactly("plan-3");
        }
    }

    @Nested
    class Crud {

        @Test
        void should_update_existing_row() {
            cut.create(range("plan-1", 9092, 9100, 9102));
            cut.update(range("plan-1", 9093, 9200, 9202));

            assertThat(cut.findByPlanId("plan-1")).hasValueSatisfying(r -> {
                assertThat(r.getBootstrapPort()).isEqualTo(9093);
                assertThat(r.getRangeStart()).isEqualTo(9200);
            });
        }

        @Test
        void should_find_by_plan_id() {
            cut.create(range("plan-1", 9092, 9100, 9102));

            assertThat(cut.findByPlanId("plan-1")).isPresent();
            assertThat(cut.findByPlanId("nope")).isEmpty();
        }

        @Test
        void should_delete_by_plan_id() {
            cut.create(range("plan-1", 9092, 9100, 9102));
            cut.delete("plan-1");
            assertThat(cut.findByPlanId("plan-1")).isEmpty();
        }
    }
}
