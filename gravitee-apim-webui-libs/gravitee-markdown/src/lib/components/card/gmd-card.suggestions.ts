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

const emptyCard: ComponentSuggestion = {
  label: 'Card - with markdown content only',
  insertText: `<gmd-card>
    <gmd-md>$1</gmd-md>
</gmd-card>`,
  detail: 'Card with markdown content only',
};

const cardWithTitle: ComponentSuggestion = {
  label: 'Card - with title and content',
  insertText: `<gmd-card>
    <gmd-card-title>$1</gmd-card-title>
    <gmd-md>$2</gmd-md>
</gmd-card>`,
  detail: 'Card with a title and markdown content',
};

const cardWithTitleAndSubtitle: ComponentSuggestion = {
  label: 'Card - with title, subtitle and content',
  insertText: `<gmd-card>
    <gmd-card-title>$1</gmd-card-title>
    <gmd-card-subtitle>$2</gmd-card-subtitle>
    <gmd-md>$3</gmd-md>
</gmd-card>`,
  detail: 'Card with a title, subtitle and markdown content',
};

export const cardConfiguration: ComponentSuggestionConfiguration = {
  suggestions: [emptyCard, cardWithTitle, cardWithTitleAndSubtitle],
  attributeSuggestions: [
    {
      label: 'backgroundColor="white"',
      insertText: 'backgroundColor="white"',
      detail: 'Change the background color to white.',
    },
    {
      label: 'textColor="black"',
      insertText: 'textColor="black"',
      detail: 'Change the text color to black.',
    },
  ],
  hoverDocumentation: {
    label: 'Card',
    description: 'Layout container for text, photos, and actions.',
  },
  attributeHoverDocumentation: {
    backgroundColor: 'The background color of the card.',
    textColor: 'The color of the text in the card.',
  },
};
