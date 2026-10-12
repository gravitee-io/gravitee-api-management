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
package io.gravitee.apim.rest.api.mcp.spi;

/**
 * A tool exposed to MCP clients. Implementations know nothing of the protocol library that serves them.
 *
 * @param <I> the arguments of the tool, bound from the call's JSON arguments
 */
public interface McpTool<I> {
    ToolDescriptor descriptor();

    Class<I> inputType();

    /**
     * @return the structured result, serialized as the tool's structured content
     * @throws ToolException when the caller can act on the failure; its message is returned to the model
     */
    Object call(McpCaller caller, I input);
}
