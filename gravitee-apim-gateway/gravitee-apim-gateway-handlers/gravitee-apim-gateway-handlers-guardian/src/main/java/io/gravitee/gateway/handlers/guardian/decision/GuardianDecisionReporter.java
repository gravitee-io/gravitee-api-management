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
package io.gravitee.gateway.handlers.guardian.decision;

import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.guardian.Guardian;
import io.gravitee.gateway.handlers.guardian.ReactableGuardian;
import io.gravitee.gateway.handlers.guardian.decision.GuardianVerdicts.Entry;
import io.gravitee.gateway.handlers.guardian.policy.GuardianPolicy;
import io.gravitee.gateway.reactive.api.ExecutionFailure;
import io.gravitee.gateway.reactive.api.context.ContextAttributes;
import io.gravitee.gateway.reactive.api.context.InternalContextAttributes;
import io.gravitee.gateway.reactive.api.context.http.HttpPlainExecutionContext;
import io.gravitee.gateway.reactive.api.policy.guardrail.GuardrailVerdict;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.gateway.report.ReporterService;
import io.gravitee.node.api.Node;
import io.gravitee.reporter.api.v4.report.DecisionReport;
import io.gravitee.reporter.api.v4.report.DecisionReport.DecisionReportBuilder;
import io.gravitee.reporter.api.v4.report.DecisionReport.Enforced;
import io.gravitee.reporter.api.v4.report.DecisionReport.IndeterminateCause;
import io.gravitee.reporter.api.v4.report.DecisionReport.MatchedRule;
import io.gravitee.reporter.api.v4.report.DecisionReport.Outcome;
import io.gravitee.reporter.api.v4.report.DecisionReport.Phase;
import io.gravitee.reporter.api.v4.report.DecisionReport.Status;
import io.gravitee.reporter.api.v4.report.DecisionReport.TransformationType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.CustomLog;

/**
 * Reports one {@link DecisionReport} per Guardian run: the only place that knows that model. A decision carries rule
 * names and locations, never the content of the exchange. Reporting never fails the call: a failure is logged and the
 * outcome of the Guardian stands.
 *
 * @author GraviteeSource Team
 */
@CustomLog
public class GuardianDecisionReporter {

    /** What the subscription processor writes in the context of a call without application or plan. */
    private static final String ANONYMOUS_ID = "1";
    private static final String SUBJECT_APPLICATION = "application";

    private final ReporterService reporterService;
    private final Node node;

    public GuardianDecisionReporter(ReporterService reporterService, Node node) {
        this.reporterService = reporterService;
        this.node = node;
    }

    /**
     * The chain ended without a failure: it completed, or a policy ended the call with {@code ctx.interrupt()}.
     */
    public void decided(
        HttpPlainExecutionContext ctx,
        String caller,
        ReactableGuardian guardian,
        Guardian.Phase phase,
        GuardianVerdicts verdicts,
        long durationNanos
    ) {
        report(ctx, () -> {
            DecisionReportBuilder<?, ?> decision = run(ctx, caller, guardian, phase, verdicts, durationNanos).status(Status.SUCCESS);
            if (verdicts.isEmpty()) {
                decision.outcome(Outcome.INDETERMINATE).indeterminateCause(IndeterminateCause.NOT_APPLICABLE).enforced(Enforced.ALLOW);
            } else if (verdicts.transformation().isPresent()) {
                decision
                    .outcome(Outcome.TRANSFORM)
                    .enforced(Enforced.TRANSFORM)
                    .transformed(true)
                    .transformationType(TransformationType.valueOf(verdicts.transformation().get().name()));
            } else {
                decision.outcome(Outcome.ALLOW).enforced(Enforced.ALLOW);
            }
            return decision.build();
        });
    }

    /**
     * The chain was interrupted with a failure. Two failures say the Guardian could not decide; any other is a refusal.
     */
    public void denied(
        HttpPlainExecutionContext ctx,
        String caller,
        ReactableGuardian guardian,
        Guardian.Phase phase,
        GuardianVerdicts verdicts,
        ExecutionFailure failure,
        long durationNanos
    ) {
        report(ctx, () -> {
            DecisionReportBuilder<?, ?> decision = run(ctx, caller, guardian, phase, verdicts, durationNanos);
            if (GuardianPolicy.NOT_DEPLOYED_KEY.equals(failure.key())) {
                undecided(decision, IndeterminateCause.NOT_READY, failure.key());
            } else if (GuardianPolicy.GUARDRAIL_UNSUPPORTED_API_TYPE_KEY.equals(failure.key())) {
                undecided(decision, IndeterminateCause.ERROR, failure.key());
            } else {
                decision
                    .outcome(Outcome.DENY)
                    .enforced(Enforced.DENY)
                    .status(Status.SUCCESS)
                    .verdict(failure.key())
                    .reasons(denialReasons(verdicts, failure));
            }
            return decision.build();
        });
    }

    /**
     * The chain failed with something else than an interruption: a resource down, a bug.
     */
    public void failed(
        HttpPlainExecutionContext ctx,
        String caller,
        ReactableGuardian guardian,
        Guardian.Phase phase,
        GuardianVerdicts verdicts,
        Throwable cause,
        long durationNanos
    ) {
        report(ctx, () -> {
            DecisionReportBuilder<?, ?> decision = run(ctx, caller, guardian, phase, verdicts, durationNanos);
            undecided(decision, IndeterminateCause.ERROR, cause.getClass().getSimpleName());
            return decision.build();
        });
    }

    /**
     * The Guardian is not deployed.
     */
    public void notReady(HttpPlainExecutionContext ctx, String caller, String guardianId, Guardian.Phase phase) {
        report(ctx, () -> {
            DecisionReportBuilder<?, ?> decision = common(ctx, caller, guardianId, null, phase);
            undecided(decision, IndeterminateCause.NOT_READY, GuardianPolicy.NOT_DEPLOYED_KEY);
            return decision.build();
        });
    }

    /**
     * The Guardian cannot run for this API or on this phase.
     */
    public void incompatible(
        HttpPlainExecutionContext ctx,
        String caller,
        ReactableGuardian guardian,
        Guardian.Phase phase,
        String failureKey
    ) {
        report(ctx, () -> {
            DecisionReportBuilder<?, ?> decision = common(ctx, caller, guardian.getId(), guardian.getRevision(), phase);
            undecided(decision, IndeterminateCause.ERROR, failureKey);
            return decision.build();
        });
    }

    private void report(HttpPlainExecutionContext ctx, Supplier<DecisionReport> decision) {
        try {
            reporterService.report(decision.get());
        } catch (Exception e) {
            ctx.withLogger(log).warn("Unable to report the decision of a Guardian", e);
        }
    }

    private static void undecided(DecisionReportBuilder<?, ?> decision, IndeterminateCause cause, String errorType) {
        decision.outcome(Outcome.INDETERMINATE).indeterminateCause(cause).enforced(Enforced.DENY).status(Status.ERROR).errorType(errorType);
    }

    private DecisionReportBuilder<?, ?> run(
        HttpPlainExecutionContext ctx,
        String caller,
        ReactableGuardian guardian,
        Guardian.Phase phase,
        GuardianVerdicts verdicts,
        long durationNanos
    ) {
        return common(ctx, caller, guardian.getId(), guardian.getRevision(), phase)
            .reasons(reasons(verdicts))
            .matchedRules(matchedRules(verdicts))
            .durationNanos(durationNanos);
    }

    private DecisionReportBuilder<?, ?> common(
        HttpPlainExecutionContext ctx,
        String caller,
        String guardianId,
        String revision,
        Guardian.Phase phase
    ) {
        // Not ctx.metrics(): they are a no-op on an API with analytics disabled, and a decision is reported whatever they are.
        ReactableApi<?> api = ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_REACTABLE_API);
        String applicationId = identified(ctx.getAttribute(ContextAttributes.ATTR_APPLICATION));
        DecisionReportBuilder<?, ?> decision = DecisionReport.builder()
            .timestamp(System.currentTimeMillis())
            .gatewayId(node.id())
            .organizationId(api.getOrganizationId())
            .environmentId(api.getEnvironmentId())
            .apiId(api.getId())
            .planId(identified(ctx.getAttribute(ContextAttributes.ATTR_PLAN)))
            .applicationId(applicationId)
            .eventId(UUID.randomUUID().toString())
            .phase(Phase.RESOLVED)
            .decisionPointType(DecisionReport.DECISION_POINT_GUARDIAN)
            .decisionPointId(guardianId)
            .decisionPointVersion(revision)
            .checkpoint(checkpoint(ctx, phase))
            .caller(caller)
            .requestId(ctx.request().id());
        if (applicationId != null) {
            decision.subjectType(SUBJECT_APPLICATION).subjectId(applicationId);
        }
        if (ctx.getTracer() != null) {
            decision.traceId(ctx.getTracer().traceId());
        }
        return decision;
    }

    /**
     * @return the id, {@code null} when blank or anonymous.
     */
    private static String identified(String id) {
        return id == null || id.isBlank() || ANONYMOUS_ID.equals(id) ? null : id;
    }

    private static String checkpoint(HttpPlainExecutionContext ctx, Guardian.Phase phase) {
        String checkpoint = phase.name().toLowerCase(Locale.ROOT);
        return Optional.ofNullable(ctx.getComponent(Api.class))
            .map(Api::getType)
            .map(ApiType::getLabel)
            .map(label -> checkpoint + "." + label)
            .orElse(checkpoint);
    }

    /**
     * The message of a failure is only reported when a guardrail denied: the one of a plain policy may be built from
     * the request.
     */
    private static List<String> denialReasons(GuardianVerdicts verdicts, ExecutionFailure failure) {
        List<Entry> entries = verdicts.entries();
        boolean deniedByGuardrail =
            !entries.isEmpty() && entries.get(entries.size() - 1).verdict().outcome() == GuardrailVerdict.Outcome.DENY;
        List<String> reasons = new ArrayList<>();
        if (deniedByGuardrail && failure.message() != null) {
            reasons.add(failure.message());
        }
        reasons.addAll(reasons(verdicts));
        return reasons;
    }

    private static List<String> reasons(GuardianVerdicts verdicts) {
        return verdicts.entries().stream().map(GuardianDecisionReporter::reason).toList();
    }

    private static String reason(Entry entry) {
        GuardrailVerdict verdict = entry.verdict();
        String line = entry.guardrail() + ": " + verdict.verdict();
        return verdict.reasons().isEmpty() ? line : line + " — " + String.join("; ", verdict.reasons());
    }

    private static List<MatchedRule> matchedRules(GuardianVerdicts verdicts) {
        return verdicts
            .entries()
            .stream()
            .flatMap(entry ->
                entry
                    .verdict()
                    .findings()
                    .stream()
                    .map(finding -> matchedRule(entry, finding))
            )
            .toList();
    }

    private static MatchedRule matchedRule(Entry entry, GuardrailVerdict.Finding finding) {
        Map<String, String> annotations = new LinkedHashMap<>();
        annotations.put("guardrail", entry.guardrail());
        annotations.put("location", finding.location());
        return new MatchedRule(finding.rule(), finding.rule(), null, entry.verdict().outcome().name(), annotations);
    }
}
