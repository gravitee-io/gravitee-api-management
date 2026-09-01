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
import io.gravitee.reporter.api.v4.metric.event.DecisionEventMetrics;
import io.reactivex.rxjava3.core.Completable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Stands in for a real decision point (a PEP, a guardian agent) so the integration test can exercise
 * the emission seam without depending on a plugin repository. Producers of {@link DecisionEventMetrics}
 * live outside APIM, so nothing else in this repository proves a policy can reach the reporter.
 */
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

    private DecisionEventMetrics decision(HttpExecutionContext ctx) {
        return DecisionEventMetrics.builder()
            .timestamp(System.currentTimeMillis())
            .gatewayId(ctx.getComponent(Node.class).id())
            .organizationId(attribute(ctx, ContextAttributes.ATTR_ORGANIZATION))
            .environmentId(attribute(ctx, ContextAttributes.ATTR_ENVIRONMENT))
            .apiId(ctx.getAttribute(ContextAttributes.ATTR_API))
            .eventId("evt-" + ctx.request().id())
            .requestId(ctx.request().id())
            .phase(DecisionEventMetrics.Phase.RESOLVED)
            .decisionPointType(DecisionEventMetrics.DECISION_POINT_GUARDIAN)
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
                List.of(new DecisionEventMetrics.MatchedRule("rule-1", "no-pii", DecisionEventMetrics.Outcome.ALLOW.getLabel(), Map.of()))
            )
            .status(DecisionEventMetrics.Status.SUCCESS)
            .durationNanos(1_234_000L)
            .additionalMetrics(
                Set.of(new AdditionalMetric.LongMetric("long_tokens", 512L), new AdditionalMetric.KeywordMetric("keyword_model", "gpt-4o"))
            )
            .build();
    }

    /**
     * {@code gatewayId}, {@code organizationId}, {@code environmentId} and {@code apiId} are
     * {@code @NonNull} on {@code BaseEventMetrics}, so a decision point must supply all four or the
     * builder throws inside the request. Organization and environment are unset on a standalone
     * gateway, hence the fallback.
     */
    private static String attribute(HttpExecutionContext ctx, String name) {
        return Objects.requireNonNullElse(ctx.getAttribute(name), "DEFAULT");
    }

    public static class ReportDecisionConfiguration implements PolicyConfiguration {

        private DecisionEventMetrics.Outcome outcome = DecisionEventMetrics.Outcome.ALLOW;
        private DecisionEventMetrics.Enforced enforced = DecisionEventMetrics.Enforced.ALLOW;
        private String verdict = "allow";
        private DecisionEventMetrics.IndeterminateCause indeterminateCause;

        public DecisionEventMetrics.Outcome getOutcome() {
            return outcome;
        }

        public void setOutcome(DecisionEventMetrics.Outcome outcome) {
            this.outcome = outcome;
        }

        public DecisionEventMetrics.Enforced getEnforced() {
            return enforced;
        }

        public void setEnforced(DecisionEventMetrics.Enforced enforced) {
            this.enforced = enforced;
        }

        public String getVerdict() {
            return verdict;
        }

        public void setVerdict(String verdict) {
            this.verdict = verdict;
        }

        public DecisionEventMetrics.IndeterminateCause getIndeterminateCause() {
            return indeterminateCause;
        }

        public void setIndeterminateCause(DecisionEventMetrics.IndeterminateCause indeterminateCause) {
            this.indeterminateCause = indeterminateCause;
        }
    }
}
