import { IRange, languages } from "monaco-editor";

export const gridSuggestions = (range: IRange, needsOpeningTag: boolean = false): languages.CompletionItem[] => {
  const prefix = needsOpeningTag ? '<' : '';
  const suffix = needsOpeningTag ? '>' : '';
  const closingTag = needsOpeningTag ? '</app-grid>' : '';
  
  return [
    {
      label: 'grid',
      insertText: prefix + 'app-grid columns="${1:3}"' + suffix + '\n\t<app-grid-cell>\n\t\t${2:Cell content goes here...}\n\t</app-grid-cell>\n' + closingTag,
      kind: languages.CompletionItemKind.Snippet,
      insertTextRules: languages.CompletionItemInsertTextRule.InsertAsSnippet,
      range,
      detail: 'Basic grid component',
      documentation: 'Creates a responsive grid with configurable columns',
    },
    {
      label: 'grid-with-markdown',
      insertText: prefix + 'app-grid columns="${1:3}"' + suffix + '\n\t<app-grid-cell markdown="true">\n\t\t${2:Markdown content goes here...}\n\t</app-grid-cell>\n' + closingTag,
      kind: languages.CompletionItemKind.Snippet,
      insertTextRules: languages.CompletionItemInsertTextRule.InsertAsSnippet,
      range,
      detail: 'Grid with markdown cells',
      documentation: 'Creates a grid with cells that render markdown content',
    },
    {
      label: 'grid-2-columns',
      insertText: prefix + 'app-grid columns="2"' + suffix + '\n\t<app-grid-cell>\n\t\t${1:First cell content}\n\t</app-grid-cell>\n\t<app-grid-cell>\n\t\t${2:Second cell content}\n\t</app-grid-cell>\n' + closingTag,
      kind: languages.CompletionItemKind.Snippet,
      insertTextRules: languages.CompletionItemInsertTextRule.InsertAsSnippet,
      range,
      detail: '2-column grid',
      documentation: 'Creates a 2-column responsive grid',
    },
    {
      label: 'grid-3-columns',
      insertText: prefix + 'app-grid columns="3"' + suffix + '\n\t<app-grid-cell>\n\t\t${1:First cell content}\n\t</app-grid-cell>\n\t<app-grid-cell>\n\t\t${2:Second cell content}\n\t</app-grid-cell>\n\t<app-grid-cell>\n\t\t${3:Third cell content}\n\t</app-grid-cell>\n' + closingTag,
      kind: languages.CompletionItemKind.Snippet,
      insertTextRules: languages.CompletionItemInsertTextRule.InsertAsSnippet,
      range,
      detail: '3-column grid',
      documentation: 'Creates a 3-column responsive grid',
    },
    {
      label: 'grid-4-columns',
      insertText: prefix + 'app-grid columns="4"' + suffix + '\n\t<app-grid-cell>\n\t\t${1:First cell content}\n\t</app-grid-cell>\n\t<app-grid-cell>\n\t\t${2:Second cell content}\n\t</app-grid-cell>\n\t<app-grid-cell>\n\t\t${3:Third cell content}\n\t</app-grid-cell>\n\t<app-grid-cell>\n\t\t${4:Fourth cell content}\n\t</app-grid-cell>\n' + closingTag,
      kind: languages.CompletionItemKind.Snippet,
      insertTextRules: languages.CompletionItemInsertTextRule.InsertAsSnippet,
      range,
      detail: '4-column grid',
      documentation: 'Creates a 4-column responsive grid',
    },
    {
      label: 'grid-styled',
      insertText: prefix + 'app-grid columns="${1:3}" gap="${2:large}" align="${3:center}"' + suffix + '\n\t<app-grid-cell>\n\t\t${4:Cell content goes here...}\n\t</app-grid-cell>\n' + closingTag,
      kind: languages.CompletionItemKind.Snippet,
      insertTextRules: languages.CompletionItemInsertTextRule.InsertAsSnippet,
      range,
      detail: 'Styled grid component',
      documentation: 'Creates a grid with custom gap and alignment',
    },
    {
      label: 'grid-with-cards',
      insertText: prefix + 'app-grid columns="${1:3}" gap="${2:large}"' + suffix + '\n\t<app-grid-cell>\n\t\t<app-card title="${3:Card 1}" centered="true">\n\t\t\t${4:Card content}\n\t\t</app-card>\n\t</app-grid-cell>\n\t<app-grid-cell>\n\t\t<app-card title="${5:Card 2}" centered="true">\n\t\t\t${6:Card content}\n\t\t</app-card>\n\t</app-grid-cell>\n\t<app-grid-cell>\n\t\t<app-card title="${7:Card 3}" centered="true">\n\t\t\t${8:Card content}\n\t\t</app-card>\n\t</app-grid-cell>\n' + closingTag,
      kind: languages.CompletionItemKind.Snippet,
      insertTextRules: languages.CompletionItemInsertTextRule.InsertAsSnippet,
      range,
      detail: 'Grid with cards',
      documentation: 'Creates a grid containing card components',
    },
    {
      label: 'grid-feature-showcase',
      insertText: prefix + 'app-grid columns="3" gap="large" align="center"' + suffix + '\n\t<app-grid-cell span="2" backgroundColor="primary" padding="large">\n\t\t## Featured Content\n\t\tThis cell spans 2 columns and has a primary background.\n\t</app-grid-cell>\n\t<app-grid-cell backgroundColor="light" shadow="elevated">\n\t\t### Side Content\n\t\tThis cell has a light background and elevated shadow.\n\t</app-grid-cell>\n\t<app-grid-cell backgroundColor="success" padding="large">\n\t\t### Success Content\n\t\tThis cell has a success background.\n\t</app-grid-cell>\n\t<app-grid-cell backgroundColor="warning" padding="large">\n\t\t### Warning Content\n\t\tThis cell has a warning background.\n\t</app-grid-cell>\n' + closingTag,
      kind: languages.CompletionItemKind.Snippet,
      insertTextRules: languages.CompletionItemInsertTextRule.InsertAsSnippet,
      range,
      detail: 'Feature showcase grid',
      documentation: 'Creates a grid showcasing various cell styling options',
    },
  ];
};

export const gridCellSuggestions = (range: IRange, needsOpeningTag: boolean = false): languages.CompletionItem[] => {
  const prefix = needsOpeningTag ? '<' : '';
  const suffix = needsOpeningTag ? '>' : '';
  const closingTag = needsOpeningTag ? '</app-grid-cell>' : '';
  
  return [
    {
      label: 'grid-cell',
      insertText: prefix + 'app-grid-cell' + suffix + '\n\t${1:Cell content goes here...}\n' + closingTag,
      kind: languages.CompletionItemKind.Snippet,
      insertTextRules: languages.CompletionItemInsertTextRule.InsertAsSnippet,
      range,
      detail: 'Basic grid cell',
      documentation: 'Creates a basic grid cell with content projection',
    },
    {
      label: 'grid-cell-markdown',
      insertText: prefix + 'app-grid-cell markdown="true"' + suffix + '\n\t${1:Markdown content goes here...}\n' + closingTag,
      kind: languages.CompletionItemKind.Snippet,
      insertTextRules: languages.CompletionItemInsertTextRule.InsertAsSnippet,
      range,
      detail: 'Grid cell with markdown rendering',
      documentation: 'Creates a grid cell that renders its content as markdown',
    },
    {
      label: 'grid-cell-styled',
      insertText: prefix + 'app-grid-cell span="${1:1}" padding="${2:medium}" border="true" shadow="${3:small}" backgroundColor="${4:light}"' + suffix + '\n\t${5:Cell content goes here...}\n' + closingTag,
      kind: languages.CompletionItemKind.Snippet,
      insertTextRules: languages.CompletionItemInsertTextRule.InsertAsSnippet,
      range,
      detail: 'Styled grid cell',
      documentation: 'Creates a grid cell with custom styling',
    },
    {
      label: 'grid-cell-wide',
      insertText: prefix + 'app-grid-cell span="2" backgroundColor="primary" padding="large"' + suffix + '\n\t## Wide Cell Content\n\t${1:This cell spans 2 columns and has a primary background.}\n' + closingTag,
      kind: languages.CompletionItemKind.Snippet,
      insertTextRules: languages.CompletionItemInsertTextRule.InsertAsSnippet,
      range,
      detail: 'Wide grid cell',
      documentation: 'Creates a grid cell that spans 2 columns',
    },
    {
      label: 'grid-cell-featured',
      insertText: prefix + 'app-grid-cell span="3" backgroundColor="success" padding="large" shadow="elevated"' + suffix + '\n\t## Featured Content\n\t${1:This cell spans 3 columns and has elevated styling.}\n' + closingTag,
      kind: languages.CompletionItemKind.Snippet,
      insertTextRules: languages.CompletionItemInsertTextRule.InsertAsSnippet,
      range,
      detail: 'Featured grid cell',
      documentation: 'Creates a featured grid cell spanning 3 columns',
    },
    {
      label: 'grid-cell-with-card',
      insertText: prefix + 'app-grid-cell padding="medium"' + suffix + '\n\t<app-card title="${1:Card Title}" centered="true">\n\t\t${2:Card content goes here...}\n\t</app-card>\n' + closingTag,
      kind: languages.CompletionItemKind.Snippet,
      insertTextRules: languages.CompletionItemInsertTextRule.InsertAsSnippet,
      range,
      detail: 'Grid cell with card',
      documentation: 'Creates a grid cell containing a card component',
    },
    {
      label: 'grid-cell-with-image',
      insertText: prefix + 'app-grid-cell padding="medium" backgroundColor="light"' + suffix + '\n\t<app-image src="${1:/path/to/image.jpg}" alt="${2:Image description}" centered="true" rounded="true">\n\t</app-image>\n\t${3:Image caption or additional content}\n' + closingTag,
      kind: languages.CompletionItemKind.Snippet,
      insertTextRules: languages.CompletionItemInsertTextRule.InsertAsSnippet,
      range,
      detail: 'Grid cell with image',
      documentation: 'Creates a grid cell containing an image component',
    },
  ];
}; 