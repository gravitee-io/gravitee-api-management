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
import { TestBed } from '@angular/core/testing';

import { AgentChatStore, DEFAULT_API_KEY_HEADER } from './agent-chat.store';
import { completed, delta, frame, sseBody } from './testing/sse-body';
import { ConfigService } from '../../services/config.service';

const TARGET = { endpoint: 'https://gw.test/agent', apiKey: 'key-1' };

describe('AgentChatStore', () => {
  const realFetch = globalThis.fetch;
  let store: AgentChatStore;
  let fetchMock: jest.Mock;
  let configuration: { portal?: { apikeyHeader?: string } };

  const respondWith = (chunks: string[]) => fetchMock.mockResolvedValue({ ok: true, status: 200, body: sseBody(chunks) } as Response);

  const init = () => {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [
        AgentChatStore,
        {
          provide: ConfigService,
          useValue: {
            get configuration() {
              return configuration;
            },
          },
        },
      ],
    });
    store = TestBed.inject(AgentChatStore);
  };

  beforeEach(() => {
    configuration = {};
    fetchMock = jest.fn();
    globalThis.fetch = fetchMock as unknown as typeof fetch;
    init();
  });

  afterEach(() => {
    globalThis.fetch = realFetch;
  });

  it('records the question and streams the answer into one agent turn', async () => {
    respondWith([delta('Hel'), delta('lo'), completed()]);

    await store.send('hi', TARGET);

    expect(store.turns().map(turn => ({ role: turn.role, text: turn.text }))).toEqual([
      { role: 'user', text: 'hi' },
      { role: 'agent', text: 'Hello' },
    ]);
    expect(store.isStreaming()).toBe(false);
    expect(store.error()).toBeNull();
  });

  it('reassembles an answer whose frame is cut in half between chunks', async () => {
    const whole = delta('split me');
    respondWith([whole.slice(0, 20), whole.slice(20), completed()]);

    await store.send('hi', TARGET);

    expect(store.turns()[1].text).toBe('split me');
  });

  it('reads a last frame that the gateway never terminated with a blank line', async () => {
    respondWith([delta('all of it').trimEnd()]);

    await store.send('hi', TARGET);

    expect(store.turns()[1].text).toBe('all of it');
  });

  it('marks the agent turn complete once the stream ends', async () => {
    respondWith([delta('done'), completed()]);

    await store.send('hi', TARGET);

    expect(store.turns()[1].isComplete).toBe(true);
  });

  it('sends the api key and asks for an event stream', async () => {
    respondWith([completed()]);

    await store.send('hi', TARGET);

    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe('https://gw.test/agent');
    expect(init.headers[DEFAULT_API_KEY_HEADER]).toBe('key-1');
    expect(init.headers['Accept']).toBe('text/event-stream');
    expect(JSON.parse(init.body).method).toBe('message/stream');
  });

  it('uses the api key header the portal is configured with', async () => {
    configuration = { portal: { apikeyHeader: 'X-Custom-Key' } };
    init();
    respondWith([completed()]);

    await store.send('hi', TARGET);

    expect(fetchMock.mock.calls[0][1].headers['X-Custom-Key']).toBe('key-1');
    expect(fetchMock.mock.calls[0][1].headers[DEFAULT_API_KEY_HEADER]).toBeUndefined();
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
    expect(store.isStreaming()).toBe(false);
  });

  it('reports a network failure in the portal language, not the browser one', async () => {
    fetchMock.mockRejectedValue(new TypeError('Failed to fetch'));

    await store.send('hi', TARGET);

    expect(store.error()).not.toBeNull();
    expect(store.error()).not.toContain('Failed to fetch');
    expect(store.isStreaming()).toBe(false);
  });

  it('surfaces an error the agent itself reported and leaves no empty bubble behind', async () => {
    respondWith([frame({ error: { message: 'model unavailable' } })]);

    await store.send('hi', TARGET);

    expect(store.error()).toBe('model unavailable');
    expect(store.turns().map(turn => turn.role)).toEqual(['user']);
  });

  it('keeps a partial answer as a finished turn when the agent fails midway', async () => {
    respondWith([delta('as far as I got'), frame({ result: { kind: 'status-update', status: { state: 'failed' } } })]);

    await store.send('hi', TARGET);

    expect(store.turns()[1]).toEqual(expect.objectContaining({ text: 'as far as I got', isComplete: true }));
    expect(store.error()).not.toBeNull();
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

  describe('a stream abandoned by switching agent', () => {
    let releaseFirst: (chunks: string[]) => void;
    let firstSend: Promise<void>;
    let abortSignal: AbortSignal;

    beforeEach(() => {
      store.resetFor('agent-A');
      const arrival = new Promise<string[]>(resolve => (releaseFirst = resolve));
      fetchMock.mockImplementationOnce(async (_url: string, init: RequestInit) => {
        abortSignal = init.signal as AbortSignal;
        return { ok: true, status: 200, body: sseBody(await arrival) } as Response;
      });
      firstSend = store.send('question for A', TARGET);
    });

    it('aborts the request and unblocks the composer', async () => {
      store.resetFor('agent-B');

      expect(abortSignal.aborted).toBe(true);
      expect(store.isStreaming()).toBe(false);

      releaseFirst([completed('ctx-A')]);
      await firstSend;
    });

    it('does not leak the old agent context id into the next question', async () => {
      store.resetFor('agent-B');
      releaseFirst([delta('late answer', 'ctx-A'), completed('ctx-A')]);
      await firstSend;

      respondWith([delta('fresh', 'ctx-B'), completed('ctx-B')]);
      await store.send('question for B', TARGET);

      expect(JSON.parse(fetchMock.mock.calls[1][1].body).params.message.contextId).toBeUndefined();
    });

    it('does not write the old agent turns or errors into the new conversation', async () => {
      store.resetFor('agent-B');
      releaseFirst([delta('late answer', 'ctx-A'), frame({ error: { message: 'A failed' } })]);
      await firstSend;

      expect(store.turns()).toEqual([]);
      expect(store.error()).toBeNull();
    });
  });
});
