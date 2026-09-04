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
import { buildStreamRequest, eventFromFrame, splitSseFrames } from './a2a-client';

describe('buildStreamRequest', () => {
  it('builds a jsonrpc message/stream request', () => {
    expect(buildStreamRequest('hello', undefined, 'msg-1', 'req-1')).toEqual({
      jsonrpc: '2.0',
      id: 'req-1',
      method: 'message/stream',
      params: { message: { role: 'user', messageId: 'msg-1', parts: [{ kind: 'text', text: 'hello' }] } },
    });
  });

  it('carries the context id when the conversation already has one', () => {
    const request = buildStreamRequest('hello', 'ctx-9', 'msg-2', 'req-2');

    expect(request.params.message.contextId).toBe('ctx-9');
  });
});

describe('splitSseFrames', () => {
  it('keeps an incomplete trailing frame in the buffer', () => {
    expect(splitSseFrames('data: a\n\ndata: b')).toEqual({ frames: ['data: a'], rest: 'data: b' });
  });

  it('returns no frames when nothing is complete yet', () => {
    expect(splitSseFrames('data: par')).toEqual({ frames: [], rest: 'data: par' });
  });

  it('drops blank frames', () => {
    expect(splitSseFrames('data: a\n\n\n\ndata: b\n\n').frames).toEqual(['data: a', 'data: b']);
  });
});

describe('eventFromFrame', () => {
  const frame = (payload: unknown) => `data: ${JSON.stringify(payload)}`;

  it('reads an artifact-update as a delta', () => {
    const event = eventFromFrame(
      frame({ result: { kind: 'artifact-update', contextId: 'ctx-1', artifact: { parts: [{ kind: 'text', text: 'par' }] } } }),
    );

    expect(event).toEqual({ kind: 'delta', text: 'par', contextId: 'ctx-1' });
  });

  it('reads a whole message as a single delta, for an agent that does not stream', () => {
    const event = eventFromFrame(frame({ result: { kind: 'message', contextId: 'ctx-2', parts: [{ kind: 'text', text: 'all of it' }] } }));

    expect(event).toEqual({ kind: 'delta', text: 'all of it', contextId: 'ctx-2' });
  });

  it('reads a completed status-update as completion', () => {
    const event = eventFromFrame(frame({ result: { kind: 'status-update', contextId: 'ctx-3', status: { state: 'completed' } } }));

    expect(event).toEqual({ kind: 'completed', contextId: 'ctx-3' });
  });

  it('reads a jsonrpc error', () => {
    expect(eventFromFrame(frame({ error: { message: 'boom' } }))).toEqual({ kind: 'error', message: 'boom' });
  });

  it('ignores a keep-alive comment', () => {
    expect(eventFromFrame(': keep-alive')).toEqual({ kind: 'ignored' });
  });

  it('ignores an unparseable payload', () => {
    expect(eventFromFrame('data: {not json')).toEqual({ kind: 'ignored' });
  });

  it('ignores an artifact-update carrying no text', () => {
    expect(eventFromFrame(frame({ result: { kind: 'artifact-update', artifact: { parts: [] } } }))).toEqual({ kind: 'ignored' });
  });
});
