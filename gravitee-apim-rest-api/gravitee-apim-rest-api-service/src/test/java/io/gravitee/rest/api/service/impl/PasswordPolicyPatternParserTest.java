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

import java.util.regex.Pattern;
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

        assertThat(rules)
            .extracting("id")
            .containsExactly("minLength", "maxLength", "digit", "uppercase", "lowercase", "special", "noConsecutive");
        assertThat(rules.getFirst().getLabel()).isEqualTo("At least 12 characters");
        assertThat(rules.getFirst().getPattern()).isEqualTo("^.{12,}$");
        assertThat(rules.get(1).getLabel()).isEqualTo("At most 128 characters");
        assertThat(rules.get(1).getPattern()).isEqualTo("^.{0,128}$");
        assertThat(rules).allMatch(rule -> rule.getPattern() != null && !rule.getPattern().isBlank());
    }

    @Test
    void should_parse_custom_minimum_length() {
        var rules = parser.parse("^(?=.*[0-9]).{8,64}$");

        assertThat(rules).extracting("id").containsExactly("minLength", "maxLength", "digit");
        assertThat(rules.getFirst().getLabel()).isEqualTo("At least 8 characters");
        assertThat(rules.getFirst().getPattern()).isEqualTo("^.{8,}$");
        assertThat(rules.get(1).getLabel()).isEqualTo("At most 64 characters");
        assertThat(rules.get(1).getPattern()).isEqualTo("^.{0,64}$");
    }

    @Test
    void should_reject_passwords_not_meeting_length_constraints() {
        var rules = parser.parse("^(?=.*[0-9]).{8,64}$");
        var minLengthRule = rules
            .stream()
            .filter(rule -> "minLength".equals(rule.getId()))
            .findFirst()
            .orElseThrow();
        var maxLengthRule = rules
            .stream()
            .filter(rule -> "maxLength".equals(rule.getId()))
            .findFirst()
            .orElseThrow();

        assertThat(Pattern.compile(minLengthRule.getPattern()).matcher("a".repeat(7)).matches()).isFalse();
        assertThat(Pattern.compile(minLengthRule.getPattern()).matcher("a".repeat(8)).matches()).isTrue();

        assertThat(Pattern.compile(maxLengthRule.getPattern()).matcher("a".repeat(65)).matches()).isFalse();
        assertThat(Pattern.compile(maxLengthRule.getPattern()).matcher("a".repeat(64)).matches()).isTrue();
    }

    @Test
    void should_parse_minimum_length_without_maximum() {
        var rules = parser.parse("^(?=.*[0-9]).{12,}$");

        assertThat(rules).extracting("id").containsExactly("minLength", "digit");
        assertThat(rules.getFirst().getPattern()).isEqualTo("^.{12,}$");
    }

    @Test
    void should_parse_exact_length_as_min_and_max_rules() {
        var rules = parser.parse("^(?=.*[0-9]).{8,8}$");

        assertThat(rules).extracting("id").containsExactly("minLength", "maxLength", "digit");

        Pattern minLength = Pattern.compile(rules.get(0).getPattern());
        Pattern maxLength = Pattern.compile(rules.get(1).getPattern());

        assertThat(minLength.matcher("1234567").matches()).isFalse();
        assertThat(minLength.matcher("12345678").matches()).isTrue();
        assertThat(minLength.matcher("123456789").matches()).isTrue();

        assertThat(maxLength.matcher("1234567").matches()).isTrue();
        assertThat(maxLength.matcher("12345678").matches()).isTrue();
        assertThat(maxLength.matcher("123456789").matches()).isFalse();

        assertThat(allRulesSatisfied("12345678", rules)).isTrue();
        assertThat(allRulesSatisfied("1234567", rules)).isFalse();
        assertThat(allRulesSatisfied("123456789", rules)).isFalse();
    }

    @Test
    void should_validate_default_pattern_password_against_decomposed_rules() {
        var rules = parser.parse(DEFAULT_PATTERN);
        String validPassword = "LongEnough1!abc";

        assertThat(allRulesSatisfied(validPassword, rules)).isTrue();
        assertThat(Pattern.compile(DEFAULT_PATTERN).matcher(validPassword).matches()).isTrue();

        assertThat(allRulesSatisfied("Short1!a", rules)).isFalse();
        assertThat(allRulesSatisfied("longenough1!abc", rules)).isFalse();
        assertThat(allRulesSatisfied("LongEnough1!aaa", rules)).isFalse();

        String maxLengthPassword = buildPasswordWithoutTripleConsecutive(validPassword, 128);
        assertThat(maxLengthPassword).hasSize(128);
        assertThat(allRulesSatisfied(maxLengthPassword, rules)).isTrue();
        assertThat(Pattern.compile(DEFAULT_PATTERN).matcher(maxLengthPassword).matches()).isTrue();

        String tooLongPassword = buildPasswordWithoutTripleConsecutive(validPassword, 129);
        assertThat(tooLongPassword).hasSize(129);
        assertThat(allRulesSatisfied(tooLongPassword, rules)).isFalse();
        assertThat(Pattern.compile(DEFAULT_PATTERN).matcher(tooLongPassword).matches()).isFalse();
    }

    @Test
    void should_not_emit_duplicate_rule_ids() {
        var rules = parser.parse(DEFAULT_PATTERN);

        assertThat(
            rules
                .stream()
                .map(rule -> rule.getId())
                .distinct()
        ).hasSameSizeAs(rules);
    }

    @Test
    void should_ignore_unrecognized_lookaheads_without_length_quantifier() {
        assertThat(parser.parse("(?=.*[unclosed")).isEmpty();
        assertThat(parser.parse("^(?=.*\\d).+$")).isEmpty();
    }

    @Test
    void should_return_empty_rules_when_pattern_is_blank() {
        assertThat(parser.parse("")).isEmpty();
        assertThat(parser.parse(null)).isEmpty();
    }

    @Test
    void should_not_hang_on_unbalanced_char_class() {
        assertThat(parser.parse("^(?=.*[0-9])(?=.*[unclosed")).extracting("id").containsExactly("digit");
        assertThat(parser.parse("(?=.*[unclosed")).isEmpty();
    }

    private static String buildPasswordWithoutTripleConsecutive(String prefix, int targetLength) {
        StringBuilder password = new StringBuilder(prefix);
        char[] alternates = { 'y', 'z' };
        int index = 0;
        while (password.length() < targetLength) {
            password.append(alternates[index % alternates.length]);
            index++;
        }
        return password.toString();
    }

    private static boolean allRulesSatisfied(String password, java.util.List<io.gravitee.rest.api.model.PasswordPolicyRuleEntity> rules) {
        return rules.stream().allMatch(rule -> Pattern.compile(rule.getPattern()).matcher(password).find());
    }
}
