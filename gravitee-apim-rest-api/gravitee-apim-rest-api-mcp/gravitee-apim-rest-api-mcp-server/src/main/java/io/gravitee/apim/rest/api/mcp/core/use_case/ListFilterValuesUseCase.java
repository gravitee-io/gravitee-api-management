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
import io.gravitee.apim.core.analytics_engine.model.FilterValue;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.rest.api.mcp.core.EnvironmentResolver;
import io.gravitee.apim.rest.api.mcp.core.Names;
import io.gravitee.apim.rest.api.mcp.core.TriageException;
import io.gravitee.apim.rest.api.mcp.core.model.EnvRef;
import io.gravitee.apim.rest.api.mcp.core.model.TimeWindow;
import io.gravitee.apim.rest.api.mcp.core.port.AnalyticsQueries;
import io.gravitee.apim.rest.api.mcp.core.port.ApiDirectory;
import io.gravitee.apim.rest.api.mcp.core.port.EnvironmentDirectory;
import io.gravitee.apim.rest.api.mcp.spi.McpCaller;
import io.gravitee.common.utils.TimeProvider;
import java.util.List;

/**
 * The values a filter took in the window: the ids with their names, so that a query can name an application, a plan
 * or a gateway.
 */
@UseCase
public class ListFilterValuesUseCase {

    static final int DEFAULT_PER_PAGE = 20;

    private final EnvironmentDirectory environments;
    private final ApiDirectory apis;
    private final AnalyticsQueries analytics;

    public ListFilterValuesUseCase(EnvironmentDirectory environments, ApiDirectory apis, AnalyticsQueries analytics) {
        this.environments = environments;
        this.apis = apis;
        this.analytics = analytics;
    }

    public record Input(McpCaller caller, String environment, String filter, String query, String window, String until, Integer perPage) {}

    public record Output(String summary, EnvRef environment, String filter, TimeWindow window, long total, List<Value> values) {}

    /** @param id what the filter takes; null when the value is its own id */
    public record Value(String value, String id) {}

    public Output execute(Input input) {
        var filter = Names.filter(input.filter());
        var window = TimeWindow.parse(input.window(), input.until(), TimeProvider.instantNow());
        var scope = EnvironmentResolver.resolve(environments, apis, input.caller(), input.environment(), null);
        var perPage = input.perPage() == null ? DEFAULT_PER_PAGE : Math.max(1, input.perPage());
        try {
            var page = analytics.filterValues(scope.auditInfo(), filter.name(), window.from(), window.to(), perPage, input.query());
            var values = page.data().stream().map(ListFilterValuesUseCase::toValue).toList();
            long total = page.totalFilteredCount() == null ? values.size() : page.totalFilteredCount();
            return new Output(
                total + " value(s) of " + filter + " in the window, " + values.size() + " returned.",
                scope.environment(),
                filter.name(),
                window,
                total,
                values
            );
        } catch (ValidationDomainException e) {
            throw new TriageException(e.getMessage());
        }
    }

    private static Value toValue(FilterValue value) {
        return new Value(value.value(), value.id());
    }
}
