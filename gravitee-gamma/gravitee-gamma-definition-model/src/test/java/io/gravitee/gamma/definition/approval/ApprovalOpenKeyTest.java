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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApprovalOpenKeyTest {

    private static final String DIGEST = "9cb2dd1e942bad79c0d7b82f0a7eb2203f49909a0678cc965cfba911ea639fed";

    @Test
    void is_the_same_for_the_same_call() {
        assertThat(ApprovalOpenKey.of("env", "api", "rule", "app", "alice", DIGEST)).isEqualTo(
            ApprovalOpenKey.of("env", "api", "rule", "app", "alice", DIGEST)
        );
    }

    @Test
    void tells_no_subject_from_a_subject_named_null() {
        assertThat(ApprovalOpenKey.of("env", "api", "rule", "app", null, DIGEST)).isNotEqualTo(
            ApprovalOpenKey.of("env", "api", "rule", "app", "null", DIGEST)
        );
    }

    @Test
    void cannot_be_forged_by_moving_text_between_parts() {
        assertThat(ApprovalOpenKey.of("env", "api", "rule", "ab", "c", DIGEST)).isNotEqualTo(
            ApprovalOpenKey.of("env", "api", "rule", "a", "bc", DIGEST)
        );
        // Joined by line breaks alone, both would read "a", "b", "c": only the lengths tell them apart.
        assertThat(ApprovalOpenKey.of("env", "api", "rule", "a\nb", "c", DIGEST)).isNotEqualTo(
            ApprovalOpenKey.of("env", "api", "rule", "a", "b\nc", DIGEST)
        );
    }

    @Test
    void refuses_a_missing_part_other_than_the_subject() {
        assertThatThrownBy(() -> ApprovalOpenKey.of(null, "api", "rule", "app", "alice", DIGEST)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> ApprovalOpenKey.of("env", null, "rule", "app", "alice", DIGEST)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> ApprovalOpenKey.of("env", "api", null, "app", "alice", DIGEST)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> ApprovalOpenKey.of("env", "api", "rule", null, "alice", DIGEST)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> ApprovalOpenKey.of("env", "api", "rule", "app", "alice", null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void changes_with_every_part() {
        var key = ApprovalOpenKey.of("env", "api", "rule", "app", "alice", DIGEST);

        assertThat(ApprovalOpenKey.of("env2", "api", "rule", "app", "alice", DIGEST)).isNotEqualTo(key);
        assertThat(ApprovalOpenKey.of("env", "api2", "rule", "app", "alice", DIGEST)).isNotEqualTo(key);
        assertThat(ApprovalOpenKey.of("env", "api", "rule2", "app", "alice", DIGEST)).isNotEqualTo(key);
        assertThat(ApprovalOpenKey.of("env", "api", "rule", "app2", "alice", DIGEST)).isNotEqualTo(key);
        assertThat(ApprovalOpenKey.of("env", "api", "rule", "app", "bob", DIGEST)).isNotEqualTo(key);
        assertThat(ApprovalOpenKey.of("env", "api", "rule", "app", "alice", "0" + DIGEST.substring(1))).isNotEqualTo(key);
    }

    @Test
    void is_lowercase_hex_sha256() {
        assertThat(ApprovalOpenKey.of("env", "api", "rule", "app", null, DIGEST)).matches("[0-9a-f]{64}");
    }

    @Test
    void refuses_text_that_is_not_valid_unicode() {
        assertThatThrownBy(() -> ApprovalOpenKey.of("env", "api", "rule", "app", "\uD800", "digest"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unicode");
    }
}
