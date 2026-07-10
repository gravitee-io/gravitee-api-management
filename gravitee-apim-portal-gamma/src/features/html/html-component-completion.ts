/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
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
import type * as Monaco from 'monaco-editor';

import { buildSlotSnippet, GRAVITEE_SLOT_COMPONENTS } from './gravitee-slot-components';

type MonacoApi = typeof Monaco;

export function registerGraviteeComponentCompletions(monaco: MonacoApi): Monaco.IDisposable {
    return monaco.languages.registerCompletionItemProvider('html', {
        triggerCharacters: ['"', ' ', '<'],
        provideCompletionItems(model, position) {
            const word = model.getWordUntilPosition(position);
            const range = {
                startLineNumber: position.lineNumber,
                endLineNumber: position.lineNumber,
                startColumn: word.startColumn,
                endColumn: position.column,
            };

            const suggestions: Monaco.languages.CompletionItem[] = GRAVITEE_SLOT_COMPONENTS.map(component => ({
                label: component.label,
                kind: monaco.languages.CompletionItemKind.Snippet,
                detail: component.id,
                documentation: component.description,
                insertText: buildSlotSnippet(component.id),
                insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet,
                range,
                sortText: component.label,
            }));

            return { suggestions };
        },
    });
}
