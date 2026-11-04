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
package io.gravitee.gateway.handlers.api.manager.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.gateway.handlers.api.manager.DeployedApprovalRule;
import io.gravitee.gateway.handlers.api.manager.DeployedApprovalRule.ArgumentCondition;
import io.gravitee.gateway.handlers.api.manager.DeployedApprovalRule.ToolBinding;
import io.gravitee.node.api.license.License;
import io.gravitee.node.api.license.LicenseManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApprovalRuleManagerImplTest {

    private static final String API = "api-1";

    @Mock
    private LicenseManager licenseManager;

    @Mock
    private License license;

    private ApprovalRuleManagerImpl manager;

    @BeforeEach
    void setUp() {
        lenient().when(licenseManager.getOrganizationLicenseOrPlatform(any())).thenReturn(license);
        lenient().when(license.isFeatureEnabled("gamma-aim-module")).thenReturn(true);
        manager = new ApprovalRuleManagerImpl(new ApprovalFeature(licenseManager, true));
    }

    @Nested
    class LicenseTest {

        @Test
        void should_still_deploy_when_gamma_is_disabled() {
            manager = new ApprovalRuleManagerImpl(new ApprovalFeature(licenseManager, false));

            deployAndCommit(rule("rule-1", "env-1", "payments", 1L));

            assertThat(manager.hasRules("env-1")).isTrue();
        }

        /** Without rule sync (Kubernetes, local or no repository), "no rule" would let every call through. */
        @Test
        void should_not_receive_rules_before_a_rule_source_loaded_them() {
            assertThat(manager.receivesRules("org-1")).isFalse();
        }

        @Test
        void should_receive_rules_once_a_sync_pass_loaded_them_even_when_there_are_none() {
            manager.commit();

            assertThat(manager.receivesRules("org-1")).isTrue();
        }

        @Test
        void should_not_receive_rules_of_an_organization_not_licensed_for_them() {
            when(license.isFeatureEnabled("gamma-aim-module")).thenReturn(false);
            manager.commit();

            assertThat(manager.receivesRules("org-1")).isFalse();
        }

        @Test
        void should_not_check_the_license_on_deploy() {
            manager.deploy(rule("rule-1", "env-1", "payments", 1L));

            verify(licenseManager, times(0)).getOrganizationLicenseOrPlatform(any());
        }

        /**
         * deploy() only ever runs once per rule, when the control plane publishes it; the license can change afterwards
         * with no new event to re-stage the rule against. Checking the license there instead of on read would let a
         * transient or stale license lookup permanently drop a rule, leaving its calls uncovered even once licensed.
         */
        @Test
        void should_cover_a_call_once_licensed_even_when_the_rule_was_deployed_while_unlicensed() {
            when(license.isFeatureEnabled("gamma-aim-module")).thenReturn(false);
            deployAndCommit(rule("rule-1", "env-1", "payments", 1L));
            assertThat(manager.receivesRules("org-1")).isFalse();

            when(license.isFeatureEnabled("gamma-aim-module")).thenReturn(true);

            assertThat(manager.receivesRules("org-1")).isTrue();
            assertThat(manager.candidates("env-1", null, "payments", null)).extracting(DeployedApprovalRule::id).containsExactly("rule-1");
        }
    }

    @Nested
    class DeployTest {

        @Test
        void should_not_show_a_staged_rule_before_the_commit() {
            manager.deploy(rule("rule-1", "env-1", "payments", 1L));

            assertThat(manager.hasRules("env-1")).isFalse();
            assertThat(manager.candidates("env-1", API, "payments", null)).isEmpty();
        }

        @Test
        void should_deploy_a_rule_on_commit() {
            DeployedApprovalRule rule = rule("rule-1", "env-1", "payments", 1L);

            deployAndCommit(rule);

            assertThat(manager.hasRules("env-1")).isTrue();
            assertThat(manager.candidates("env-1", API, "payments", null)).containsExactly(rule);
        }

        @Test
        void should_replace_a_rule_with_a_newer_copy() {
            deployAndCommit(rule("rule-1", "env-1", "payments", 1L));

            deployAndCommit(rule("rule-1", "env-1", "refunds", 2L));

            assertThat(manager.candidates("env-1", API, "payments", null)).isEmpty();
            assertThat(manager.candidates("env-1", API, "refunds", null)).extracting(DeployedApprovalRule::updatedAt).containsExactly(2L);
        }

        @Test
        void should_keep_the_newer_copy_when_an_older_one_arrives_in_a_later_pass() {
            deployAndCommit(rule("rule-1", "env-1", "refunds", 2L));

            deployAndCommit(rule("rule-1", "env-1", "payments", 1L));

            assertThat(manager.candidates("env-1", API, "refunds", null)).hasSize(1);
            assertThat(manager.candidates("env-1", API, "payments", null)).isEmpty();
        }

        @Test
        void should_keep_the_newer_copy_when_an_older_one_is_staged_after_it_in_the_same_pass() {
            manager.deploy(rule("rule-1", "env-1", "refunds", 2L));
            manager.deploy(rule("rule-1", "env-1", "payments", 1L));
            manager.commit();

            assertThat(manager.candidates("env-1", API, "refunds", null)).hasSize(1);
            assertThat(manager.candidates("env-1", API, "payments", null)).isEmpty();
        }

        @Test
        void should_rebuild_an_environment_once_per_commit_whatever_the_number_of_rules() {
            ApprovalRuleManagerImpl spied = spy(manager);

            IntStream.range(0, 100).forEach(i -> spied.deploy(rule("rule-" + i, "env-1", "tool-" + i, 1L)));
            spied.commit();

            verify(spied, times(1)).index(anyCollection());
            assertThat(IntStream.range(0, 100).mapToObj(i -> spied.candidates("env-1", API, "tool-" + i, null))).allMatch(
                rules -> rules.size() == 1
            );
        }

        @Test
        void should_rebuild_only_the_environments_touched_by_the_pass() {
            deployAndCommit(rule("rule-1", "env-1", "payments", 1L));
            ApprovalRuleManagerImpl spied = spy(manager);

            spied.deploy(rule("rule-2", "env-2", "payments", 1L));
            spied.commit();

            verify(spied, times(1)).index(anyCollection());
        }

        @Test
        void should_not_rebuild_anything_when_nothing_was_staged() {
            ApprovalRuleManagerImpl spied = spy(manager);

            spied.commit();

            verify(spied, times(0)).index(anyCollection());
        }

        @Test
        void should_not_lose_rules_staged_from_several_threads() throws Exception {
            ExecutorService executor = Executors.newFixedThreadPool(8);
            try {
                CountDownLatch start = new CountDownLatch(1);
                List<Future<?>> stagings = new ArrayList<>();
                for (int i = 0; i < 1_000; i++) {
                    String id = "rule-" + i;
                    stagings.add(
                        executor.submit(() -> {
                            start.await();
                            manager.deploy(rule(id, "env-1", "payments", 1L));
                            return null;
                        })
                    );
                }
                start.countDown();
                for (Future<?> staging : stagings) {
                    staging.get();
                }
                manager.commit();

                assertThat(manager.candidates("env-1", API, "payments", null)).hasSize(1_000);
            } finally {
                executor.shutdownNow();
            }
        }
    }

    @Nested
    class CandidatesTest {

        @Test
        void should_order_rules_by_priority_then_creation_then_id() {
            DeployedApprovalRule lowPriority = rule("a-low", "env-1", 20, 1L);
            DeployedApprovalRule olderB = rule("b-older", "env-1", 10, 100L);
            DeployedApprovalRule olderA = rule("a-older", "env-1", 10, 100L);
            DeployedApprovalRule newer = rule("a-newer", "env-1", 10, 200L);
            DeployedApprovalRule highPriority = rule("z-high", "env-1", 1, 999L);
            List.of(lowPriority, olderB, newer, highPriority, olderA).forEach(manager::deploy);
            manager.commit();

            assertThat(manager.candidates("env-1", API, "payments", null)).containsExactly(
                highPriority,
                olderA,
                olderB,
                newer,
                lowPriority
            );
        }

        @Test
        void should_return_the_rules_naming_the_tool_in_any_trimmed_alternative() {
            DeployedApprovalRule refunds = rule("refunds", "env-1", " refund_order | cancel_order ", 1L);
            DeployedApprovalRule payments = rule("payments", "env-1", "pay", 1L);
            deployAndCommit(refunds, payments);

            assertThat(manager.candidates("env-1", API, "cancel_order", null)).containsExactly(refunds);
            assertThat(manager.candidates("env-1", API, "refund_order", null)).containsExactly(refunds);
            assertThat(manager.candidates("env-1", API, "pay", null)).containsExactly(payments);
            assertThat(manager.candidates("env-1", API, "refund", null)).isEmpty();
        }

        @Test
        void should_match_exact_names_case_sensitively() {
            deployAndCommit(rule("refunds", "env-1", "refund_order", 1L));

            assertThat(manager.candidates("env-1", API, "REFUND_ORDER", null)).isEmpty();
        }

        @Test
        void should_merge_wildcard_rules_with_exact_ones_in_evaluation_order() {
            DeployedApprovalRule first = new DeployedApprovalRule(
                "first",
                "env-1",
                "org-1",
                "refund_*",
                List.of(),
                List.of(),
                List.of(),
                1,
                1L,
                1L
            );
            DeployedApprovalRule second = new DeployedApprovalRule(
                "second",
                "env-1",
                "org-1",
                "refund_order",
                List.of(),
                List.of(),
                List.of(),
                2,
                1L,
                1L
            );
            DeployedApprovalRule third = new DeployedApprovalRule(
                "third",
                "env-1",
                "org-1",
                "other|*",
                List.of(),
                List.of(),
                List.of(),
                3,
                1L,
                1L
            );
            DeployedApprovalRule fourth = new DeployedApprovalRule(
                "fourth",
                "env-1",
                "org-1",
                "x|refund_order",
                List.of(),
                List.of(),
                List.of(),
                4,
                1L,
                1L
            );
            deployAndCommit(fourth, third, second, first);

            assertThat(manager.candidates("env-1", API, "refund_order", null)).containsExactly(first, second, third, fourth);
            assertThat(manager.candidates("env-1", API, "unknown", null)).containsExactly(first, third);
        }

        @Test
        void should_return_a_rule_once_when_it_names_the_tool_twice() {
            DeployedApprovalRule rule = rule("refunds", "env-1", "refund_order|refund_order", 1L);
            deployAndCommit(rule);

            assertThat(manager.candidates("env-1", API, "refund_order", null)).containsExactly(rule);
        }

        @Test
        void should_never_return_a_rule_without_any_name() {
            deployAndCommit(rule("empty", "env-1", " | ", 1L));

            assertThat(manager.candidates("env-1", API, "", null)).isEmpty();
            assertThat(manager.candidates("env-1", API, " ", null)).isEmpty();
            assertThat(manager.hasRules("env-1")).isTrue();
        }

        @Test
        void should_not_return_rules_from_another_environment() {
            deployAndCommit(rule("rule-1", "env-1", "payments", 1L), rule("rule-2", "env-1", "*", 1L));

            assertThat(manager.candidates("env-2", API, "payments", null)).isEmpty();
            assertThat(manager.hasRules("env-2")).isFalse();
        }

        @Test
        void should_return_no_rule_without_an_environment() {
            deployAndCommit(rule("rule-1", "env-1", "payments", 1L));

            assertThat(manager.candidates(null, API, "payments", null)).isEmpty();
            assertThat(manager.hasRules(null)).isFalse();
        }

        @Test
        void should_return_a_snapshot_that_cannot_be_modified() {
            deployAndCommit(rule("rule-1", "env-1", "payments", 1L), rule("rule-2", "env-1", "pay*", 1L));
            List<DeployedApprovalRule> merged = manager.candidates("env-1", API, "payments", null);
            List<DeployedApprovalRule> wildcardOnly = manager.candidates("env-1", API, "payroll", null);
            List<DeployedApprovalRule> exactOnly = manager.candidates("env-1", API, "payments", null);

            deployAndCommit(rule("rule-3", "env-1", "payments", 1L));

            assertThat(merged).hasSize(2);
            assertThatThrownBy(merged::clear).isInstanceOf(UnsupportedOperationException.class);
            assertThatThrownBy(wildcardOnly::clear).isInstanceOf(UnsupportedOperationException.class);
            assertThatThrownBy(exactOnly::clear).isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void should_return_the_same_rules_as_a_full_scan_for_random_rules_and_tool_names() {
            Random random = new Random(42);
            String[] names = { "a", "b", "ab", "ba", "refund", "refund_order", "pay", "pay_x", "x_pay", "" };
            String[] apis = { "api-1", "api-2" };
            for (int round = 0; round < 50; round++) {
                ApprovalRuleManagerImpl fresh = new ApprovalRuleManagerImpl(new ApprovalFeature(licenseManager, true));
                List<DeployedApprovalRule> rules = new ArrayList<>();
                for (int i = 0; i < 200; i++) {
                    String pattern = random.nextInt(5) == 0
                        ? null
                        : IntStream.range(0, 1 + random.nextInt(3))
                            .mapToObj(alternative -> randomAlternative(random, names))
                            .collect(Collectors.joining("|"));
                    List<ToolBinding> bindings = IntStream.range(0, random.nextInt(3))
                        .mapToObj(binding -> new ToolBinding(apis[random.nextInt(apis.length)], names[random.nextInt(names.length)]))
                        .toList();
                    rules.add(
                        new DeployedApprovalRule(
                            "rule-" + i,
                            "env-1",
                            "org-1",
                            pattern,
                            bindings,
                            List.of(),
                            List.of(),
                            random.nextInt(5),
                            random.nextInt(5),
                            1L
                        )
                    );
                }
                rules.forEach(fresh::deploy);
                fresh.commit();
                List<DeployedApprovalRule> ordered = rules
                    .stream()
                    .sorted(
                        Comparator.comparingInt(DeployedApprovalRule::priority)
                            .thenComparingLong(DeployedApprovalRule::createdAt)
                            .thenComparing(DeployedApprovalRule::id)
                    )
                    .toList();

                for (String apiId : List.of("api-1", "api-2", "api-3")) {
                    for (String toolName : List.of(
                        "a",
                        "b",
                        "ab",
                        "ba",
                        "aab",
                        "refund",
                        "refund_order",
                        "pay",
                        "pay_x",
                        "x_pay",
                        "zz",
                        ""
                    )) {
                        List<DeployedApprovalRule> expected = ordered
                            .stream()
                            .filter(rule -> fullScanMatches(rule, apiId, toolName))
                            .toList();
                        List<DeployedApprovalRule> candidates = fresh.candidates("env-1", apiId, toolName, null);
                        List<DeployedApprovalRule> actual = candidates
                            .stream()
                            .filter(rule -> fullScanMatches(rule, apiId, toolName))
                            .toList();

                        assertThat(actual).as("round %d, api [%s], tool [%s]", round, apiId, toolName).isEqualTo(expected);
                        assertThat(candidates).as("round %d, api [%s], tool [%s]", round, apiId, toolName).doesNotHaveDuplicates();
                    }
                }
            }
        }

        private static String randomAlternative(Random random, String[] names) {
            String name = names[random.nextInt(names.length)];
            if (random.nextInt(4) == 0) {
                int at = random.nextInt(name.length() + 1);
                name = name.substring(0, at) + "*" + name.substring(at);
            }
            return random.nextBoolean() ? " " + name + " " : name;
        }

        /**
         * The matching the policy applies, written independently: the call is one of the rule's bindings, or an
         * alternative of its pattern is a glob over the whole name.
         */
        private static boolean fullScanMatches(DeployedApprovalRule rule, String apiId, String toolName) {
            if (rule.toolBindings().contains(new ToolBinding(apiId, toolName))) {
                return true;
            }
            if (rule.toolPattern() == null) {
                return false;
            }
            return Arrays.stream(rule.toolPattern().split("\\|", -1))
                .map(String::strip)
                .filter(alternative -> !alternative.isEmpty())
                .anyMatch(alternative ->
                    Pattern.matches(
                        Arrays.stream(alternative.split("\\*", -1)).map(Pattern::quote).collect(Collectors.joining(".*")),
                        toolName
                    )
                );
        }
    }

    @Nested
    class ToolBindingTest {

        @Test
        void should_return_a_rule_bound_to_the_called_api_and_tool() {
            DeployedApprovalRule rule = boundRule("anchored", null, 1, new ToolBinding(API, "transfer"));
            deployAndCommit(rule);

            assertThat(manager.candidates("env-1", API, "transfer", null)).containsExactly(rule);
        }

        @Test
        void should_scope_a_binding_to_its_api() {
            deployAndCommit(boundRule("anchored", null, 1, new ToolBinding(API, "transfer")));

            assertThat(manager.candidates("env-1", "api-2", "transfer", null)).isEmpty();
            assertThat(manager.candidates("env-1", null, "transfer", null)).isEmpty();
            assertThat(manager.candidates("env-1", API, "refund", null)).isEmpty();
        }

        @Test
        void should_deploy_a_rule_with_bindings_and_no_pattern() {
            deployAndCommit(boundRule("anchored", null, 1, new ToolBinding(API, "transfer")));

            assertThat(manager.hasRules("env-1")).isTrue();
            assertThat(manager.candidates("env-1", API, "anything", null)).isEmpty();
        }

        @Test
        void should_return_bound_named_and_wildcard_rules_once_in_evaluation_order() {
            DeployedApprovalRule wildcard = new DeployedApprovalRule(
                "w",
                "env-1",
                "org-1",
                "*",
                List.of(),
                List.of(),
                List.of(),
                1,
                1L,
                1L
            );
            DeployedApprovalRule bound = boundRule("b", null, 2, new ToolBinding(API, "transfer"));
            DeployedApprovalRule named = new DeployedApprovalRule(
                "n",
                "env-1",
                "org-1",
                "transfer",
                List.of(),
                List.of(),
                List.of(),
                3,
                1L,
                1L
            );
            DeployedApprovalRule boundAndNamed = boundRule("bn", "transfer", 4, new ToolBinding(API, "transfer"));
            DeployedApprovalRule boundAndWildcard = boundRule("bw", "tr*", 5, new ToolBinding(API, "transfer"));
            deployAndCommit(boundAndWildcard, boundAndNamed, named, bound, wildcard);

            assertThat(manager.candidates("env-1", API, "transfer", null)).containsExactly(
                wildcard,
                bound,
                named,
                boundAndNamed,
                boundAndWildcard
            );
        }

        @Test
        void should_return_a_rule_once_when_it_is_bound_twice_to_the_same_call() {
            DeployedApprovalRule rule = boundRule("anchored", null, 1, new ToolBinding(API, "transfer"), new ToolBinding(API, "transfer"));
            deployAndCommit(rule);

            assertThat(manager.candidates("env-1", API, "transfer", null)).containsExactly(rule);
        }

        @Test
        void should_drop_a_binding_when_its_rule_is_updated_without_it() {
            deployAndCommit(boundRule("anchored", null, 1, new ToolBinding(API, "transfer")));

            deployAndCommit(
                new DeployedApprovalRule(
                    "anchored",
                    "env-1",
                    "org-1",
                    null,
                    List.of(new ToolBinding(API, "refund")),
                    List.of(),
                    List.of(),
                    1,
                    1L,
                    2L
                )
            );

            assertThat(manager.candidates("env-1", API, "transfer", null)).isEmpty();
            assertThat(manager.candidates("env-1", API, "refund", null)).extracting(DeployedApprovalRule::id).containsExactly("anchored");
        }

        @Test
        void should_drop_bindings_of_an_undeployed_rule() {
            deployAndCommit(boundRule("anchored", null, 1, new ToolBinding(API, "transfer")));

            manager.undeploy("env-1", "anchored");
            manager.commit();

            assertThat(manager.candidates("env-1", API, "transfer", null)).isEmpty();
        }

        @Test
        void should_tell_whether_a_rule_is_bound_to_a_call() {
            DeployedApprovalRule rule = boundRule("anchored", null, 1, new ToolBinding(API, "transfer"));

            assertThat(rule.isBoundTo(API, "transfer")).isTrue();
            assertThat(rule.isBoundTo("api-2", "transfer")).isFalse();
            assertThat(rule.isBoundTo(API, "refund")).isFalse();
            assertThat(rule.isBoundTo(null, "transfer")).isFalse();
        }

        private static DeployedApprovalRule boundRule(String id, String toolPattern, int priority, ToolBinding... bindings) {
            return new DeployedApprovalRule(id, "env-1", "org-1", toolPattern, List.of(bindings), List.of(), List.of(), priority, 1L, 1L);
        }
    }

    @Nested
    class ToolEntityTest {

        @Test
        void should_return_a_rule_anchored_on_the_entity_of_the_called_tool_whatever_its_name() {
            DeployedApprovalRule anchored = anchoredRule("a", null, 1, "payments.refund");
            deployAndCommit(anchored);

            assertThat(manager.candidates("env-1", API, "issue_refund", "payments.refund")).containsExactly(anchored);
            assertThat(manager.candidates("env-1", API, "refund", "payments.refund")).containsExactly(anchored);
        }

        @Test
        void should_not_return_an_anchored_rule_for_another_entity_or_without_one() {
            deployAndCommit(anchoredRule("a", null, 1, "payments.refund"));

            assertThat(manager.candidates("env-1", API, "issue_refund", "payments.charge")).isEmpty();
            assertThat(manager.candidates("env-1", API, "issue_refund", null)).isEmpty();
            assertThat(manager.candidates("env-1", API, "issue_refund", " ")).isEmpty();
        }

        @Test
        void should_not_return_an_anchored_rule_from_another_environment() {
            deployAndCommit(anchoredRule("a", null, 1, "payments.refund"));

            assertThat(manager.candidates("env-2", API, "issue_refund", "payments.refund")).isEmpty();
        }

        @Test
        void should_return_a_rule_once_when_it_is_found_by_entity_name_binding_and_wildcard() {
            DeployedApprovalRule everything = new DeployedApprovalRule(
                "all",
                "env-1",
                "org-1",
                "issue_*",
                List.of(new ToolBinding(API, "issue_refund")),
                List.of("payments.refund", "payments.refund"),
                List.of(),
                1,
                1L,
                1L
            );
            deployAndCommit(everything);

            assertThat(manager.candidates("env-1", API, "issue_refund", "payments.refund")).containsExactly(everything);
        }

        @Test
        void should_order_rules_found_through_different_paths_by_priority_then_creation_then_id() {
            DeployedApprovalRule wildcard = new DeployedApprovalRule(
                "w",
                "env-1",
                "org-1",
                "*",
                List.of(),
                List.of(),
                List.of(),
                3,
                1L,
                1L
            );
            DeployedApprovalRule bound = new DeployedApprovalRule(
                "b",
                "env-1",
                "org-1",
                null,
                List.of(new ToolBinding(API, "issue_refund")),
                List.of(),
                List.of(),
                2,
                1L,
                1L
            );
            DeployedApprovalRule anchored = anchoredRule("a", null, 1, "payments.refund");
            DeployedApprovalRule named = new DeployedApprovalRule(
                "n",
                "env-1",
                "org-1",
                "issue_refund",
                List.of(),
                List.of(),
                List.of(),
                4,
                1L,
                1L
            );
            deployAndCommit(named, wildcard, bound, anchored);

            assertThat(manager.candidates("env-1", API, "issue_refund", "payments.refund")).containsExactly(
                anchored,
                bound,
                wildcard,
                named
            );
        }

        @Test
        void should_behave_as_before_for_a_rule_without_entity_ids() {
            DeployedApprovalRule named = new DeployedApprovalRule(
                "n",
                "env-1",
                "org-1",
                "issue_refund",
                List.of(),
                null,
                List.of(),
                1,
                1L,
                1L
            );
            deployAndCommit(named);

            assertThat(named.toolEntityIds()).isEmpty();
            assertThat(manager.candidates("env-1", API, "issue_refund", null)).containsExactly(named);
            assertThat(manager.candidates("env-1", API, "issue_refund", "payments.refund")).containsExactly(named);
        }

        @Test
        void should_drop_the_entity_index_when_the_rule_is_updated_without_it() {
            deployAndCommit(anchoredRule("a", null, 1, "payments.refund"));
            DeployedApprovalRule updated = new DeployedApprovalRule(
                "a",
                "env-1",
                "org-1",
                "issue_refund",
                List.of(),
                List.of(),
                List.of(),
                1,
                1L,
                2L
            );
            deployAndCommit(updated);

            assertThat(manager.candidates("env-1", API, "other", "payments.refund")).isEmpty();
            assertThat(manager.candidates("env-1", API, "issue_refund", "payments.refund")).containsExactly(updated);
        }

        @Test
        void should_drop_the_entity_index_of_an_undeployed_rule() {
            deployAndCommit(anchoredRule("a", null, 1, "payments.refund"));

            manager.undeploy("env-1", "a");
            manager.commit();

            assertThat(manager.candidates("env-1", API, "issue_refund", "payments.refund")).isEmpty();
        }

        @Test
        void should_keep_deploying_without_checking_the_license_for_an_anchored_rule() {
            when(license.isFeatureEnabled("gamma-aim-module")).thenReturn(false);
            DeployedApprovalRule anchored = anchoredRule("a", null, 1, "payments.refund");

            deployAndCommit(anchored);

            assertThat(manager.receivesRules("org-1")).isFalse();
            assertThat(manager.candidates("env-1", API, "issue_refund", "payments.refund")).containsExactly(anchored);
        }

        @Test
        void should_tell_whether_a_rule_is_anchored_on_an_entity() {
            DeployedApprovalRule anchored = anchoredRule("a", null, 1, "payments.refund");

            assertThat(anchored.isAnchoredOn("payments.refund")).isTrue();
            assertThat(anchored.isAnchoredOn("payments.charge")).isFalse();
            assertThat(anchored.isAnchoredOn(null)).isFalse();
        }

        private static DeployedApprovalRule anchoredRule(String id, String toolPattern, int priority, String... entityIds) {
            return new DeployedApprovalRule(id, "env-1", "org-1", toolPattern, List.of(), List.of(entityIds), List.of(), priority, 1L, 1L);
        }
    }

    @Nested
    class UndeployTest {

        @Test
        void should_undeploy_a_rule_on_commit() {
            deployAndCommit(rule("rule-1", "env-1", "payments", 1L));

            manager.undeploy("env-1", "rule-1");
            assertThat(manager.hasRules("env-1")).isTrue();
            manager.commit();

            assertThat(manager.hasRules("env-1")).isFalse();
            assertThat(manager.candidates("env-1", API, "payments", null)).isEmpty();
        }

        @Test
        void should_only_undeploy_from_the_given_environment() {
            deployAndCommit(rule("rule-1", "env-1", "payments", 1L), rule("rule-1", "env-2", "payments", 1L));

            manager.undeploy("env-1", "rule-1");
            manager.commit();

            assertThat(manager.hasRules("env-1")).isFalse();
            assertThat(manager.candidates("env-2", API, "payments", null)).hasSize(1);
        }

        @Test
        void should_undeploy_from_every_environment_when_the_environment_is_unknown() {
            deployAndCommit(rule("rule-1", "env-1", "payments", 1L), rule("rule-1", "env-2", "payments", 1L));

            manager.undeploy(null, "rule-1");
            manager.commit();

            assertThat(manager.hasRules("env-1")).isFalse();
            assertThat(manager.hasRules("env-2")).isFalse();
        }

        @Test
        void should_ignore_undeploying_an_unknown_rule() {
            manager.undeploy("env-1", "unknown");
            manager.commit();

            assertThat(manager.hasRules("env-1")).isFalse();
        }

        @Test
        void should_apply_the_last_change_staged_for_a_rule() {
            manager.deploy(rule("rule-1", "env-1", "payments", 1L));
            manager.undeploy("env-1", "rule-1");
            manager.deploy(rule("rule-2", "env-1", "refunds", 1L));
            manager.undeploy("env-1", "rule-2");
            manager.deploy(rule("rule-2", "env-1", "refunds", 2L));
            manager.commit();

            assertThat(manager.candidates("env-1", API, "payments", null)).isEmpty();
            assertThat(manager.candidates("env-1", API, "refunds", null)).extracting(DeployedApprovalRule::id).containsExactly("rule-2");
        }
    }

    @Nested
    class ArgumentConditionTest {

        @Test
        void should_compile_a_matches_expression_once() {
            ArgumentCondition condition = new ArgumentCondition("repo", "MATCHES", "acme/.*");

            assertThat(condition.compiledPattern()).isSameAs(condition.compiledPattern());
            assertThat(condition.compiledPattern().matcher("acme/api").matches()).isTrue();
        }

        @Test
        void should_not_compile_the_value_of_another_operator() {
            assertThat(new ArgumentCondition("amount", "GT", "(").compiledPattern()).isNull();
        }

        @Test
        void should_leave_an_uncompilable_expression_without_pattern() {
            assertThat(new ArgumentCondition("repo", "MATCHES", "acme/(").compiledPattern()).isNull();
            assertThat(new ArgumentCondition("repo", "MATCHES", null).compiledPattern()).isNull();
        }

        @Test
        void should_compare_conditions_by_path_operator_and_value() {
            assertThat(new ArgumentCondition("repo", "MATCHES", "a.*"))
                .isEqualTo(new ArgumentCondition("repo", "MATCHES", "a.*"))
                .hasSameHashCodeAs(new ArgumentCondition("repo", "MATCHES", "a.*"))
                .isNotEqualTo(new ArgumentCondition("repo", "MATCHES", "b.*"));
        }
    }

    @Test
    void should_default_missing_tool_bindings_to_an_empty_list() {
        assertThat(
            new DeployedApprovalRule("rule-1", "env-1", "org-1", "payments", null, List.of(), List.of(), 1, 1L, 1L).toolBindings()
        ).isEmpty();
    }

    @Test
    void should_default_missing_argument_conditions_to_an_empty_list() {
        assertThat(
            new DeployedApprovalRule("rule-1", "env-1", "org-1", "payments", List.of(), List.of(), null, 1, 1L, 1L).argumentConditions()
        ).isEmpty();
    }

    @Test
    void should_receive_rules_only_when_gamma_is_enabled_and_the_organization_licensed() {
        manager.commit();
        ApprovalRuleManagerImpl gammaDisabled = new ApprovalRuleManagerImpl(new ApprovalFeature(licenseManager, false));
        gammaDisabled.commit();

        assertThat(manager.receivesRules("org-1")).isTrue();
        assertThat(gammaDisabled.receivesRules("org-1")).isFalse();

        when(licenseManager.getOrganizationLicenseOrPlatform("org-2")).thenReturn(null);
        assertThat(manager.receivesRules("org-2")).isFalse();
    }

    private void deployAndCommit(DeployedApprovalRule... rules) {
        Arrays.stream(rules).forEach(manager::deploy);
        manager.commit();
    }

    private static DeployedApprovalRule rule(String id, String environmentId, String toolPattern, long updatedAt) {
        return new DeployedApprovalRule(
            id,
            environmentId,
            "org-1",
            toolPattern,
            List.of(),
            List.of(),
            List.of(new ArgumentCondition("amount", "GT", "1000")),
            1,
            1L,
            updatedAt
        );
    }

    private static DeployedApprovalRule rule(String id, String environmentId, int priority, long createdAt) {
        return new DeployedApprovalRule(
            id,
            environmentId,
            "org-1",
            "payments",
            List.of(),
            List.of(),
            List.of(),
            priority,
            createdAt,
            createdAt
        );
    }
}
