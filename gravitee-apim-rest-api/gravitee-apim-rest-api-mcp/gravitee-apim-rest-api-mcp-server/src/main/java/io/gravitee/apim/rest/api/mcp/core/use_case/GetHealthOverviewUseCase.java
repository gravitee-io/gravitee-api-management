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
import io.gravitee.apim.rest.api.mcp.core.FindingRules;
import io.gravitee.apim.rest.api.mcp.core.TriageException;
import io.gravitee.apim.rest.api.mcp.core.model.ApiRef;
import io.gravitee.apim.rest.api.mcp.core.model.Dimension;
import io.gravitee.apim.rest.api.mcp.core.model.Finding;
import io.gravitee.apim.rest.api.mcp.core.model.TimeWindow;
import io.gravitee.apim.rest.api.mcp.core.model.TrafficSample;
import io.gravitee.apim.rest.api.mcp.core.model.TrafficScope;
import io.gravitee.apim.rest.api.mcp.core.model.TrafficStats;
import io.gravitee.apim.rest.api.mcp.core.port.TrafficAnalytics;
import io.gravitee.apim.rest.api.mcp.core.port.TriageAccess;
import io.gravitee.common.utils.TimeProvider;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Compares the traffic of an environment with the time range just before, and reports what moved and on which APIs.
 */
@UseCase
public class GetHealthOverviewUseCase {

    static final int MAX_APIS = 10;

    private final TriageAccess access;
    private final TrafficAnalytics analytics;

    public GetHealthOverviewUseCase(TriageAccess access, TrafficAnalytics analytics) {
        this.access = access;
        this.analytics = analytics;
    }

    public record Input(AuditInfo caller, String window, String until) {}

    public record Output(
        Status status,
        String summary,
        TimeWindow window,
        TimeWindow baseline,
        Comparison environment,
        List<Finding> findings,
        List<ApiHealth> apis
    ) {}

    public enum Status {
        HEALTHY,
        DEGRADED,
        NO_TRAFFIC,
    }

    public record Comparison(TrafficStats current, TrafficStats baseline) {}

    public record ApiHealth(ApiRef api, TrafficStats current, TrafficStats baseline, List<Finding.Kind> findings) {}

    public Output execute(Input input) {
        var caller = input.caller();
        if (!access.canReadTraffic(caller)) {
            throw new TriageException("You are not allowed to read the analytics of this environment.");
        }
        var window = TimeWindow.parse(input.window(), input.until(), TimeProvider.instantNow());
        var baseline = window.baseline();

        var current = analytics.total(caller, window, TrafficScope.ENVIRONMENT);
        var before = analytics.total(caller, baseline, TrafficScope.ENVIRONMENT);

        var findings = new ArrayList<Finding>();
        for (var kind : FindingRules.evaluate(current, before)) {
            findings.add(new Finding(kind, null, FindingRules.describe(kind, current, before)));
        }

        var apis = new ArrayList<ApiHealth>();
        var beforeByApi = byKey(analytics.by(caller, baseline, TrafficScope.ENVIRONMENT, Dimension.API));
        var currentByApi = byKey(analytics.by(caller, window, TrafficScope.ENVIRONMENT, Dimension.API));
        var seen = new LinkedHashMap<String, TrafficSample>(currentByApi);
        beforeByApi.forEach(seen::putIfAbsent);
        for (var sample : seen.values()) {
            var api = new ApiRef(sample.key(), sample.name());
            var apiCurrent = statsOf(currentByApi, sample.key());
            var apiBefore = statsOf(beforeByApi, sample.key());
            var kinds = FindingRules.evaluate(apiCurrent, apiBefore);
            for (var kind : kinds) {
                findings.add(new Finding(kind, api, FindingRules.describe(kind, apiCurrent, apiBefore)));
            }
            apis.add(new ApiHealth(api, apiCurrent, apiBefore, kinds));
        }
        apis.sort(
            Comparator.comparingInt((ApiHealth api) -> api.findings().size())
                .thenComparingLong(api -> api.current().requests())
                .reversed()
        );

        var status = status(current, before, findings);
        return new Output(
            status,
            summarize(status, findings),
            window,
            baseline,
            new Comparison(current, before),
            findings,
            apis.stream().limit(MAX_APIS).toList()
        );
    }

    private static Status status(TrafficStats current, TrafficStats before, List<Finding> findings) {
        if (!findings.isEmpty()) {
            return Status.DEGRADED;
        }
        return current.requests() == 0 && before.requests() == 0 ? Status.NO_TRAFFIC : Status.HEALTHY;
    }

    private static String summarize(Status status, List<Finding> findings) {
        return switch (status) {
            case NO_TRAFFIC -> "No traffic in the window or in the baseline.";
            case HEALTHY -> "Nothing moved against the baseline.";
            case DEGRADED -> {
                long apis = findings
                    .stream()
                    .filter(finding -> finding.api() != null)
                    .map(finding -> finding.api().id())
                    .distinct()
                    .count();
                yield findings.size() + " finding(s), on " + apis + " API(s). Break the traffic of an affected API down to localize it.";
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
