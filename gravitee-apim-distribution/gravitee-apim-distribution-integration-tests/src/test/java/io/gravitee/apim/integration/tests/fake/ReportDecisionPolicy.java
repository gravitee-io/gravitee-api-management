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
package io.gravitee.apim.integration.tests.fake;

import io.gravitee.gateway.reactive.api.context.ContextAttributes;
import io.gravitee.gateway.reactive.api.context.HttpExecutionContext;
import io.gravitee.gateway.reactive.api.policy.Policy;
import io.gravitee.gateway.report.ReporterService;
import io.gravitee.node.api.Node;
import io.gravitee.policy.api.PolicyConfiguration;
import io.gravitee.reporter.api.v4.metric.AdditionalMetric;
import io.gravitee.reporter.api.v4.report.DecisionReport;
import io.reactivex.rxjava3.core.Completable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class ReportDecisionPolicy implements Policy {

    public static final String POLICY_ID = "report-decision";

    private final ReportDecisionConfiguration configuration;

    public ReportDecisionPolicy(ReportDecisionConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public String id() {
        return POLICY_ID;
    }

    @Override
    public Completable onRequest(HttpExecutionContext ctx) {
        return Completable.fromRunnable(() -> ctx.getComponent(ReporterService.class).report(decision(ctx)));
    }

    private DecisionReport decision(HttpExecutionContext ctx) {
        return DecisionReport.builder()
            .timestamp(System.currentTimeMillis())
            .gatewayId(ctx.getComponent(Node.class).id())
            .organizationId(attribute(ctx, ContextAttributes.ATTR_ORGANIZATION))
            .environmentId(attribute(ctx, ContextAttributes.ATTR_ENVIRONMENT))
            .apiId(ctx.getAttribute(ContextAttributes.ATTR_API))
            .eventId("evt-" + ctx.request().id())
            .requestId(ctx.request().id())
            .phase(DecisionReport.Phase.RESOLVED)
            .decisionPointType(DecisionReport.DECISION_POINT_GUARDIAN)
            .decisionPointId("guardian-agent")
            .decisionPointVersion("gpt-4o-2024-11")
            .checkpoint("request.prompt")
            .caller("gateway")
            .subjectType("user")
            .subjectId("jane@example.com")
            .actorType("agent")
            .actorId("support-bot")
            .action("send_message")
            .resourceType("conversation")
            .resourceId("conv-42")
            .outcome(configuration.getOutcome())
            .enforced(configuration.getEnforced())
            .verdict(configuration.getVerdict())
            .indeterminateCause(configuration.getIndeterminateCause())
            .confidence(0.94)
            .reasons(List.of("no_pii_detected", "within_policy"))
            .matchedRules(
                List.of(new DecisionReport.MatchedRule("rule-1", "no-pii", "rev-1", DecisionReport.Outcome.ALLOW.getLabel(), Map.of()))
            )
            .status(DecisionReport.Status.SUCCESS)
            .durationNanos(1_234_000L)
            .additionalMetrics(
                Set.of(new AdditionalMetric.LongMetric("long_tokens", 512L), new AdditionalMetric.KeywordMetric("keyword_model", "gpt-4o"))
            )
            .build();
    }

    private static String attribute(HttpExecutionContext ctx, String name) {
        return Objects.requireNonNullElse(ctx.getAttribute(name), "DEFAULT");
    }

    public static class ReportDecisionConfiguration implements PolicyConfiguration {

        private DecisionReport.Outcome outcome = DecisionReport.Outcome.ALLOW;
        private DecisionReport.Enforced enforced = DecisionReport.Enforced.ALLOW;
        private String verdict = "allow";
        private DecisionReport.IndeterminateCause indeterminateCause;

        public DecisionReport.Outcome getOutcome() {
            return outcome;
        }

        public void setOutcome(DecisionReport.Outcome outcome) {
            this.outcome = outcome;
        }

        public DecisionReport.Enforced getEnforced() {
            return enforced;
        }

        public void setEnforced(DecisionReport.Enforced enforced) {
            this.enforced = enforced;
        }

        public String getVerdict() {
            return verdict;
        }

        public void setVerdict(String verdict) {
            this.verdict = verdict;
        }

        public DecisionReport.IndeterminateCause getIndeterminateCause() {
            return indeterminateCause;
        }

        public void setIndeterminateCause(DecisionReport.IndeterminateCause indeterminateCause) {
            this.indeterminateCause = indeterminateCause;
        }
    }
}
