import { IRange, languages } from "monaco-editor";

export const cardSuggestions = (range: IRange, needsOpeningTag: boolean = false): languages.CompletionItem[] => {
  const prefix = needsOpeningTag ? '<' : '';
  const suffix = needsOpeningTag ? '>' : '';
  const closingTag = needsOpeningTag ? '</app-card>' : '';
  
  return [
    {
      label: 'card',
      insertText: prefix + 'app-card title="${1:Card Title}"' + suffix + '\n\t${2:Card content goes here...}\n' + closingTag,
      kind: languages.CompletionItemKind.Snippet,
      insertTextRules: languages.CompletionItemInsertTextRule.InsertAsSnippet,
      range,
      detail: 'Basic card component',
      documentation: 'Creates a card with title and content',
    },
    {
      label: 'card-centered',
      insertText: prefix + 'app-card title="${1:Card Title}" centered="true"' + suffix + '\n\t${2:Card content goes here...}\n' + closingTag,
      kind: languages.CompletionItemKind.Snippet,
      insertTextRules: languages.CompletionItemInsertTextRule.InsertAsSnippet,
      range,
      detail: 'Centered card component',
      documentation: 'Creates a card with centered title and content',
    },
    {
      label: 'card-with-actions',
      insertText: prefix + 'app-card title="${1:Card Title}"' + suffix + '\n\t${2:Card content goes here...}\n\t<card-actions>\n\t\t<app-button href="${3:/link1}" text="${4:Action 1}">${4:Action 1}</app-button>\n\t\t<app-button href="${5:/link2}" text="${6:Action 2}" variant="outlined">${6:Action 2}</app-button>\n\t</card-actions>\n' + closingTag,
      kind: languages.CompletionItemKind.Snippet,
      insertTextRules: languages.CompletionItemInsertTextRule.InsertAsSnippet,
      range,
      detail: 'Card with action buttons',
      documentation: 'Creates a card with title, content, and action buttons using card-actions component',
    },
    {
      label: 'card-elevated',
      insertText: prefix + 'app-card title="${1:Card Title}" elevation="${2:3}"' + suffix + '\n\t${3:Card content goes here...}\n' + closingTag,
      kind: languages.CompletionItemKind.Snippet,
      insertTextRules: languages.CompletionItemInsertTextRule.InsertAsSnippet,
      range,
      detail: 'Card with custom elevation',
      documentation: 'Creates a card with custom shadow elevation (1-5)',
    },
    {
      label: 'card-custom-styled',
      insertText: prefix + 'app-card title="${1:Card Title}" backgroundColor="${2:#f8f9fa}" borderColor="${3:#dee2e6}" borderRadius="${4:12px}" borderWidth="${5:2px}"' + suffix + '\n\t${6:Card content goes here...}\n' + closingTag,
      kind: languages.CompletionItemKind.Snippet,
      insertTextRules: languages.CompletionItemInsertTextRule.InsertAsSnippet,
      range,
      detail: 'Custom styled card',
      documentation: 'Creates a card with custom background, border, and border radius',
    },
    {
      label: 'card-complete',
      insertText: prefix + 'app-card title="${1:Card Title}" elevation="${2:3}" backgroundColor="${3:#ffffff}" borderColor="${4:#e0e0e0}" borderRadius="${5:8px}"' + suffix + '\n\t${6:Card content goes here...}\n\t<card-actions>\n\t\t<app-button href="${7:/primary}" text="${8:Primary Action}" variant="filled">${8:Primary Action}</app-button>\n\t\t<app-button href="${9:/secondary}" text="${10:Secondary Action}" variant="outlined">${10:Secondary Action}</app-button>\n\t</card-actions>\n' + closingTag,
      kind: languages.CompletionItemKind.Snippet,
      insertTextRules: languages.CompletionItemInsertTextRule.InsertAsSnippet,
      range,
      detail: 'Complete card with all options',
      documentation: 'Creates a fully customized card with all styling and action options',
    },
    {
      label: 'card-external-actions',
      insertText: prefix + 'app-card title="${1:Card Title}"' + suffix + '\n\t${2:Card content goes here...}\n\t<card-actions>\n\t\t<app-button href="${3:https://docs.example.com}" text="${4:View Docs}" type="external" variant="filled">${4:View Docs}</app-button>\n\t\t<app-button href="${5:https://github.com}" text="${6:GitHub}" type="external" variant="outlined">${6:GitHub}</app-button>\n\t</card-actions>\n' + closingTag,
      kind: languages.CompletionItemKind.Snippet,
      insertTextRules: languages.CompletionItemInsertTextRule.InsertAsSnippet,
      range,
      detail: 'Card with external action links',
      documentation: 'Creates a card with action buttons that open external links in new tabs',
    },
  ];
}; 