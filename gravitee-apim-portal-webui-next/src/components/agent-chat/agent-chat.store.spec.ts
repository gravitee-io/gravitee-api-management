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
import { AgentChatStore } from './agent-chat.store';

// jsdom ships no streams API, so the response body is faked down to the two members the store uses.
const sseBody = (chunks: string[]): ReadableStream<Uint8Array> => {
  const encoder = new TextEncoder();
  const encoded = chunks.map(chunk => encoder.encode(chunk));
  let next = 0;
  const reader = {
    read: () => Promise.resolve(next < encoded.length ? { done: false, value: encoded[next++] } : { done: true, value: undefined }),
  };
  return { getReader: () => reader } as unknown as ReadableStream<Uint8Array>;
};

const frame = (payload: unknown) => `data: ${JSON.stringify(payload)}\n\n`;
const delta = (text: string, contextId = 'ctx-1') =>
  frame({ result: { kind: 'artifact-update', contextId, artifact: { parts: [{ kind: 'text', text }] } } });
const completed = (contextId = 'ctx-1') => frame({ result: { kind: 'status-update', contextId, status: { state: 'completed' } } });

const TARGET = { endpoint: 'https://gw.test/agent', apiKey: 'key-1' };

describe('AgentChatStore', () => {
  let store: AgentChatStore;
  let fetchMock: jest.Mock;

  const respondWith = (chunks: string[]) => fetchMock.mockResolvedValue({ ok: true, status: 200, body: sseBody(chunks) } as Response);

  beforeEach(() => {
    fetchMock = jest.fn();
    globalThis.fetch = fetchMock as unknown as typeof fetch;
    store = new AgentChatStore();
  });

  it('records the question and streams the answer into one agent turn', async () => {
    respondWith([delta('Hel'), delta('lo'), completed()]);

    await store.send('hi', TARGET);

    expect(store.turns().map(turn => ({ role: turn.role, text: turn.text }))).toEqual([
      { role: 'user', text: 'hi' },
      { role: 'agent', text: 'Hello' },
    ]);
    expect(store.streaming()).toBe(false);
    expect(store.error()).toBeNull();
  });

  it('reassembles an answer whose frame is cut in half between chunks', async () => {
    const whole = delta('split me');
    respondWith([whole.slice(0, 20), whole.slice(20), completed()]);

    await store.send('hi', TARGET);

    expect(store.turns()[1].text).toBe('split me');
  });

  it('marks the agent turn complete once the stream ends', async () => {
    respondWith([delta('done'), completed()]);

    await store.send('hi', TARGET);

    expect(store.turns()[1].complete).toBe(true);
  });

  it('sends the api key and asks for an event stream', async () => {
    respondWith([completed()]);

    await store.send('hi', TARGET);

    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe('https://gw.test/agent');
    expect(init.headers['X-Gravitee-Api-Key']).toBe('key-1');
    expect(init.headers['Accept']).toBe('text/event-stream');
    expect(JSON.parse(init.body).method).toBe('message/stream');
  });

  it('carries the context id into the next message, which is what makes it a conversation', async () => {
    respondWith([delta('one', 'ctx-42'), completed('ctx-42')]);
    await store.send('first', TARGET);

    respondWith([delta('two', 'ctx-42'), completed('ctx-42')]);
    await store.send('second', TARGET);

    expect(JSON.parse(fetchMock.mock.calls[0][1].body).params.message.contextId).toBeUndefined();
    expect(JSON.parse(fetchMock.mock.calls[1][1].body).params.message.contextId).toBe('ctx-42');
  });

  it('reports a gateway failure and drops the empty agent turn', async () => {
    fetchMock.mockResolvedValue({ ok: false, status: 401, body: null } as Response);

    await store.send('hi', TARGET);

    expect(store.error()).toContain('401');
    expect(store.turns().map(turn => turn.role)).toEqual(['user']);
    expect(store.streaming()).toBe(false);
  });

  it('reports a network failure', async () => {
    fetchMock.mockRejectedValue(new Error('offline'));

    await store.send('hi', TARGET);

    expect(store.error()).not.toBeNull();
    expect(store.streaming()).toBe(false);
  });

  it('surfaces an error the agent itself reported', async () => {
    respondWith([frame({ error: { message: 'model unavailable' } })]);

    await store.send('hi', TARGET);

    expect(store.error()).toBe('model unavailable');
  });

  it('ignores an empty message', async () => {
    await store.send('   ', TARGET);

    expect(fetchMock).not.toHaveBeenCalled();
    expect(store.turns()).toEqual([]);
  });

  it('ignores a second question while the first answer is still streaming', async () => {
    let releaseFirst: () => void = () => undefined;
    const firstAnswerArrives = new Promise<void>(resolve => (releaseFirst = resolve));
    fetchMock.mockImplementationOnce(async () => {
      await firstAnswerArrives;
      return { ok: true, status: 200, body: sseBody([delta('one'), completed()]) } as Response;
    });

    const firstSend = store.send('first', TARGET);
    await store.send('second', TARGET);

    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(store.turns().map(turn => turn.text)).toEqual(['first', '']);

    releaseFirst();
    await firstSend;
  });

  it('clears the conversation when the agent changes', async () => {
    store.resetFor('agent-1');
    respondWith([delta('hello'), completed()]);
    await store.send('hi', TARGET);

    store.resetFor('agent-2');

    expect(store.turns()).toEqual([]);
    expect(store.error()).toBeNull();
  });

  it('keeps the conversation when told to reset for the same agent again', async () => {
    store.resetFor('agent-1');
    respondWith([delta('hello'), completed()]);
    await store.send('hi', TARGET);

    store.resetFor('agent-1');

    expect(store.turns()).toHaveLength(2);
  });
});
