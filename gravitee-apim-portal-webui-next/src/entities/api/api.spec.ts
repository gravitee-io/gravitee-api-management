/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { ApiType, isAgentApi } from './api';
import { fakeApi } from './api.fixtures';

describe('isAgentApi', () => {
  it.each([
    ['A2A_PROXY' as const, true],
    ['MCP_PROXY' as const, true],
    ['LLM_PROXY' as const, true],
    ['PROXY' as const, false],
    ['NATIVE' as const, false],
    [undefined, false],
  ])('reads type %s as agent=%s', (type: ApiType | undefined, expected) => {
    expect(isAgentApi({ type })).toBe(expected);
  });

  it('does not treat a PROXY with an MCP server as an agent', () => {
    expect(isAgentApi(fakeApi({ type: 'PROXY', mcp: { mcpPath: '/mcp' } }))).toBe(false);
  });

  it('treats a missing api as not an agent', () => {
    expect(isAgentApi(undefined)).toBe(false);
  });
});
