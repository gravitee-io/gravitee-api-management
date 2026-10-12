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

import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.rest.api.mcp.core.use_case.GetHealthOverviewUseCase;

public class GetHealthOverviewTool extends TriageTool<GetHealthOverviewTool.Arguments> {

    private final GetHealthOverviewUseCase useCase;

    public GetHealthOverviewTool(GetHealthOverviewUseCase useCase) {
        super("get_health_overview", Arguments.class);
        this.useCase = useCase;
    }

    public record Arguments(String window, String until) {}

    @Override
    Object execute(AuditInfo caller, Arguments arguments) {
        return useCase.execute(new GetHealthOverviewUseCase.Input(caller, arguments.window(), arguments.until()));
    }
}
