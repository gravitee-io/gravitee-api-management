/*
 * Copyright (C) 2025 The Gravitee team (http://gravitee.io)
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
import { resolveElExpressions } from './el-resolver';

describe('resolveElExpressions', () => {
  const api = {
    name: 'Payments API',
    version: '1.2.0',
    description: 'Process payments',
    labels: ['finance', 'payments'],
    picture: 'https://example.com/pic.png',
    mcp: true,
    type: 'PROXY',
    status: 'STARTED',
    owner: 'Alex Morgan',
    metadata: {
      ownerTeam: 'Platform',
      regions: ['eu', 'us'],
    },
  };

  it('should replace simple api fields', () => {
    expect(resolveElExpressions('{#api.name} v{#api.version}', { api })).toBe('Payments API v1.2.0');
  });

  it('should replace metadata bracket access', () => {
    expect(resolveElExpressions("Owner: {#api.metadata['ownerTeam']}", { api })).toBe('Owner: Platform');
    expect(resolveElExpressions('Regions: {#api.metadata["regions"]}', { api })).toBe('Regions: eu, us');
  });

  it('should join label arrays', () => {
    expect(resolveElExpressions('{#api.labels}', { api })).toBe('finance, payments');
  });

  it('should fall back to empty string for missing paths', () => {
    expect(resolveElExpressions('{#api.missing} / {#api.metadata[\'gone\']}', { api })).toBe(' / ');
  });

  it('should leave non-EL text untouched', () => {
    expect(resolveElExpressions('Static {not-el} text', { api })).toBe('Static {not-el} text');
  });

  it('should stringify mcp true as true and false as empty', () => {
    expect(resolveElExpressions('{#api.mcp}', { api })).toBe('true');
    expect(resolveElExpressions('{#api.mcp}', { api: { ...api, mcp: false } })).toBe('');
  });
});
