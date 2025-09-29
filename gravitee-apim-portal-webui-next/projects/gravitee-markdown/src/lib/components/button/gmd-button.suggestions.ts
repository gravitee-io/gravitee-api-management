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
import { ComponentSuggestion } from '../../models/componentSuggestion';
import { ComponentSuggestionConfiguration } from '../../models/componentSuggestionConfiguration';

const filledButton: ComponentSuggestion = {
  label: 'Button - Filled',
  insertText: `<gmd-button appearance="filled" link="/$1">$2</gmd-button>`,
  detail: 'Filled button component with internal link',
};

const outlinedButton: ComponentSuggestion = {
  label: 'Button - Outlined',
  insertText: `<gmd-button appearance="outlined" link="/$1">$2</gmd-button>`,
  detail: 'Outlined button component with internal link',
};

const textButton: ComponentSuggestion = {
  label: 'Button - Text',
  insertText: `<gmd-button appearance="text" link="/$1">$2</gmd-button>`,
  detail: 'Text button component with internal link',
};

const filledButtonExternal: ComponentSuggestion = {
  label: 'Button - Filled (External)',
  insertText: `<gmd-button appearance="filled" link="$1" target="_blank">$2</gmd-button>`,
  detail: 'Filled button component with external link',
};

const outlinedButtonExternal: ComponentSuggestion = {
  label: 'Button - Outlined (External)',
  insertText: `<gmd-button appearance="outlined" link="$1" target="_blank">$2</gmd-button>`,
  detail: 'Outlined button component with external link',
};

const textButtonExternal: ComponentSuggestion = {
  label: 'Button - Text (External)',
  insertText: `<gmd-button appearance="text" link="$1" target="_blank">$2</gmd-button>`,
  detail: 'Text button component with external link',
};

export const buttonConfiguration: ComponentSuggestionConfiguration = {
  suggestions: [filledButton, outlinedButton, textButton, filledButtonExternal, outlinedButtonExternal, textButtonExternal],
  attributeSuggestions: [
    {
      label: 'appearance="filled"',
      insertText: 'appearance="filled"',
      detail: 'Filled button style',
    },
    {
      label: 'appearance="outlined"',
      insertText: 'appearance="outlined"',
      detail: 'Outlined button style',
    },
    {
      label: 'appearance="text"',
      insertText: 'appearance="text"',
      detail: 'Text button style',
    },
    {
      label: 'link="/path"',
      insertText: 'link="/$1"',
      detail: 'Internal link path (starts with /)',
    },
    {
      label: 'link="https://example.com"',
      insertText: 'link="$1"',
      detail: 'External link URL',
    },
    {
      label: 'target="_self"',
      insertText: 'target="_self"',
      detail: 'Open link in same tab/window',
    },
    {
      label: 'target="_blank"',
      insertText: 'target="_blank"',
      detail: 'Open link in new tab/window',
    },
  ],
  hoverDocumentation: {
    label: 'Button',
    description:
      'A customizable button component with three appearance styles: filled, outlined, and text. Supports both internal and external links.',
  },
  attributeHoverDocumentation: {
    appearance: 'The visual style of the button (filled, outlined, text)',
    link: 'The URL or path for the button link. Use "/" prefix for internal links or full URLs for external links.',
    target: 'Where to open the link: "_self" for same tab, "_blank" for new tab',
  },
};
