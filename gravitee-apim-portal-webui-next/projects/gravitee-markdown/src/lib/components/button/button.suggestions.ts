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
  insertText: `<button appearance="filled">$1</button>`,
  detail: 'Filled button component',
};

const outlinedButton: ComponentSuggestion = {
  label: 'Button - Outlined',
  insertText: `<button appearance="outlined">$1</button>`,
  detail: 'Outlined button component',
};

const textButton: ComponentSuggestion = {
  label: 'Button - Text',
  insertText: `<button appearance="text">$1</button>`,
  detail: 'Text button component',
};

export const buttonConfiguration: ComponentSuggestionConfiguration = {
  suggestions: [filledButton, outlinedButton, textButton],
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
  ],
  hoverDocumentation: {
    label: 'Button',
    description: 'A customizable button component with three appearance styles: filled, outlined, and text.',
  },
  attributeHoverDocumentation: {
    appearance: {
      label: 'appearance',
      description: 'The visual style of the button (filled, outlined, text)',
    },
  },
};
