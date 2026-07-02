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
package io.gravitee.gateway.services.sync.process.repository.synchronizer.authz;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AuthzAppliedRevisionsTest {

    private final AuthzAppliedRevisions revisions = new AuthzAppliedRevisions();

    @Test
    void applies_when_absent_then_skips_same_or_older() {
        assertThat(revisions.shouldApply("env", "scope", "doc", 100L)).isTrue();
        revisions.markApplied("env", "scope", "doc", 100L);
        assertThat(revisions.shouldApply("env", "scope", "doc", 100L)).isFalse();
        assertThat(revisions.shouldApply("env", "scope", "doc", 90L)).isFalse();
        assertThat(revisions.shouldApply("env", "scope", "doc", 101L)).isTrue();
    }

    @Test
    void markApplied_is_monotonic() {
        revisions.markApplied("env", "scope", "doc", 100L);
        revisions.markApplied("env", "scope", "doc", 50L);
        assertThat(revisions.shouldApply("env", "scope", "doc", 100L)).isFalse();
        assertThat(revisions.shouldApply("env", "scope", "doc", 101L)).isTrue();
    }

    @Test
    void forget_lets_the_same_revision_apply_again() {
        revisions.markApplied("env", "scope", "doc", 100L);
        revisions.forget("env", "scope", "doc");
        assertThat(revisions.shouldApply("env", "scope", "doc", 100L)).isTrue();
    }

    @Test
    void forgetScope_clears_only_that_scope() {
        revisions.markApplied("env", "scope-a", "doc", 100L);
        revisions.markApplied("env", "scope-b", "doc", 100L);
        revisions.forgetScope("env", "scope-a");
        assertThat(revisions.shouldApply("env", "scope-a", "doc", 100L)).isTrue();
        assertThat(revisions.shouldApply("env", "scope-b", "doc", 100L)).isFalse();
    }

    @Test
    void forgetScope_does_not_clear_a_prefix_sibling_scope() {
        revisions.markApplied("env", "scope", "doc", 100L);
        revisions.markApplied("env", "scope-b", "doc", 100L); // "scope" is a prefix of "scope-b"
        revisions.forgetScope("env", "scope");
        assertThat(revisions.shouldApply("env", "scope", "doc", 100L)).isTrue(); // cleared
        assertThat(revisions.shouldApply("env", "scope-b", "doc", 100L)).isFalse(); // NOT cleared
    }
}
