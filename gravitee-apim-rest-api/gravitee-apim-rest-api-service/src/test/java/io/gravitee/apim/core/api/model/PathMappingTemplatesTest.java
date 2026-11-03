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
package io.gravitee.apim.core.api.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.apim.core.api.exception.InvalidPathMappingsException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.PatternSyntaxException;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PathMappingTemplatesTest {

    @Test
    void should_accept_template_with_parameters() {
        assertThatCode(() -> PathMappingTemplates.validate(Set.of("/charges/:chargeId/refunds"))).doesNotThrowAnyException();
    }

    @Test
    void should_reject_glob_stars_via_form_rule() {
        assertThat(PathMappingTemplates.matchesFormRule("/a/**")).isFalse();
        assertThatThrownBy(() -> PathMappingTemplates.validate(Set.of("/a/**"))).isInstanceOf(InvalidPathMappingsException.class);
    }

    @Test
    void should_reject_backslash() {
        assertThatThrownBy(() -> PathMappingTemplates.validate(Set.of("/a\\"))).isInstanceOf(InvalidPathMappingsException.class);
    }

    @Test
    void should_reject_values_outside_form_rule() {
        assertThatThrownBy(() -> PathMappingTemplates.validate(List.of("/", "/a b", "/users/@me", "/a/.*")))
            .isInstanceOf(InvalidPathMappingsException.class)
            .hasMessageContaining("[/, /a b, /users/@me, /a/.*]");
    }

    @Test
    void should_reject_value_that_only_matches_a_prefix_of_the_form_rule() {
        assertThat(PathMappingTemplates.matchesFormRule("/charges/:chargeId extra")).isFalse();
        assertThatThrownBy(() -> PathMappingTemplates.validate(Set.of("/charges/:chargeId extra"))).isInstanceOf(
            InvalidPathMappingsException.class
        );
    }

    @Test
    void should_reject_blank() {
        assertThatThrownBy(() -> PathMappingTemplates.validate(Set.of("  "))).isInstanceOf(InvalidPathMappingsException.class);
    }

    @Test
    void should_reject_null() {
        var setWithNull = new HashSet<String>();
        setWithNull.add((String) null);
        assertThatThrownBy(() -> PathMappingTemplates.validate(setWithNull)).isInstanceOf(InvalidPathMappingsException.class);
    }

    @Test
    void should_name_every_offending_value_in_message() {
        assertThatThrownBy(() -> PathMappingTemplates.validate(List.of("/a/**", "*")))
            .isInstanceOf(InvalidPathMappingsException.class)
            .hasMessageContaining("[/a/**, *]")
            .cause()
            .isInstanceOf(PatternSyntaxException.class);
    }

    @Test
    void should_accept_null_collection() {
        assertThatCode(() -> PathMappingTemplates.validate(null)).doesNotThrowAnyException();
    }

    @Test
    void isCompilable_should_return_true_for_valid_template() {
        assertThat(PathMappingTemplates.isCompilable("/charges/:chargeId")).isTrue();
    }

    @Test
    void isCompilable_should_return_false_for_blank() {
        assertThat(PathMappingTemplates.isCompilable("  ")).isFalse();
    }

    @Test
    void isCompilable_should_return_false_for_null() {
        assertThat(PathMappingTemplates.isCompilable(null)).isFalse();
    }

    @Test
    void isCompilable_should_return_false_for_glob_stars_outside_form_rule() {
        assertThat(PathMappingTemplates.isCompilable("/a/**")).isFalse();
    }

    @Test
    void isCompilable_should_return_false_for_value_outside_form_rule() {
        assertThat(PathMappingTemplates.isCompilable("/")).isFalse();
        assertThat(PathMappingTemplates.isCompilable("/a b")).isFalse();
        assertThat(PathMappingTemplates.isCompilable("/users/@me")).isFalse();
        assertThat(PathMappingTemplates.isCompilable("/a/.*")).isFalse();
        assertThat(PathMappingTemplates.isCompilable("/a\\")).isFalse();
    }
}
