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
import { buildWorkspaceCallSnippet } from './workspace-call-snippets';

describe('buildWorkspaceCallSnippet', () => {
  const context = {
    endpointUrl: 'https://gateway.example/ws',
    apiKey: 'test-key',
    modelName: 'gpt-4o-mini',
  };

  it('builds curl snippet', () => {
    const snippet = buildWorkspaceCallSnippet('curl', context);
    expect(snippet).toContain('curl --request POST');
    expect(snippet).toContain('https://gateway.example/ws/chat/completions');
    expect(snippet).toContain('Bearer test-key');
    expect(snippet).toContain('gpt-4o-mini');
  });

  it('builds LiteLLM snippet with api_base', () => {
    const snippet = buildWorkspaceCallSnippet('python-litellm', context);
    expect(snippet).toContain('from litellm import completion');
    expect(snippet).toContain('api_base="https://gateway.example/ws"');
  });
});
