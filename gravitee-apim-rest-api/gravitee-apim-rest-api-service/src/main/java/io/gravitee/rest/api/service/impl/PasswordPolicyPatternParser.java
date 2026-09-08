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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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

        for (PasswordPolicyRuleEntity lookaheadRule : lookaheadContributions(policyPattern).values()) {
            if (lookaheadRule != null) {
                addRule(rules, seenRuleIds, lookaheadRule);
            }
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

    /**
     * The lookaheads this parser examined without producing a rule for them.
     *
     * <p>A requirement it cannot express is one a user only discovers by having their password
     * rejected, so the fragments are named rather than dropped in silence. A lookahead is reported
     * whether it could not be classified at all or was classified onto a rule another lookahead had
     * already claimed — {@code (?=.*[!@#])(?=.*[\-_])} yields one "special" rule, and the second
     * requirement is as invisible as one the parser never recognised.
     */
    public List<String> untranslatedFragments(String policyPattern) {
        if (policyPattern == null || policyPattern.isBlank()) {
            return List.of();
        }

        List<String> untranslated = new ArrayList<>();
        lookaheadContributions(policyPattern).forEach((charClass, rule) -> {
            if (rule == null) {
                untranslated.add(POSITIVE_LOOKAHEAD_PREFIX + charClass + "])");
            }
        });
        return List.copyOf(untranslated);
    }

    /**
     * Each distinct lookahead character class in the pattern, mapped to the rule it contributes to
     * {@link #parse}, or {@code null} when it contributes none.
     *
     * <p>One walk answers both "which rules does this pattern produce?" and "which of its lookaheads
     * produced nothing?", so the two cannot disagree about what was dropped.
     */
    private static Map<String, PasswordPolicyRuleEntity> lookaheadContributions(String policyPattern) {
        Map<String, PasswordPolicyRuleEntity> contributions = new LinkedHashMap<>();
        Set<String> claimedRuleIds = new LinkedHashSet<>();

        for (String charClass : extractPositiveLookaheadCharClasses(policyPattern)) {
            if (contributions.containsKey(charClass)) {
                continue;
            }
            PasswordPolicyRuleEntity rule = classifyLookahead(charClass).orElse(null);
            contributions.put(charClass, rule != null && claimedRuleIds.add(rule.getId()) ? rule : null);
        }

        return contributions;
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
