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

import io.gravitee.gateway.handlers.api.manager.ApprovalRuleManager;
import io.gravitee.gateway.handlers.api.manager.DeployedApprovalRule;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import lombok.CustomLog;

@CustomLog
public class ApprovalRuleManagerImpl implements ApprovalRuleManager {

    private static final Comparator<DeployedApprovalRule> EVALUATION_ORDER = Comparator.comparingInt(DeployedApprovalRule::priority)
        .thenComparingLong(DeployedApprovalRule::createdAt)
        .thenComparing(DeployedApprovalRule::id);

    private final ApprovalFeature approvalFeature;
    // Changes are applied in the order they were staged, so the last one staged for a rule wins.
    private final Queue<StagedChange> stagedChanges = new ConcurrentLinkedQueue<>();
    private final Map<String, EnvironmentRules> rulesByEnvironment = new ConcurrentHashMap<>();
    // Set by the first sync pass: until a rule source loaded the rules, "no rule" does not mean no call is covered.
    private volatile boolean loaded;

    public ApprovalRuleManagerImpl(ApprovalFeature approvalFeature) {
        this.approvalFeature = approvalFeature;
    }

    @Override
    public void deploy(DeployedApprovalRule rule) {
        // Staged regardless of license: deploy() only runs once, when the rule is published, with no later event to
        // re-stage it if the license changes afterwards. The license is checked fresh on every read instead (receivesRules).
        stagedChanges.add(new StagedChange(rule.environmentId(), rule.id(), rule));
    }

    @Override
    public void undeploy(String environmentId, String ruleId) {
        stagedChanges.add(new StagedChange(environmentId, ruleId, null));
    }

    @Override
    public synchronized void commit() {
        Map<String, Map<String, DeployedApprovalRule>> touched = new HashMap<>();
        StagedChange change;
        while ((change = stagedChanges.poll()) != null) {
            if (change.rule() != null) {
                applyDeployment(touched, change.rule());
            } else {
                applyUndeployment(touched, change.environmentId(), change.ruleId());
            }
        }
        touched.forEach((environmentId, rules) -> {
            if (rules.isEmpty()) {
                rulesByEnvironment.remove(environmentId);
            } else {
                rulesByEnvironment.put(environmentId, index(rules.values()));
            }
            log.debug("{} approval rules deployed in environment [{}]", rules.size(), environmentId);
        });
        loaded = true;
    }

    @Override
    public List<DeployedApprovalRule> candidates(String environmentId, String apiId, String toolName, String entityId) {
        EnvironmentRules environmentRules = environmentId == null ? null : rulesByEnvironment.get(environmentId);
        if (environmentRules == null) {
            return List.of();
        }
        List<DeployedApprovalRule> bound = bound(environmentRules, apiId, toolName);
        List<DeployedApprovalRule> anchored = entityId == null || entityId.isBlank()
            ? List.of()
            : environmentRules.byEntityId().getOrDefault(entityId, List.of());
        List<DeployedApprovalRule> exact = toolName == null ? List.of() : environmentRules.byExactName().getOrDefault(toolName, List.of());
        return mergeInEvaluationOrder(mergeInEvaluationOrder(mergeInEvaluationOrder(bound, anchored), exact), environmentRules.wildcards());
    }

    private static List<DeployedApprovalRule> bound(EnvironmentRules environmentRules, String apiId, String toolName) {
        if (apiId == null || toolName == null) {
            return List.of();
        }
        Map<String, List<DeployedApprovalRule>> byToolName = environmentRules.byBinding().get(apiId);
        return byToolName == null ? List.of() : byToolName.getOrDefault(toolName, List.of());
    }

    @Override
    public boolean hasRules(String environmentId) {
        return environmentId != null && rulesByEnvironment.containsKey(environmentId);
    }

    @Override
    public boolean receivesRules(String organizationId) {
        return loaded && approvalFeature.isEnabledFor(organizationId);
    }

    EnvironmentRules index(Collection<DeployedApprovalRule> rules) {
        Map<String, DeployedApprovalRule> byId = new HashMap<>();
        Map<String, List<DeployedApprovalRule>> byExactName = new HashMap<>();
        Map<String, Map<String, List<DeployedApprovalRule>>> byBinding = new HashMap<>();
        Map<String, List<DeployedApprovalRule>> byEntityId = new HashMap<>();
        List<DeployedApprovalRule> wildcards = new ArrayList<>();
        rules
            .stream()
            .sorted(EVALUATION_ORDER)
            .forEach(rule -> {
                byId.put(rule.id(), rule);
                Set<String> alternatives = alternatives(rule.toolPattern());
                if (alternatives.stream().anyMatch(alternative -> alternative.indexOf('*') >= 0)) {
                    wildcards.add(rule);
                } else {
                    alternatives.forEach(name -> byExactName.computeIfAbsent(name, key -> new ArrayList<>()).add(rule));
                }
                new LinkedHashSet<>(rule.toolBindings()).forEach(binding ->
                    byBinding
                        .computeIfAbsent(binding.apiId(), key -> new HashMap<>())
                        .computeIfAbsent(binding.toolName(), key -> new ArrayList<>())
                        .add(rule)
                );
                new LinkedHashSet<>(rule.toolEntityIds()).forEach(entityId ->
                    byEntityId.computeIfAbsent(entityId, key -> new ArrayList<>()).add(rule)
                );
            });
        byExactName.replaceAll((name, named) -> List.copyOf(named));
        byEntityId.replaceAll((entityId, anchored) -> List.copyOf(anchored));
        byBinding.replaceAll((apiId, byToolName) -> {
            byToolName.replaceAll((toolName, bound) -> List.copyOf(bound));
            return Map.copyOf(byToolName);
        });
        return new EnvironmentRules(
            Map.copyOf(byId),
            Map.copyOf(byExactName),
            Map.copyOf(byBinding),
            Map.copyOf(byEntityId),
            List.copyOf(wildcards)
        );
    }

    private void applyDeployment(Map<String, Map<String, DeployedApprovalRule>> touched, DeployedApprovalRule rule) {
        Map<String, DeployedApprovalRule> rules = touched.computeIfAbsent(rule.environmentId(), this::deployedRules);
        DeployedApprovalRule current = rules.get(rule.id());
        if (current == null || current.updatedAt() <= rule.updatedAt()) {
            rules.put(rule.id(), rule);
        }
    }

    private void applyUndeployment(Map<String, Map<String, DeployedApprovalRule>> touched, String environmentId, String ruleId) {
        Set<String> environmentIds;
        if (environmentId != null) {
            environmentIds = Set.of(environmentId);
        } else {
            environmentIds = new HashSet<>(touched.keySet());
            environmentIds.addAll(rulesByEnvironment.keySet());
        }
        for (String candidate : environmentIds) {
            Map<String, DeployedApprovalRule> rules = touched.get(candidate);
            if (rules == null && containsRule(candidate, ruleId)) {
                rules = touched.computeIfAbsent(candidate, this::deployedRules);
            }
            if (rules != null) {
                rules.remove(ruleId);
            }
        }
    }

    private boolean containsRule(String environmentId, String ruleId) {
        EnvironmentRules environmentRules = rulesByEnvironment.get(environmentId);
        return environmentRules != null && environmentRules.byId().containsKey(ruleId);
    }

    private Map<String, DeployedApprovalRule> deployedRules(String environmentId) {
        EnvironmentRules environmentRules = rulesByEnvironment.get(environmentId);
        return environmentRules == null ? new HashMap<>() : new HashMap<>(environmentRules.byId());
    }

    /** The trimmed, non-empty {@code |} alternatives of a tool pattern, as the policy matches them. */
    private static Set<String> alternatives(String toolPattern) {
        if (toolPattern == null) {
            return Set.of();
        }
        Set<String> alternatives = new LinkedHashSet<>();
        for (String alternative : toolPattern.split("\\|", -1)) {
            String trimmed = alternative.strip();
            if (!trimmed.isEmpty()) {
                alternatives.add(trimmed);
            }
        }
        return alternatives;
    }

    /** Both lists are in evaluation order, which is total within an environment, so a rule in both compares equal. */
    private static List<DeployedApprovalRule> mergeInEvaluationOrder(List<DeployedApprovalRule> left, List<DeployedApprovalRule> right) {
        if (right.isEmpty()) {
            return left;
        }
        if (left.isEmpty()) {
            return right;
        }
        List<DeployedApprovalRule> merged = new ArrayList<>(left.size() + right.size());
        int l = 0;
        int r = 0;
        while (l < left.size() && r < right.size()) {
            int comparison = EVALUATION_ORDER.compare(left.get(l), right.get(r));
            if (comparison == 0) {
                r++;
            }
            merged.add(comparison <= 0 ? left.get(l++) : right.get(r++));
        }
        merged.addAll(left.subList(l, left.size()));
        merged.addAll(right.subList(r, right.size()));
        return Collections.unmodifiableList(merged);
    }

    /** A deployment when {@code rule} is set, otherwise an undeployment. */
    private record StagedChange(String environmentId, String ruleId, DeployedApprovalRule rule) {}

    /**
     * The rules of one environment, indexed once per sync pass for the lookup made on every tool call.
     *
     * @param byExactName the rules whose alternatives are all exact names, under each of those names, in evaluation order
     * @param byBinding the rules bound to a tool of an API, by API id then tool name, in evaluation order
     * @param byEntityId the rules anchored on a catalog tool, by the entity id of that tool, in evaluation order
     * @param wildcards the rules with a {@code *} in at least one alternative, in evaluation order
     */
    record EnvironmentRules(
        Map<String, DeployedApprovalRule> byId,
        Map<String, List<DeployedApprovalRule>> byExactName,
        Map<String, Map<String, List<DeployedApprovalRule>>> byBinding,
        Map<String, List<DeployedApprovalRule>> byEntityId,
        List<DeployedApprovalRule> wildcards
    ) {}
}
