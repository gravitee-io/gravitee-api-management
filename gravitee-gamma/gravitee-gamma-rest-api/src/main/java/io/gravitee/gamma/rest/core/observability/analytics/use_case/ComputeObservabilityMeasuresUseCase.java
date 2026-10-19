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
package io.gravitee.gamma.rest.core.observability.analytics.use_case;

import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.apim.core.UseCase;
import io.gravitee.gamma.rest.core.observability.analytics.model.AnalyticsMetricQuery;
import io.gravitee.gamma.rest.core.observability.analytics.port.service_provider.ObservabilityAnalyticsDataPort;
import io.gravitee.gamma.rest.core.observability.analytics.port.service_provider.ObservabilityAnalyticsDataPort.MeasuresQuery;
import io.gravitee.gamma.rest.core.observability.analytics.use_case.AnalyticsRequestPipeline.PreparedScope;
import io.gravitee.gamma.rest.core.observability.exception.InvalidObservabilityQueryException;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterCondition;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.AllArgsConstructor;

/**
 * Computes measures, each metric's own conditions ANDed with the request's. Metrics sharing a set of
 * conditions share one engine query; groups run one after the other.
 *
 * <p>Without ids the response is read by metric name, so a metric queried twice cannot carry two
 * results: its own conditions are reported as not applied rather than mixed into one entry. Without ids,
 * more condition sets than the cap fall back the same way instead of being refused, so a library already
 * deployed keeps an answer.
 *
 * @author GraviteeSource Team
 */
@UseCase
@AllArgsConstructor
public class ComputeObservabilityMeasuresUseCase {

    static final int MAX_METRIC_CONDITION_SETS = 10;

    private final ObservabilityAnalyticsDataPort analyticsDataPort;
    private final AnalyticsRequestPipeline pipeline;

    public record Input(
        String organizationId,
        String environmentId,
        List<FilterCondition> filters,
        Instant from,
        Instant to,
        List<AnalyticsMetricQuery> metrics
    ) {}

    public record Output(JsonNode response) {}

    public Output execute(Input input) {
        if (input.metrics == null || input.metrics.isEmpty()) {
            throw InvalidObservabilityQueryException.missingMetrics();
        }
        // Before the request is prepared: a refused metric never reaches the data.
        var identified = validateIds(input.metrics);
        pipeline.validateMetricConditions(input.metrics.stream().map(AnalyticsMetricQuery::filters).toList());
        var applied = identified ? appliedWithIds(input.metrics) : appliedWithoutIds(input.metrics);

        var kind = pipeline.dataKindOf(input.filters, input.metrics.stream().map(AnalyticsMetricQuery::filters).toList(), List.of());
        var prepared = pipeline.prepare(
            input.organizationId,
            input.environmentId,
            input.filters,
            input.from,
            input.to,
            kind,
            input.metrics.stream().map(AnalyticsMetricQuery::metricName).toList(),
            analyticsDataPort
        );
        var groupOf = new HashMap<Set<FilterCondition>, Integer>();
        var groups = new ArrayList<PreparedScope>();
        var metrics = new ArrayList<MeasuresQuery.Metric>();
        for (int i = 0; i < input.metrics.size(); i++) {
            var metric = input.metrics.get(i);
            var conditions = applied.get(i);
            int group = groupOf.computeIfAbsent(AnalyticsRequestPipeline.conditionSet(conditions), set -> {
                groups.add(prepared.scopeWith(conditions));
                return groups.size() - 1;
            });
            metrics.add(new MeasuresQuery.Metric(metric, group, notApplied(input.filters, metric, conditions)));
        }

        var query = new MeasuresQuery(input.organizationId, input.environmentId, List.copyOf(groups), List.copyOf(metrics));
        analyticsDataPort.validate(query);
        return new Output(analyticsDataPort.computeMeasures(query));
    }

    /** Whether the metrics carry ids: all of them, each once, or none. */
    private static boolean validateIds(List<AnalyticsMetricQuery> metrics) {
        var ids = metrics.stream().map(AnalyticsMetricQuery::id).filter(Objects::nonNull).toList();
        if (ids.isEmpty()) {
            return false;
        }
        if (ids.size() != metrics.size()) {
            throw InvalidObservabilityQueryException.partialMetricIds();
        }
        var seen = new HashSet<String>();
        ids
            .stream()
            .filter(id -> !seen.add(id))
            .findFirst()
            .ifPresent(id -> {
                throw InvalidObservabilityQueryException.duplicateMetricId(id);
            });
        return true;
    }

    private static List<List<FilterCondition>> appliedWithIds(List<AnalyticsMetricQuery> metrics) {
        var applied = metrics.stream().map(AnalyticsMetricQuery::filters).toList();
        var sets = conditionSets(applied);
        if (sets > MAX_METRIC_CONDITION_SETS) {
            throw InvalidObservabilityQueryException.tooManyMetricConditionSets(sets, MAX_METRIC_CONDITION_SETS);
        }
        return applied;
    }

    private static List<List<FilterCondition>> appliedWithoutIds(List<AnalyticsMetricQuery> metrics) {
        // A HashMap takes the null name of a malformed metric, which the engine validation refuses later.
        var countByName = new HashMap<String, Integer>();
        metrics.forEach(metric -> countByName.merge(metric.metricName(), 1, Integer::sum));
        var applied = metrics
            .stream()
            .map(metric -> countByName.get(metric.metricName()) > 1 ? List.<FilterCondition>of() : metric.filters())
            .toList();
        if (conditionSets(applied) > MAX_METRIC_CONDITION_SETS) {
            return metrics
                .stream()
                .map(metric -> List.<FilterCondition>of())
                .toList();
        }
        return applied;
    }

    private static long conditionSets(List<List<FilterCondition>> conditions) {
        return conditions
            .stream()
            .filter(set -> !set.isEmpty())
            .map(AnalyticsRequestPipeline::conditionSet)
            .distinct()
            .count();
    }

    private List<String> notApplied(List<FilterCondition> requestFilters, AnalyticsMetricQuery metric, List<FilterCondition> applied) {
        var names = new LinkedHashSet<>(pipeline.recordTypesNotApplied(requestFilters, metric.metricName()));
        if (applied.isEmpty()) {
            metric.filters().stream().map(FilterCondition::name).forEach(names::add);
        }
        return List.copyOf(names);
    }
}
