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
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.logs_engine.model.ApiLog;
import io.gravitee.apim.core.logs_engine.model.ArrayFilter;
import io.gravitee.apim.core.logs_engine.model.Filter;
import io.gravitee.apim.core.logs_engine.model.FilterName;
import io.gravitee.apim.core.logs_engine.model.NumericFilter;
import io.gravitee.apim.core.logs_engine.model.Operator;
import io.gravitee.apim.core.logs_engine.model.SearchLogsRequest;
import io.gravitee.apim.core.logs_engine.model.StringFilter;
import io.gravitee.apim.core.logs_engine.model.TimeRange;
import io.gravitee.apim.rest.api.mcp.core.ApiResolver;
import io.gravitee.apim.rest.api.mcp.core.EnvironmentResolver;
import io.gravitee.apim.rest.api.mcp.core.Names;
import io.gravitee.apim.rest.api.mcp.core.TriageException;
import io.gravitee.apim.rest.api.mcp.core.model.AnalyticsQuery;
import io.gravitee.apim.rest.api.mcp.core.model.ApiRef;
import io.gravitee.apim.rest.api.mcp.core.model.EnvRef;
import io.gravitee.apim.rest.api.mcp.core.model.TimeWindow;
import io.gravitee.apim.rest.api.mcp.core.port.ApiDirectory;
import io.gravitee.apim.rest.api.mcp.core.port.EnvironmentDirectory;
import io.gravitee.apim.rest.api.mcp.core.port.LogSearch;
import io.gravitee.apim.rest.api.mcp.spi.McpCaller;
import io.gravitee.common.utils.TimeProvider;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * Logged requests of one environment, in the logs engine's filter vocabulary, newest first.
 */
@UseCase
public class SearchLogsUseCase {

    static final int DEFAULT_PER_PAGE = 20;

    private final EnvironmentDirectory environments;
    private final ApiDirectory apis;
    private final LogSearch logs;

    public SearchLogsUseCase(EnvironmentDirectory environments, ApiDirectory apis, LogSearch logs) {
        this.environments = environments;
        this.apis = apis;
        this.logs = logs;
    }

    public record Input(
        McpCaller caller,
        String environment,
        List<AnalyticsQuery.Filter> filters,
        Integer page,
        Integer perPage,
        String window,
        String until
    ) {}

    public record Output(String summary, EnvRef environment, TimeWindow window, int page, int perPage, long total, List<Entry> logs) {}

    /**
     * @param errorKey the reason the gateway recorded when it produced the response itself; null when the backend answered
     */
    public record Entry(
        Instant at,
        ApiRef api,
        String apiType,
        String method,
        String uri,
        Integer status,
        Named application,
        Named plan,
        String gateway,
        String requestId,
        String transactionId,
        Integer gatewayResponseTimeMs,
        String errorKey,
        String errorComponent,
        String message,
        String mcpMethod
    ) {}

    public record Named(String id, String name) {}

    public Output execute(Input input) {
        var window = TimeWindow.parse(input.window(), input.until(), TimeProvider.instantNow());
        var scope = EnvironmentResolver.resolve(environments, apis, input.caller(), input.environment(), null);
        int page = input.page() == null ? 1 : Math.max(1, input.page());
        int perPage = input.perPage() == null ? DEFAULT_PER_PAGE : Math.max(1, input.perPage());
        var range = new TimeRange(window.from().atOffset(ZoneOffset.UTC), window.to().atOffset(ZoneOffset.UTC));
        var request = new SearchLogsRequest(range, filters(scope.auditInfo(), input.filters()), page, perPage);
        try {
            var response = logs.search(scope.auditInfo(), request);
            var entries = response.data().stream().map(SearchLogsUseCase::toEntry).toList();
            long total = response.pagination() == null || response.pagination().totalCount() == null
                ? entries.size()
                : response.pagination().totalCount();
            return new Output(
                total + " request(s) in the window, " + entries.size() + " returned, newest first.",
                scope.environment(),
                window,
                page,
                perPage,
                total,
                entries
            );
        } catch (ValidationDomainException e) {
            throw new TriageException(e.getMessage());
        }
    }

    /** API names are accepted where the engine wants ids. */
    private List<Filter> filters(AuditInfo caller, List<AnalyticsQuery.Filter> filters) {
        var result = new ArrayList<Filter>();
        if (filters == null) {
            return result;
        }
        for (var filter : filters) {
            var name = Names.logFilter(filter.name());
            var operator = Names.logOperator(filter.operator());
            if (filter.value() == null) {
                throw new TriageException("Filter " + filter.name() + " needs a value.");
            }
            if (filter.value() instanceof List<?> list) {
                var values = list
                    .stream()
                    .map(String::valueOf)
                    .map(v -> name == FilterName.API ? apiId(caller, v) : v)
                    .toList();
                result.add(new Filter(new ArrayFilter(name, operator == Operator.EQ ? Operator.IN : operator, values)));
            } else if (filter.value() instanceof Number number && (operator == Operator.GTE || operator == Operator.LTE)) {
                result.add(new Filter(new NumericFilter(name, operator, number.intValue())));
            } else {
                var value = String.valueOf(filter.value());
                result.add(new Filter(new StringFilter(name, operator, name == FilterName.API ? apiId(caller, value) : value)));
            }
        }
        return result;
    }

    private String apiId(AuditInfo caller, String idOrName) {
        return ApiResolver.resolve(apis, caller, idOrName).id();
    }

    private static Entry toEntry(ApiLog log) {
        return new Entry(
            log.timestamp() == null ? null : log.timestamp().toInstant(),
            new ApiRef(log.apiId(), log.apiName()),
            log.apiType() == null ? null : log.apiType().name(),
            log.method() == null ? null : log.method().name(),
            log.uri(),
            log.status(),
            log.application() == null ? null : new Named(log.application().id(), log.application().name()),
            log.plan() == null ? null : new Named(log.plan().id(), log.plan().name()),
            log.gateway(),
            log.requestId(),
            log.transactionId(),
            log.gatewayResponseTime(),
            log.errorKey(),
            log.errorComponentName() == null ? log.errorComponentType() : log.errorComponentName(),
            log.message(),
            log.mcpMethod()
        );
    }
}
