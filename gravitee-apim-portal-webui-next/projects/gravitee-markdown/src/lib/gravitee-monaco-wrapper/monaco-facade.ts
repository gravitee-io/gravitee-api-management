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

// Monaco facade interfaces - these mirror Monaco types without importing them directly
export interface IMonacoRange {
  startLineNumber: number;
  endLineNumber: number;
  startColumn: number;
  endColumn: number;
}

export interface IMonacoPosition {
  lineNumber: number;
  column: number;
}

export interface IMonacoCompletionItem {
  label: string;
  kind: number;
  insertText: string;
  range: IMonacoRange;
  detail?: string;
  documentation?: string;
  sortText?: string;
  filterText?: string;
  insertTextRules?: number;
  command?: any;
  commitCharacters?: string[];
  additionalTextEdits?: any[];
  insertTextMode?: number;
}

// Monaco completion item kinds
export const MonacoCompletionItemKind = {
  Snippet: 25,
  Text: 1,
  Method: 2,
  Function: 3,
  Constructor: 4,
  Field: 5,
  Variable: 6,
  Class: 7,
  Interface: 8,
  Module: 9,
  Property: 10,
  Unit: 11,
  Value: 12,
  Enum: 13,
  Keyword: 14,
  Color: 15,
  File: 16,
  Reference: 17,
  Folder: 18,
  EnumMember: 19,
  Constant: 20,
  Struct: 21,
  Event: 22,
  Operator: 23,
  TypeParameter: 24,
} as const;

// Monaco completion item insert text rules
export const MonacoCompletionItemInsertTextRule = {
  InsertAsSnippet: 4,
  KeepWhitespace: 1,
} as const;

export interface IMonacoCompletionList {
  suggestions: IMonacoCompletionItem[];
}

export interface IMonacoEditor {
  dispose(): void;
  getValue(): string;
  setValue(value: string): void;
  getPosition(): IMonacoPosition;
  setPosition(position: IMonacoPosition): void;
  updateOptions(options: any): void;
  addAction(action: any): void;
  pushUndoStop(): void;
  popUndoStop(): void;
  onDidChangeContent(callback: () => void): any;
  onDidBlurEditorWidget(callback: () => void): any;
  getAction(id: string): any;
}

export interface IMonacoTextModel {
  dispose(): void;
  getValue(): string;
  setValue(value: string): void;
  onDidChangeContent(callback: () => void): any;
}

export interface IMonacoUri {
  parse(uri: string): any;
}

export interface IMonacoLanguages {
  registerCompletionItemProvider(language: string, provider: any): any;
  register(language: any): void;
  setLanguageConfiguration(language: string, config: any): void;
  setMonarchTokensProvider(language: string, provider: any): void;
}

export interface IMonacoEditorStatic {
  create(domElement: HTMLElement, options: any): IMonacoEditor;
  createModel(value: string, language: string, uri: any): IMonacoTextModel;
  KeyCode: any;
}

export interface IMonacoKeyCode {
  Enter: number;
}

// Add missing interfaces that Monaco expects
export interface IMonacoIDisposable {
  dispose(): void;
}

export interface IMonacoEmitter {
  // Add basic emitter interface
  event: any;
  fire: any;
  dispose: () => void;
}

export interface IMonacoMarkerTag {
  // Add marker tag interface
}

export interface IMonacoMarkerSeverity {
  // Add marker severity interface
}

export interface IMonacoCancellationTokenSource {
  // Add cancellation token source interface
}

export interface IMonaco {
  editor: IMonacoEditorStatic;
  languages: IMonacoLanguages;
  Uri: IMonacoUri;
  IDisposable: IMonacoIDisposable;
  KeyCode: IMonacoKeyCode;
  // Add missing properties that Monaco expects
  Emitter: IMonacoEmitter;
  MarkerTag: IMonacoMarkerTag;
  MarkerSeverity: IMonacoMarkerSeverity;
  CancellationTokenSource: IMonacoCancellationTokenSource;
  // Add any other properties that might be needed
  [key: string]: any;
}

// Export the global Monaco instance getter
export function getMonaco(): IMonaco {
  if (typeof window !== 'undefined' && (window as any).monaco) {
    return (window as any).monaco as IMonaco;
  }
  throw new Error('Monaco is not loaded. Make sure to call loadEditor() first.');
} 