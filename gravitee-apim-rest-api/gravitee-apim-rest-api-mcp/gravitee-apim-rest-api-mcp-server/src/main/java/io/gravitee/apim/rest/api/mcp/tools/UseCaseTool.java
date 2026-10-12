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

import io.gravitee.apim.rest.api.mcp.core.RejectedCallException;
import io.gravitee.apim.rest.api.mcp.spi.McpCaller;
import io.gravitee.apim.rest.api.mcp.spi.McpTool;
import io.gravitee.apim.rest.api.mcp.spi.ToolDescriptor;
import io.gravitee.apim.rest.api.mcp.spi.ToolException;

/**
 * A tool backed by one use case. Its contract is the JSON file named after it.
 */
abstract class UseCaseTool<I> implements McpTool<I> {

    private final ToolDescriptor descriptor;
    private final Class<I> inputType;

    UseCaseTool(String name, Class<I> inputType) {
        this.descriptor = ToolDescriptor.load(name);
        this.inputType = inputType;
    }

    @Override
    public ToolDescriptor descriptor() {
        return descriptor;
    }

    @Override
    public Class<I> inputType() {
        return inputType;
    }

    @Override
    public final Object call(McpCaller caller, I input) {
        try {
            return execute(caller, input);
        } catch (RejectedCallException e) {
            throw new ToolException(e.getMessage());
        }
    }

    abstract Object execute(McpCaller caller, I input);
}
