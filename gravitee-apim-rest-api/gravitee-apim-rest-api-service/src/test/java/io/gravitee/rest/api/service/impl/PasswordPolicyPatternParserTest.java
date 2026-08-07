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
package io.gravitee.rest.api.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PasswordPolicyPatternParserTest {

    private static final String DEFAULT_PATTERN =
        "^(?=.*[0-9])(?=.*[A-Z])(?=.*[a-z])(?=.*[!~<>.,;:_=?/*+\\-#\\\"'&§`£€%°()|\\[\\]$^@])(?!.*(.)\\1{2,}).{12,128}$";

    private PasswordPolicyPatternParser parser;

    @BeforeEach
    void setUp() {
        parser = new PasswordPolicyPatternParser();
    }

    @Test
    void should_parse_default_gravitee_pattern() {
        var rules = parser.parse(DEFAULT_PATTERN);

        assertThat(rules).extracting("id").containsExactly("minLength", "digit", "uppercase", "lowercase", "special", "noConsecutive");
        assertThat(rules.getFirst().getLabel()).isEqualTo("At least 12 characters");
        assertThat(rules.getFirst().getPattern()).isEqualTo(".{12,}");
        assertThat(rules).allMatch(rule -> rule.getPattern() != null && !rule.getPattern().isBlank());
    }

    @Test
    void should_parse_custom_minimum_length() {
        var rules = parser.parse("^(?=.*[0-9]).{8,64}$");

        assertThat(rules).extracting("id").containsExactly("minLength", "digit");
        assertThat(rules.getFirst().getLabel()).isEqualTo("At least 8 characters");
    }

    @Test
    void should_return_empty_rules_when_pattern_is_blank() {
        assertThat(parser.parse("")).isEmpty();
        assertThat(parser.parse(null)).isEmpty();
    }
}
