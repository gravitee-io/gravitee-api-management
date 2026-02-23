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

const basicRadio: ComponentSuggestion = {
  label: 'Radio - Basic',
  insertText: `<gmd-radio name="$1" label="$2" options="Option 1,Option 2,Option 3"></gmd-radio>`,
  detail: 'Basic radio button group',
};

const requiredRadio: ComponentSuggestion = {
  label: 'Radio - Required',
  insertText: `<gmd-radio name="$1" label="$2" required="true" options="Option 1,Option 2,Option 3"></gmd-radio>`,
  detail: 'Required radio button group',
};

const radioWithJson: ComponentSuggestion = {
  label: 'Radio - JSON Options',
  insertText: `<gmd-radio name="$1" label="$2" options='["Option 1","Option 2","Option 3"]'></gmd-radio>`,
  detail: 'Radio group with options defined as JSON array',
};

const formRadio: ComponentSuggestion = {
  label: 'Radio - Form Field',
  insertText: `<gmd-radio name="$1" label="$2" fieldKey="$3" required="true" options="Option 1,Option 2"></gmd-radio>`,
  detail: 'Radio group with a field key for validation/data collection',
};

export const radioConfiguration: ComponentSuggestionConfiguration = {
  suggestions: [basicRadio, requiredRadio, radioWithJson, formRadio],
  attributeSuggestions: [
    {
      label: 'name="fieldName"',
      insertText: 'name="$1"',
      detail: 'HTML name/id for label association (same name groups radio buttons)',
    },
    {
      label: 'label="Label Text"',
      insertText: 'label="$1"',
      detail: 'Display label for the radio button group',
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
      detail: 'Makes the field required (one option must be selected)',
    },
    {
      label: 'fieldKey="key"',
      insertText: 'fieldKey="$1"',
      detail: 'Field key for validation and data collection',
    },
    {
      label: 'value="selectedOption"',
      insertText: 'value="$1"',
      detail: 'Default/initial selected value',
    },
    {
      label: 'disabled="true"',
      insertText: 'disabled="true"',
      detail: 'Disables all radio buttons in the group',
    },
  ],
  hoverDocumentation: {
    label: 'Radio',
    description:
      'A radio button group component that allows users to select a single option from a list. All radio buttons with the same name are grouped together. Options can be provided as a comma-separated string or JSON array.',
  },
  attributeHoverDocumentation: {
    name: 'HTML name/id attribute for label association and grouping radio buttons',
    label: 'Display label shown above the radio button group',
    options:
      'List of options as comma-separated string (e.g., "Option 1,Option 2") or JSON array string (e.g., \'["Option 1","Option 2"]\')',
    required: 'Whether one option must be selected (true/false)',
    fieldKey: 'Field key used for validation and data collection',
    value: 'Initial/default selected value',
    disabled: 'Whether all radio buttons in the group are disabled (true/false)',
  },
};
