import { IMonacoRange, IMonacoCompletionItem, MonacoCompletionItemKind, MonacoCompletionItemInsertTextRule } from "../../../gravitee-monaco-wrapper/monaco-facade";

export const buttonSuggestions = (range: IMonacoRange, needsOpeningTag: boolean = false): IMonacoCompletionItem[] => {
  const prefix = needsOpeningTag ? '<' : '';
  const suffix = needsOpeningTag ? '>' : '';

  return [
    {
      label: 'button',
      insertText: prefix + 'app-button href="${1:url}" text="${2:Button Text}"' + suffix + '${3:Button Text}' + (needsOpeningTag ? '</app-button>' : ''),
      kind: MonacoCompletionItemKind.Snippet,
      insertTextRules: MonacoCompletionItemInsertTextRule.InsertAsSnippet,
      range,
      detail: 'Button component with href and text',
      documentation: 'Creates a button that links to a URL with customizable text',
    },
    {
      label: 'button-filled',
      insertText: prefix + 'app-button href="${1:url}" text="${2:Button Text}" variant="filled"' + suffix + '${3:Button Text}' + (needsOpeningTag ? '</app-button>' : ''),
      kind: MonacoCompletionItemKind.Snippet,
      insertTextRules: MonacoCompletionItemInsertTextRule.InsertAsSnippet,
      range,
      detail: 'Filled button component',
      documentation: 'Creates a filled button with solid background color',
    },
    {
      label: 'button-outlined',
      insertText: prefix + 'app-button href="${1:url}" text="${2:Button Text}" variant="outlined"' + suffix + '${3:Button Text}' + (needsOpeningTag ? '</app-button>' : ''),
      kind: MonacoCompletionItemKind.Snippet,
      insertTextRules: MonacoCompletionItemInsertTextRule.InsertAsSnippet,
      range,
      detail: 'Outlined button component',
      documentation: 'Creates an outlined button with border and transparent background',
    },
    {
      label: 'button-text',
      insertText: prefix + 'app-button href="${1:url}" text="${2:Button Text}" variant="text"' + suffix + '${3:Button Text}' + (needsOpeningTag ? '</app-button>' : ''),
      kind: MonacoCompletionItemKind.Snippet,
      insertTextRules: MonacoCompletionItemInsertTextRule.InsertAsSnippet,
      range,
      detail: 'Text button component',
      documentation: 'Creates a text button with no background or border',
    },
    {
      label: 'button-external',
      insertText: prefix + 'app-button href="${1:url}" text="${2:Button Text}" type="external"' + suffix + '${3:Button Text}' + (needsOpeningTag ? '</app-button>' : ''),
      kind: MonacoCompletionItemKind.Snippet,
      insertTextRules: MonacoCompletionItemInsertTextRule.InsertAsSnippet,
      range,
      detail: 'External link button',
      documentation: 'Creates a button that opens the link in a new tab',
    },
    {
      label: 'button-custom-styled',
      insertText: prefix + 'app-button href="${1:url}" text="${2:Button Text}" variant="${3:filled}" backgroundColor="${4:#1976d2}" textColor="${5:#ffffff}" borderRadius="${6:8px}"' + suffix + '${7:Button Text}' + (needsOpeningTag ? '</app-button>' : ''),
      kind: MonacoCompletionItemKind.Snippet,
      insertTextRules: MonacoCompletionItemInsertTextRule.InsertAsSnippet,
      range,
      detail: 'Custom styled button',
      documentation: 'Creates a button with custom background color, text color, and border radius',
    },
  ];
};
