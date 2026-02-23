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

const basicInput: ComponentSuggestion = {
  label: 'Input - Basic',
  insertText: `<gmd-input name="$1" label="$2" placeholder="$3"></gmd-input>`,
  detail: 'Basic text input field',
};

const requiredInput: ComponentSuggestion = {
  label: 'Input - Required',
  insertText: `<gmd-input name="$1" label="$2" required="true"></gmd-input>`,
  detail: 'Required text input field',
};

const emailInput: ComponentSuggestion = {
  label: 'Input - Email',
  insertText: `<gmd-input name="email" label="Email" required="true"></gmd-input>`,
  detail: 'Email input field with required validation',
};

const inputWithValidation: ComponentSuggestion = {
  label: 'Input - With Validation',
  insertText: `<gmd-input name="$1" label="$2" required="true" minLength="3" maxLength="50"></gmd-input>`,
  detail: 'Input field with min/max length validation',
};

const formInput: ComponentSuggestion = {
  label: 'Input - Form Field',
  insertText: `<gmd-input name="$1" label="$2" fieldKey="$3" required="true"></gmd-input>`,
  detail: 'Input field with a field key for validation/data collection',
};

export const inputConfiguration: ComponentSuggestionConfiguration = {
  suggestions: [basicInput, requiredInput, emailInput, inputWithValidation, formInput],
  attributeSuggestions: [
    {
      label: 'name="fieldName"',
      insertText: 'name="$1"',
      detail: 'HTML name/id for label association',
    },
    {
      label: 'label="Label Text"',
      insertText: 'label="$1"',
      detail: 'Display label for the input field',
    },
    {
      label: 'placeholder="Placeholder"',
      insertText: 'placeholder="$1"',
      detail: 'Placeholder text shown when field is empty',
    },
    {
      label: 'required="true"',
      insertText: 'required="true"',
      detail: 'Makes the field required',
    },
    {
      label: 'minLength="3"',
      insertText: 'minLength="$1"',
      detail: 'Minimum character length',
    },
    {
      label: 'maxLength="100"',
      insertText: 'maxLength="$1"',
      detail: 'Maximum character length',
    },
    {
      label: 'pattern="[A-Za-z]+"',
      insertText: 'pattern="$1"',
      detail: 'Regular expression pattern for validation',
    },
    {
      label: 'fieldKey="key"',
      insertText: 'fieldKey="$1"',
      detail: 'Field key for validation and data collection',
    },
    {
      label: 'value="default"',
      insertText: 'value="$1"',
      detail: 'Default/initial value for the field',
    },
  ],
  hoverDocumentation: {
    label: 'Input',
    description: 'A text input component with validation options including required, minLength, maxLength, and pattern matching.',
  },
  attributeHoverDocumentation: {
    name: 'HTML name/id attribute for label association and accessibility',
    label: 'Display label shown above the input field',
    placeholder: 'Hint text displayed when the field is empty',
    required: 'Whether the field is required (true/false)',
    minLength: 'Minimum number of characters allowed',
    maxLength: 'Maximum number of characters allowed',
    pattern: 'Regular expression pattern for value validation',
    fieldKey: 'Field key used for validation and data collection',
    value: 'Initial/default value for the field',
  },
};
