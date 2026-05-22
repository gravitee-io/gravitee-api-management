/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import type * as monacoNs from 'monaco-editor';

export const GAPL_LANGUAGE_ID = 'gapl';

export function registerGaplLanguage(monaco: typeof monacoNs): void {
    if (monaco.languages.getLanguages().some(l => l.id === GAPL_LANGUAGE_ID)) {
        return;
    }

    monaco.languages.register({ id: GAPL_LANGUAGE_ID });

    monaco.languages.setMonarchTokensProvider(GAPL_LANGUAGE_ID, {
        defaultToken: '',
        tokenPostfix: '.gapl',
        keywords: ['permit', 'forbid', 'principal', 'action', 'resource', 'when', 'entity', 'in', 'appliesTo'],
        typeKeywords: ['String', 'Boolean', 'Long', 'Double'],
        operators: ['==', '!=', '<=', '>=', '<', '>', '&&', '||', '!', '='],
        symbols: /[=><!&|+\-*/%]+/,
        tokenizer: {
            root: [
                [/\/\/.*$/, 'comment'],
                [/"([^"\\]|\\.)*"/, 'string'],
                [
                    /[A-Za-z_][\w]*/,
                    {
                        cases: {
                            '@keywords': 'keyword',
                            '@typeKeywords': 'type',
                            '@default': 'identifier',
                        },
                    },
                ],
                [/\d+/, 'number'],
                [/[{}()[\],;:]/, 'delimiter'],
                [/@symbols/, { cases: { '@operators': 'operator', '@default': '' } }],
                [/\s+/, 'white'],
            ],
        },
    });

    monaco.languages.setLanguageConfiguration(GAPL_LANGUAGE_ID, {
        comments: { lineComment: '//' },
        brackets: [
            ['{', '}'],
            ['[', ']'],
            ['(', ')'],
        ],
        autoClosingPairs: [
            { open: '{', close: '}' },
            { open: '[', close: ']' },
            { open: '(', close: ')' },
            { open: '"', close: '"' },
        ],
    });
}
