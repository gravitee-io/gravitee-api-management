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
import type { PortalNavigationPage } from '../portals/types';
import {
    PORTAL_HTML_SHOW_CATEGORY_COMMAND,
    registerPortalHtmlCompletions,
    type PortalHtmlCompletionCategory,
} from './html-portal-completion';

const portalPages: PortalNavigationPage[] = [
    {
        id: 'page-home',
        portalId: 'p1',
        title: 'Getting Started',
        type: 'PAGE',
        parentId: null,
        order: 0,
        slug: 'getting-started-nav001',
    },
    {
        id: 'page-about',
        portalId: 'p1',
        title: 'About',
        type: 'PAGE',
        parentId: null,
        order: 1,
        slug: 'about-def456',
    },
];

function createTestSetup(portalPagesProvider: () => readonly PortalNavigationPage[] = () => portalPages) {
    const provider = {
        triggerCharacters: [] as string[],
        provideCompletionItems: jest.fn(),
    };
    const editor = {
        getId: () => 'editor-test',
        trigger: jest.fn(),
    };

    const monaco = {
        editor: {
            registerCommand: jest.fn(
                (_commandId: string, handler: (_accessor: unknown, category: PortalHtmlCompletionCategory) => void) => {
                    (editor as { categoryRun?: (category: PortalHtmlCompletionCategory) => void }).categoryRun = (
                        category: PortalHtmlCompletionCategory,
                    ) => {
                        handler(null, category);
                    };
                    return { dispose: jest.fn() };
                },
            ),
        },
        languages: {
            CompletionItemKind: { Snippet: 27, Folder: 18, Value: 12 },
            CompletionItemInsertTextRule: { InsertAsSnippet: 4 },
            registerCompletionItemProvider: jest.fn((_language, nextProvider) => {
                Object.assign(provider, nextProvider);
                return { dispose: jest.fn() };
            }),
        },
    };

    const disposable = registerPortalHtmlCompletions(monaco as never, editor as never, {
        getPortalPages: portalPagesProvider,
    });

    const model = {
        getWordUntilPosition: () => ({ word: '', startColumn: 1, endColumn: 1 }),
        getValueInRange: () => '',
    };

    const position = { lineNumber: 1, column: 1 };

    return { provider, editor, model, position, disposable };
}

describe('html-portal-completion', () => {
    it('should show two category folders by default', () => {
        const { provider, model, position } = createTestSetup();

        const result = provider.provideCompletionItems(model as never, position as never);

        expect(result?.suggestions).toHaveLength(2);
        expect(result?.suggestions.map((item: { label: string }) => item.label)).toEqual([
            'Relative Links',
            'Gravitee Component',
        ]);
        expect(result?.suggestions[0]).toMatchObject({
            kind: 18,
            command: { id: `${PORTAL_HTML_SHOW_CATEGORY_COMMAND}.editor-test`, arguments: ['links'] },
        });
    });

    it('should show gravitee slot snippets when gravitee category is active', () => {
        const { provider, editor, model, position } = createTestSetup();
        const categoryRun = (editor as { categoryRun?: (category: PortalHtmlCompletionCategory) => void }).categoryRun;

        categoryRun?.('gravitee');

        const result = provider.provideCompletionItems(model as never, position as never);

        expect(result?.suggestions).toHaveLength(4);
        expect(result?.suggestions[0]).toMatchObject({
            label: 'API Catalog',
            insertText: '<div data-gravitee-component="api-catalog"></div>',
        });
    });

    it('should show full anchor snippets when links category is active', () => {
        const { provider, editor, model, position } = createTestSetup();
        const categoryRun = (editor as { categoryRun?: (category: PortalHtmlCompletionCategory) => void }).categoryRun;

        categoryRun?.('links');

        const result = provider.provideCompletionItems(model as never, position as never);

        expect(result?.suggestions).toHaveLength(2);
        expect(result?.suggestions[0]).toMatchObject({
            label: 'Getting Started',
            insertText: '<a href="./getting-started-nav001">Getting Started</a>',
        });
    });

    it('should show slug-only suggestions inside href attributes', () => {
        const { provider, model, position } = createTestSetup();
        model.getValueInRange = () => '<a href="';

        const result = provider.provideCompletionItems(model as never, position as never);

        expect(result?.suggestions).toHaveLength(2);
        expect(result?.suggestions[0]).toMatchObject({
            label: 'Getting Started',
            insertText: './getting-started-nav001',
            kind: 12,
        });
        expect(result?.suggestions[0]?.insertText).not.toContain('<a ');
    });

    it('should return empty link suggestions when no portal pages are available', () => {
        const { provider, editor, model, position } = createTestSetup(() => []);
        const categoryRun = (editor as { categoryRun?: (category: PortalHtmlCompletionCategory) => void }).categoryRun;

        categoryRun?.('links');

        const result = provider.provideCompletionItems(model as never, position as never);

        expect(result?.suggestions).toEqual([]);
    });
});
