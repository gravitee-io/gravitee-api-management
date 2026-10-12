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
import io.gravitee.apim.rest.api.mcp.core.TriageException;
import io.gravitee.apim.rest.api.mcp.core.model.ApiRef;
import io.gravitee.apim.rest.api.mcp.core.model.Dimension;
import io.gravitee.apim.rest.api.mcp.core.model.StatusGroup;
import io.gravitee.apim.rest.api.mcp.core.model.TimeWindow;
import io.gravitee.apim.rest.api.mcp.core.model.TrafficSample;
import io.gravitee.apim.rest.api.mcp.core.model.TrafficScope;
import io.gravitee.apim.rest.api.mcp.core.model.TrafficStats;
import io.gravitee.apim.rest.api.mcp.core.port.ApiDirectory;
import io.gravitee.apim.rest.api.mcp.core.port.TrafficAnalytics;
import io.gravitee.apim.rest.api.mcp.core.port.TriageAccess;
import io.gravitee.common.utils.TimeProvider;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

/**
 * Splits traffic along one dimension, in the window and in its baseline, so that a value that vanished and one that
 * appeared are both visible.
 */
@UseCase
public class BreakDownTrafficUseCase {

    static final int MAX_BUCKETS = 20;

    /** The id the gateway records when a request is rejected before its application is known. */
    static final String UNRESOLVED_APPLICATION_ID = "1";
    static final String UNRESOLVED_APPLICATION_NAME = "Unknown (credential not resolved)";

    private final TriageAccess access;
    private final ApiDirectory apis;
    private final TrafficAnalytics analytics;

    public BreakDownTrafficUseCase(TriageAccess access, ApiDirectory apis, TrafficAnalytics analytics) {
        this.access = access;
        this.apis = apis;
        this.analytics = analytics;
    }

    public record Input(AuditInfo caller, String by, String api, String statusGroup, String window, String until) {}

    public record Output(
        String summary,
        String by,
        ApiRef api,
        String statusGroup,
        TimeWindow window,
        TimeWindow baseline,
        List<Bucket> buckets
    ) {}

    public record Bucket(String key, String name, Presence presence, TrafficStats current, TrafficStats baseline) {}

    public enum Presence {
        /** Traffic in the window, none in the baseline. */
        APPEARED,
        /** Traffic in the baseline, none in the window. */
        VANISHED,
        BOTH,
    }

    public Output execute(Input input) {
        var caller = input.caller();
        if (!access.canReadTraffic(caller)) {
            throw new TriageException("You are not allowed to read the analytics of this environment.");
        }
        var dimension = Dimension.parse(input.by());
        var statusGroup = StatusGroup.parse(input.statusGroup());
        var window = TimeWindow.parse(input.window(), input.until(), TimeProvider.instantNow());
        var baseline = window.baseline();
        var api = ApiResolver.resolve(apis, caller, input.api());
        var scope = TrafficScope.ofApi(api).with(statusGroup);

        var current = analytics.by(caller, window, scope, dimension);
        var before = analytics.by(caller, baseline, scope, dimension);

        var merged = new LinkedHashMap<String, Bucket>();
        for (var sample : current) {
            merged.put(
                sample.key(),
                new Bucket(sample.key(), nameOf(dimension, sample), Presence.APPEARED, sample.stats(), TrafficStats.NONE)
            );
        }
        for (var sample : before) {
            merged.merge(
                sample.key(),
                new Bucket(sample.key(), nameOf(dimension, sample), Presence.VANISHED, TrafficStats.NONE, sample.stats()),
                (inWindow, inBaseline) ->
                    new Bucket(inWindow.key(), inWindow.name(), Presence.BOTH, inWindow.current(), inBaseline.baseline())
            );
        }
        var buckets = new ArrayList<>(merged.values());
        buckets.sort(
            Comparator.comparingLong((Bucket bucket) -> Math.max(bucket.current().requests(), bucket.baseline().requests())).reversed()
        );
        var kept = buckets.stream().limit(MAX_BUCKETS).toList();
        return new Output(
            summarize(kept),
            dimension.name().toLowerCase(Locale.ROOT),
            api,
            statusGroup == null ? null : statusGroup.label(),
            window,
            baseline,
            kept
        );
    }

    private static String nameOf(Dimension dimension, TrafficSample sample) {
        return dimension == Dimension.APPLICATION && UNRESOLVED_APPLICATION_ID.equals(sample.key())
            ? UNRESOLVED_APPLICATION_NAME
            : sample.name();
    }

    private static String summarize(List<Bucket> buckets) {
        long appeared = buckets
            .stream()
            .filter(bucket -> bucket.presence() == Presence.APPEARED)
            .count();
        long vanished = buckets
            .stream()
            .filter(bucket -> bucket.presence() == Presence.VANISHED)
            .count();
        return buckets.size() + " value(s): " + appeared + " appeared and " + vanished + " vanished since the baseline.";
    }
}
