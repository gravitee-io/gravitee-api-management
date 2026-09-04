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

// An SSE frame ends with a blank line, so anything after the last blank line is incomplete
// and has to stay in the buffer until more bytes arrive.
export function splitSseFrames(buffer: string): SseSplit {
  const parts = buffer.split('\n\n');
  const rest = parts.pop() ?? '';
  return { frames: parts.filter(frame => frame.trim().length > 0), rest };
}

export type A2AEvent =
  | { kind: 'delta'; text: string; contextId?: string }
  | { kind: 'completed'; contextId?: string }
  | { kind: 'error'; message: string }
  | { kind: 'ignored' };

interface JsonRpcFrame {
  result?: {
    kind?: string;
    contextId?: string;
    artifact?: { parts?: Array<{ kind?: string; text?: string }> };
    parts?: Array<{ kind?: string; text?: string }>;
    status?: { state?: string };
  };
  error?: { message?: string };
}

function textOfParts(parts: Array<{ kind?: string; text?: string }> | undefined): string {
  return (parts ?? [])
    .filter(part => part.kind === 'text' && typeof part.text === 'string')
    .map(part => part.text)
    .join('');
}

export function eventFromFrame(frame: string): A2AEvent {
  const dataLine = frame
    .split('\n')
    .map(line => line.trim())
    .find(line => line.startsWith('data:'));
  if (!dataLine) {
    return { kind: 'ignored' };
  }

  let payload: JsonRpcFrame;
  try {
    payload = JSON.parse(dataLine.slice('data:'.length).trim()) as JsonRpcFrame;
  } catch {
    return { kind: 'ignored' };
  }

  if (payload.error) {
    return { kind: 'error', message: payload.error.message ?? $localize`:@@agentChatAgentError:The agent returned an error.` };
  }

  const result = payload.result;
  if (!result) {
    return { kind: 'ignored' };
  }

  if (result.kind === 'artifact-update') {
    const text = textOfParts(result.artifact?.parts);
    return text ? { kind: 'delta', text, contextId: result.contextId } : { kind: 'ignored' };
  }

  if (result.kind === 'message') {
    const text = textOfParts(result.parts);
    return text ? { kind: 'delta', text, contextId: result.contextId } : { kind: 'ignored' };
  }

  if (result.kind === 'status-update' && result.status?.state === 'completed') {
    return { kind: 'completed', contextId: result.contextId };
  }

  return { kind: 'ignored' };
}
