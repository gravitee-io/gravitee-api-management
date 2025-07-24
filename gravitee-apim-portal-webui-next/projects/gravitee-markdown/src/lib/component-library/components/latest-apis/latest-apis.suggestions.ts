import { IMonacoRange, IMonacoCompletionItem, MonacoCompletionItemKind, MonacoCompletionItemInsertTextRule } from "../../../gravitee-monaco-wrapper/monaco-facade";

export const latestApisSuggestions = (range: IMonacoRange, needsOpeningTag: boolean = false): IMonacoCompletionItem[] => {
  const prefix = needsOpeningTag ? '<' : '';
  const suffix = needsOpeningTag ? '>' : '';

  return [
    {
      label: 'latest-apis',
      insertText: prefix + 'app-latest-apis' + suffix + (needsOpeningTag ? '</app-latest-apis>' : ''),
      kind: MonacoCompletionItemKind.Snippet,
      insertTextRules: MonacoCompletionItemInsertTextRule.InsertAsSnippet,
      range,
      detail: 'Latest APIs component',
      documentation: 'Displays a grid of latest APIs with cards and action buttons',
    },
    {
      label: 'latest-apis-basic',
      insertText: prefix + 'app-latest-apis title="${1:Latest APIs}" [maxApis]="${2:5}"' + suffix + (needsOpeningTag ? '</app-latest-apis>' : ''),
      kind: MonacoCompletionItemKind.Snippet,
      insertTextRules: MonacoCompletionItemInsertTextRule.InsertAsSnippet,
      range,
      detail: 'Basic latest APIs component',
      documentation: 'Creates a latest APIs component with custom title and number of APIs',
    },
    {
      label: 'latest-apis-featured',
      insertText: prefix + 'app-latest-apis title="${1:Featured APIs}" subtitle="${2:Discover our most popular APIs}" [maxApis]="${3:4}" category="${4:featured}"' + suffix + (needsOpeningTag ? '</app-latest-apis>' : ''),
      kind: MonacoCompletionItemKind.Snippet,
      insertTextRules: MonacoCompletionItemInsertTextRule.InsertAsSnippet,
      range,
      detail: 'Featured APIs component',
      documentation: 'Creates a featured APIs component with subtitle and category filtering',
    },
    {
      label: 'latest-apis-search',
      insertText: prefix + 'app-latest-apis title="${1:Search Results}" searchQuery="${2:payment}" [maxApis]="${3:6}" actionButtonText="${4:View API}"' + suffix + (needsOpeningTag ? '</app-latest-apis>' : ''),
      kind: MonacoCompletionItemKind.Snippet,
      insertTextRules: MonacoCompletionItemInsertTextRule.InsertAsSnippet,
      range,
      detail: 'Search results APIs component',
      documentation: 'Creates a search results component with query filtering and custom button text',
    },
    {
      label: 'latest-apis-custom-styled',
      insertText: prefix + 'app-latest-apis title="${1:Custom APIs}" [cardElevation]="${2:3}" cardBackgroundColor="${3:#f8f9fa}" actionButtonVariant="${4:outlined}" actionButtonText="${5:Explore API}"' + suffix + (needsOpeningTag ? '</app-latest-apis>' : ''),
      kind: MonacoCompletionItemKind.Snippet,
      insertTextRules: MonacoCompletionItemInsertTextRule.InsertAsSnippet,
      range,
      detail: 'Custom styled latest APIs component',
      documentation: 'Creates a latest APIs component with custom card styling and button appearance',
    },
    {
      label: 'latest-apis-external-links',
      insertText: prefix + 'app-latest-apis title="${1:External APIs}" actionButtonType="${2:external}" actionButtonText="${3:Visit API}" [maxApis]="${4:3}"' + suffix + (needsOpeningTag ? '</app-latest-apis>' : ''),
      kind: MonacoCompletionItemKind.Snippet,
      insertTextRules: MonacoCompletionItemInsertTextRule.InsertAsSnippet,
      range,
      detail: 'External links latest APIs component',
      documentation: 'Creates a latest APIs component with external link buttons that open in new tabs',
    },
    {
      label: 'latest-apis-minimal',
      insertText: prefix + 'app-latest-apis [maxApis]="${1:3}" [cardElevation]="${2:0}" actionButtonVariant="${3:text}"' + suffix + (needsOpeningTag ? '</app-latest-apis>' : ''),
      kind: MonacoCompletionItemKind.Snippet,
      insertTextRules: MonacoCompletionItemInsertTextRule.InsertAsSnippet,
      range,
      detail: 'Minimal latest APIs component',
      documentation: 'Creates a minimal latest APIs component with no elevation and text buttons',
    },
  ];
}; 