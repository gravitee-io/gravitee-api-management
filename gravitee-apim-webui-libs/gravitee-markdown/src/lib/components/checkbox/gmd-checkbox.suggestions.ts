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

const basicCheckbox: ComponentSuggestion = {
  label: 'Checkbox - Basic',
  insertText: `<gmd-checkbox name="$1" label="$2"></gmd-checkbox>`,
  detail: 'Basic checkbox field',
};

const requiredCheckbox: ComponentSuggestion = {
  label: 'Checkbox - Required',
  insertText: `<gmd-checkbox name="$1" label="$2" required="true"></gmd-checkbox>`,
  detail: 'Required checkbox (must be checked)',
};

const checkedCheckbox: ComponentSuggestion = {
  label: 'Checkbox - Pre-checked',
  insertText: `<gmd-checkbox name="$1" label="$2" value="true"></gmd-checkbox>`,
  detail: 'Checkbox that is checked by default',
};

const formCheckbox: ComponentSuggestion = {
  label: 'Checkbox - Form Field',
  insertText: `<gmd-checkbox name="$1" label="$2" fieldKey="$3" required="true"></gmd-checkbox>`,
  detail: 'Checkbox with a field key for validation/data collection',
};

export const checkboxConfiguration: ComponentSuggestionConfiguration = {
  suggestions: [basicCheckbox, requiredCheckbox, checkedCheckbox, formCheckbox],
  attributeSuggestions: [
    {
      label: 'name="fieldName"',
      insertText: 'name="$1"',
      detail: 'HTML name/id for label association',
    },
    {
      label: 'label="Label Text"',
      insertText: 'label="$1"',
      detail: 'Display label for the checkbox',
    },
    {
      label: 'required="true"',
      insertText: 'required="true"',
      detail: 'Makes the checkbox required (must be checked)',
    },
    {
      label: 'value="true"',
      insertText: 'value="true"',
      detail: 'Initial checked state (true/false)',
    },
    {
      label: 'fieldKey="key"',
      insertText: 'fieldKey="$1"',
      detail: 'Field key for validation and data collection',
    },
    {
      label: 'disabled="true"',
      insertText: 'disabled="true"',
      detail: 'Disables the checkbox',
    },
  ],
  hoverDocumentation: {
    label: 'Checkbox',
    description:
      'A single checkbox component that can be checked or unchecked. When required, the checkbox must be checked for the form to be valid.',
  },
  attributeHoverDocumentation: {
    name: 'HTML name/id attribute for label association and accessibility',
    label: 'Display label shown next to the checkbox',
    required: 'Whether the checkbox must be checked (true/false)',
    value: 'Initial checked state: "true" for checked, "false" or omitted for unchecked',
    fieldKey: 'Field key used for validation and data collection (saves as "true"/"false" string)',
    disabled: 'Whether the checkbox is disabled (true/false)',
  },
};
