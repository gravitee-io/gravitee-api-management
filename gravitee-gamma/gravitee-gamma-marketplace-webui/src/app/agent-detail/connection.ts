/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import type { Api, McpToolDefinition } from '../../api/types';

export function gatewayEndpoint(api: Api): string | undefined {
    return api.entrypoints?.[0];
}

export function mcpServerUrl(api: Api): string | undefined {
    const mcpPath = api.mcp?.mcpPath;
    const entrypoint = api.entrypoints?.[0];
    if (!mcpPath || !entrypoint) {
        return undefined;
    }
    return entrypoint.replace(/\/$/, '') + mcpPath;
}

export function mcpTools(api: Api): Array<{ name: string; description: string }> {
    return (api.mcp?.tools ?? []).flatMap(tool => {
        const definition: McpToolDefinition | undefined = tool.toolDefinition;
        if (!definition?.name) {
            return [];
        }
        return [{ name: definition.name, description: definition.description ?? '' }];
    });
}
