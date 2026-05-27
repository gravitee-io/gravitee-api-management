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
import { cursorInstaller } from './cursor.installer';

describe('cursorInstaller', () => {
  const spec = {
    name: 'weather',
    transport: 'http' as const,
    url: 'https://api.example.com/mcp',
  };

  it('should build a cursor deep link', () => {
    expect(cursorInstaller.buildDeepLink?.(spec)).toContain('cursor://anysphere.cursor-deeplink/mcp/install');
  });

  it('should build a cursor web fallback link', () => {
    expect(cursorInstaller.buildFallbackLink?.(spec)).toContain('https://cursor.com/en/install-mcp');
  });

  it('should build an mcp.json snippet', () => {
    expect(cursorInstaller.buildSnippet(spec)).toContain('"mcpServers"');
    expect(cursorInstaller.buildSnippet(spec)).toContain('"weather"');
  });
});
