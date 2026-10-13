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
package io.gravitee.apim.rest.api.mcp.tools;

import io.gravitee.apim.rest.api.mcp.core.model.AnalyticsQuery;
import io.gravitee.apim.rest.api.mcp.core.use_case.QueryAnalyticsUseCase;
import io.gravitee.apim.rest.api.mcp.spi.McpCaller;
import java.util.List;

public class QueryAnalyticsTool extends TriageTool<QueryAnalyticsTool.Arguments> {

    private final QueryAnalyticsUseCase useCase;

    public QueryAnalyticsTool(QueryAnalyticsUseCase useCase) {
        super("query_analytics", Arguments.class);
        this.useCase = useCase;
    }

    public record Arguments(
        String environment,
        String mode,
        List<AnalyticsQuery.Metric> metrics,
        List<AnalyticsQuery.Filter> filters,
        List<String> by,
        Object interval,
        Integer limit,
        List<AnalyticsQuery.Range> ranges,
        String window,
        String until,
        String compareWith
    ) {}

    @Override
    Object execute(McpCaller caller, Arguments arguments) {
        return useCase.execute(
            new QueryAnalyticsUseCase.Input(
                caller,
                arguments.environment(),
                arguments.mode(),
                arguments.metrics(),
                arguments.filters(),
                arguments.by(),
                arguments.interval(),
                arguments.limit(),
                arguments.ranges(),
                arguments.window(),
                arguments.until(),
                arguments.compareWith()
            )
        );
    }
}
