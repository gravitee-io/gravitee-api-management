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
import io.gravitee.apim.rest.api.mcp.core.ApiResolver;
import io.gravitee.apim.rest.api.mcp.core.FindingRules;
import io.gravitee.apim.rest.api.mcp.core.TriageException;
import io.gravitee.apim.rest.api.mcp.core.model.ApiRef;
import io.gravitee.apim.rest.api.mcp.core.model.Finding;
import io.gravitee.apim.rest.api.mcp.core.model.TimeWindow;
import io.gravitee.apim.rest.api.mcp.core.model.TimelinePoint;
import io.gravitee.apim.rest.api.mcp.core.model.TrafficScope;
import io.gravitee.apim.rest.api.mcp.core.model.TrafficStats;
import io.gravitee.apim.rest.api.mcp.core.port.ApiDirectory;
import io.gravitee.apim.rest.api.mcp.core.port.TrafficAnalytics;
import io.gravitee.apim.rest.api.mcp.core.port.TriageAccess;
import io.gravitee.common.utils.TimeProvider;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Traffic over time, and the first interval that departs from the baseline.
 */
@UseCase
public class GetTrafficTimelineUseCase {

    static final int POINTS = 30;
    static final Duration SHORTEST_INTERVAL = Duration.ofSeconds(10);

    private final TriageAccess access;
    private final ApiDirectory apis;
    private final TrafficAnalytics analytics;

    public GetTrafficTimelineUseCase(TriageAccess access, ApiDirectory apis, TrafficAnalytics analytics) {
        this.access = access;
        this.apis = apis;
        this.analytics = analytics;
    }

    public record Input(AuditInfo caller, String api, String window, String until) {}

    /**
     * @param baselinePerInterval what one interval looked like, on average, in the range before the window
     * @param onset null when no interval departs from the baseline
     */
    public record Output(
        String summary,
        ApiRef api,
        TimeWindow window,
        long intervalSeconds,
        TrafficStats baselinePerInterval,
        Onset onset,
        List<TimelinePoint> points
    ) {}

    public record Onset(Instant at, List<Finding.Kind> findings) {}

    public Output execute(Input input) {
        var caller = input.caller();
        if (!access.canReadTraffic(caller)) {
            throw new TriageException("You are not allowed to read the analytics of this environment.");
        }
        var window = TimeWindow.parse(input.window(), input.until(), TimeProvider.instantNow());
        var api = ApiResolver.resolve(apis, caller, input.api());
        var scope = TrafficScope.ofApi(api);
        var interval = intervalFor(window);

        var points = analytics.timeline(caller, window, interval, scope);
        var baseline = perInterval(analytics.total(caller, window.baseline(), scope), window, interval);
        var onset = points
            .stream()
            .map(point -> new Onset(point.at(), FindingRules.evaluate(point.stats(), baseline)))
            .filter(candidate -> !candidate.findings().isEmpty())
            .findFirst()
            .orElse(null);

        var summary = onset == null
            ? "No interval departs from the baseline."
            : "Traffic departs from the baseline at " + onset.at() + ". List the changes made just before that time.";
        return new Output(summary, api, window, interval.toSeconds(), baseline, onset, points);
    }

    private static Duration intervalFor(TimeWindow window) {
        var interval = Duration.ofSeconds(window.length().toSeconds() / POINTS);
        return interval.compareTo(SHORTEST_INTERVAL) < 0 ? SHORTEST_INTERVAL : interval;
    }

    private static TrafficStats perInterval(TrafficStats total, TimeWindow window, Duration interval) {
        double share = (double) interval.toSeconds() / window.length().toSeconds();
        return TrafficStats.of(
            Math.round(total.requests() * share),
            Math.round(total.clientErrors() * share),
            Math.round(total.serverErrors() * share),
            total.responseTimeP95Ms(),
            total.endpointResponseTimeP95Ms()
        );
    }
}
