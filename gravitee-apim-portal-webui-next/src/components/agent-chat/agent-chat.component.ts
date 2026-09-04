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
import { Component, computed, effect, ElementRef, inject, input, viewChild } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

import { GraviteeMarkdownViewerModule } from '@gravitee/gravitee-markdown';

import { AgentChatStore, ChatTarget } from './agent-chat.store';

@Component({
  selector: 'app-agent-chat',
  imports: [ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatInputModule, GraviteeMarkdownViewerModule],
  templateUrl: './agent-chat.component.html',
  styleUrl: './agent-chat.component.scss',
})
export class AgentChatComponent {
  private readonly store = inject(AgentChatStore);

  agentName = input.required<string>();
  applicationName = input.required<string>();
  target = input.required<ChatTarget>();

  protected readonly draft = new FormControl<string>('', { nonNullable: true });

  protected readonly turns = this.store.turns;
  protected readonly streaming = this.store.streaming;
  protected readonly error = this.store.error;

  private readonly draftValue = toSignal(this.draft.valueChanges, { initialValue: '' });
  protected readonly canSend = computed(() => this.draftValue().trim().length > 0 && !this.streaming());

  private readonly log = viewChild<ElementRef<HTMLElement>>('log');

  constructor() {
    effect(() => {
      this.turns();
      const log = this.log()?.nativeElement;
      if (log) {
        log.scrollTop = log.scrollHeight;
      }
    });
  }

  protected async submit(): Promise<void> {
    if (!this.canSend()) {
      return;
    }
    const question = this.draft.value;
    this.draft.setValue('');
    await this.store.send(question, this.target());
  }

  // Bound to keydown.enter, which Angular already narrows to enter without modifiers,
  // so shift+enter never reaches here and keeps its newline.
  protected onEnter(event: Event): void {
    event.preventDefault();
    void this.submit();
  }
}
