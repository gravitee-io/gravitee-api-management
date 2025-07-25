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
    // Individual attribute suggestions
    {
      label: 'image:src',
      insertText: 'src="${1:https://example.com/image.jpg}"',
      kind: MonacoCompletionItemKind.Property,
      insertTextRules: MonacoCompletionItemInsertTextRule.InsertAsSnippet,
      range,
      detail: 'Image source attribute',
      documentation: 'Sets the source URL of the image',
    },
    {
      label: 'image:alt',
      insertText: 'alt="${1:Image description}"',
      kind: MonacoCompletionItemKind.Property,
      insertTextRules: MonacoCompletionItemInsertTextRule.InsertAsSnippet,
      range,
      detail: 'Image alt text attribute',
      documentation: 'Sets the alternative text for accessibility',
    },
    {
      label: 'image:centered',
      insertText: 'centered="true"',
      kind: MonacoCompletionItemKind.Property,
      insertTextRules: MonacoCompletionItemInsertTextRule.InsertAsSnippet,
      range,
      detail: 'Image centered attribute',
      documentation: 'Centers the image horizontally (boolean)',
    },
    {
      label: 'image:rounded',
      insertText: 'rounded="${1|none,sm,md,lg,full|}"',
      kind: MonacoCompletionItemKind.Property,
      insertTextRules: MonacoCompletionItemInsertTextRule.InsertAsSnippet,
      range,
      detail: 'Image rounded attribute',
      documentation: 'Sets the border radius style (none, sm, md, lg, full)',
    },
    {
      label: 'image:maxWidth',
      insertText: 'maxWidth="${1:100%}"',
      kind: MonacoCompletionItemKind.Property,
      insertTextRules: MonacoCompletionItemInsertTextRule.InsertAsSnippet,
      range,
      detail: 'Image max width',
      documentation: 'Sets the maximum width of the image (CSS length value)',
    },
    {
      label: 'image:maxHeight',
      insertText: 'maxHeight="${1:auto}"',
      kind: MonacoCompletionItemKind.Property,
      insertTextRules: MonacoCompletionItemInsertTextRule.InsertAsSnippet,
      range,
      detail: 'Image max height',
      documentation: 'Sets the maximum height of the image (CSS length value)',
    },
    {
      label: 'image:width',
      insertText: 'width="${1:auto}"',
      kind: MonacoCompletionItemKind.Property,
      insertTextRules: MonacoCompletionItemInsertTextRule.InsertAsSnippet,
      range,
      detail: 'Image width',
      documentation: 'Sets the width of the image (CSS length value)',
    },
    {
      label: 'image:height',
      insertText: 'height="${1:auto}"',
      kind: MonacoCompletionItemKind.Property,
      insertTextRules: MonacoCompletionItemInsertTextRule.InsertAsSnippet,
      range,
      detail: 'Image height',
      documentation: 'Sets the height of the image (CSS length value)',
    },
    // Existing snippets with improved documentation
    {
      label: 'image',
      insertText: prefix + 'app-image src="${1:https://example.com/image.jpg}" alt="${2:Image description}"' + suffix + closingTag,
      kind: MonacoCompletionItemKind.Snippet,
      insertTextRules: MonacoCompletionItemInsertTextRule.InsertAsSnippet,
      range,
      detail: 'Basic image component',
      documentation: 'Creates an image with source and alt text\n\nAvailable attributes:\n• src: Image source URL\n• alt: Alternative text\n• centered: Center image (boolean)\n• rounded: Border radius style (none/sm/md/lg/full)\n• maxWidth: Maximum width\n• maxHeight: Maximum height\n• width: Fixed width\n• height: Fixed height',
    },
    {
      label: 'image-centered',
      insertText: prefix + 'app-image src="${1:https://example.com/image.jpg}" alt="${2:Image description}" centered="true"' + suffix + closingTag,
      kind: MonacoCompletionItemKind.Snippet,
      insertTextRules: MonacoCompletionItemInsertTextRule.InsertAsSnippet,
      range,
      detail: 'Centered image',
      documentation: 'Creates a centered image with source and alt text\n\nAvailable attributes:\n• src: Image source URL\n• alt: Alternative text\n• centered: Center image (boolean)\n• rounded: Border radius style (none/sm/md/lg/full)\n• maxWidth: Maximum width\n• maxHeight: Maximum height\n• width: Fixed width\n• height: Fixed height',
    },
    {
      label: 'image-rounded',
      insertText: prefix + 'app-image src="${1:https://example.com/image.jpg}" alt="${2:Image description}" rounded="${3:md}"' + suffix + closingTag,
      kind: MonacoCompletionItemKind.Snippet,
      insertTextRules: MonacoCompletionItemInsertTextRule.InsertAsSnippet,
      range,
      detail: 'Rounded image',
      documentation: 'Creates an image with rounded corners\n\nAvailable attributes:\n• src: Image source URL\n• alt: Alternative text\n• centered: Center image (boolean)\n• rounded: Border radius style (none/sm/md/lg/full)\n• maxWidth: Maximum width\n• maxHeight: Maximum height\n• width: Fixed width\n• height: Fixed height',
    },
    {
      label: 'image-custom-size',
      insertText: prefix + 'app-image src="${1:https://example.com/image.jpg}" alt="${2:Image description}" maxWidth="${3:300px}" maxHeight="${4:200px}"' + suffix + closingTag,
      kind: MonacoCompletionItemKind.Snippet,
      insertTextRules: MonacoCompletionItemInsertTextRule.InsertAsSnippet,
      range,
      detail: 'Image with custom size',
      documentation: 'Creates an image with custom maximum width and height\n\nAvailable attributes:\n• src: Image source URL\n• alt: Alternative text\n• centered: Center image (boolean)\n• rounded: Border radius style (none/sm/md/lg/full)\n• maxWidth: Maximum width\n• maxHeight: Maximum height\n• width: Fixed width\n• height: Fixed height',
    },
    {
      label: 'image-complete',
      insertText: prefix + 'app-image src="${1:https://example.com/image.jpg}" alt="${2:Image description}" centered="true" rounded="${3:lg}" maxWidth="${4:500px}"' + suffix + closingTag,
      kind: MonacoCompletionItemKind.Snippet,
      insertTextRules: MonacoCompletionItemInsertTextRule.InsertAsSnippet,
      range,
      detail: 'Complete image with all options',
      documentation: 'Creates a fully customized image with centering, rounded corners, and size constraints\n\nAvailable attributes:\n• src: Image source URL\n• alt: Alternative text\n• centered: Center image (boolean)\n• rounded: Border radius style (none/sm/md/lg/full)\n• maxWidth: Maximum width\n• maxHeight: Maximum height\n• width: Fixed width\n• height: Fixed height',
    },
    {
      label: 'image-avatar',
      insertText: prefix + 'app-image src="${1:https://example.com/avatar.jpg}" alt="${2:User avatar}" rounded="full" maxWidth="${3:100px}" maxHeight="${4:100px}"' + suffix + closingTag,
      kind: MonacoCompletionItemKind.Snippet,
      insertTextRules: MonacoCompletionItemInsertTextRule.InsertAsSnippet,
      range,
      detail: 'Avatar image',
      documentation: 'Creates a circular avatar image with fixed dimensions\n\nAvailable attributes:\n• src: Image source URL\n• alt: Alternative text\n• centered: Center image (boolean)\n• rounded: Border radius style (none/sm/md/lg/full)\n• maxWidth: Maximum width\n• maxHeight: Maximum height\n• width: Fixed width\n• height: Fixed height',
    },
    {
      label: 'image-banner',
      insertText: prefix + 'app-image src="${1:https://example.com/banner.jpg}" alt="${2:Banner image}" centered="true" maxWidth="${3:100%}" maxHeight="${4:300px}"' + suffix + closingTag,
      kind: MonacoCompletionItemKind.Snippet,
      insertTextRules: MonacoCompletionItemInsertTextRule.InsertAsSnippet,
      range,
      detail: 'Banner image',
      documentation: 'Creates a centered banner image with responsive width and fixed height\n\nAvailable attributes:\n• src: Image source URL\n• alt: Alternative text\n• centered: Center image (boolean)\n• rounded: Border radius style (none/sm/md/lg/full)\n• maxWidth: Maximum width\n• maxHeight: Maximum height\n• width: Fixed width\n• height: Fixed height',
    },
    {
      label: 'image-thumbnail',
      insertText: prefix + 'app-image src="${1:https://example.com/thumbnail.jpg}" alt="${2:Thumbnail}" rounded="sm" maxWidth="${3:150px}" maxHeight="${4:150px}"' + suffix + closingTag,
      kind: MonacoCompletionItemKind.Snippet,
      insertTextRules: MonacoCompletionItemInsertTextRule.InsertAsSnippet,
      range,
      detail: 'Thumbnail image',
      documentation: 'Creates a small thumbnail image with slightly rounded corners\n\nAvailable attributes:\n• src: Image source URL\n• alt: Alternative text\n• centered: Center image (boolean)\n• rounded: Border radius style (none/sm/md/lg/full)\n• maxWidth: Maximum width\n• maxHeight: Maximum height\n• width: Fixed width\n• height: Fixed height',
    },
  ];
};
