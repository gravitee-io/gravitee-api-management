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

public class InMemoryTrafficAnalytics implements TrafficAnalytics {

    private final Map<TimeWindow, TrafficStats> totals = new HashMap<>();
    private final Map<TimeWindow, List<TrafficSample>> samples = new HashMap<>();
    private List<TimelinePoint> points = List.of();

    public final List<TrafficScope> scopes = new ArrayList<>();
    public final List<Dimension> dimensions = new ArrayList<>();
    public Duration interval;

    public InMemoryTrafficAnalytics total(TimeWindow window, TrafficStats stats) {
        totals.put(window, stats);
        return this;
    }

    public InMemoryTrafficAnalytics samples(TimeWindow window, TrafficSample... given) {
        samples.put(window, List.of(given));
        return this;
    }

    public InMemoryTrafficAnalytics timeline(TimelinePoint... given) {
        points = List.of(given);
        return this;
    }

    @Override
    public TrafficStats total(AuditInfo caller, TimeWindow window, TrafficScope scope) {
        scopes.add(scope);
        return totals.getOrDefault(window, TrafficStats.NONE);
    }

    @Override
    public List<TrafficSample> by(AuditInfo caller, TimeWindow window, TrafficScope scope, Dimension dimension) {
        scopes.add(scope);
        dimensions.add(dimension);
        return samples.getOrDefault(window, List.of());
    }

    @Override
    public List<TimelinePoint> timeline(AuditInfo caller, TimeWindow window, Duration interval, TrafficScope scope) {
        scopes.add(scope);
        this.interval = interval;
        return points;
    }
}
