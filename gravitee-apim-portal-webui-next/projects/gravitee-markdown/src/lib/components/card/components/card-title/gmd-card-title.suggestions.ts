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
import { ComponentSuggestion } from '../../../../models/componentSuggestion';
import { ComponentSuggestionConfiguration } from '../../../../models/componentSuggestionConfiguration';

const cardTitle: ComponentSuggestion = {
  label: 'Card title',
  insertText: `<gmd-card-title>$1</gmd-card-title>`,
  detail: 'Add a card title.',
};

export const cardTitleConfiguration: ComponentSuggestionConfiguration = {
  suggestions: [cardTitle],
  attributeSuggestions: [],
  attributeHoverDocumentation: {},
  hoverDocumentation: {
    label: 'Card title',
    description: 'Layout component to add a title in a `<gmd-card>`',
  },
};
