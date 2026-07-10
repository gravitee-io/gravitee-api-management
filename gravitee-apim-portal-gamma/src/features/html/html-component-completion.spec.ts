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
import { registerGraviteeComponentCompletions } from './html-component-completion';

describe('html-component-completion', () => {
    it('should register one completion item per gravitee slot component', () => {
        const provider = {
            triggerCharacters: [] as string[],
            provideCompletionItems: jest.fn(),
        };

        const monaco = {
            languages: {
                CompletionItemKind: { Snippet: 27 },
                CompletionItemInsertTextRule: { InsertAsSnippet: 4 },
                registerCompletionItemProvider: jest.fn((_language, nextProvider) => {
                    Object.assign(provider, nextProvider);
                    return { dispose: jest.fn() };
                }),
            },
        };

        registerGraviteeComponentCompletions(monaco as never);

        const model = {
            getWordUntilPosition: () => ({ word: '', startColumn: 1, endColumn: 1 }),
        };

        const result = provider.provideCompletionItems(model as never, { lineNumber: 1, column: 1 } as never);

        expect(result?.suggestions).toHaveLength(4);
        expect(result?.suggestions[0]).toMatchObject({
            label: 'API Catalog',
            insertText: '<div data-gravitee-component="api-catalog"></div>',
        });
        expect(result?.suggestions[1]).toMatchObject({
            label: 'Subscription Viewer',
            insertText: '<div data-gravitee-component="subscription-viewer"></div>',
        });
    });
});
