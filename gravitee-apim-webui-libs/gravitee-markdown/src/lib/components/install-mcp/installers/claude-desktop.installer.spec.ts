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
import { claudeDesktopInstaller } from './claude-desktop.installer';

describe('claudeDesktopInstaller', () => {
  const spec = {
    name: 'weather',
    transport: 'http' as const,
    url: 'https://api.example.com/mcp',
  };

  it('should not build a deep link', () => {
    expect(claudeDesktopInstaller.buildDeepLink).toBeUndefined();
    expect(claudeDesktopInstaller.mode).toBe('snippet-only');
  });

  it('should build a Claude Desktop snippet', () => {
    expect(claudeDesktopInstaller.buildSnippet(spec)).toContain('"mcpServers"');
    expect(claudeDesktopInstaller.buildSnippet(spec)).toContain('"weather"');
    expect(claudeDesktopInstaller.snippetFileName).toBe('claude_desktop_config.json');
  });
});
