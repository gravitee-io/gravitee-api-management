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
import { isMcpAgent, protocolLabel } from './protocol';

describe('protocolLabel', () => {
    it('should map known api types to short protocol names', () => {
        expect(protocolLabel('A2A_PROXY')).toBe('A2A');
        expect(protocolLabel('MCP_PROXY')).toBe('MCP');
        expect(protocolLabel('LLM_PROXY')).toBe('LLM');
        expect(protocolLabel('PROXY')).toBe('REST');
    });

    it('should fall back when the type is missing', () => {
        expect(protocolLabel(undefined)).toBe('API');
    });
});

describe('isMcpAgent', () => {
    it('should treat MCP proxies as MCP agents', () => {
        expect(isMcpAgent({ type: 'MCP_PROXY' })).toBe(true);
    });

    it('should treat agents with mcp metadata as MCP agents', () => {
        expect(isMcpAgent({ type: 'PROXY', mcp: { mcpPath: '/mcp' } })).toBe(true);
    });

    it('should not treat A2A-only agents as MCP agents', () => {
        expect(isMcpAgent({ type: 'A2A_PROXY' })).toBe(false);
    });
});
