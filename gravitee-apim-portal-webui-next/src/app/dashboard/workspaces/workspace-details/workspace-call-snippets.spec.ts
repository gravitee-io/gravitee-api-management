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
import { buildWorkspaceCallSnippet, WORKSPACE_CALL_SNIPPET_FORMATS } from './workspace-call-snippets';

describe('buildWorkspaceCallSnippet', () => {
  const context = {
    endpointUrl: 'https://gateway.example/ws/',
    apiKey: 'test-key',
    modelName: 'gpt-4o-mini',
  };

  it('builds a snippet for every client and trims a trailing slash', () => {
    expect(WORKSPACE_CALL_SNIPPET_FORMATS.map(format => format.id)).toEqual([
      'curl',
      'python-openai',
      'nodejs-openai',
      'python-litellm',
      'python-anthropic',
      'python-gemini',
    ]);

    const curl = buildWorkspaceCallSnippet('curl', context);
    expect(curl).toContain('curl --request POST');
    expect(curl).toContain('https://gateway.example/ws/chat/completions');
    expect(curl).toContain('Bearer test-key');
    expect(curl).toContain('gpt-4o-mini');

    expect(buildWorkspaceCallSnippet('python-openai', context)).toContain('base_url="https://gateway.example/ws"');
    expect(buildWorkspaceCallSnippet('nodejs-openai', context)).toContain("baseURL: 'https://gateway.example/ws'");
    expect(buildWorkspaceCallSnippet('python-litellm', context)).toContain('model="openai/gpt-4o-mini"');
    expect(buildWorkspaceCallSnippet('python-litellm', context)).toContain('api_base="https://gateway.example/ws"');
    expect(buildWorkspaceCallSnippet('python-anthropic', context)).toContain('import anthropic');
    expect(buildWorkspaceCallSnippet('python-gemini', context)).toContain('from google import genai');
  });

  it('escapes quotes and falls back when the key is blank', () => {
    const snippet = buildWorkspaceCallSnippet('curl', {
      endpointUrl: 'https://gateway.example/ws',
      apiKey: "o'brian",
      modelName: 'gpt-4o',
    });
    expect(snippet).toContain(`Bearer o'\\''brian`);

    const python = buildWorkspaceCallSnippet('python-openai', {
      endpointUrl: 'https://gateway.example/a"b\\c',
      apiKey: '   ',
      modelName: 'gpt"4',
    });
    expect(python).toContain('api_key="YOUR_API_KEY"');
    expect(python).toContain('base_url="https://gateway.example/a\\"b\\\\c"');
    expect(python).toContain('model="gpt\\"4"');
  });
});
