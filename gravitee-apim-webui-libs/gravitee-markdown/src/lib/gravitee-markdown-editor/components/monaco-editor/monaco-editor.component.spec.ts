/*
 * Copyright (C) 2025 The Gravitee team (http://gravitee.io)
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
import { Component } from '@angular/core';
import { ComponentFixture, fakeAsync, flush, TestBed, tick } from '@angular/core/testing';

import { MonacoEditorComponent } from './monaco-editor.component';
import { MonacoEditorService } from '../../services/monaco-editor.service';

jest.mock('monaco-editor', () => ({}));

/** The subset of a Monaco text model the component relies on: a value, and listeners notified on every change. */
class FakeTextModel {
  private listeners: (() => void)[] = [];

  constructor(private value: string) {}

  getValue(): string {
    return this.value;
  }

  setValue(value: string): void {
    this.value = value;
    this.listeners.forEach(listener => listener());
  }

  onDidChangeContent(listener: () => void) {
    this.listeners.push(listener);
    return { dispose: () => undefined };
  }

  dispose(): void {
    this.listeners = [];
  }
}

const disposable = () => ({ dispose: () => undefined });

@Component({
  template: `<gmd-monaco-editor [value]="value" (valueChange)="value = $event" />`,
  // eslint-disable-next-line @angular-eslint/prefer-standalone
  standalone: false,
})
class HostComponent {
  value = '';
}

describe('MonacoEditorComponent', () => {
  let fixture: ComponentFixture<HostComponent>;
  let textModel: FakeTextModel;

  beforeEach(async () => {
    const fakeMonaco = {
      editor: {
        createModel: (value: string) => (textModel = new FakeTextModel(value)),
        create: () => ({
          updateOptions: () => undefined,
          onDidBlurEditorWidget: disposable,
          getAction: () => null,
          dispose: () => undefined,
        }),
        defineTheme: () => undefined,
        setTheme: () => undefined,
      },
      languages: {
        setLanguageConfiguration: () => undefined,
        registerCompletionItemProvider: disposable,
        registerHoverProvider: disposable,
      },
      Uri: { parse: (uri: string) => uri },
    };

    await TestBed.configureTestingModule({
      declarations: [HostComponent, MonacoEditorComponent],
      providers: [{ provide: MonacoEditorService, useValue: { loadEditor: () => Promise.resolve(), monaco: () => fakeMonaco } }],
    }).compileComponents();

    fixture = TestBed.createComponent(HostComponent);
    fixture.detectChanges();
  });

  /** The parent pushes a value, as a form control does on writeValue. */
  function setParentValue(value: string): void {
    fixture.componentInstance.value = value;
    fixture.detectChanges();
    flush();
    fixture.detectChanges();
  }

  /** The user types in the editor: Monaco changes its model, and the component reports it to the parent. */
  function typeInEditor(value: string): void {
    textModel.setValue(value);
    flush();
    fixture.detectChanges();
  }

  it('should show what the parent pushes after the user typed, even when it equals what the user typed', fakeAsync(() => {
    setParentValue('Form A');
    typeInEditor('Form A edited');
    expect(fixture.componentInstance.value).toBe('Form A edited');

    // Another document is opened, emptied first while it loads...
    setParentValue('');
    setParentValue('Form B');
    expect(textModel.getValue()).toBe('Form B');

    // ...then the first one again, saved with what the user typed.
    setParentValue('');
    setParentValue('Form A edited');

    expect(textModel.getValue()).toBe('Form A edited');
  }));

  it('should not overwrite what the user is typing with the late echo of an earlier keystroke', fakeAsync(() => {
    setParentValue('a');

    textModel.setValue('ab');
    tick();
    textModel.setValue('abc');
    // The parent only now receives 'ab', while the editor already holds 'abc'.
    fixture.detectChanges();
    expect(textModel.getValue()).toBe('abc');

    flush();
    fixture.detectChanges();
    expect(textModel.getValue()).toBe('abc');
    expect(fixture.componentInstance.value).toBe('abc');
  }));
});
