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
package io.gravitee.rest.api.validator;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author GraviteeSource Team
 */
class NoDuplicatesValidatorTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void should_accept_null_list() {
        assertThat(validator.validate(new CaseSensitiveHolder(null))).isEmpty();
        assertThat(validator.validate(new CaseInsensitiveHolder(null))).isEmpty();
    }

    @Test
    void should_accept_distinct_values() {
        assertThat(validator.validate(new CaseSensitiveHolder(List.of("a", "b", "c")))).isEmpty();
    }

    @Test
    void should_ignore_a_single_null_element() {
        assertThat(validator.validate(new CaseSensitiveHolder(Arrays.asList("a", null)))).isEmpty();
    }

    // --- ignoreCase = false (the default branch) ---

    @Test
    void should_reject_exact_duplicates_when_case_sensitive() {
        assertThat(validator.validate(new CaseSensitiveHolder(List.of("a", "a")))).isNotEmpty();
    }

    @Test
    void should_treat_case_differing_values_as_distinct_when_case_sensitive() {
        assertThat(validator.validate(new CaseSensitiveHolder(List.of("a", "A")))).isEmpty();
    }

    @Test
    void should_reject_whitespace_differing_duplicates_when_case_sensitive() {
        // both branches trim before comparing, so "a" and " a " collide even when case-sensitive
        assertThat(validator.validate(new CaseSensitiveHolder(List.of("a", " a ")))).isNotEmpty();
    }

    // --- ignoreCase = true ---

    @Test
    void should_reject_case_differing_duplicates_when_ignoring_case() {
        assertThat(validator.validate(new CaseInsensitiveHolder(List.of("a", " A ")))).isNotEmpty();
    }

    static class CaseSensitiveHolder {

        @NoDuplicates
        final List<String> values;

        CaseSensitiveHolder(List<String> values) {
            this.values = values;
        }
    }

    static class CaseInsensitiveHolder {

        @NoDuplicates(ignoreCase = true)
        final List<String> values;

        CaseInsensitiveHolder(List<String> values) {
            this.values = values;
        }
    }
}
