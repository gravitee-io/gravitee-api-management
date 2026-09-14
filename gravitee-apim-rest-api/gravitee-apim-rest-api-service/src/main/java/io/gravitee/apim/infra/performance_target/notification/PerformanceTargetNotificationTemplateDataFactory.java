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
package io.gravitee.apim.infra.performance_target.notification;

import io.gravitee.apim.core.analytics_engine.model.MetricSpec;
import io.gravitee.apim.core.analytics_engine.query_service.AnalyticsDefinitionQueryService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.environment.model.Environment;
import io.gravitee.apim.core.installation.query_service.InstallationAccessQueryService;
import io.gravitee.apim.core.notification.model.PerformanceTargetNotificationTemplateData;
import io.gravitee.apim.core.performance_target.model.PerformanceTarget;
import io.gravitee.apim.core.performance_target.model.PerformanceTargetRuleTransition;
import io.gravitee.definition.model.v4.ApiType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Words a performance target report in the terms the templates print: the subject in the host's words, the rules as
 * "metric measure operator threshold unit", observed values with their unit, windows and intervals in minutes or
 * hours, and the deep link to the subject's Targets page in the Gamma console. Modules owning a subject core cannot
 * name (an agent) call it with their own {@link Subject}.
 */
@Service
public class PerformanceTargetNotificationTemplateDataFactory {

    private static final DateTimeFormatter CHANGED_AT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'").withZone(ZoneOffset.UTC);

    private final AnalyticsDefinitionQueryService analyticsDefinition;
    private final InstallationAccessQueryService installationAccess;

    public PerformanceTargetNotificationTemplateDataFactory(
        AnalyticsDefinitionQueryService analyticsDefinition,
        InstallationAccessQueryService installationAccess
    ) {
        this.analyticsDefinition = analyticsDefinition;
        this.installationAccess = installationAccess;
    }

    /**
     * @param transitions the rules of one subject that changed the same way in one run; the report is dated by the
     *                    most recent evaluation among them
     */
    public PerformanceTargetNotificationTemplateData build(Subject subject, List<PerformanceTargetRuleTransition> transitions) {
        var changedAt = transitions
            .stream()
            .map(transition -> transition.evaluation().evaluatedAt())
            .max(Comparator.naturalOrder())
            .orElseGet(Instant::now);
        return PerformanceTargetNotificationTemplateData.builder()
            .subjectKind(subject.kind())
            .subjectName(subject.name())
            .subjectReference(subject.reference())
            .subjectUrl(subject.url())
            .changedAt(Date.from(changedAt))
            .changedAtText(CHANGED_AT.format(changedAt))
            .rules(transitions.stream().map(this::ruleChange).toList())
            .build();
    }

    /**
     * An API as a report introduces it, by the kind of API it is: a plain API lives in the Gamma APIM pages, an LLM,
     * MCP or A2A proxy in the AIM module's pages, and the deep link follows.
     */
    public Subject apiSubject(Environment environment, Api api) {
        var kind = ApiKind.of(api.getType());
        return new Subject(kind.label, api.getName(), api.getId(), targetsUrl(environment, kind.pages + "/" + api.getId() + "/targets"));
    }

    private enum ApiKind {
        API("API", "apim/apis"),
        LLM_PROXY("LLM proxy", "aim/llm-proxy"),
        MCP_PROXY("MCP proxy", "aim/mcp-proxy"),
        A2A_PROXY("A2A proxy", "aim/agent-runtime");

        private final String label;
        private final String pages;

        ApiKind(String label, String pages) {
            this.label = label;
            this.pages = pages;
        }

        static ApiKind of(ApiType type) {
            if (type == null) {
                return API;
            }
            return switch (type) {
                case LLM_PROXY -> LLM_PROXY;
                case MCP_PROXY -> MCP_PROXY;
                case A2A_PROXY -> A2A_PROXY;
                default -> API;
            };
        }
    }

    /**
     * A page of the Gamma console under the environment: {@code {gammaUrl}/environments/{hrid}/{path}}, the
     * environment named by its first human-readable id as the console's routes do, or its id when it has none.
     */
    public String targetsUrl(Environment environment, String path) {
        var gammaUrl = installationAccess.getGammaUrl(environment.getOrganizationId());
        if (gammaUrl == null || gammaUrl.isBlank()) {
            return null;
        }
        var hrid = environment.getHrids() == null || environment.getHrids().isEmpty()
            ? environment.getId()
            : environment.getHrids().getFirst();
        return gammaUrl.replaceAll("/+$", "") + "/environments/" + hrid + "/" + path;
    }

    private PerformanceTargetNotificationTemplateData.RuleChange ruleChange(PerformanceTargetRuleTransition transition) {
        var rule = transition.rule();
        var result = transition.result();
        var metric = analyticsDefinition.findMetric(rule.metric());
        var unit = metric.map(MetricSpec::unit).orElse(MetricSpec.Unit.NUMBER);
        var label = metric
            .map(MetricSpec::label)
            .filter(text -> !text.isBlank())
            .orElse(rule.metric().name());
        return PerformanceTargetNotificationTemplateData.RuleChange.builder()
            .targetId(transition.target().id())
            .ruleId(rule.id())
            .metric(rule.metric().name())
            .metricLabel(label)
            .measure(rule.measure().name())
            .operator(rule.operator().name())
            .description(describe(label, rule, unit))
            .threshold(rule.threshold())
            .thresholdText(format(rule.threshold(), unit))
            .observed(result.observed())
            .observedText(
                Optional.ofNullable(result.observed())
                    .map(observed -> format(observed, unit))
                    .orElse("no data")
            )
            .unit(unit.name())
            .sampleCount(result.sampleCount())
            .status(result.status().name())
            .windowText(durationText(transition.target().window()))
            .intervalText(durationText(transition.target().interval()))
            .build();
    }

    /** {@code Gateway response time P95 ≤ 2000 ms}; a percentage or a count needs no measure word. */
    static String describe(String metricLabel, PerformanceTarget.Rule rule, MetricSpec.Unit unit) {
        var measure = switch (rule.measure()) {
            case PERCENTAGE, COUNT, RATE -> "";
            default -> " " + rule.measure().name();
        };
        return metricLabel + measure + " " + symbol(rule.operator()) + " " + format(rule.threshold(), unit);
    }

    static String symbol(PerformanceTarget.Operator operator) {
        return switch (operator) {
            case LT -> "<";
            case LTE -> "≤";
            case GT -> ">";
            case GTE -> "≥";
        };
    }

    /** A value with its unit, trailing zeros dropped: {@code 2000 ms}, {@code 1.6 %}, {@code 0.012}. */
    static String format(double value, MetricSpec.Unit unit) {
        var number = BigDecimal.valueOf(value).setScale(3, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
        return switch (unit) {
            case MILLISECONDS -> number + " ms";
            case PERCENT -> number + " %";
            case BYTES -> number + " B";
            case PER_SECOND -> number + "/s";
            case NUMBER -> number;
        };
    }

    /** {@code 30 s}, {@code 15 min}, {@code 1 h}, {@code 24 h}: whole units, as the schedule form takes them. */
    static String durationText(Duration duration) {
        if (duration == null) {
            return "";
        }
        if (duration.toSeconds() % 3600 == 0) {
            return duration.toHours() + " h";
        }
        if (duration.toSeconds() % 60 == 0) {
            return duration.toMinutes() + " min";
        }
        return duration.toSeconds() + " s";
    }

    /**
     * @param kind {@code API} or {@code Agent}, as a sentence names it
     * @param url  the subject's Targets page, or {@code null} when unknown
     */
    public record Subject(String kind, String name, String reference, String url) {}
}
