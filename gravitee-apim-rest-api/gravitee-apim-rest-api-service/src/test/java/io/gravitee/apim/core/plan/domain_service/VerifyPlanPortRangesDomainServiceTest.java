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
package io.gravitee.apim.core.plan.domain_service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import inmemory.KafkaPortRangeCrudServiceInMemory;
import io.gravitee.apim.core.plan.exception.PlanInvalidException;
import io.gravitee.apim.core.plan.exception.PortRangeConflictException;
import io.gravitee.apim.core.plan.model.KafkaPortRange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class VerifyPlanPortRangesDomainServiceTest {

    private KafkaPortRangeCrudServiceInMemory portRanges;
    private VerifyPlanPortRangesDomainService cut;

    @BeforeEach
    void setUp() {
        portRanges = new KafkaPortRangeCrudServiceInMemory();
        cut = new VerifyPlanPortRangesDomainService(portRanges);
    }

    private static KafkaPortRange existing(String planId, int bootstrap, int rangeStart, int rangeEnd) {
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
    class PortBoundsValidation {

        @Test
        void should_reject_bootstrap_port_below_1024() {
            assertThatThrownBy(() -> cut.verify("env-1", null, null, 1023, 9100, 9102))
                .isInstanceOf(PlanInvalidException.class)
                .hasMessageContaining("bootstrapPort (1023)")
                .hasMessageContaining("[1024-65535]");
        }

        @Test
        void should_reject_bootstrap_port_above_65535() {
            assertThatThrownBy(() -> cut.verify("env-1", null, null, 65536, 65530, 65534))
                .isInstanceOf(PlanInvalidException.class)
                .hasMessageContaining("bootstrapPort (65536)");
        }

        @Test
        void should_reject_broker_range_start_below_1024() {
            assertThatThrownBy(() -> cut.verify("env-1", null, null, 9092, 1023, 9100))
                .isInstanceOf(PlanInvalidException.class)
                .hasMessageContaining("brokerRangeStart (1023)");
        }

        @Test
        void should_reject_broker_range_end_above_65535() {
            assertThatThrownBy(() -> cut.verify("env-1", null, null, 9092, 9100, 65536))
                .isInstanceOf(PlanInvalidException.class)
                .hasMessageContaining("brokerRangeEnd (65536)");
        }

        @Test
        void should_accept_port_65535_as_valid_upper_bound() {
            assertThatCode(() -> cut.verify("env-1", null, null, 9092, 65530, 65535)).doesNotThrowAnyException();
        }

        @Test
        void should_accept_port_1024_as_valid_lower_bound() {
            assertThatCode(() -> cut.verify("env-1", null, null, 1024, 1025, 1027)).doesNotThrowAnyException();
        }
    }

    @Nested
    class InternalConsistency {

        @Test
        void should_reject_inverted_broker_range() {
            assertThatThrownBy(() -> cut.verify("env-1", null, null, 9092, 9102, 9100))
                .isInstanceOf(PlanInvalidException.class)
                .hasMessageContaining("brokerRangeStart (9102) must be <= brokerRangeEnd (9100)");
        }

        @Test
        void should_accept_equal_range_start_and_end() {
            assertThatCode(() -> cut.verify("env-1", null, null, 9092, 9100, 9100)).doesNotThrowAnyException();
        }

        @Test
        void should_reject_bootstrap_inside_broker_range() {
            assertThatThrownBy(() -> cut.verify("env-1", null, null, 9101, 9100, 9102))
                .isInstanceOf(PlanInvalidException.class)
                .hasMessageContaining("bootstrapPort (9101)")
                .hasMessageContaining("must not fall within brokerRange [9100-9102]");
        }

        @Test
        void should_reject_bootstrap_on_range_start_boundary() {
            assertThatThrownBy(() -> cut.verify("env-1", null, null, 9100, 9100, 9102)).isInstanceOf(PlanInvalidException.class);
        }

        @Test
        void should_reject_bootstrap_on_range_end_boundary() {
            assertThatThrownBy(() -> cut.verify("env-1", null, null, 9102, 9100, 9102)).isInstanceOf(PlanInvalidException.class);
        }

        @Test
        void should_accept_bootstrap_adjacent_to_range() {
            assertThatCode(() -> cut.verify("env-1", null, null, 9099, 9100, 9102)).doesNotThrowAnyException();
            assertThatCode(() -> cut.verify("env-1", null, null, 9103, 9100, 9102)).doesNotThrowAnyException();
        }
    }

    @Nested
    class SiblingConflictDetection {

        @Test
        void should_reject_broker_range_overlap_with_sibling() {
            portRanges.create(existing("sibling", 9092, 9100, 9105));

            assertThatThrownBy(() -> cut.verify("env-1", null, null, 9192, 9103, 9108))
                .isInstanceOf(PortRangeConflictException.class)
                .hasMessageContaining("Port range [9103-9108]")
                .hasMessageContaining("plan 'sibling'")
                .hasMessageContaining("API 'api-1'");
        }

        @Test
        void should_reject_new_bootstrap_inside_existing_range() {
            portRanges.create(existing("sibling", 9092, 9100, 9110));

            assertThatThrownBy(() -> cut.verify("env-1", null, null, 9105, 9200, 9202)).isInstanceOf(PortRangeConflictException.class);
        }

        @Test
        void should_reject_existing_bootstrap_inside_new_range() {
            portRanges.create(existing("sibling", 9205, 9300, 9302));

            assertThatThrownBy(() -> cut.verify("env-1", null, null, 9092, 9200, 9210)).isInstanceOf(PortRangeConflictException.class);
        }

        @Test
        void should_reject_bootstrap_port_collision() {
            portRanges.create(existing("sibling", 9092, 9300, 9302));

            assertThatThrownBy(() -> cut.verify("env-1", null, null, 9092, 9400, 9402)).isInstanceOf(PortRangeConflictException.class);
        }

        @Test
        void should_accept_non_overlapping_allocation() {
            portRanges.create(existing("sibling", 9092, 9100, 9102));

            assertThatCode(() -> cut.verify("env-1", null, null, 9192, 9200, 9202)).doesNotThrowAnyException();
        }

        @Test
        void should_exclude_plan_being_updated_from_conflict_check() {
            portRanges.create(existing("plan-1", 9092, 9100, 9102));

            // Re-verifying the same allocation with plan-1's id excluded should succeed — update path.
            assertThatCode(() -> cut.verify("env-1", null, "plan-1", 9092, 9100, 9102)).doesNotThrowAnyException();
        }

        @Test
        void should_scope_conflict_check_to_environment() {
            portRanges.create(existing("sibling", 9092, 9100, 9102).toBuilder().environmentId("env-2").build());

            assertThatCode(() -> cut.verify("env-1", null, null, 9092, 9100, 9102)).doesNotThrowAnyException();
        }

        @Test
        void should_scope_conflict_check_to_sharding_tag() {
            portRanges.create(existing("sibling", 9092, 9100, 9102).toBuilder().shardingTag("us-east").build());

            assertThatCode(() -> cut.verify("env-1", "eu-west", null, 9092, 9100, 9102)).doesNotThrowAnyException();
        }
    }
}
