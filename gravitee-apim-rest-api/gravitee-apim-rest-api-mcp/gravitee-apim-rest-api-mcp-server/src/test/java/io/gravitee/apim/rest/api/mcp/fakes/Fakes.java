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

import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.rest.api.mcp.core.model.ApiRef;
import io.gravitee.apim.rest.api.mcp.core.model.Change;
import io.gravitee.apim.rest.api.mcp.core.model.TimeWindow;
import io.gravitee.apim.rest.api.mcp.core.model.TrafficStats;
import io.gravitee.apim.rest.api.mcp.core.port.ApiDirectory;
import io.gravitee.apim.rest.api.mcp.core.port.ChangeLog;
import io.gravitee.apim.rest.api.mcp.core.port.RequestLogs;
import io.gravitee.apim.rest.api.mcp.core.port.TriageAccess;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class Fakes {

    public static final Instant NOW = Instant.parse("2026-01-31T14:00:00Z");
    public static final AuditInfo CALLER = new AuditInfo("DEFAULT", "DEFAULT", new AuditActor("user-id", "memory", "user"));

    private Fakes() {}

    /** Traffic with the given numbers of 4xx and 5xx, answered in 100 ms at the 95th percentile. */
    public static TrafficStats traffic(long requests, long clientErrors, long serverErrors) {
        return TrafficStats.of(requests, clientErrors, serverErrors, 100.0, 80.0);
    }

    public static TrafficStats trafficAnsweredIn(long requests, double responseTimeP95Ms) {
        return TrafficStats.of(requests, 0, 0, responseTimeP95Ms, responseTimeP95Ms - 20);
    }

    public static ApiDirectory apis(ApiRef... apis) {
        return caller -> List.of(apis);
    }

    public static class Access implements TriageAccess {

        public boolean traffic = true;
        public boolean events = true;
        public boolean audit = true;

        @Override
        public boolean canReadTraffic(AuditInfo caller) {
            return traffic;
        }

        @Override
        public boolean canReadApiEvents(AuditInfo caller, String apiId) {
            return events;
        }

        @Override
        public boolean canReadApiAudit(AuditInfo caller, String apiId) {
            return audit;
        }
    }

    public static class InMemoryRequestLogs implements RequestLogs {

        public Page page = new Page(0, List.of());
        public Query query;
        public int limit;

        @Override
        public Page failures(AuditInfo caller, TimeWindow window, Query query, int limit) {
            this.query = query;
            this.limit = limit;
            return page;
        }
    }

    public static class InMemoryChangeLog implements ChangeLog {

        public final List<Change> deployments = new ArrayList<>();
        public final List<Change> audit = new ArrayList<>();

        @Override
        public List<Change> deployments(AuditInfo caller, String apiId, TimeWindow window) {
            return deployments;
        }

        @Override
        public List<Change> audit(AuditInfo caller, String apiId, TimeWindow window) {
            return audit;
        }
    }
}
