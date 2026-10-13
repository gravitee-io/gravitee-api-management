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

import io.gravitee.apim.rest.api.mcp.core.use_case.ListChangesUseCase;
import io.gravitee.apim.rest.api.mcp.spi.McpCaller;

public class ListChangesTool extends TriageTool<ListChangesTool.Arguments> {

    private final ListChangesUseCase useCase;

    public ListChangesTool(ListChangesUseCase useCase) {
        super("list_changes", Arguments.class);
        this.useCase = useCase;
    }

    public record Arguments(String environment, String api, String window, String until) {}

    @Override
    Object execute(McpCaller caller, Arguments arguments) {
        return useCase.execute(
            new ListChangesUseCase.Input(caller, arguments.environment(), arguments.api(), arguments.window(), arguments.until())
        );
    }
}
