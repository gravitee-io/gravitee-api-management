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
import * as Monaco from 'monaco-editor';

export interface ComponentSuggestion {
  label: string;
  kind: Monaco.languages.CompletionItemKind;
  insertText: string;
  insertTextRules: Monaco.languages.CompletionItemInsertTextRule;
  detail: string;
  documentation: {
    value: string;
  };
}

export const cellSuggestions: ComponentSuggestion[] = [
  // Basic cell suggestion
  {
    label: 'cell',
    kind: Monaco.languages.CompletionItemKind.Snippet,
    insertText: `<cell>
    <h3>Cell Title</h3>
    <p>Cell content goes here</p>
</cell>`,
    insertTextRules: Monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet,
    detail: 'Grid cell component',
    documentation: {
      value: 'Creates a cell component that can be placed inside a grid. Each cell will take up one grid position.',
    },
  },
  // Cell with heading
  {
    label: 'cell-with-heading',
    kind: Monaco.languages.CompletionItemKind.Snippet,
    insertText: `<cell>
    <h2>Section Title</h2>
    <p>This cell contains a main heading and content.</p>
</cell>`,
    insertTextRules: Monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet,
    detail: 'Cell with main heading',
    documentation: {
      value: 'Creates a cell component with a main heading (h2) and content. Useful for important sections.',
    },
  },
  // Cell with subheading
  {
    label: 'cell-with-subheading',
    kind: Monaco.languages.CompletionItemKind.Snippet,
    insertText: `<cell>
    <h3>Subsection Title</h3>
    <p>This cell contains a subheading and content.</p>
</cell>`,
    insertTextRules: Monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet,
    detail: 'Cell with subheading',
    documentation: {
      value: 'Creates a cell component with a subheading (h3) and content. Good for organizing content within a grid.',
    },
  },
  // Cell with list
  {
    label: 'cell-with-list',
    kind: Monaco.languages.CompletionItemKind.Snippet,
    insertText: `<cell>
    <h3>List Title</h3>
    <ul>
        <li>First item</li>
        <li>Second item</li>
        <li>Third item</li>
    </ul>
</cell>`,
    insertTextRules: Monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet,
    detail: 'Cell with unordered list',
    documentation: {
      value: 'Creates a cell component with a heading and an unordered list. Perfect for feature lists or bullet points.',
    },
  },
  // Cell with ordered list
  {
    label: 'cell-with-ordered-list',
    kind: Monaco.languages.CompletionItemKind.Snippet,
    insertText: `<cell>
    <h3>Steps Title</h3>
    <ol>
        <li>First step</li>
        <li>Second step</li>
        <li>Third step</li>
    </ol>
</cell>`,
    insertTextRules: Monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet,
    detail: 'Cell with ordered list',
    documentation: {
      value: 'Creates a cell component with a heading and an ordered list. Great for step-by-step instructions or procedures.',
    },
  },
  // Cell with code
  {
    label: 'cell-with-code',
    kind: Monaco.languages.CompletionItemKind.Snippet,
    insertText: `<cell>
    <h3>Code Example</h3>
    <pre><code>// Your code here
function example() {
    return "Hello, World!";
}</code></pre>
</cell>`,
    insertTextRules: Monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet,
    detail: 'Cell with code block',
    documentation: {
      value: 'Creates a cell component with a heading and a code block. Perfect for displaying code examples or snippets.',
    },
  },
];
