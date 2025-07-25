import { IMonacoRange, IMonacoCompletionItem, MonacoCompletionItemKind, MonacoCompletionItemInsertTextRule } from "../../../gravitee-monaco-wrapper/monaco-facade";

export const copyCodeSuggestions = (range: IMonacoRange, needsOpeningTag: boolean = false): IMonacoCompletionItem[] => {
  const prefix = needsOpeningTag ? '<' : '';
  const suffix = needsOpeningTag ? '>' : '';
  
  return [
    // Individual attribute suggestions
    {
      label: 'copy-code:text',
      insertText: 'text="${1:Code to copy}"',
      kind: MonacoCompletionItemKind.Property,
      insertTextRules: MonacoCompletionItemInsertTextRule.InsertAsSnippet,
      range,
      detail: 'Copy code text attribute',
      documentation: 'Sets the text content to be copied to clipboard',
    },
    // Existing snippets with improved documentation
    {
      label: 'copy-code',
      insertText: prefix + 'copy-code text="${1:text}"' + suffix,
      kind: MonacoCompletionItemKind.Snippet,
      insertTextRules: MonacoCompletionItemInsertTextRule.InsertAsSnippet,
      range,
      detail: 'Copy code component with text attribute',
      documentation: 'Inserts a copy-code component that allows users to copy text to clipboard\n\nAvailable attributes:\n• text: Text content to be copied',
    },
    {
      label: 'copy-code (self-closing)',
      insertText: prefix + 'copy-code text="${1:text}" /' + suffix,
      kind: MonacoCompletionItemKind.Snippet,
      insertTextRules: MonacoCompletionItemInsertTextRule.InsertAsSnippet,
      range,
      detail: 'Copy code component (self-closing)',
      documentation: 'Inserts a self-closing copy-code component\n\nAvailable attributes:\n• text: Text content to be copied',
    },
  ];
};