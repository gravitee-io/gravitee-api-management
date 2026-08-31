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
import { gatewayEndpoint, mcpServerUrl, mcpTools } from './connection';
import { buildApi } from '../../testing/factories';

describe('gatewayEndpoint', () => {
    it('should return the first entrypoint', () => {
        expect(gatewayEndpoint(buildApi())).toBe('https://gw.example/a2a/it-helpdesk');
    });
});

describe('mcpServerUrl', () => {
    it('should join the entrypoint and MCP path', () => {
        expect(
            mcpServerUrl(
                buildApi({
                    type: 'MCP_PROXY',
                    entrypoints: ['https://gw.example/mcp/slack/'],
                    mcp: { mcpPath: '/mcp' },
                }),
            ),
        ).toBe('https://gw.example/mcp/slack/mcp');
    });

    it('should be undefined when the agent has no MCP path', () => {
        expect(mcpServerUrl(buildApi())).toBeUndefined();
    });
});

describe('mcpTools', () => {
    it('should skip tools without a name', () => {
        expect(
            mcpTools(
                buildApi({
                    mcp: {
                        tools: [
                            { toolDefinition: { name: 'create_ticket', description: 'Open a ticket' } },
                            { toolDefinition: { description: 'unnamed' } },
                        ],
                    },
                }),
            ),
        ).toEqual([{ name: 'create_ticket', description: 'Open a ticket' }]);
    });
});
