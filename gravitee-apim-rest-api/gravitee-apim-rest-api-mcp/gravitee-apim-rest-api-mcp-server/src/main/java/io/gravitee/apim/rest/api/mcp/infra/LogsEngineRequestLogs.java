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
package io.gravitee.apim.rest.api.mcp.infra;

import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.logs_engine.model.ApiLog;
import io.gravitee.apim.core.logs_engine.model.ArrayFilter;
import io.gravitee.apim.core.logs_engine.model.Filter;
import io.gravitee.apim.core.logs_engine.model.FilterName;
import io.gravitee.apim.core.logs_engine.model.NumericFilter;
import io.gravitee.apim.core.logs_engine.model.Operator;
import io.gravitee.apim.core.logs_engine.model.SearchLogsRequest;
import io.gravitee.apim.core.logs_engine.model.StringFilter;
import io.gravitee.apim.core.logs_engine.model.TimeRange;
import io.gravitee.apim.core.logs_engine.use_case.SearchEnvironmentLogsUseCase;
import io.gravitee.apim.rest.api.mcp.core.model.ApiRef;
import io.gravitee.apim.rest.api.mcp.core.model.FailedRequest;
import io.gravitee.apim.rest.api.mcp.core.model.TimeWindow;
import io.gravitee.apim.rest.api.mcp.core.port.RequestLogs;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads failed requests from the logs engine, which restricts every search to the APIs the caller can see.
 */
public class LogsEngineRequestLogs implements RequestLogs {

    private static final int FIRST_ERROR_STATUS = 400;

    private final SearchEnvironmentLogsUseCase searchLogs;

    public LogsEngineRequestLogs(SearchEnvironmentLogsUseCase searchLogs) {
        this.searchLogs = searchLogs;
    }

    @Override
    public Page failures(AuditInfo caller, TimeWindow window, Query query, int limit) {
        var filters = new ArrayList<Filter>();
        if (query.statusGroup() == null) {
            filters.add(new Filter(new NumericFilter(FilterName.HTTP_STATUS, Operator.GTE, FIRST_ERROR_STATUS)));
        } else {
            filters.add(new Filter(new ArrayFilter(FilterName.HTTP_STATUS_CODE_GROUP, Operator.IN, List.of(query.statusGroup().label()))));
        }
        if (query.apiId() != null) {
            filters.add(new Filter(new StringFilter(FilterName.API, Operator.EQ, query.apiId())));
        }
        if (query.errorKey() != null) {
            filters.add(new Filter(new StringFilter(FilterName.ERROR_KEY, Operator.EQ, query.errorKey())));
        }
        var range = new TimeRange(window.from().atOffset(ZoneOffset.UTC), window.to().atOffset(ZoneOffset.UTC));

        var request = new SearchEnvironmentLogsUseCase.Input(caller, new SearchLogsRequest(range, filters, 1, limit));
        var response = ApimContext.run(caller, () -> searchLogs.execute(request)).response();

        var total = response.pagination() == null || response.pagination().totalCount() == null ? 0 : response.pagination().totalCount();
        return new Page(total, response.data().stream().map(LogsEngineRequestLogs::toFailedRequest).toList());
    }

    private static FailedRequest toFailedRequest(ApiLog log) {
        return new FailedRequest(
            log.timestamp() == null ? null : log.timestamp().toInstant(),
            new ApiRef(log.apiId(), log.apiName()),
            log.method() == null ? null : log.method().name(),
            log.uri(),
            log.status(),
            log.errorKey(),
            log.errorComponentName() == null ? log.errorComponentType() : log.errorComponentName(),
            log.message(),
            log.application() == null ? null : log.application().name(),
            log.plan() == null ? null : log.plan().name(),
            log.gateway(),
            log.gatewayResponseTime(),
            log.requestId()
        );
    }
}
