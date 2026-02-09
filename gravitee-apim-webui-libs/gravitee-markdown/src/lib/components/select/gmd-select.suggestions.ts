/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
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

const basicSelect: ComponentSuggestion = {
  label: 'Select - Basic',
  insertText: `<gmd-select name="$1" label="$2" options="Option 1,Option 2,Option 3"></gmd-select>`,
  detail: 'Basic dropdown select field',
};

const requiredSelect: ComponentSuggestion = {
  label: 'Select - Required',
  insertText: `<gmd-select name="$1" label="$2" required="true" options="Option 1,Option 2,Option 3"></gmd-select>`,
  detail: 'Required dropdown select field',
};

const selectWithJson: ComponentSuggestion = {
  label: 'Select - JSON Options',
  insertText: `<gmd-select name="$1" label="$2" options='["Option 1","Option 2","Option 3"]'></gmd-select>`,
  detail: 'Select with options defined as JSON array',
};

const formSelect: ComponentSuggestion = {
  label: 'Select - Form Field',
  insertText: `<gmd-select name="$1" label="$2" fieldKey="$3" required="true" options="Option 1,Option 2"></gmd-select>`,
  detail: 'Select field with a field key for validation/data collection',
};

export const selectConfiguration: ComponentSuggestionConfiguration = {
  suggestions: [basicSelect, requiredSelect, selectWithJson, formSelect],
  attributeSuggestions: [
    {
      label: 'name="fieldName"',
      insertText: 'name="$1"',
      detail: 'HTML name/id for label association',
    },
    {
      label: 'label="Label Text"',
      insertText: 'label="$1"',
      detail: 'Display label for the select field',
    },
    {
      label: 'options="Option 1,Option 2,Option 3"',
      insertText: 'options="$1"',
      detail: 'Comma-separated list of options or JSON array string',
    },
    {
      label: 'options=\'["Option 1","Option 2"]\'',
      insertText: 'options=\'["$1"]\'',
      detail: 'JSON array string format for options',
    },
    {
      label: 'required="true"',
      insertText: 'required="true"',
      detail: 'Makes the field required',
    },
    {
      label: 'fieldKey="key"',
      insertText: 'fieldKey="$1"',
      detail: 'Field key for validation and data collection',
    },
    {
      label: 'value="Initial selected value"',
      insertText: 'value="$1"',
      detail: 'Initial selected value',
    },
    {
      label: 'disabled="true"',
      insertText: 'disabled="true"',
      detail: 'Disables the field',
    },
  ],
  hoverDocumentation: {
    label: 'Select',
    description:
      'A dropdown select component that allows users to choose from a list of options. Options can be provided as a comma-separated string or JSON array.',
  },
  attributeHoverDocumentation: {
    name: 'HTML name/id attribute for label association and accessibility',
    label: 'Display label shown above the select field',
    options:
      'List of options as comma-separated string (e.g., "Option 1,Option 2") or JSON array string (e.g., \'["Option 1","Option 2"]\')',
    required: 'Whether the field is required (true/false)',
    fieldKey: 'Field key used for validation and data collection',
    value: 'Initial selected value',
    disabled: 'Whether the field is disabled (true/false)',
  },
};
