/*
 * Copyright (C) 2023 The Gravitee team (http://gravitee.io)
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
import {
  AfterViewInit,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  ElementRef,
  Inject,
  input,
  NgZone,
  OnDestroy,
  Optional,
  Self,
} from '@angular/core';
import { ControlValueAccessor, NgControl } from '@angular/forms';
import { isEqual, isString, uniqueId } from 'lodash';
import Monaco from 'monaco-editor';
import { ReplaySubject, Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import { GraviteeMonacoEditorConfig, GRAVITEE_MONACO_EDITOR_CONFIG } from './data/gravitee-monaco-editor-config';
import { GraviteeMonacoWrapperService } from './gravitee-monaco-wrapper.service';
import { componentLibrarySuggestions } from '../component-library/components/index.suggestions';

export type MonacoEditorLanguageConfig = {
  language: 'markdown' | 'html';
};

// Default language configuration
export const DEFAULT_LANGUAGE_CONFIG: MonacoEditorLanguageConfig = {
  language: 'html'
};

@Component({
  selector: 'gravitee-monaco-wrapper',
  template: ` @if (loaded$ | async) {
    <div class="loading">Loading...</div>
  }`,
  styleUrls: ['./gravitee-monaco-wrapper.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
  host: {
    '[class.single-line]': 'singleLineMode()'
  }
})
export class GraviteeMonacoWrapperComponent implements ControlValueAccessor, AfterViewInit, OnDestroy {
  // Signal inputs
  languageConfig = input<MonacoEditorLanguageConfig | undefined>(DEFAULT_LANGUAGE_CONFIG);
  options = input<Monaco.editor.IStandaloneEditorConstructionOptions>({});
  disableMiniMap = input<boolean>(false);
  disableAutoFormat = input<boolean>(false);
  singleLineMode = input<boolean>(false);

  // Public properties
  public loaded$ = new ReplaySubject<boolean>(1);
  public value = '';
  public readOnly = false;
  public standaloneCodeEditor?: Monaco.editor.IStandaloneCodeEditor;

  // Protected properties
  protected _onChange: (_value: string | null) => void = () => ({});
  protected _onTouched: () => void = () => ({});

  // Private properties
  private textModel?: Monaco.editor.ITextModel;
  private toDisposes: Monaco.IDisposable[] = [];
  private unsubscribe$ = new Subject<void>();

  // Default editor options
  private readonly defaultOptions: Monaco.editor.IStandaloneEditorConstructionOptions = {
    contextmenu: false,
    minimap: {
      enabled: true,
    },
    automaticLayout: true,
    scrollBeyondLastLine: false,
    acceptSuggestionOnEnter: 'on',
    autoClosingBrackets: 'always',
    autoClosingQuotes: 'always',
    autoClosingOvertype: 'always',
  };

  constructor(
    public readonly hostElement: ElementRef,
    @Inject(GRAVITEE_MONACO_EDITOR_CONFIG) private readonly config: GraviteeMonacoEditorConfig,
    private readonly monacoEditorService: GraviteeMonacoWrapperService,
    private readonly changeDetectorRef: ChangeDetectorRef,
    private readonly ngZone: NgZone,
    @Optional() @Self() public readonly ngControl: NgControl,
  ) {
    if (this.ngControl) {
      // Set the value accessor directly to avoid circular import
      this.ngControl.valueAccessor = this;
    }
    this.monacoEditorService.loadEditor();
  }

  public ngAfterViewInit(): void {
    this.monacoEditorService.loaded$.pipe(takeUntil(this.unsubscribe$)).subscribe(({ monaco }) => {
      this.setupEditor(monaco);
      this.loaded$.next(false);
      this.changeDetectorRef.detectChanges();
    });
  }

  public ngOnDestroy(): void {
    this.toDisposes.forEach(d => d.dispose());

    if (this.standaloneCodeEditor) {
      this.standaloneCodeEditor.dispose();
      this.standaloneCodeEditor = undefined;
    }

    if (this.textModel) {
      this.textModel.dispose();
      this.textModel = undefined;
    }

    this.unsubscribe$.next();
    this.unsubscribe$.complete();
  }

  // ControlValueAccessor interface implementation
  public writeValue(_value: string): void {
    if (_value) {
      this.value = isString(_value) ? _value : JSON.stringify(_value);
    }
    if (this.textModel) {
      this.textModel.setValue(this.value);
    }
  }

  public registerOnChange(fn: (_value: string | null) => void): void {
    this._onChange = fn;
  }

  public registerOnTouched(fn: () => void): void {
    this._onTouched = fn;
  }

  public setDisabledState(isDisabled: boolean): void {
    this.readOnly = isDisabled;

    if (this.standaloneCodeEditor) {
      this.standaloneCodeEditor.updateOptions({
        readOnly: isDisabled,
      });
    }
  }

  private setupEditor(monaco: typeof Monaco): void {
    if (!this.hostElement) {
      throw new Error('No editor ref found.');
    }

    const domElement = this.hostElement.nativeElement;
    const settings = {
      value: this.value,
      language: 'html', // Use HTML for better component syntax highlighting
      uri: `code-${uniqueId()}`,
    };

    // Register completion provider for HTML
    const completionProvider = monaco.languages.registerCompletionItemProvider('html', {
      provideCompletionItems: (model: any, position: any) => {
        const word = model.getWordUntilPosition(position);
        const range = {
          startLineNumber: position.lineNumber,
          endLineNumber: position.lineNumber,
          startColumn: word.startColumn,
          endColumn: word.endColumn
        };
        // Check the context to determine if we need to include the opening "<"
        const lineContent = model.getLineContent(position.lineNumber);
        const beforeCursor = lineContent.substring(0, position.column - 1);
        const needsOpeningTag = !beforeCursor.trim().endsWith('<');

        return {
          suggestions: componentLibrarySuggestions(range, needsOpeningTag),
        };
      },
    });

    // Add to disposables to clean up later
    this.toDisposes.push(completionProvider);

    this.ngZone.runOutsideAngular(() => {
      this.textModel = monaco.editor.createModel(settings.value, settings.language, monaco.Uri.parse(settings.uri));
    });

    const options = Object.assign({}, this.defaultOptions, this.options(), {
      readOnly: this.readOnly,
      theme: this.config.theme ?? 'vs',
      model: this.textModel,
      minimap: {
        enabled: !this.disableMiniMap(),
      },
    });

    this.standaloneCodeEditor = monaco.editor.create(domElement, options);

    if (!this.disableAutoFormat()) {
      setTimeout(() => {
        this.standaloneCodeEditor?.getAction('editor.action.formatDocument')?.run();
      }, 80);
    }

    this.setupSingleLineMode(monaco);
    this.setupContentChangeHandler();
    this.setupBlurHandler();
    this.setupLanguage(settings.uri, this.languageConfig());
  }

  private setupSingleLineMode(monaco: typeof Monaco): void {
    if (!this.singleLineMode()) {
      return;
    }

    this.standaloneCodeEditor?.addAction({
      id: 'custom.action',
      label: 'custom action',
      keybindings: [monaco.KeyCode.Enter],
      precondition: '!suggestWidgetVisible && !renameInputVisible && !inSnippetMode && !quickFixWidgetVisible',
      run: () => {
        // Ignore Enter key in single line mode
        return;
      },
    });

    // Source: https://farzadyz.com/blog/single-line-monaco-editor
    this.standaloneCodeEditor?.updateOptions({
      fixedOverflowWidgets: true,
      acceptSuggestionOnEnter: 'on',
      hover: {
        delay: 100,
      },
      roundedSelection: false,
      contextmenu: false,
      cursorStyle: 'line-thin',
      links: false,
      find: {
        addExtraSpaceOnTop: false,
        autoFindInSelection: 'never',
        seedSearchStringFromSelection: 'never',
      },
      fontSize: 14,
      fontWeight: 'normal',
      overviewRulerLanes: 0,
      scrollBeyondLastColumn: 0,
      wordWrap: 'on',
      minimap: {
        enabled: false,
      },
      lineNumbers: 'off',
      hideCursorInOverviewRuler: true,
      overviewRulerBorder: false,
      glyphMargin: false,
      folding: false,
      lineDecorationsWidth: 0,
      lineNumbersMinChars: 0,
      renderLineHighlight: 'none',
      scrollbar: {
        horizontal: 'hidden',
        vertical: 'hidden',
        alwaysConsumeMouseWheel: false,
      },
    });
  }

  private setupContentChangeHandler(): void {
    const onDidChangeContent = this.textModel?.onDidChangeContent(() => {
      const textModelValue = this.textModel?.getValue() ?? '';

      if (this.singleLineMode()) {
        this.handleSingleLineContentChange(textModelValue);
        return;
      }

      this.ngZone.run(() => {
        if (!this.readOnly && !isEqual(this.value, textModelValue)) {
          setTimeout(() => {
            this.value = textModelValue;
            this._onChange(textModelValue);
            this._onTouched();
          }, 0);
        }
      });
    });

    if (onDidChangeContent) {
      this.toDisposes.push(onDidChangeContent);
    }
  }

  private handleSingleLineContentChange(textModelValue: string): void {
    // Remove line breaks when pasting text in single line mode
    const hasLineBreak = new RegExp(/(\r\n|\n|\r)/gm);
    if (hasLineBreak.test(textModelValue)) {
      const currentPosition = this.standaloneCodeEditor?.getPosition();
      setTimeout(() => {
        this.standaloneCodeEditor?.pushUndoStop();
        this.textModel?.setValue(textModelValue.replace(/(\r\n|\n|\r)/gm, ''));
        if (currentPosition) {
          this.standaloneCodeEditor?.setPosition(currentPosition);
        }
        this.standaloneCodeEditor?.popUndoStop();
      }, 0);
      return;
    }

    this.ngZone.run(() => {
      if (!this.readOnly && !isEqual(this.value, textModelValue)) {
        setTimeout(() => {
          this.value = textModelValue;
          this._onChange(textModelValue);
          this._onTouched();
        }, 0);
      }
    });
  }

  private setupBlurHandler(): void {
    const onDidBlurEditorWidget = this.standaloneCodeEditor?.onDidBlurEditorWidget(() => {
      this.ngZone.run(() => {
        if (!this.readOnly) {
          this._onTouched();
        }
      });
    });

    if (onDidBlurEditorWidget) {
      this.toDisposes.push(onDidBlurEditorWidget);
    }
  }

  private setupLanguage(uri: string, languageConfig?: MonacoEditorLanguageConfig): void {
    if (!languageConfig) {
      return;
    }

    // Language-specific setup can be added here in the future
    // For now, only markdown is supported
  }
}

const isJsonString = (str: string): boolean => {
  try {
    JSON.parse(str);
    return true;
  } catch (e) {
    return false;
  }
};
