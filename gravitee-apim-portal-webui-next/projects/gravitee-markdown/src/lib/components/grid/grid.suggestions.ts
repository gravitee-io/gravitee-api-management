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

export const gridSuggestions: ComponentSuggestion[] = [
  // Basic grid suggestion
  {
    label: 'grid',
    kind: Monaco.languages.CompletionItemKind.Snippet,
    insertText: `<grid columns="2">
    <cell>
        <h3>Column 1</h3>
        <p>First column content</p>
    </cell>
    <cell>
        <h3>Column 2</h3>
        <p>Second column content</p>
    </cell>
</grid>`,
    insertTextRules: Monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet,
    detail: 'Basic grid component',
    documentation: {
      value: 'Creates a basic grid component. You can specify the number of columns using the columns attribute.',
    },
  },
  // Grid with 1 column
  {
    label: 'grid-1-columns',
    kind: Monaco.languages.CompletionItemKind.Snippet,
    insertText: `<grid columns="1">
    <cell>
        <h3>Column 1</h3>
        <p>Content for column 1</p>
    </cell>
</grid>`,
    insertTextRules: Monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet,
    detail: 'Grid with 1 column',
    documentation: {
      value: 'Creates a responsive grid layout with 1 column. The grid will automatically adjust to different screen sizes.',
    },
  },
  // Grid with 2 columns
  {
    label: 'grid-2-columns',
    kind: Monaco.languages.CompletionItemKind.Snippet,
    insertText: `<grid columns="2">
    <cell>
        <h3>Column 1</h3>
        <p>Content for column 1</p>
    </cell>
    <cell>
        <h3>Column 2</h3>
        <p>Content for column 2</p>
    </cell>
</grid>`,
    insertTextRules: Monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet,
    detail: 'Grid with 2 columns',
    documentation: {
      value: 'Creates a responsive grid layout with 2 columns. The grid will automatically adjust to different screen sizes.',
    },
  },
  // Grid with 3 columns
  {
    label: 'grid-3-columns',
    kind: Monaco.languages.CompletionItemKind.Snippet,
    insertText: `<grid columns="3">
    <cell>
        <h3>Column 1</h3>
        <p>Content for column 1</p>
    </cell>
    <cell>
        <h3>Column 2</h3>
        <p>Content for column 2</p>
    </cell>
    <cell>
        <h3>Column 3</h3>
        <p>Content for column 3</p>
    </cell>
</grid>`,
    insertTextRules: Monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet,
    detail: 'Grid with 3 columns',
    documentation: {
      value: 'Creates a responsive grid layout with 3 columns. The grid will automatically adjust to different screen sizes.',
    },
  },
  // Grid with 4 columns
  {
    label: 'grid-4-columns',
    kind: Monaco.languages.CompletionItemKind.Snippet,
    insertText: `<grid columns="4">
    <cell>
        <h3>Column 1</h3>
        <p>Content for column 1</p>
    </cell>
    <cell>
        <h3>Column 2</h3>
        <p>Content for column 2</p>
    </cell>
    <cell>
        <h3>Column 3</h3>
        <p>Content for column 3</p>
    </cell>
    <cell>
        <h3>Column 4</h3>
        <p>Content for column 4</p>
    </cell>
</grid>`,
    insertTextRules: Monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet,
    detail: 'Grid with 4 columns',
    documentation: {
      value: 'Creates a responsive grid layout with 4 columns. The grid will automatically adjust to different screen sizes.',
    },
  },
  // Grid with 5 columns
  {
    label: 'grid-5-columns',
    kind: Monaco.languages.CompletionItemKind.Snippet,
    insertText: `<grid columns="5">
    <cell>
        <h3>Column 1</h3>
        <p>Content for column 1</p>
    </cell>
    <cell>
        <h3>Column 2</h3>
        <p>Content for column 2</p>
    </cell>
    <cell>
        <h3>Column 3</h3>
        <p>Content for column 3</p>
    </cell>
    <cell>
        <h3>Column 4</h3>
        <p>Content for column 4</p>
    </cell>
    <cell>
        <h3>Column 5</h3>
        <p>Content for column 5</p>
    </cell>
</grid>`,
    insertTextRules: Monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet,
    detail: 'Grid with 5 columns',
    documentation: {
      value: 'Creates a responsive grid layout with 5 columns. The grid will automatically adjust to different screen sizes.',
    },
  },
  // Grid with 6 columns
  {
    label: 'grid-6-columns',
    kind: Monaco.languages.CompletionItemKind.Snippet,
    insertText: `<grid columns="6">
    <cell>
        <h3>Column 1</h3>
        <p>Content for column 1</p>
    </cell>
    <cell>
        <h3>Column 2</h3>
        <p>Content for column 2</p>
    </cell>
    <cell>
        <h3>Column 3</h3>
        <p>Content for column 3</p>
    </cell>
    <cell>
        <h3>Column 4</h3>
        <p>Content for column 4</p>
    </cell>
    <cell>
        <h3>Column 5</h3>
        <p>Content for column 5</p>
    </cell>
    <cell>
        <h3>Column 6</h3>
        <p>Content for column 6</p>
    </cell>
</grid>`,
    insertTextRules: Monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet,
    detail: 'Grid with 6 columns',
    documentation: {
      value: 'Creates a responsive grid layout with 6 columns. The grid will automatically adjust to different screen sizes.',
    },
  },
  // Cell suggestion
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
];
