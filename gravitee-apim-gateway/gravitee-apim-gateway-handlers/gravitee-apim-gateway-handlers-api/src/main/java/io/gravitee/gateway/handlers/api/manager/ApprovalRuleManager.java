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
package io.gravitee.gateway.handlers.api.manager;

import java.util.List;

/**
 * The approval rules deployed on this gateway. Deployments and undeployments are staged and become visible together on
 * the next {@link #commit()}, so that a sync pass rebuilds each environment's lookup once rather than once per rule.
 */
public interface ApprovalRuleManager {
    /**
     * Stage the deployment (or update) of an approval rule, regardless of licensing: the license is checked fresh on
     * every read ({@link #receivesRules}) instead, since deploy only runs once, when the rule is published, with no
     * later event to re-stage it if the license changes afterwards. Ignored when a newer copy of the same rule is
     * already staged or deployed.
     *
     * @param rule the approval rule to deploy
     */
    void deploy(DeployedApprovalRule rule);

    /**
     * Stage the undeployment of an approval rule.
     *
     * @param environmentId the environment the rule belongs to, or {@code null} to remove it from every environment
     * @param ruleId the id of the rule
     */
    void undeploy(String environmentId, String ruleId);

    /** Apply every staged deployment and undeployment, rebuilding each environment they touch once. */
    void commit();

    /**
     * Get the approval rules of the given environment that may cover a call to {@code toolName} on {@code apiId}, each
     * once, in evaluation order: priority ascending, then creation time ascending, then id. They are the rules bound to
     * that API and tool (see {@link DeployedApprovalRule#isBoundTo}), the rules anchored on the catalog entity of the
     * tool (see {@link DeployedApprovalRule#isAnchoredOn}), the rules naming the tool exactly in one of their
     * {@code |} alternatives, and every rule with a {@code *} wildcard, which the caller still has to match. Rules are
     * never returned from another environment.
     *
     * @param environmentId the environment asking for its rules
     * @param apiId the id of the called API, or {@code null} when unknown, which leaves bound rules out
     * @param toolName the name of the called tool
     * @param entityId the catalog entity id the called tool resolves to, or {@code null} when it has none, which leaves
     *     anchored rules out
     * @return an immutable list, empty when no rule can cover the call or the environment is {@code null}
     */
    List<DeployedApprovalRule> candidates(String environmentId, String apiId, String toolName, String entityId);

    /**
     * @param environmentId the environment asking
     * @return whether at least one approval rule is deployed in that environment
     */
    boolean hasRules(String environmentId);

    /**
     * Whether this gateway receives the approval rules of the given organization: Gamma is enabled, the organization is
     * licensed for them, and a sync pass loaded them, which no pass does on a gateway that does not sync them from a
     * repository or a distributed sync. A gateway that does not cannot tell which calls a rule covers, and must not let
     * them through.
     *
     * @param organizationId the organization asking
     * @return whether rules of that organization are deployed here
     */
    boolean receivesRules(String organizationId);
}
