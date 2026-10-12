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
package io.gravitee.apim.rest.api.mcp.core.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.rest.api.mcp.core.EnvironmentResolver;
import io.gravitee.apim.rest.api.mcp.core.FindingRules;
import io.gravitee.apim.rest.api.mcp.core.model.ApiRef;
import io.gravitee.apim.rest.api.mcp.core.model.Dimension;
import io.gravitee.apim.rest.api.mcp.core.model.EnvRef;
import io.gravitee.apim.rest.api.mcp.core.model.Finding;
import io.gravitee.apim.rest.api.mcp.core.model.TimeWindow;
import io.gravitee.apim.rest.api.mcp.core.model.TrafficSample;
import io.gravitee.apim.rest.api.mcp.core.model.TrafficScope;
import io.gravitee.apim.rest.api.mcp.core.model.TrafficStats;
import io.gravitee.apim.rest.api.mcp.core.port.EnvironmentDirectory;
import io.gravitee.apim.rest.api.mcp.core.port.TrafficAnalytics;
import io.gravitee.apim.rest.api.mcp.spi.McpCaller;
import io.gravitee.common.utils.TimeProvider;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Compares the traffic of each environment with the time range just before, and reports what moved and where.
 */
@UseCase
public class GetHealthOverviewUseCase {

    static final int MAX_APIS = 10;

    private final EnvironmentDirectory environments;
    private final TrafficAnalytics analytics;

    public GetHealthOverviewUseCase(EnvironmentDirectory environments, TrafficAnalytics analytics) {
        this.environments = environments;
        this.analytics = analytics;
    }

    public record Input(McpCaller caller, String environment, String window, String until) {}

    /**
     * @param omittedEnvironments environments of the organization the caller may not read: they are not covered
     */
    public record Output(
        Status status,
        String summary,
        TimeWindow window,
        TimeWindow baseline,
        List<EnvironmentHealth> environments,
        List<EnvRef> omittedEnvironments,
        List<Finding> findings,
        List<ApiHealth> apis
    ) {}

    public enum Status {
        HEALTHY,
        DEGRADED,
        NO_TRAFFIC,
    }

    public record EnvironmentHealth(
        EnvRef environment,
        Status status,
        TrafficStats current,
        TrafficStats baseline,
        List<Finding.Kind> findings
    ) {}

    public record ApiHealth(EnvRef environment, ApiRef api, TrafficStats current, TrafficStats baseline, List<Finding.Kind> findings) {}

    public Output execute(Input input) {
        var caller = input.caller();
        var covered = EnvironmentResolver.environments(environments, caller, input.environment());
        var omitted = input.environment() == null || input.environment().isBlank()
            ? environments.environments(caller).unreadable()
            : List.<EnvRef>of();
        var window = TimeWindow.parse(input.window(), input.until(), TimeProvider.instantNow());
        var baseline = window.baseline();

        var healths = new ArrayList<EnvironmentHealth>();
        var findings = new ArrayList<Finding>();
        var apis = new ArrayList<ApiHealth>();
        for (var env : covered) {
            var auditInfo = caller.on(env.id());
            var current = analytics.total(auditInfo, window, TrafficScope.ENVIRONMENT);
            var before = analytics.total(auditInfo, baseline, TrafficScope.ENVIRONMENT);
            var kinds = FindingRules.evaluate(current, before);
            for (var kind : kinds) {
                findings.add(new Finding(kind, env, null, FindingRules.describe(kind, current, before)));
            }
            int apiFindings = apis(env, auditInfo, window, baseline, findings, apis);
            var status = kinds.isEmpty() && apiFindings == 0
                ? current.requests() == 0 && before.requests() == 0
                    ? Status.NO_TRAFFIC
                    : Status.HEALTHY
                : Status.DEGRADED;
            healths.add(new EnvironmentHealth(env, status, current, before, kinds));
        }
        healths.sort(
            Comparator.comparingInt((EnvironmentHealth health) -> health.status() == Status.DEGRADED ? 1 : 0)
                .thenComparingLong(health -> health.current().requests())
                .reversed()
        );
        apis.sort(
            Comparator.comparingInt((ApiHealth api) -> api.findings().size())
                .thenComparingLong(api -> api.current().requests())
                .reversed()
        );

        var status = status(healths);
        return new Output(
            status,
            summarize(status, healths, findings),
            window,
            baseline,
            healths,
            omitted,
            findings,
            apis.stream().limit(MAX_APIS).toList()
        );
    }

    /** Adds the findings and health of the APIs of one environment; returns how many findings the APIs carry. */
    private int apis(
        EnvRef env,
        AuditInfo auditInfo,
        TimeWindow window,
        TimeWindow baseline,
        List<Finding> findings,
        List<ApiHealth> apis
    ) {
        var beforeByApi = byKey(analytics.by(auditInfo, baseline, TrafficScope.ENVIRONMENT, Dimension.API));
        var currentByApi = byKey(analytics.by(auditInfo, window, TrafficScope.ENVIRONMENT, Dimension.API));
        var seen = new LinkedHashMap<String, TrafficSample>(currentByApi);
        beforeByApi.forEach(seen::putIfAbsent);
        int count = 0;
        for (var sample : seen.values()) {
            var api = new ApiRef(sample.key(), sample.name());
            var apiCurrent = statsOf(currentByApi, sample.key());
            var apiBefore = statsOf(beforeByApi, sample.key());
            var kinds = FindingRules.evaluate(apiCurrent, apiBefore);
            for (var kind : kinds) {
                findings.add(new Finding(kind, env, api, FindingRules.describe(kind, apiCurrent, apiBefore)));
            }
            count += kinds.size();
            apis.add(new ApiHealth(env, api, apiCurrent, apiBefore, kinds));
        }
        return count;
    }

    private static Status status(List<EnvironmentHealth> healths) {
        if (healths.stream().anyMatch(health -> health.status() == Status.DEGRADED)) {
            return Status.DEGRADED;
        }
        return healths.stream().allMatch(health -> health.status() == Status.NO_TRAFFIC) ? Status.NO_TRAFFIC : Status.HEALTHY;
    }

    private static String summarize(Status status, List<EnvironmentHealth> healths, List<Finding> findings) {
        var scope = healths.size() == 1 ? healths.getFirst().environment().label() : healths.size() + " environments";
        return switch (status) {
            case NO_TRAFFIC -> "No traffic in the window or in the baseline, in " + scope + ".";
            case HEALTHY -> "Nothing moved against the baseline in " + scope + ".";
            case DEGRADED -> {
                var degraded = healths
                    .stream()
                    .filter(health -> health.status() == Status.DEGRADED)
                    .map(health -> health.environment().label())
                    .toList();
                long apis = findings
                    .stream()
                    .filter(finding -> finding.api() != null)
                    .map(finding -> finding.environment().id() + "/" + finding.api().id())
                    .distinct()
                    .count();
                yield findings.size() +
                " finding(s) on " +
                apis +
                " API(s) in " +
                String.join(", ", degraded) +
                ". Break the traffic of an affected API down to localize it.";
            }
        };
    }

    private static Map<String, TrafficSample> byKey(List<TrafficSample> samples) {
        var byKey = new LinkedHashMap<String, TrafficSample>();
        samples.forEach(sample -> byKey.put(sample.key(), sample));
        return byKey;
    }

    private static TrafficStats statsOf(Map<String, TrafficSample> samples, String key) {
        var sample = samples.get(key);
        return sample == null ? TrafficStats.NONE : sample.stats();
    }
}
