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
package io.gravitee.apim.rest.api.mcp.fakes;

import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.rest.api.mcp.core.model.Dimension;
import io.gravitee.apim.rest.api.mcp.core.model.TimeWindow;
import io.gravitee.apim.rest.api.mcp.core.model.TimelinePoint;
import io.gravitee.apim.rest.api.mcp.core.model.TrafficSample;
import io.gravitee.apim.rest.api.mcp.core.model.TrafficScope;
import io.gravitee.apim.rest.api.mcp.core.model.TrafficStats;
import io.gravitee.apim.rest.api.mcp.core.port.TrafficAnalytics;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Traffic given per time range, for every environment or for one of them.
 */
public class InMemoryTrafficAnalytics implements TrafficAnalytics {

    private final Map<String, TrafficStats> totals = new HashMap<>();
    private final Map<String, List<TrafficSample>> samples = new HashMap<>();
    private List<TimelinePoint> points = List.of();

    public final List<AuditInfo> callers = new ArrayList<>();
    public final List<TrafficScope> scopes = new ArrayList<>();
    public final List<Dimension> dimensions = new ArrayList<>();
    public Duration interval;

    public InMemoryTrafficAnalytics total(TimeWindow window, TrafficStats stats) {
        totals.put(key(null, window), stats);
        return this;
    }

    public InMemoryTrafficAnalytics totalIn(String environmentId, TimeWindow window, TrafficStats stats) {
        totals.put(key(environmentId, window), stats);
        return this;
    }

    public InMemoryTrafficAnalytics samples(TimeWindow window, TrafficSample... given) {
        samples.put(key(null, window), List.of(given));
        return this;
    }

    public InMemoryTrafficAnalytics samplesIn(String environmentId, TimeWindow window, TrafficSample... given) {
        samples.put(key(environmentId, window), List.of(given));
        return this;
    }

    public InMemoryTrafficAnalytics timeline(TimelinePoint... given) {
        points = List.of(given);
        return this;
    }

    @Override
    public TrafficStats total(AuditInfo caller, TimeWindow window, TrafficScope scope) {
        callers.add(caller);
        scopes.add(scope);
        return totals.getOrDefault(key(caller.environmentId(), window), totals.getOrDefault(key(null, window), TrafficStats.NONE));
    }

    @Override
    public List<TrafficSample> by(AuditInfo caller, TimeWindow window, TrafficScope scope, Dimension dimension) {
        callers.add(caller);
        scopes.add(scope);
        dimensions.add(dimension);
        return samples.getOrDefault(key(caller.environmentId(), window), samples.getOrDefault(key(null, window), List.of()));
    }

    @Override
    public List<TimelinePoint> timeline(AuditInfo caller, TimeWindow window, Duration interval, TrafficScope scope) {
        callers.add(caller);
        scopes.add(scope);
        this.interval = interval;
        return points;
    }

    private static String key(String environmentId, TimeWindow window) {
        return environmentId + "|" + window;
    }
}
