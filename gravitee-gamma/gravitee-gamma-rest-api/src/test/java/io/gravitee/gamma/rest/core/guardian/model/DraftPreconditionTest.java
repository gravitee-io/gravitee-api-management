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
package io.gravitee.gamma.rest.core.guardian.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DraftPreconditionTest {

    @Test
    void should_be_satisfied_by_a_stated_version_only() {
        var precondition = DraftPrecondition.oneOf(Set.of(2, 4));

        assertThat(precondition.isSatisfiedBy(4)).isTrue();
        assertThat(precondition.isSatisfiedBy(3)).isFalse();
    }

    /** A draft stored before the counter existed is at no version, which is not one a caller can state. */
    @Test
    void should_not_be_satisfied_by_a_draft_that_has_no_version() {
        assertThat(DraftPrecondition.version(1).isSatisfiedBy(null)).isFalse();
    }

    /** Stating no version is not asking to overwrite: the two are different shapes on purpose. */
    @Test
    void should_refuse_to_state_no_version_at_all() {
        assertThatThrownBy(() -> DraftPrecondition.oneOf(Set.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DraftPrecondition.oneOf(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_not_follow_the_set_it_was_built_from() {
        var versions = new HashSet<>(Set.of(2));
        var precondition = DraftPrecondition.oneOf(versions);

        versions.add(3);

        assertThat(precondition.isSatisfiedBy(3)).isFalse();
    }
}
