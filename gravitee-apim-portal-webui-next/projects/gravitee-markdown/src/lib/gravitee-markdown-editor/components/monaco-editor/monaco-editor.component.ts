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
import { ChangeDetectorRef, Component, computed, effect, ElementRef, input, NgZone, OnDestroy, output, signal } from '@angular/core';
import { isEqual, uniqueId } from 'lodash';
import * as Monaco from 'monaco-editor';
import { editor } from 'monaco-editor';

import { componentSuggestionMap } from '../../../components/suggestions.index';
import { ComponentSuggestion } from '../../../models/componentSuggestion';
import { MonacoEditorService } from '../../services/monaco-editor.service';

@Component({
  selector: 'gmd-monaco-editor',
  templateUrl: './monaco-editor.component.html',
  styleUrl: './monaco-editor.component.scss',
  // eslint-disable-next-line @angular-eslint/prefer-standalone
  standalone: false,
})
export class MonacoEditorComponent implements OnDestroy {
  value = input<string>('');
  readOnly = input<boolean>(false);
  valueChange = output<string>();
  touched = output<void>();

  public standaloneCodeEditor?: editor.IStandaloneCodeEditor;

  private readonly isEditorSetup = signal<boolean>(false);
  private readonly isUpdatingFromParent = signal<boolean>(false);
  private readonly lastEmittedValue = signal<string>('');
  private readonly shouldUpdateTextModel = computed(() => !this.isUpdatingFromParent() && this.value() !== this.lastEmittedValue());
  private textModel?: editor.ITextModel;
  private toDisposes: Monaco.IDisposable[] = [];

  private readonly defaultOptions: editor.IStandaloneEditorConstructionOptions = {
    contextmenu: false,
    minimap: {
      enabled: false,
    },
    automaticLayout: true,
    scrollBeyondLastLine: false,
    theme: 'vs',
    renderLineHighlight: 'none',
    scrollbar: {
      verticalScrollbarSize: 5,
      horizontalScrollbarSize: 5,
    },
  };

  constructor(
    private readonly hostElement: ElementRef,
    private readonly monacoEditorService: MonacoEditorService,
    private readonly changeDetectorRef: ChangeDetectorRef,
    private readonly ngZone: NgZone,
  ) {
    this.monacoEditorService.loadEditor();

    // Effect to handle editor setup and value updates
    effect(() => {
      const newValue = this.value();
      if (this.standaloneCodeEditor && this.textModel) {
        const currentValue = this.textModel.getValue();
        if (currentValue !== newValue && this.shouldUpdateTextModel()) {
          this.isUpdatingFromParent.set(true);
          this.textModel.setValue(newValue);

          this.autoFormatValue();

          // Reset the flag in a callback to allow Monaco to process the change
          setTimeout(() => {
            this.isUpdatingFromParent.set(false);
          }, 0);
        }
      }
    });

    // Effect to handle Monaco editor initialization
    effect(() => {
      const monaco = this.monacoEditorService.monaco();
      if (monaco && !this.isEditorSetup()) {
        this.setupEditor(monaco);
        this.registerCompletionItems(monaco);
        this.registerHoverDocumentation(monaco);
        this.isEditorSetup.set(true);
        this.changeDetectorRef.detectChanges();
      }
    });

    // Effect to handle readonly state
    effect(() => {
      const isReadOnly = this.readOnly();
      if (this.standaloneCodeEditor) {
        this.standaloneCodeEditor.updateOptions({
          readOnly: isReadOnly,
        });
      }
    });
  }

  public ngOnDestroy() {
    this.toDisposes.forEach(d => d.dispose());

    if (this.standaloneCodeEditor) {
      this.standaloneCodeEditor.dispose();
      this.standaloneCodeEditor = undefined;
    }

    if (this.textModel) {
      this.textModel.dispose();
      this.textModel = undefined;
    }
  }

  private setupEditor(monaco: typeof Monaco) {
    if (!this.hostElement) {
      throw new Error('No editor ref found.');
    }

    const domElement = this.hostElement.nativeElement;

    this.ngZone.runOutsideAngular(() => {
      this.textModel = monaco.editor.createModel(this.value(), 'markdown', monaco.Uri.parse(`code-${uniqueId()}`));
    });

    this.standaloneCodeEditor = monaco.editor.create(domElement, {
      ...this.defaultOptions,
      model: this.textModel,
      readOnly: this.readOnly(),
    });

    const onDidChangeContent = this.textModel?.onDidChangeContent(() => {
      const textModelValue = this.textModel?.getValue() ?? '';

      this.ngZone.run(() => {
        if (!isEqual(this.value(), textModelValue)) {
          setTimeout(() => {
            this.lastEmittedValue.set(textModelValue);
            this.valueChange.emit(textModelValue);
          }, 0);
        }
      });
    });

    const onDidBlurEditorWidget = this.standaloneCodeEditor?.onDidBlurEditorWidget(() => {
      this.ngZone.run(() => {
        this.touched.emit();
      });
    });

    monaco.editor.defineTheme('gmdTheme', {
      base: 'vs',
      inherit: true,
      rules: [],
      colors: {
        'editorHoverWidget.background': '#FFFFFF',
        'editorHoverWidget.border': '#626271',
      },
    });
    monaco.editor.setTheme('gmdTheme');

    this.toDisposes = [onDidChangeContent, onDidBlurEditorWidget].filter(d => !!d) as Monaco.IDisposable[];
  }

  private autoFormatValue() {
    setTimeout(() => {
      this.standaloneCodeEditor
        ?.getAction('editor.action.formatDocument')
        ?.run()
        .finally(() => {
          this.changeDetectorRef.detectChanges();
        });
    }, 80);
  }

  private registerCompletionItems(monaco: typeof Monaco): void {
    const completionProvider = monaco.languages.registerCompletionItemProvider('markdown', {
      provideCompletionItems: (model: Monaco.editor.ITextModel, position: Monaco.Position) => {
        const word = model.getWordUntilPosition(position);
        const range = {
          startLineNumber: position.lineNumber,
          endLineNumber: position.lineNumber,
          startColumn: word.startColumn,
          endColumn: word.endColumn,
        };

        // Get the current line text to determine context
        const currentLine = model.getLineContent(position.lineNumber);
        const textBeforeCursor = currentLine.substring(0, position.column - 1);

        // Check if user is typing within a component tag
        const { componentSuggestions, itemKind } = this.getComponentSuggestionsAndItemKind(monaco, textBeforeCursor);

        return {
          suggestions: componentSuggestions.map(componentSuggestion => ({
            label: componentSuggestion.label,
            kind: itemKind,
            detail: componentSuggestion.detail,
            insertText: componentSuggestion.insertText,
            insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet,
            range,
          })),
        };
      },
    });

    this.toDisposes.push(completionProvider);
  }

  private registerHoverDocumentation(monaco: typeof Monaco): void {
    const hoverProvider = monaco.languages.registerHoverProvider('markdown', {
      provideHover: (model: Monaco.editor.ITextModel, position: Monaco.Position) => {
        const word = model.getWordAtPosition(position);
        if (!word) {
          return null;
        }

        const wordText = word.word;
        const lineText = model.getLineContent(position.lineNumber);

        const hoverInfo = this.getHoverInfo(wordText, lineText);

        if (hoverInfo) {
          return {
            range: new monaco.Range(position.lineNumber, word.startColumn, position.lineNumber, word.endColumn),
            contents: [{ value: `**${hoverInfo.label}**` }, { value: hoverInfo.description }],
          };
        }

        return null;
      },
    });

    this.toDisposes.push(hoverProvider);
  }

  private getComponentSuggestionsAndItemKind(
    monaco: typeof Monaco,
    textBeforeCursor: string,
  ): { componentSuggestions: ComponentSuggestion[]; itemKind: Monaco.languages.CompletionItemKind } {
    const componentTag = this.getComponentTag(textBeforeCursor);
    if (componentTag) {
      return {
        componentSuggestions: componentSuggestionMap[componentTag]?.attributeSuggestions || [],
        itemKind: monaco.languages.CompletionItemKind.Property,
      };
    }

    return {
      componentSuggestions: Object.values(componentSuggestionMap).flatMap(config => config.suggestions),
      itemKind: monaco.languages.CompletionItemKind.Snippet,
    };
  }

  private getHoverInfo(wordText: string, lineText: string): { label: string; description: string } | null {
    // Check if hovering over a component tag (grid, cell, etc.)
    const config = componentSuggestionMap[wordText];
    if (config) {
      return config.hoverDocumentation;
    }

    // Check if hovering over an attribute name with a value
    if (lineText.includes(`${wordText}=`)) {
      // Get component tag for the attribute context
      const componentTag = this.getComponentTag(lineText);
      if (componentTag) {
        return componentSuggestionMap[componentTag].attributeHoverDocumentation[wordText];
      }
    }

    return null;
  }

  private getComponentTag(text: string): string | null {
    // Look for the most recent opening tag that contains the current position
    const openingTagRegex = /<(\w+)(?:\s[^>]*)?/g;
    const match = openingTagRegex.exec(text);

    if (!match) {
      return null;
    }

    const tagName = match[1];

    // Check if this tag is in our component suggestion map
    const tagNameStartIndex = match.index! + tagName.length;
    if (componentSuggestionMap[tagName] && this.checkIfInsideTag(text, tagName, tagNameStartIndex)) {
      return tagName;
    }

    return null;
  }

  private checkIfInsideTag(textBeforeCursor: string, tagName: string, tagNameStartIndex: number): boolean {
    const afterOpeningTag = textBeforeCursor.substring(tagNameStartIndex);
    const closingTagRegex = new RegExp(`</${tagName}>`);

    // If we do not find a closing tag after the opening tag, we are inside the tag
    return !closingTagRegex.test(afterOpeningTag);
  }
}
