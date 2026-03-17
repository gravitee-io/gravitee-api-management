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

const basicCheckboxGroup: ComponentSuggestion = {
  label: 'Checkbox Group - Basic',
  insertText: `<gmd-checkbox-group name="$1" label="$2" options="Option 1,Option 2,Option 3"></gmd-checkbox-group>`,
  detail: 'Basic checkbox group allowing multiple selections',
};

const requiredCheckboxGroup: ComponentSuggestion = {
  label: 'Checkbox Group - Required',
  insertText: `<gmd-checkbox-group name="$1" label="$2" required="true" options="Option 1,Option 2,Option 3"></gmd-checkbox-group>`,
  detail: 'Required checkbox group (at least one option must be selected)',
};

const formCheckboxGroup: ComponentSuggestion = {
  label: 'Checkbox Group - Form Field',
  insertText: `<gmd-checkbox-group name="$1" label="$2" fieldKey="$3" required="true" options="Option 1,Option 2"></gmd-checkbox-group>`,
  detail: 'Checkbox group with a field key for validation/data collection',
};

const csvOptionsCheckboxGroup: ComponentSuggestion = {
  label: 'Checkbox Group - CSV Options',
  insertText: `<gmd-checkbox-group name="$1" label="$2" options="$3"></gmd-checkbox-group>`,
  detail: 'Checkbox group with comma-separated options',
};

export const checkboxGroupConfiguration: ComponentSuggestionConfiguration = {
  suggestions: [basicCheckboxGroup, requiredCheckboxGroup, formCheckboxGroup, csvOptionsCheckboxGroup],
  attributeSuggestions: [
    {
      label: 'name="fieldName"',
      insertText: 'name="$1"',
      detail: 'HTML name attribute for the checkbox group',
    },
    {
      label: 'label="Label Text"',
      insertText: 'label="$1"',
      detail: 'Display label for the checkbox group',
    },
    {
      label: 'options="Option 1,Option 2,Option 3"',
      insertText: 'options="$1"',
      detail: 'Comma-separated list of options',
    },
    {
      label: 'required="true"',
      insertText: 'required="true"',
      detail: 'Makes the field required (at least one option must be selected)',
    },
    {
      label: 'fieldKey="key"',
      insertText: 'fieldKey="$1"',
      detail: 'Field key for validation and data collection',
    },
    {
      label: 'value="Option 1,Option 2"',
      insertText: 'value="$1"',
      detail: 'Default/initial selected values as comma-separated string',
    },
    {
      label: 'disabled="true"',
      insertText: 'disabled="true"',
      detail: 'Disables all checkboxes in the group',
    },
  ],
  hoverDocumentation: {
    label: 'Checkbox Group',
    description:
      'A checkbox group component that allows users to select multiple options from a list. Value is serialized as a comma-separated string of selected items. Options can be provided as a comma-separated string.',
  },
  attributeHoverDocumentation: {
    name: 'HTML name attribute for the checkbox group',
    label: 'Display label shown above the checkbox group',
    options: 'List of options as comma-separated string (e.g., "Option 1,Option 2,Option 3")',
    required: 'Whether at least one option must be selected (true/false)',
    fieldKey: 'Field key used for validation and data collection',
    value: 'Initial/default selected values as comma-separated string (e.g., "Option 1,Option 3")',
    disabled: 'Whether all checkboxes in the group are disabled (true/false)',
  },
};
