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

/** jsdom ships no streams API, so a response body is faked down to the two members the store reads. */
export const sseBody = (chunks: string[]): ReadableStream<Uint8Array> => {
  const encoder = new TextEncoder();
  const encoded = chunks.map(chunk => encoder.encode(chunk));
  let next = 0;
  const reader = {
    read: () => Promise.resolve(next < encoded.length ? { done: false, value: encoded[next++] } : { done: true, value: undefined }),
  };
  return { getReader: () => reader } as unknown as ReadableStream<Uint8Array>;
};

export const frame = (payload: unknown) => `data: ${JSON.stringify(payload)}\n\n`;

export const delta = (text: string, contextId = 'ctx-1') =>
  frame({ result: { kind: 'artifact-update', contextId, artifact: { parts: [{ kind: 'text', text }] } } });

export const completed = (contextId = 'ctx-1') => frame({ result: { kind: 'status-update', contextId, status: { state: 'completed' } } });

export const respondingGateway = (...chunks: string[]) =>
  jest.fn().mockResolvedValue({ ok: true, status: 200, body: sseBody(chunks) } as Response);
