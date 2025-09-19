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
  label: 'Card - with content only',
  insertText: `<gmd-card>
    <pre></pre>
</gmd-card>`,
  detail: 'Component template',
};

const cardWithTitle: ComponentSuggestion = {
  label: 'Card - with title and content',
  insertText: `<gmd-card>
    <gmd-card-title>Your card title here</gmd-card-title>
    <pre>Your card content here</pre>
</gmd-card>`,
  detail: 'Component template',
};

const cardWithTitleAndSubtitle: ComponentSuggestion = {
  label: 'Card - with title, subtitle and content',
  insertText: `<gmd-card>
    <gmd-card-title>Your card title here</gmd-card-title>
    <gmd-card-subtitle>Your card subtitle here</gmd-card-subtitle>
    <pre>Your card content here</pre>
</gmd-card>`,
  detail: 'Component template',
};

export const cardConfiguration: ComponentSuggestionConfiguration = {
  suggestions: [emptyCard, cardWithTitle, cardWithTitleAndSubtitle],
  attributeSuggestions: [
    {
      label: 'backgroundColor="white"',
      insertText: 'backgroundColor="white"',
      detail: 'Change the background color to black.',
    },
    {
      label: 'textColor="black"',
      insertText: 'textColor="black"',
      detail: 'Change the text color to black.',
    },
  ],
  hoverDocumentation: {
    label: 'Card',
    description: 'Layout container for text, photos, and actions in the context of a single subject.',
  },
  attributeHoverDocumentation: {
    backgroundColor: {
      label: 'backgroundColor',
      description: 'The background color of the card.',
    },
    textColor: {
      label: 'textColor',
      description: 'The text color of the card.',
    },
  },
};
