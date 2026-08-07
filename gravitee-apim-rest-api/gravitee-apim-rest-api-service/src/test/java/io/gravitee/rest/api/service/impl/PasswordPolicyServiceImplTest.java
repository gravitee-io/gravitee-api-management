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

import io.gravitee.rest.api.model.PasswordPolicyEntity;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class PasswordPolicyServiceImplTest {

    private PasswordPolicyServiceImpl passwordPolicyService;

    @BeforeEach
    void setUp() {
        passwordPolicyService = new PasswordPolicyServiceImpl();
    }

    @Test
    void should_return_rules_parsed_from_configured_pattern() {
        ReflectionTestUtils.setField(passwordPolicyService, "passwordPolicyDescription", "   ");
        ReflectionTestUtils.setField(
            passwordPolicyService,
            "passwordPolicyPattern",
            "^(?=.*[0-9])(?=.*[A-Z])(?=.*[a-z])(?=.*[!@#$])(?!.*(.)\\1{2,}).{10,64}$"
        );

        PasswordPolicyEntity policy = passwordPolicyService.getPasswordPolicy();

        assertThat(policy.getDescription()).contains("At least 10 characters");
        assertThat(policy.getDescription()).contains("At most 64 characters");
        assertThat(policy.getDescription()).doesNotContain("At least 12 characters");
        assertThat(policy.getPattern()).contains(".{10,64}");
        assertThat(policy.getRules())
            .extracting("id")
            .containsExactly("minLength", "maxLength", "digit", "uppercase", "lowercase", "special", "noConsecutive");
        assertThat(policy.getRules().getFirst().getLabel()).isEqualTo("At least 10 characters");
    }

    @Test
    void should_fallback_to_full_pattern_when_parser_yields_no_rules() {
        ReflectionTestUtils.setField(passwordPolicyService, "passwordPolicyDescription", "   ");
        ReflectionTestUtils.setField(passwordPolicyService, "passwordPolicyPattern", "^\\d{8,}$");

        PasswordPolicyEntity policy = passwordPolicyService.getPasswordPolicy();

        assertThat(policy.getRules()).hasSize(1);
        assertThat(policy.getRules().getFirst().getId()).isEqualTo("policyPattern");
        assertThat(policy.getRules().getFirst().getPattern()).isEqualTo("^\\d{8,}$");
        assertThat(policy.getDescription()).contains("Matches the configured password policy");
    }

    @Test
    void should_fallback_to_full_pattern_for_non_standard_pattern() {
        ReflectionTestUtils.setField(passwordPolicyService, "passwordPolicyDescription", "   ");
        ReflectionTestUtils.setField(passwordPolicyService, "passwordPolicyPattern", "^\\w{8,}$");

        PasswordPolicyEntity policy = passwordPolicyService.getPasswordPolicy();

        assertThat(policy.getRules()).hasSize(1);
        assertThat(policy.getRules().getFirst().getId()).isEqualTo("policyPattern");
        assertThat(policy.getRules().getFirst().getPattern()).isEqualTo("^\\w{8,}$");

        Pattern compiledPattern = Pattern.compile(policy.getRules().getFirst().getPattern());
        assertThat(compiledPattern.matcher("Password1").matches()).isTrue();
        assertThat(compiledPattern.matcher("short").matches()).isFalse();
    }

    @Test
    void should_return_empty_rules_for_whitespace_only_pattern() {
        ReflectionTestUtils.setField(passwordPolicyService, "passwordPolicyDescription", "   ");
        ReflectionTestUtils.setField(passwordPolicyService, "passwordPolicyPattern", "   ");

        PasswordPolicyEntity policy = passwordPolicyService.getPasswordPolicy();

        assertThat(policy.getPattern()).isEmpty();
        assertThat(policy.getRules()).isEmpty();
        assertThat(policy.getDescription()).isEmpty();
    }

    @Test
    void should_trim_pattern_before_parsing() {
        ReflectionTestUtils.setField(passwordPolicyService, "passwordPolicyDescription", "   ");
        ReflectionTestUtils.setField(passwordPolicyService, "passwordPolicyPattern", "  ^(?=.*[0-9]).{8,64}$  ");

        PasswordPolicyEntity policy = passwordPolicyService.getPasswordPolicy();

        assertThat(policy.getPattern()).isEqualTo("^(?=.*[0-9]).{8,64}$");
        assertThat(policy.getRules()).extracting("id").containsExactly("minLength", "maxLength", "digit");
    }

    @Test
    void should_not_fallback_when_parser_returns_partial_rules() {
        ReflectionTestUtils.setField(passwordPolicyService, "passwordPolicyDescription", "   ");
        ReflectionTestUtils.setField(passwordPolicyService, "passwordPolicyPattern", "^(?=.*[0-9])(?=.*[unclosed).{8,64}$");

        PasswordPolicyEntity policy = passwordPolicyService.getPasswordPolicy();

        assertThat(policy.getRules()).extracting("id").containsExactly("minLength", "maxLength", "digit");
    }

    @Test
    void should_return_configured_description_when_present() {
        ReflectionTestUtils.setField(passwordPolicyService, "passwordPolicyDescription", "Custom password policy description.");
        ReflectionTestUtils.setField(passwordPolicyService, "passwordPolicyPattern", "^(?=.*[0-9]).{8,64}$");

        PasswordPolicyEntity policy = passwordPolicyService.getPasswordPolicy();

        assertThat(policy.getDescription()).isEqualTo("Custom password policy description.");
    }
}
