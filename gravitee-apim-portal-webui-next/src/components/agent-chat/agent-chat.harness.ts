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
import { ComponentHarness } from '@angular/cdk/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatInputHarness } from '@angular/material/input/testing';

import { GraviteeMarkdownViewerHarness } from '@gravitee/gravitee-markdown';

export class AgentChatComponentHarness extends ComponentHarness {
  static readonly hostSelector = 'app-agent-chat';

  private readonly getHeader = this.locatorFor('.agent-chat__header');
  private readonly getComposer = this.locatorFor(MatInputHarness);
  private readonly getSendButton = this.locatorFor(MatButtonHarness.with({ selector: '[data-testid="agent-chat-send"]' }));
  private readonly getError = this.locatorForOptional('.agent-chat__error');
  private readonly getEmptyState = this.locatorForOptional('.agent-chat__empty');
  private readonly getMarkdownViewer = this.locatorForOptional(GraviteeMarkdownViewerHarness);
  private readonly getPlainTurns = this.locatorForAll('.agent-chat__turn__text');
  private readonly getComposerElement = this.locatorFor('textarea[matInput]');

  async headerText(): Promise<string> {
    return (await this.getHeader()).text();
  }

  async type(text: string): Promise<void> {
    await (await this.getComposer()).setValue(text);
  }

  async pressEnter({ shift }: { shift?: boolean } = {}): Promise<void> {
    await (await this.getComposerElement()).dispatchEvent('keydown', { key: 'Enter', shiftKey: !!shift });
  }

  async composerValue(): Promise<string> {
    return (await this.getComposer()).getValue();
  }

  async clickSend(): Promise<void> {
    await (await this.getSendButton()).click();
  }

  async isSendDisabled(): Promise<boolean> {
    return (await this.getSendButton()).isDisabled();
  }

  async errorText(): Promise<string | null> {
    const error = await this.getError();
    return error ? error.text() : null;
  }

  async emptyStateText(): Promise<string | null> {
    const emptyState = await this.getEmptyState();
    return emptyState ? emptyState.text() : null;
  }

  async hasMarkdownViewer(): Promise<boolean> {
    return (await this.getMarkdownViewer()) !== null;
  }

  async plainTurnTexts(): Promise<string[]> {
    const turns = await this.getPlainTurns();
    return Promise.all(turns.map(turn => turn.text()));
  }
}
