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

import io.gravitee.rest.api.model.PasswordPolicyRuleEntity;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Derives UI password requirement rules from {@code user.password.policy.pattern} in gravitee.yml.
 */
public class PasswordPolicyPatternParser {

    private static final String POSITIVE_LOOKAHEAD_PREFIX = "(?=.*[";
    private static final Pattern LENGTH_QUANTIFIER = Pattern.compile("\\.\\{(\\d+)(?:,(\\d+)?)?\\}\\$?");

    public List<PasswordPolicyRuleEntity> parse(String policyPattern) {
        if (policyPattern == null || policyPattern.isBlank()) {
            return List.of();
        }

        List<PasswordPolicyRuleEntity> rules = new ArrayList<>();
        Set<String> seenRuleIds = new LinkedHashSet<>();

        Matcher lengthMatcher = LENGTH_QUANTIFIER.matcher(policyPattern);
        if (lengthMatcher.find()) {
            int minLength = Integer.parseInt(lengthMatcher.group(1));
            String maxLengthGroup = lengthMatcher.group(2);
            addRule(
                rules,
                seenRuleIds,
                PasswordPolicyRuleEntity.builder()
                    .id("minLength")
                    .label("At least " + minLength + " characters")
                    .pattern(String.format("^.{%d,}$", minLength))
                    .build()
            );
            if (maxLengthGroup != null && !maxLengthGroup.isBlank()) {
                int maxLength = Integer.parseInt(maxLengthGroup);
                addRule(
                    rules,
                    seenRuleIds,
                    PasswordPolicyRuleEntity.builder()
                        .id("maxLength")
                        .label("At most " + maxLength + " characters")
                        .pattern(String.format("^.{0,%d}$", maxLength))
                        .build()
                );
            }
        }

        for (String charClass : extractPositiveLookaheadCharClasses(policyPattern)) {
            classifyLookahead(charClass).ifPresent(rule -> addRule(rules, seenRuleIds, rule));
        }

        if (policyPattern.contains("(?!.*(.)\\1{2,})")) {
            addRule(
                rules,
                seenRuleIds,
                PasswordPolicyRuleEntity.builder()
                    .id("noConsecutive")
                    .label("No more than 2 consecutive equal characters")
                    .pattern("^(?!.*(.)\\1{2,}).+$")
                    .build()
            );
        }

        return List.copyOf(rules);
    }

    private static List<String> extractPositiveLookaheadCharClasses(String policyPattern) {
        List<String> charClasses = new ArrayList<>();
        int index = 0;

        while ((index = policyPattern.indexOf(POSITIVE_LOOKAHEAD_PREFIX, index)) >= 0) {
            int start = index + POSITIVE_LOOKAHEAD_PREFIX.length();
            int end = findCharClassEnd(policyPattern, start);
            if (end > start) {
                charClasses.add(policyPattern.substring(start, end));
                index = end + 1;
            } else if (end >= 0) {
                index = end + 1;
            } else {
                index += POSITIVE_LOOKAHEAD_PREFIX.length();
            }
        }

        return charClasses;
    }

    private static int findCharClassEnd(String policyPattern, int start) {
        for (int i = start; i < policyPattern.length(); i++) {
            if (policyPattern.charAt(i) == '\\' && i + 1 < policyPattern.length()) {
                i++;
                continue;
            }
            if (policyPattern.charAt(i) == ']') {
                return i;
            }
        }
        return -1;
    }

    private static void addRule(List<PasswordPolicyRuleEntity> rules, Set<String> seenRuleIds, PasswordPolicyRuleEntity rule) {
        if (seenRuleIds.add(rule.getId())) {
            rules.add(rule);
        }
    }

    private static java.util.Optional<PasswordPolicyRuleEntity> classifyLookahead(String charClass) {
        if ("0-9".equals(charClass)) {
            return java.util.Optional.of(
                PasswordPolicyRuleEntity.builder().id("digit").label("Contains a number").pattern("[0-9]").build()
            );
        }
        if ("A-Z".equals(charClass)) {
            return java.util.Optional.of(
                PasswordPolicyRuleEntity.builder().id("uppercase").label("Contains uppercase letter").pattern("[A-Z]").build()
            );
        }
        if ("a-z".equals(charClass)) {
            return java.util.Optional.of(
                PasswordPolicyRuleEntity.builder().id("lowercase").label("Contains lowercase letter").pattern("[a-z]").build()
            );
        }
        if (containsSpecialCharacter(charClass)) {
            return java.util.Optional.of(
                PasswordPolicyRuleEntity.builder()
                    .id("special")
                    .label("Contains a special character")
                    .pattern("[" + charClass + "]")
                    .build()
            );
        }
        return java.util.Optional.empty();
    }

    private static boolean containsSpecialCharacter(String charClass) {
        for (int i = 0; i < charClass.length(); i++) {
            char current = charClass.charAt(i);
            if (current == '\\' && i + 1 < charClass.length()) {
                i++;
                continue;
            }
            if (!Character.isLetterOrDigit(current) && current != '-') {
                return true;
            }
        }
        return false;
    }
}
