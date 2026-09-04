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
import type { LocalizeFn } from '@angular/localize/init';

declare const $localize: LocalizeFn;

export const A2A_STREAM_METHOD = 'message/stream';

export interface A2ARequestBody {
  jsonrpc: '2.0';
  id: string;
  method: string;
  params: {
    message: {
      role: 'user';
      messageId: string;
      contextId?: string;
      parts: Array<{ kind: 'text'; text: string }>;
    };
  };
}

export function buildStreamRequest(text: string, contextId: string | undefined, messageId: string, requestId: string): A2ARequestBody {
  return {
    jsonrpc: '2.0',
    id: requestId,
    method: A2A_STREAM_METHOD,
    params: {
      message: {
        role: 'user',
        messageId,
        ...(contextId ? { contextId } : {}),
        parts: [{ kind: 'text', text }],
      },
    },
  };
}

export interface SseSplit {
  frames: string[];
  rest: string;
}

// An SSE frame ends with a blank line, which the spec allows to be LF or CRLF. Anything after
// the last blank line is incomplete and has to stay in the buffer until more bytes arrive.
const FRAME_SEPARATOR = /\r?\n\r?\n/;

export function splitSseFrames(buffer: string): SseSplit {
  const parts = buffer.split(FRAME_SEPARATOR);
  const rest = parts.pop() ?? '';
  return { frames: parts.filter(frame => frame.trim().length > 0), rest };
}

export type A2AEvent =
  | { kind: 'delta'; text: string; contextId?: string }
  | { kind: 'completed'; contextId?: string }
  | { kind: 'error'; message: string }
  | { kind: 'ignored' };

// States that end the task. 'input-required' ends the agent's turn too: it has said its piece
// and is waiting for the next question.
const COMPLETED_STATES = ['completed', 'input-required'];
const FAILED_STATES = ['failed', 'canceled', 'cancelled', 'rejected'];

interface JsonRpcPart {
  kind?: string;
  text?: string;
}

interface JsonRpcFrame {
  result?: {
    kind?: string;
    contextId?: string;
    artifact?: { parts?: JsonRpcPart[] };
    parts?: JsonRpcPart[];
    status?: { state?: string; message?: { parts?: JsonRpcPart[] } };
  };
  error?: { message?: string };
}

function textOfParts(parts: JsonRpcPart[] | undefined): string {
  return (parts ?? [])
    .filter(part => part.kind === 'text' && typeof part.text === 'string')
    .map(part => part.text)
    .join('');
}

// The spec splits a payload containing newlines across consecutive data: lines, to be rejoined with \n.
function payloadOfFrame(frame: string): string | null {
  const data = frame
    .split(/\r?\n/)
    .map(line => line.trim())
    .filter(line => line.startsWith('data:'))
    .map(line => line.slice('data:'.length).trim());
  return data.length ? data.join('\n') : null;
}

export function eventFromFrame(frame: string): A2AEvent {
  const payload = payloadOfFrame(frame);
  if (payload === null) {
    return { kind: 'ignored' };
  }

  let parsed: JsonRpcFrame;
  try {
    parsed = JSON.parse(payload) as JsonRpcFrame;
  } catch {
    return { kind: 'ignored' };
  }

  if (parsed.error) {
    return { kind: 'error', message: parsed.error.message ?? $localize`:@@agentChatAgentError:The agent returned an error.` };
  }

  const result = parsed.result;
  if (!result) {
    return { kind: 'ignored' };
  }

  if (result.kind === 'artifact-update' || result.kind === 'message') {
    const text = textOfParts(result.artifact?.parts ?? result.parts);
    return text ? { kind: 'delta', text, contextId: result.contextId } : { kind: 'ignored' };
  }

  if (result.kind === 'status-update') {
    const state = result.status?.state ?? '';
    if (COMPLETED_STATES.includes(state)) {
      return { kind: 'completed', contextId: result.contextId };
    }
    if (FAILED_STATES.includes(state)) {
      const reason = textOfParts(result.status?.message?.parts);
      return { kind: 'error', message: reason || $localize`:@@agentChatTaskFailed:The agent stopped before answering.` };
    }
  }

  return { kind: 'ignored' };
}
