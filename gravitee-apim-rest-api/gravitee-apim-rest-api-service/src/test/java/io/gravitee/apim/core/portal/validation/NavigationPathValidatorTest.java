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
package io.gravitee.apim.core.portal.validation;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.core.validation.Validator;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class NavigationPathValidatorTest {

    @Test
    void should_accept_root_path() {
        assertThat(NavigationPathValidator.validate("/", "f")).isEmpty();
    }

    @Test
    void should_accept_nested_path() {
        assertThat(NavigationPathValidator.validate("/projects/alpha", "f")).isEmpty();
    }

    @Test
    void should_reject_null() {
        var errors = NavigationPathValidator.validate(null, "f");
        assertThat(errors).hasSize(1);
        assertThat(errors.get(0).isSevere()).isTrue();
        assertThat(errors.get(0).getMessage()).contains("f").contains("must not be empty");
    }

    @Test
    void should_reject_blank() {
        var errors = NavigationPathValidator.validate("  ", "f");
        assertThat(errors).anyMatch(e -> e.getMessage().contains("must not be empty"));
    }

    @Test
    void should_reject_path_not_starting_with_slash() {
        var errors = NavigationPathValidator.validate("projects/alpha", "f");
        assertThat(errors).anyMatch(e -> e.getMessage().contains("must start with '/'"));
    }

    @Test
    void should_reject_path_with_consecutive_slashes() {
        var errors = NavigationPathValidator.validate("/projects//alpha", "f");
        assertThat(errors).anyMatch(e -> e.getMessage().contains("consecutive"));
    }

    @Test
    void should_reject_path_with_parent_traversal() {
        var errors = NavigationPathValidator.validate("/projects/../alpha", "f");
        assertThat(errors).anyMatch(e -> e.getMessage().contains(".."));
    }

    @Test
    void should_reject_path_with_trailing_slash() {
        var errors = NavigationPathValidator.validate("/projects/alpha/", "f");
        assertThat(errors).anyMatch(e -> e.getMessage().contains("end with"));
    }

    @Test
    void should_only_emit_severe_errors() {
        var errors = NavigationPathValidator.validate("bad", "f");
        assertThat(errors).allMatch(Validator.Error::isSevere);
    }

    @Test
    void should_interpolate_field_name_into_messages() {
        var errors = NavigationPathValidator.validate("bad", "apis[2].location");
        assertThat(errors).allMatch(e -> e.getMessage().startsWith("apis[2].location"));
    }
}
