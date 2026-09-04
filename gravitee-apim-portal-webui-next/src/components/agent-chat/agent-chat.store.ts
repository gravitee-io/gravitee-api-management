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
import { Injectable, signal } from '@angular/core';
import type { LocalizeFn } from '@angular/localize/init';

import { A2AEvent, buildStreamRequest, eventFromFrame, splitSseFrames } from './a2a-client';
import { randomId } from '../../utils/random-id';

declare const $localize: LocalizeFn;

export const API_KEY_HEADER = 'X-Gravitee-Api-Key';

export interface ChatTurn {
  id: string;
  role: 'user' | 'agent';
  text: string;
  complete: boolean;
}

export interface ChatTarget {
  endpoint: string;
  apiKey: string;
}

@Injectable()
export class AgentChatStore {
  private readonly turnsState = signal<ChatTurn[]>([]);
  private readonly streamingState = signal(false);
  private readonly errorState = signal<string | null>(null);
  private contextId: string | undefined;
  private agentId: string | null = null;

  readonly turns = this.turnsState.asReadonly();
  readonly streaming = this.streamingState.asReadonly();
  readonly error = this.errorState.asReadonly();

  resetFor(agentId: string): void {
    if (this.agentId === agentId) {
      return;
    }
    this.agentId = agentId;
    this.contextId = undefined;
    this.turnsState.set([]);
    this.errorState.set(null);
  }

  async send(text: string, target: ChatTarget): Promise<void> {
    const question = text.trim();
    if (!question || this.streamingState()) {
      return;
    }

    const userTurn: ChatTurn = { id: randomId(), role: 'user', text: question, complete: true };
    const agentTurnId = randomId();
    this.turnsState.update(turns => [...turns, userTurn, { id: agentTurnId, role: 'agent', text: '', complete: false }]);
    this.streamingState.set(true);
    this.errorState.set(null);

    try {
      await this.stream(question, userTurn.id, agentTurnId, target);
      this.turnsState.update(turns => turns.map(turn => (turn.id === agentTurnId ? { ...turn, complete: true } : turn)));
    } catch (cause) {
      this.errorState.set(
        cause instanceof Error ? cause.message : $localize`:@@agentChatUnreachable:The agent could not be reached. Try again.`,
      );
      this.turnsState.update(turns => turns.filter(turn => turn.id !== agentTurnId || turn.text.length > 0));
    } finally {
      this.streamingState.set(false);
    }
  }

  private async stream(question: string, messageId: string, agentTurnId: string, target: ChatTarget): Promise<void> {
    // fetch, not HttpClient: the portal interceptors would send the session cookie and two extra
    // headers to the gateway, which is a different service on a different origin.
    const response = await fetch(target.endpoint, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'text/event-stream',
        [API_KEY_HEADER]: target.apiKey,
      },
      body: JSON.stringify(buildStreamRequest(question, this.contextId, messageId, randomId())),
    });

    if (!response.ok || !response.body) {
      throw new Error($localize`:@@agentChatGatewayStatus:The gateway answered ${response.status}:status:.`);
    }

    const reader = response.body.getReader();
    const decoder = new TextDecoder();
    let buffer = '';

    for (;;) {
      const { done, value } = await reader.read();
      if (done) {
        break;
      }
      buffer += decoder.decode(value, { stream: true });
      const { frames, rest } = splitSseFrames(buffer);
      buffer = rest;
      frames.forEach(frame => this.apply(eventFromFrame(frame), agentTurnId));
    }
  }

  private apply(event: A2AEvent, agentTurnId: string): void {
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
}
