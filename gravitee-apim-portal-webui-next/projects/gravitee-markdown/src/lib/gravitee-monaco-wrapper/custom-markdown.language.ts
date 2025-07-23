/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import * as monaco from 'monaco-editor';

// Custom language ID
export const CUSTOM_MARKDOWN_LANGUAGE_ID = 'custom-markdown';

// Custom components that should be highlighted as HTML tags
const CUSTOM_COMPONENTS = [
  'app-card',
  'card-actions', 
  'app-button',
  'copy-code'
];

// Register the custom language
export function registerCustomMarkdownLanguage(): void {
  // Register the language
  monaco.languages.register({ id: CUSTOM_MARKDOWN_LANGUAGE_ID });

  // Set the language configuration
  monaco.languages.setLanguageConfiguration(CUSTOM_MARKDOWN_LANGUAGE_ID, {
    comments: {
      lineComment: '<!--',
      blockComment: ['<!--', '-->']
    },
    brackets: [
      ['{', '}'],
      ['[', ']'],
      ['(', ')'],
      ['<', '>']
    ],
    autoClosingPairs: [
      { open: '{', close: '}' },
      { open: '[', close: ']' },
      { open: '(', close: ')' },
      { open: '<', close: '>' },
      { open: '"', close: '"' },
      { open: "'", close: "'" }
    ],
    surroundingPairs: [
      { open: '{', close: '}' },
      { open: '[', close: ']' },
      { open: '(', close: ')' },
      { open: '<', close: '>' },
      { open: '"', close: '"' },
      { open: "'", close: "'" }
    ],
    folding: {
      markers: {
        start: new RegExp('^\\s*<!--\\s*#?region\\b.*-->'),
        end: new RegExp('^\\s*<!--\\s*#?endregion\\b.*-->')
      }
    }
  });

  // Set the monarch language definition
  monaco.languages.setMonarchTokensProvider(CUSTOM_MARKDOWN_LANGUAGE_ID, {
    // Set defaultToken to invalid to see what you do not tokenize as a default state
    defaultToken: '',

    tokenizer: {
      root: [
        // Markdown headers
        [/^(#{1,6})\s+(.*)$/, 'heading'],
        
        // Markdown emphasis
        [/\*\*(.*?)\*\*/, 'strong'],
        [/\*(.*?)\*/, 'emphasis'],
        [/__(.*?)__/, 'strong'],
        [/_(.*?)_/, 'emphasis'],
        
        // Markdown code blocks
        [/```[\s\S]*?```/, 'code'],
        [/`([^`]+)`/, 'code'],
        
        // Markdown links and images
        [/\[([^\]]+)\]\(([^)]+)\)/, 'link'],
        [/!\[([^\]]+)\]\(([^)]+)\)/, 'image'],
        
        // Markdown lists
        [/^(\s*)([-*+]|\d+\.)\s+/, 'list'],
        
        // Markdown blockquotes
        [/^>\s+.*$/, 'quote'],
        
        // HTML tags (including custom components)
        [/<(\/?)([a-zA-Z][a-zA-Z0-9-]*)/, [
          { token: 'tag', next: '@tag.$2' }
        ]],
        
        // HTML attributes
        [/"([^"]*)"/, 'string'],
        [/'([^']*)'/, 'string'],
        
        // Markdown horizontal rules
        [/^([-*_]){3,}$/, 'hr'],
        
        // Everything else
        [/./, 'text']
      ],

      // Tag states for better HTML handling
      tag: [
        { include: '@tagCommon' }
      ],

      tagCommon: [
        // HTML attributes
        [/\s+([a-zA-Z][a-zA-Z0-9-]*)\s*=\s*/, 'attribute'],
        [/\s+([a-zA-Z][a-zA-Z0-9-]*)\s*/, 'attribute'],
        
        // String values
        [/"([^"]*)"/, 'string'],
        [/'([^']*)'/, 'string'],
        
        // Self-closing tags
        [/\//, 'tag'],
        
        // Closing tags
        [/>/, { token: 'tag', next: '@root' }],
        
        // Everything else
        [/./, 'tag']
      ]
    }
  });

  // Define the theme colors for our custom language
  monaco.editor.defineTheme('custom-markdown-theme', {
    base: 'vs',
    inherit: true,
    rules: [
      { token: 'heading', foreground: '000080', fontStyle: 'bold' },
      { token: 'strong', foreground: '000000', fontStyle: 'bold' },
      { token: 'emphasis', foreground: '000000', fontStyle: 'italic' },
      { token: 'code', foreground: 'A31515', background: 'F5F5F5' },
      { token: 'link', foreground: '0000FF' },
      { token: 'image', foreground: '0000FF' },
      { token: 'list', foreground: '000000' },
      { token: 'quote', foreground: '008000', fontStyle: 'italic' },
      { token: 'hr', foreground: '808080' },
      { token: 'tag', foreground: '0000FF' },
      { token: 'attribute', foreground: 'FF0000' },
      { token: 'string', foreground: 'A31515' },
      { token: 'text', foreground: '000000' }
    ],
    colors: {}
  });
} 