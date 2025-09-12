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
import { Injectable } from '@angular/core';
import * as Monaco from 'monaco-editor';

import { allComponentSuggestions } from '../../components/suggestions.index';

@Injectable({
  providedIn: 'root',
})
export class MonacoSuggestionsService {
  private disposables: Monaco.IDisposable[] = [];

  public registerSuggestions(monaco: typeof Monaco): void {
    // Register completion provider for markdown language
    const completionProvider = monaco.languages.registerCompletionItemProvider('markdown', {
      provideCompletionItems: (model, position) => {
        const word = model.getWordUntilPosition(position);
        const range = {
          startLineNumber: position.lineNumber,
          endLineNumber: position.lineNumber,
          startColumn: word.startColumn,
          endColumn: word.endColumn,
        };

        // Convert component suggestions to Monaco completion items
        const suggestions: Monaco.languages.CompletionItem[] = allComponentSuggestions.map(suggestion => ({
          ...suggestion,
          range: range,
        }));

        return { suggestions };
      },
    });

    this.disposables.push(completionProvider);
  }

  public dispose(): void {
    this.disposables.forEach(disposable => disposable.dispose());
    this.disposables = [];
  }
}
