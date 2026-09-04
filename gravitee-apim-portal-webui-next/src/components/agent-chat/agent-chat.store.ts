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
import { DestroyRef, inject, Injectable, signal } from '@angular/core';
import type { LocalizeFn } from '@angular/localize/init';

import { A2AEvent, buildStreamRequest, eventFromFrame, splitSseFrames } from './a2a-client';
import { ConfigService } from '../../services/config.service';
import { randomId } from '../../utils/random-id';

declare const $localize: LocalizeFn;

export const DEFAULT_API_KEY_HEADER = 'X-Gravitee-Api-Key';

export interface ChatTurn {
  id: string;
  role: 'user' | 'agent';
  text: string;
  isComplete: boolean;
}

export interface ChatTarget {
  endpoint: string;
  apiKey: string;
}

/** Thrown for a gateway answer we can describe; anything else is reported as unreachable. */
class GatewayResponseError extends Error {}

@Injectable()
export class AgentChatStore {
  private readonly configService = inject(ConfigService);

  private readonly turnsState = signal<ChatTurn[]>([]);
  private readonly isStreamingState = signal(false);
  private readonly errorState = signal<string | null>(null);
  private contextId: string | undefined;
  private agentId: string | null = null;
  private inFlight: AbortController | null = null;
  /** Bumped whenever a stream stops being relevant, so its late writes can be discarded. */
  private generation = 0;

  readonly turns = this.turnsState.asReadonly();
  readonly isStreaming = this.isStreamingState.asReadonly();
  readonly error = this.errorState.asReadonly();

  constructor() {
    inject(DestroyRef).onDestroy(() => this.abandonStream());
  }

  resetFor(agentId: string): void {
    if (this.agentId === agentId) {
      return;
    }
    this.agentId = agentId;
    this.abandonStream();
    this.contextId = undefined;
    this.turnsState.set([]);
    this.errorState.set(null);
  }

  async send(text: string, target: ChatTarget): Promise<void> {
    const question = text.trim();
    if (!question || this.isStreamingState()) {
      return;
    }

    const userTurn: ChatTurn = { id: randomId(), role: 'user', text: question, isComplete: true };
    const agentTurnId = randomId();
    this.turnsState.update(turns => [...turns, userTurn, { id: agentTurnId, role: 'agent', text: '', isComplete: false }]);
    this.isStreamingState.set(true);
    this.errorState.set(null);

    const controller = new AbortController();
    this.inFlight = controller;
    const generation = this.generation;

    try {
      await this.stream(question, userTurn.id, agentTurnId, target, controller.signal, generation);
    } catch (cause) {
      if (generation === this.generation) {
        this.errorState.set(
          cause instanceof GatewayResponseError
            ? cause.message
            : $localize`:@@agentChatUnreachable:The agent could not be reached. Check the subscription and try again.`,
        );
      }
    } finally {
      if (generation === this.generation) {
        this.settleTurn(agentTurnId);
        this.isStreamingState.set(false);
        this.inFlight = null;
      }
    }
  }

  private async stream(
    question: string,
    messageId: string,
    agentTurnId: string,
    target: ChatTarget,
    signal: AbortSignal,
    generation: number,
  ): Promise<void> {
    // fetch, not HttpClient: the portal interceptors would send the session cookie and two extra
    // headers to the gateway, which is a different service on a different origin.
    const response = await fetch(target.endpoint, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'text/event-stream',
        [this.apiKeyHeader()]: target.apiKey,
      },
      body: JSON.stringify(buildStreamRequest(question, this.contextId, messageId, randomId())),
      signal,
    });

    if (!response.ok || !response.body) {
      throw new GatewayResponseError($localize`:@@agentChatGatewayStatus:The gateway answered ${response.status}:status:.`);
    }

    const reader = response.body.getReader();
    const decoder = new TextDecoder();
    let buffer = '';

    for (;;) {
      const { done, value } = await reader.read();
      if (done) {
        // Flush the decoder and read the unterminated tail as a last frame: a stream that ends at
        // EOF need not close with a blank line.
        buffer += decoder.decode();
        this.consume(buffer, agentTurnId, generation, true);
        return;
      }
      buffer += decoder.decode(value, { stream: true });
      const { frames, rest } = splitSseFrames(buffer);
      buffer = rest;
      frames.forEach(frame => this.apply(eventFromFrame(frame), agentTurnId, generation));
    }
  }

  private consume(buffer: string, agentTurnId: string, generation: number, includeTail: boolean): void {
    const { frames, rest } = splitSseFrames(buffer);
    frames.forEach(frame => this.apply(eventFromFrame(frame), agentTurnId, generation));
    if (includeTail && rest.trim().length > 0) {
      this.apply(eventFromFrame(rest), agentTurnId, generation);
    }
  }

  private apply(event: A2AEvent, agentTurnId: string, generation: number): void {
    if (generation !== this.generation) {
      return;
    }
    switch (event.kind) {
      case 'delta':
        this.contextId = event.contextId ?? this.contextId;
        this.turnsState.update(turns => turns.map(turn => (turn.id === agentTurnId ? { ...turn, text: turn.text + event.text } : turn)));
        break;
      case 'completed':
        this.contextId = event.contextId ?? this.contextId;
        break;
      case 'error':
        this.errorState.set(event.message);
        break;
      case 'ignored':
        break;
    }
  }

  /** An answer that produced no text is not an answer, so it leaves no bubble behind. */
  private settleTurn(agentTurnId: string): void {
    this.turnsState.update(turns =>
      turns.flatMap(turn => {
        if (turn.id !== agentTurnId) {
          return [turn];
        }
        return turn.text.length > 0 ? [{ ...turn, isComplete: true }] : [];
      }),
    );
  }

  private abandonStream(): void {
    this.generation += 1;
    this.inFlight?.abort();
    this.inFlight = null;
    this.isStreamingState.set(false);
  }

  private apiKeyHeader(): string {
    return this.configService.configuration?.portal?.apikeyHeader ?? DEFAULT_API_KEY_HEADER;
  }
}
