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

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class LinkUrlValidatorTest {

    @Test
    void should_accept_an_https_url() {
        assertThat(LinkUrlValidator.isWellFormedAbsoluteUrl("https://docs.example.com")).isTrue();
    }

    @Test
    void should_accept_an_http_url_with_a_path() {
        assertThat(LinkUrlValidator.isWellFormedAbsoluteUrl("http://example.com/path")).isTrue();
    }

    @Test
    void should_reject_a_relative_path() {
        assertThat(LinkUrlValidator.isWellFormedAbsoluteUrl("/relative/path")).isFalse();
    }

    @Test
    void should_reject_a_string_with_no_scheme() {
        assertThat(LinkUrlValidator.isWellFormedAbsoluteUrl("invalid-url")).isFalse();
    }

    @Test
    void should_reject_blank() {
        assertThat(LinkUrlValidator.isWellFormedAbsoluteUrl("  ")).isFalse();
    }

    @Test
    void should_reject_null() {
        assertThat(LinkUrlValidator.isWellFormedAbsoluteUrl(null)).isFalse();
    }
}
