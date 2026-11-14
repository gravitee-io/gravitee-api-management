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
package io.gravitee.gamma.definition.approval;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApprovalStatusTest {

    @ParameterizedTest
    @ValueSource(strings = { "PENDING", "APPROVED", "EXECUTING", "EXECUTED", "UNKNOWN", "REJECTED", "EXPIRED", "ENDED", "NOT_COLLECTED" })
    void reads_every_status_aim_publishes(String published) {
        assertThat(ApprovalStatus.parse(published).name()).isEqualTo(published);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = { "ESCALATED", "approved", "", "UNRECOGNIZED" })
    void reads_a_status_this_node_does_not_know_as_one_that_grants_nothing(String published) {
        ApprovalStatus status = ApprovalStatus.parse(published);

        assertThat(status).isEqualTo(ApprovalStatus.UNRECOGNIZED);
        assertThat(status.isGranting()).isFalse();
        assertThat(status.isFoundByOpenKey()).isFalse();
    }

    @Test
    void lets_a_time_based_grant_through_only_while_approved() {
        assertThat(Arrays.stream(ApprovalStatus.values()).filter(ApprovalStatus::isGranting)).containsExactlyInAnyOrder(
            ApprovalStatus.APPROVED,
            ApprovalStatus.EXECUTING,
            ApprovalStatus.EXECUTED
        );
    }

    @Test
    void is_found_by_its_open_key_until_aim_releases_the_request() {
        assertThat(Arrays.stream(ApprovalStatus.values()).filter(ApprovalStatus::isFoundByOpenKey)).containsExactlyInAnyOrder(
            ApprovalStatus.PENDING,
            ApprovalStatus.APPROVED,
            ApprovalStatus.EXECUTING,
            ApprovalStatus.REJECTED,
            ApprovalStatus.EXPIRED
        );
    }
}
