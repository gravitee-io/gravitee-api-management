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

import type { PortalNavigationPage } from '../portals/types';
import { buildSlotSnippet, GRAVITEE_SLOT_COMPONENTS } from './gravitee-slot-components';
import {
    buildPortalPageHrefSlug,
    buildPortalPageLinkSnippet,
    isInsideHrefAttribute,
} from './html-portal-completion.utils';

type MonacoApi = typeof Monaco;

export type PortalHtmlCompletionCategory = 'links' | 'gravitee';

export const PORTAL_HTML_SHOW_CATEGORY_COMMAND = 'portal-html.showCategoryCompletions';

const CATEGORY_BY_EDITOR = new WeakMap<Monaco.editor.IStandaloneCodeEditor, PortalHtmlCompletionCategory>();

export interface RegisterPortalHtmlCompletionsOptions {
    readonly getPortalPages: () => readonly PortalNavigationPage[];
}

function getTextBeforeCursor(model: Monaco.editor.ITextModel, position: Monaco.Position): string {
    return model.getValueInRange({
        startLineNumber: position.lineNumber,
        startColumn: 1,
        endLineNumber: position.lineNumber,
        endColumn: position.column,
    });
}

function getCompletionRange(model: Monaco.editor.ITextModel, position: Monaco.Position): Monaco.IRange {
    const word = model.getWordUntilPosition(position);
    return {
        startLineNumber: position.lineNumber,
        endLineNumber: position.lineNumber,
        startColumn: word.startColumn,
        endColumn: position.column,
    };
}

function buildCategorySuggestions(
    monaco: MonacoApi,
    range: Monaco.IRange,
    categoryCommandId: string,
): Monaco.languages.CompletionItem[] {
    const categories: { label: string; category: PortalHtmlCompletionCategory; sortText: string }[] = [
        { label: 'Relative Links', category: 'links', sortText: '0_links' },
        { label: 'Gravitee Component', category: 'gravitee', sortText: '0_gravitee' },
    ];

    return categories.map(({ label, category, sortText }) => ({
        label,
        kind: monaco.languages.CompletionItemKind.Folder,
        detail: 'Show suggestions',
        insertText: '',
        range,
        sortText,
        command: {
            id: categoryCommandId,
            title: 'Show category completions',
            arguments: [category],
        },
    }));
}

function buildGraviteeSuggestions(
    monaco: MonacoApi,
    range: Monaco.IRange,
): Monaco.languages.CompletionItem[] {
    return GRAVITEE_SLOT_COMPONENTS.map(component => ({
        label: component.label,
        kind: monaco.languages.CompletionItemKind.Snippet,
        detail: component.id,
        documentation: component.description,
        insertText: buildSlotSnippet(component.id),
        insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet,
        range,
        sortText: component.label,
    }));
}

function buildRelativeLinkSuggestions(
    monaco: MonacoApi,
    range: Monaco.IRange,
    portalPages: readonly PortalNavigationPage[],
): Monaco.languages.CompletionItem[] {
    return portalPages.map(page => ({
        label: page.title,
        kind: monaco.languages.CompletionItemKind.Snippet,
        detail: buildPortalPageHrefSlug(page.slug),
        insertText: buildPortalPageLinkSnippet(page.slug, page.title),
        range,
        sortText: page.title,
    }));
}

function buildHrefSlugSuggestions(
    monaco: MonacoApi,
    range: Monaco.IRange,
    portalPages: readonly PortalNavigationPage[],
): Monaco.languages.CompletionItem[] {
    return portalPages.map(page => ({
        label: page.title,
        kind: monaco.languages.CompletionItemKind.Value,
        detail: buildPortalPageHrefSlug(page.slug),
        insertText: buildPortalPageHrefSlug(page.slug),
        range,
        sortText: page.title,
    }));
}

export function registerPortalHtmlCompletions(
    monaco: MonacoApi,
    editor: Monaco.editor.IStandaloneCodeEditor,
    options: RegisterPortalHtmlCompletionsOptions,
): Monaco.IDisposable {
    const categoryCommandId = `${PORTAL_HTML_SHOW_CATEGORY_COMMAND}.${editor.getId()}`;
    const categoryCommand = monaco.editor.registerCommand(
        categoryCommandId,
        (_accessor, category: PortalHtmlCompletionCategory) => {
            CATEGORY_BY_EDITOR.set(editor, category);
            editor.trigger('portal-html', 'editor.action.triggerSuggest', {});
        },
    );

    const completionProvider = monaco.languages.registerCompletionItemProvider('html', {
        triggerCharacters: ['"', ' ', '<'],
        provideCompletionItems(model, position) {
            const range = getCompletionRange(model, position);
            const textBeforeCursor = getTextBeforeCursor(model, position);
            const portalPages = options.getPortalPages();

            if (isInsideHrefAttribute(textBeforeCursor)) {
                return { suggestions: buildHrefSlugSuggestions(monaco, range, portalPages) };
            }

            const category = CATEGORY_BY_EDITOR.get(editor);
            if (category) {
                CATEGORY_BY_EDITOR.delete(editor);

                if (category === 'gravitee') {
                    return { suggestions: buildGraviteeSuggestions(monaco, range) };
                }

                return { suggestions: buildRelativeLinkSuggestions(monaco, range, portalPages) };
            }

            return { suggestions: buildCategorySuggestions(monaco, range, categoryCommandId) };
        },
    });

    return {
        dispose: () => {
            completionProvider.dispose();
            categoryCommand.dispose();
            CATEGORY_BY_EDITOR.delete(editor);
        },
    };
}
