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
import io.gravitee.apim.rest.api.mcp.core.model.FailedRequest;
import io.gravitee.apim.rest.api.mcp.core.model.StatusGroup;
import io.gravitee.apim.rest.api.mcp.core.model.TimeWindow;
import io.gravitee.apim.rest.api.mcp.core.port.ApiDirectory;
import io.gravitee.apim.rest.api.mcp.core.port.RequestLogs;
import io.gravitee.apim.rest.api.mcp.core.port.TriageAccess;
import io.gravitee.common.utils.TimeProvider;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Samples the requests that failed, with the reason the gateway recorded for each.
 */
@UseCase
public class SearchFailedRequestsUseCase {

    static final int DEFAULT_LIMIT = 20;
    static final int MAX_LIMIT = 50;
    static final String ANSWERED_BY_BACKEND = "NONE (answered by the backend)";

    private final TriageAccess access;
    private final ApiDirectory apis;
    private final RequestLogs logs;

    public SearchFailedRequestsUseCase(TriageAccess access, ApiDirectory apis, RequestLogs logs) {
        this.access = access;
        this.apis = apis;
        this.logs = logs;
    }

    public record Input(AuditInfo caller, String api, String statusGroup, String errorKey, String window, String until, Integer limit) {}

    /**
     * @param total failed requests in the window, of which {@code requests} is the newest part
     * @param errorKeys how many of the returned requests carry each error key
     */
    public record Output(
        String summary,
        ApiRef api,
        TimeWindow window,
        long total,
        Map<String, Long> errorKeys,
        List<FailedRequest> requests
    ) {}

    public Output execute(Input input) {
        var caller = input.caller();
        if (!access.canReadTraffic(caller)) {
            throw new TriageException("You are not allowed to read the logs of this environment.");
        }
        var window = TimeWindow.parse(input.window(), input.until(), TimeProvider.instantNow());
        var api = ApiResolver.resolve(apis, caller, input.api());
        var statusGroup = StatusGroup.parse(input.statusGroup());
        var errorKey = input.errorKey() == null || input.errorKey().isBlank() ? null : input.errorKey().trim();
        var limit = input.limit() == null ? DEFAULT_LIMIT : Math.clamp(input.limit(), 1, MAX_LIMIT);

        var page = logs.failures(caller, window, new RequestLogs.Query(api == null ? null : api.id(), statusGroup, errorKey), limit);

        var errorKeys = new TreeMap<String, Long>();
        for (var request : page.requests()) {
            errorKeys.merge(request.errorKey() == null ? ANSWERED_BY_BACKEND : request.errorKey(), 1L, Long::sum);
        }
        var summary = page.total() + " failed request(s) in the window, " + page.requests().size() + " returned, newest first.";
        return new Output(summary, api, window, page.total(), errorKeys, page.requests());
    }
}
