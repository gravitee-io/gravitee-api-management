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
