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
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class PasswordPolicyServiceImpl implements PasswordPolicyService {

    private final PasswordPolicyPatternParser patternParser = new PasswordPolicyPatternParser();

    @Value("${user.password.policy.description:}")
    private String passwordPolicyDescription;

    @Value(
        "${user.password.policy.pattern:^(?=.*[0-9])(?=.*[A-Z])(?=.*[a-z])(?=.*[!~<>.,;:_=?/*+\\-#\\\"'&§`£€%°()|\\[\\]$^@])(?!.*(.)\\1{2,}).{12,128}$}"
    )
    private String passwordPolicyPattern;

    @Override
    public PasswordPolicyEntity getPasswordPolicy() {
        String pattern = passwordPolicyPattern == null ? null : passwordPolicyPattern.trim();
        List<PasswordPolicyRuleEntity> rules = resolveRules(pattern);
        return PasswordPolicyEntity.builder().description(resolveDescription(rules)).pattern(pattern).rules(rules).build();
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

    private String resolveDescription(List<PasswordPolicyRuleEntity> rules) {
        if (StringUtils.hasText(passwordPolicyDescription)) {
            return passwordPolicyDescription.trim();
        }
        if (rules.isEmpty()) {
            return "";
        }
        return rules
            .stream()
            .map(PasswordPolicyRuleEntity::getLabel)
            .collect(Collectors.joining(", ", "Password must meet the following requirements: ", "."));
    }
}
