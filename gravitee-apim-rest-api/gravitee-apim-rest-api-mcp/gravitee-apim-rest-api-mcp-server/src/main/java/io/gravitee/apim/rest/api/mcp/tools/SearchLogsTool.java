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
import io.gravitee.apim.rest.api.mcp.core.use_case.SearchLogsUseCase;
import io.gravitee.apim.rest.api.mcp.spi.McpCaller;
import java.util.List;

public class SearchLogsTool extends TriageTool<SearchLogsTool.Arguments> {

    private final SearchLogsUseCase useCase;

    public SearchLogsTool(SearchLogsUseCase useCase) {
        super("search_logs", Arguments.class);
        this.useCase = useCase;
    }

    public record Arguments(
        String environment,
        List<AnalyticsQuery.Filter> filters,
        Integer page,
        Integer perPage,
        String window,
        String until
    ) {}

    @Override
    Object execute(McpCaller caller, Arguments arguments) {
        return useCase.execute(
            new SearchLogsUseCase.Input(
                caller,
                arguments.environment(),
                arguments.filters(),
                arguments.page(),
                arguments.perPage(),
                arguments.window(),
                arguments.until()
            )
        );
    }
}
