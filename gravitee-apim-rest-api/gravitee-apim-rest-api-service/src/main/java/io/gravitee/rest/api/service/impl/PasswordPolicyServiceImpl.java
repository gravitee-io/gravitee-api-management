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

import io.gravitee.rest.api.model.PasswordPolicyEntity;
import io.gravitee.rest.api.model.PasswordPolicyRuleEntity;
import io.gravitee.rest.api.service.PasswordPolicyService;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@CustomLog
@Component
public class PasswordPolicyServiceImpl implements PasswordPolicyService {

    private final PasswordPolicyPatternParser patternParser = new PasswordPolicyPatternParser();

    @Value("${user.password.policy.description:}")
    private String passwordPolicyDescription;

    /**
     * The pattern shipped in gravitee.yml since Nov 2023, named here because the startup check has to
     * compare the configured pattern against it to tell a policy an operator chose from one they
     * inherited. {@code RegexPasswordValidator} still holds its own copy of the same literal.
     */
    static final String DEFAULT_PATTERN =
        "^(?=.*[0-9])(?=.*[A-Z])(?=.*[a-z])(?=.*[!~<>.,;:_=?/*+\\-#\\\"'&§`£€%°()|\\[\\]$^@])(?!.*(.)\\1{2,}).{12,128}$";

    @Value("${user.password.policy.pattern:" + DEFAULT_PATTERN + "}")
    private String passwordPolicyPattern;

    /**
     * The configured policy, reported rather than embellished.
     *
     * <p>{@code description} carries the operator's own sentence and nothing else. It used to fall
     * back to a join of the rule labels, which left a client unable to tell a sentence someone wrote
     * from one this service invented, and so unable to decide whether it was worth showing. An empty
     * description now means exactly one thing: nobody wrote one.
     */
    @Override
    public PasswordPolicyEntity getPasswordPolicy() {
        String pattern = trimmedPattern();
        return PasswordPolicyEntity.builder()
            .description(StringUtils.hasText(passwordPolicyDescription) ? passwordPolicyDescription.trim() : "")
            .pattern(pattern)
            .rules(resolveRules(pattern))
            .build();
    }

    /**
     * Ways this configuration will leave users guessing, reported once at startup.
     *
     * <p>Package-private and pure so the conditions can be exercised without a log appender.
     */
    @PostConstruct
    void reportConfiguration() {
        configurationWarnings().forEach(log::warn);
    }

    List<String> configurationWarnings() {
        List<String> warnings = new ArrayList<>();
        String pattern = trimmedPattern();

        if (!StringUtils.hasText(pattern)) {
            warnings.add(
                "user.password.policy.pattern is set but blank. The password policy carries no rule at all, and the " +
                    "validator built from the same property accepts no password. Set a pattern, or remove the key to " +
                    "fall back to the shipped default."
            );
        } else if (!DEFAULT_PATTERN.equals(pattern) && !StringUtils.hasText(passwordPolicyDescription)) {
            warnings.add(
                "user.password.policy.pattern is customized but user.password.policy.description is blank. The password " +
                    "policy then carries only the rules this service can derive from the pattern, so a requirement it " +
                    "cannot express reaches no client before a password is rejected. Write a description to explain the " +
                    "policy in your own words."
            );
        }

        List<String> untranslated = patternParser.untranslatedFragments(pattern);
        if (!untranslated.isEmpty()) {
            warnings.add(
                "user.password.policy.pattern contains requirements that could not be turned into a readable rule: " +
                    String.join(", ", untranslated) +
                    ". The password policy carries no rule for them. This check only inspects '(?=.*[...])' groups, so " +
                    "it finds gaps rather than proving there are none: a requirement written another way can be just as " +
                    "invisible without appearing here."
            );
        }

        return warnings;
    }

    private String trimmedPattern() {
        return passwordPolicyPattern == null ? null : passwordPolicyPattern.trim();
    }

    private List<PasswordPolicyRuleEntity> resolveRules(String policyPattern) {
        List<PasswordPolicyRuleEntity> rules = patternParser.parse(policyPattern);
        if (rules.isEmpty() && StringUtils.hasText(policyPattern)) {
            return List.of(buildFallbackRule(policyPattern));
        }
        return rules;
    }

    private static PasswordPolicyRuleEntity buildFallbackRule(String policyPattern) {
        return PasswordPolicyRuleEntity.builder()
            .id("policyPattern")
            .label("Matches the configured password policy")
            .pattern(policyPattern)
            .build();
    }
}
