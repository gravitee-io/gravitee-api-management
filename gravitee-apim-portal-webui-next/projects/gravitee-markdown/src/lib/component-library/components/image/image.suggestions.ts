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
import { IMonacoRange, IMonacoCompletionItem, MonacoCompletionItemKind, MonacoCompletionItemInsertTextRule } from '../../../gravitee-monaco-wrapper/monaco-facade';

export const imageSuggestions = (range: IMonacoRange, needsOpeningTag: boolean = false): IMonacoCompletionItem[] => {
  const prefix = needsOpeningTag ? '<' : '';
  const suffix = needsOpeningTag ? '>' : '';
  const closingTag = needsOpeningTag ? '</app-image>' : '';

  return [
    {
      label: 'image',
      insertText: prefix + 'app-image src="${1:https://example.com/image.jpg}" alt="${2:Image description}"' + suffix + closingTag,
      kind: MonacoCompletionItemKind.Snippet,
      insertTextRules: MonacoCompletionItemInsertTextRule.InsertAsSnippet,
      range,
      detail: 'Basic image component',
      documentation: 'Creates an image with source and alt text',
    },
    {
      label: 'image-centered',
      insertText: prefix + 'app-image src="${1:https://example.com/image.jpg}" alt="${2:Image description}" centered="true"' + suffix + closingTag,
      kind: MonacoCompletionItemKind.Snippet,
      insertTextRules: MonacoCompletionItemInsertTextRule.InsertAsSnippet,
      range,
      detail: 'Centered image',
      documentation: 'Creates a centered image with source and alt text',
    },
    {
      label: 'image-rounded',
      insertText: prefix + 'app-image src="${1:https://example.com/image.jpg}" alt="${2:Image description}" rounded="${3:md}"' + suffix + closingTag,
      kind: MonacoCompletionItemKind.Snippet,
      insertTextRules: MonacoCompletionItemInsertTextRule.InsertAsSnippet,
      range,
      detail: 'Rounded image',
      documentation: 'Creates an image with rounded corners (sm, md, lg, full)',
    },
    {
      label: 'image-custom-size',
      insertText: prefix + 'app-image src="${1:https://example.com/image.jpg}" alt="${2:Image description}" maxWidth="${3:300px}" maxHeight="${4:200px}"' + suffix + closingTag,
      kind: MonacoCompletionItemKind.Snippet,
      insertTextRules: MonacoCompletionItemInsertTextRule.InsertAsSnippet,
      range,
      detail: 'Image with custom size',
      documentation: 'Creates an image with custom maximum width and height',
    },
    {
      label: 'image-complete',
      insertText: prefix + 'app-image src="${1:https://example.com/image.jpg}" alt="${2:Image description}" centered="true" rounded="${3:lg}" maxWidth="${4:500px}"' + suffix + closingTag,
      kind: MonacoCompletionItemKind.Snippet,
      insertTextRules: MonacoCompletionItemInsertTextRule.InsertAsSnippet,
      range,
      detail: 'Complete image with all options',
      documentation: 'Creates a fully customized image with centering, rounded corners, and size constraints',
    },
    {
      label: 'image-avatar',
      insertText: prefix + 'app-image src="${1:https://example.com/avatar.jpg}" alt="${2:User avatar}" rounded="full" maxWidth="${3:100px}" maxHeight="${4:100px}"' + suffix + closingTag,
      kind: MonacoCompletionItemKind.Snippet,
      insertTextRules: MonacoCompletionItemInsertTextRule.InsertAsSnippet,
      range,
      detail: 'Avatar image',
      documentation: 'Creates a circular avatar image with fixed dimensions',
    },
    {
      label: 'image-banner',
      insertText: prefix + 'app-image src="${1:https://example.com/banner.jpg}" alt="${2:Banner image}" centered="true" maxWidth="${3:100%}" maxHeight="${4:300px}"' + suffix + closingTag,
      kind: MonacoCompletionItemKind.Snippet,
      insertTextRules: MonacoCompletionItemInsertTextRule.InsertAsSnippet,
      range,
      detail: 'Banner image',
      documentation: 'Creates a centered banner image with responsive width and fixed height',
    },
    {
      label: 'image-thumbnail',
      insertText: prefix + 'app-image src="${1:https://example.com/thumbnail.jpg}" alt="${2:Thumbnail}" rounded="sm" maxWidth="${3:150px}" maxHeight="${4:150px}"' + suffix + closingTag,
      kind: MonacoCompletionItemKind.Snippet,
      insertTextRules: MonacoCompletionItemInsertTextRule.InsertAsSnippet,
      range,
      detail: 'Thumbnail image',
      documentation: 'Creates a small thumbnail image with slightly rounded corners',
    },
  ];
};
