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

import io.gravitee.apim.core.analytics_engine.model.FacetsRequest;
import io.gravitee.apim.core.analytics_engine.model.FacetsResponse;
import io.gravitee.apim.core.analytics_engine.model.FilterValue;
import io.gravitee.apim.core.analytics_engine.model.FilterValuesPage;
import io.gravitee.apim.core.analytics_engine.model.MeasuresRequest;
import io.gravitee.apim.core.analytics_engine.model.MeasuresResponse;
import io.gravitee.apim.core.analytics_engine.model.TimeSeriesRequest;
import io.gravitee.apim.core.analytics_engine.model.TimeSeriesResponse;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.logs_engine.model.Pagination;
import io.gravitee.apim.core.logs_engine.model.SearchLogsRequest;
import io.gravitee.apim.core.logs_engine.model.SearchLogsResponse;
import io.gravitee.apim.rest.api.mcp.core.model.ApiRef;
import io.gravitee.apim.rest.api.mcp.core.model.ApiSummary;
import io.gravitee.apim.rest.api.mcp.core.model.Change;
import io.gravitee.apim.rest.api.mcp.core.model.EnvRef;
import io.gravitee.apim.rest.api.mcp.core.model.Notification;
import io.gravitee.apim.rest.api.mcp.core.model.TimeWindow;
import io.gravitee.apim.rest.api.mcp.core.port.AnalyticsQueries;
import io.gravitee.apim.rest.api.mcp.core.port.ApiDirectory;
import io.gravitee.apim.rest.api.mcp.core.port.ChangeLog;
import io.gravitee.apim.rest.api.mcp.core.port.EnvironmentDirectory;
import io.gravitee.apim.rest.api.mcp.core.port.LogSearch;
import io.gravitee.apim.rest.api.mcp.core.port.Messaging;
import io.gravitee.apim.rest.api.mcp.core.port.TriageAccess;
import io.gravitee.apim.rest.api.mcp.spi.McpCaller;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class Fakes {

    public static final Instant NOW = Instant.parse("2026-01-31T14:00:00Z");
    public static final McpCaller MCP_CALLER = new McpCaller("DEFAULT", "user-id", "memory", "user");
    public static final EnvRef PROD = new EnvRef("e-prod", "prod", "Production");
    public static final EnvRef STAGING = new EnvRef("e-staging", "staging", "Staging");
    /** The caller on the production environment. */
    public static final AuditInfo CALLER = MCP_CALLER.on(PROD.id());

    private Fakes() {}

    public static ApiSummary api(String id, String name) {
        return new ApiSummary(id, name, "1.0", "PROXY", "V4", NOW);
    }

    /** The same APIs in every environment. */
    public static ApiDirectory apis(ApiRef... apis) {
        var summaries = Arrays.stream(apis)
            .map(api -> api(api.id(), api.name()))
            .toList();
        return caller -> summaries;
    }

    /** APIs per environment id. */
    public static ApiDirectory apisByEnvironment(Map<String, List<ApiRef>> apis) {
        return caller ->
            apis
                .getOrDefault(caller.environmentId(), List.of())
                .stream()
                .map(api -> api(api.id(), api.name()))
                .toList();
    }

    /** Readable environments; none unreadable. */
    public static Directory environments(EnvRef... readable) {
        var directory = new Directory();
        directory.readable.addAll(List.of(readable));
        return directory;
    }

    public static class Directory implements EnvironmentDirectory {

        public final List<EnvRef> readable = new ArrayList<>();
        public final List<EnvRef> unreadable = new ArrayList<>();

        @Override
        public Environments environments(McpCaller caller) {
            return new Environments(List.copyOf(readable), List.copyOf(unreadable));
        }
    }

    public static class Access implements TriageAccess {

        public boolean traffic = true;
        public boolean events = true;
        public boolean audit = true;
        public boolean notify = true;

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

        @Override
        public boolean canNotify(AuditInfo caller, String apiId) {
            return notify;
        }
    }

    public static class StubMessaging implements Messaging {

        public List<Recipient> recipients = List.of(new Recipient("jane-id", "Jane Owner", "jane@acme.example"));
        public Notification sent;
        public String sentApiId;

        @Override
        public List<Recipient> recipients(AuditInfo caller, String apiId, Notification notification) {
            return notification.channel() == Notification.Channel.WEBHOOK ? List.of() : recipients;
        }

        @Override
        public int send(AuditInfo caller, String apiId, Notification notification) {
            sent = notification;
            sentApiId = apiId;
            return notification.channel() == Notification.Channel.WEBHOOK ? 1 : recipients.size();
        }
    }

    /** Records the requests and answers with what it was given. */
    public static class StubAnalyticsQueries implements AnalyticsQueries {

        public MeasuresResponse measures = new MeasuresResponse(List.of());
        public FacetsResponse facets = new FacetsResponse(List.of());
        public TimeSeriesResponse timeSeries = new TimeSeriesResponse(List.of());
        public FilterValuesPage filterValues = new FilterValuesPage(List.of(new FilterValue("Mobile app", "app-id")), null, 1L);
        public RuntimeException failure;

        public final List<AuditInfo> callers = new ArrayList<>();
        public final List<MeasuresRequest> measuresRequests = new ArrayList<>();
        public final List<FacetsRequest> facetsRequests = new ArrayList<>();
        public final List<TimeSeriesRequest> timeSeriesRequests = new ArrayList<>();
        public String filterValuesQuery;

        @Override
        public MeasuresResponse measures(AuditInfo caller, MeasuresRequest request) {
            callers.add(caller);
            measuresRequests.add(request);
            return answer(measures);
        }

        @Override
        public FacetsResponse facets(AuditInfo caller, FacetsRequest request) {
            callers.add(caller);
            facetsRequests.add(request);
            return answer(facets);
        }

        @Override
        public TimeSeriesResponse timeSeries(AuditInfo caller, TimeSeriesRequest request) {
            callers.add(caller);
            timeSeriesRequests.add(request);
            return answer(timeSeries);
        }

        @Override
        public FilterValuesPage filterValues(AuditInfo caller, String filterName, Instant from, Instant to, int perPage, String query) {
            callers.add(caller);
            filterValuesQuery = filterName + "|" + from + "|" + to + "|" + perPage + "|" + query;
            return answer(filterValues);
        }

        private <T> T answer(T response) {
            if (failure != null) {
                throw failure;
            }
            return response;
        }
    }

    public static class StubLogSearch implements LogSearch {

        public SearchLogsResponse response = new SearchLogsResponse(List.of(), new Pagination(1, 20, 0, 0, 0L));
        public AuditInfo caller;
        public SearchLogsRequest request;

        @Override
        public SearchLogsResponse search(AuditInfo caller, SearchLogsRequest request) {
            this.caller = caller;
            this.request = request;
            return response;
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
