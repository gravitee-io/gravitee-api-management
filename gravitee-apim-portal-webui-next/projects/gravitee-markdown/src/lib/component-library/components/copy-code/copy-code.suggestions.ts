import { IMonacoRange, IMonacoCompletionItem, MonacoCompletionItemKind, MonacoCompletionItemInsertTextRule } from "../../../gravitee-monaco-wrapper/monaco-facade";

export const copyCodeSuggestions = (range: IMonacoRange, needsOpeningTag: boolean = false): IMonacoCompletionItem[] => {
  const prefix = needsOpeningTag ? '<' : '';
  const suffix = needsOpeningTag ? '>' : '';
  
  return [
    {
      label: 'copy-code',
      insertText: prefix + 'copy-code text="${1:text}"' + suffix,
      kind: MonacoCompletionItemKind.Snippet,
      insertTextRules: MonacoCompletionItemInsertTextRule.InsertAsSnippet,
      range,
      detail: 'Copy code component with text attribute',
      documentation: 'Inserts a copy-code component that allows users to copy text to clipboard',
    },
    {
      label: 'copy-code (self-closing)',
      insertText: prefix + 'copy-code text="${1:text}" /' + suffix,
      kind: MonacoCompletionItemKind.Snippet,
      insertTextRules: MonacoCompletionItemInsertTextRule.InsertAsSnippet,
      range,
      detail: 'Copy code component (self-closing)',
      documentation: 'Inserts a self-closing copy-code component',
    },
  ];
};