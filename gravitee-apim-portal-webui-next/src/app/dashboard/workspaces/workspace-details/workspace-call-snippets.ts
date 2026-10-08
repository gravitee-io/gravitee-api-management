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

export type WorkspaceCallSnippetFormatId =
  | 'curl'
  | 'python-openai'
  | 'nodejs-openai'
  | 'python-litellm'
  | 'python-anthropic'
  | 'python-gemini';

export interface WorkspaceCallSnippetFormat {
  id: WorkspaceCallSnippetFormatId;
  label: string;
}

export const WORKSPACE_CALL_SNIPPET_FORMATS: WorkspaceCallSnippetFormat[] = [
  { id: 'curl', label: 'cURL (OpenAI-compatible)' },
  { id: 'python-openai', label: 'Python — OpenAI SDK' },
  { id: 'nodejs-openai', label: 'Node.js — OpenAI SDK' },
  { id: 'python-litellm', label: 'Python — LiteLLM' },
  { id: 'python-anthropic', label: 'Python — Anthropic SDK' },
  { id: 'python-gemini', label: 'Python — Google Gen AI SDK' },
];

export interface WorkspaceCallSnippetContext {
  endpointUrl: string;
  apiKey: string;
  modelName: string;
}

export function buildWorkspaceCallSnippet(format: WorkspaceCallSnippetFormatId, context: WorkspaceCallSnippetContext): string {
  const chatUrl = joinChatCompletionsUrl(context.endpointUrl);
  const baseUrl = normalizeEndpointBase(context.endpointUrl);
  const apiKey = context.apiKey.trim() || 'YOUR_API_KEY';
  const model = context.modelName;

  switch (format) {
    case 'curl':
      return buildCurlSnippet(chatUrl, apiKey, model);
    case 'python-openai':
      return buildPythonOpenAiSnippet(baseUrl, apiKey, model);
    case 'nodejs-openai':
      return buildNodeOpenAiSnippet(baseUrl, apiKey, model);
    case 'python-litellm':
      return buildPythonLiteLlmSnippet(baseUrl, apiKey, model);
    case 'python-anthropic':
      return buildPythonAnthropicSnippet(baseUrl, apiKey, model);
    case 'python-gemini':
      return buildPythonGeminiSnippet(baseUrl, apiKey, model);
  }
}

function joinChatCompletionsUrl(endpointUrl: string): string {
  const base = endpointUrl.replace(/\/+$/, '');
  return `${base}/chat/completions`;
}

function normalizeEndpointBase(endpointUrl: string): string {
  return endpointUrl.replace(/\/+$/, '');
}

function buildCurlSnippet(chatUrl: string, apiKey: string, model: string): string {
  const body = JSON.stringify({
    model,
    messages: [{ role: 'user', content: 'Hello' }],
  });
  return [
    'curl --request POST \\',
    `  --url '${escapeSingleQuotes(chatUrl)}' \\`,
    `  --header 'Authorization: Bearer ${escapeSingleQuotes(apiKey)}' \\`,
    `  --header 'Content-Type: application/json' \\`,
    `  --data '${escapeSingleQuotes(body)}'`,
  ].join('\n');
}

function buildPythonOpenAiSnippet(baseUrl: string, apiKey: string, model: string): string {
  return [
    'from openai import OpenAI',
    '',
    'client = OpenAI(',
    `    base_url="${escapeDoubleQuotes(baseUrl)}",`,
    `    api_key="${escapeDoubleQuotes(apiKey)}",`,
    ')',
    '',
    'response = client.chat.completions.create(',
    `    model="${escapeDoubleQuotes(model)}",`,
    '    messages=[{"role": "user", "content": "Hello"}],',
    ')',
    '',
    'print(response.choices[0].message.content)',
  ].join('\n');
}

function buildNodeOpenAiSnippet(baseUrl: string, apiKey: string, model: string): string {
  return [
    "import OpenAI from 'openai';",
    '',
    'const client = new OpenAI({',
    `  baseURL: '${escapeSingleQuotes(baseUrl)}',`,
    `  apiKey: '${escapeSingleQuotes(apiKey)}',`,
    '});',
    '',
    'const response = await client.chat.completions.create({',
    `  model: '${escapeSingleQuotes(model)}',`,
    "  messages: [{ role: 'user', content: 'Hello' }],",
    '});',
    '',
    'console.log(response.choices[0]?.message?.content);',
  ].join('\n');
}

function buildPythonLiteLlmSnippet(baseUrl: string, apiKey: string, model: string): string {
  return [
    'from litellm import completion',
    '',
    'response = completion(',
    `    model="${escapeDoubleQuotes(model)}",`,
    `    api_base="${escapeDoubleQuotes(baseUrl)}",`,
    `    api_key="${escapeDoubleQuotes(apiKey)}",`,
    '    messages=[{"role": "user", "content": "Hello"}],',
    ')',
    '',
    'print(response.choices[0].message.content)',
  ].join('\n');
}

function buildPythonAnthropicSnippet(baseUrl: string, apiKey: string, model: string): string {
  return [
    'import anthropic',
    '',
    'client = anthropic.Anthropic(',
    `    base_url="${escapeDoubleQuotes(baseUrl)}",`,
    `    api_key="${escapeDoubleQuotes(apiKey)}",`,
    ')',
    '',
    'message = client.messages.create(',
    `    model="${escapeDoubleQuotes(model)}",`,
    '    max_tokens=1024,',
    '    messages=[{"role": "user", "content": "Hello"}],',
    ')',
    '',
    'print(message.content[0].text)',
  ].join('\n');
}

function buildPythonGeminiSnippet(baseUrl: string, apiKey: string, model: string): string {
  return [
    'from google import genai',
    '',
    'client = genai.Client(',
    `    api_key="${escapeDoubleQuotes(apiKey)}",`,
    `    http_options={"base_url": "${escapeDoubleQuotes(baseUrl)}"},`,
    ')',
    '',
    'response = client.models.generate_content(',
    `    model="${escapeDoubleQuotes(model)}",`,
    '    contents="Hello",',
    ')',
    '',
    'print(response.text)',
  ].join('\n');
}

function escapeSingleQuotes(value: string): string {
  return value.replace(/'/g, `'\\''`);
}

function escapeDoubleQuotes(value: string): string {
  return value.replace(/\\/g, '\\\\').replace(/"/g, '\\"');
}
