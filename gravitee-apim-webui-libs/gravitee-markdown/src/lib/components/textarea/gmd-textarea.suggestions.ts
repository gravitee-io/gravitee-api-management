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

const basicTextarea: ComponentSuggestion = {
  label: 'Textarea - Basic',
  insertText: `<gmd-textarea name="$1" label="$2" placeholder="$3"></gmd-textarea>`,
  detail: 'Basic multi-line text input field',
};

const requiredTextarea: ComponentSuggestion = {
  label: 'Textarea - Required',
  insertText: `<gmd-textarea name="$1" label="$2" required="true"></gmd-textarea>`,
  detail: 'Required multi-line text input field',
};

const textareaWithRows: ComponentSuggestion = {
  label: 'Textarea - Custom Rows',
  insertText: `<gmd-textarea name="$1" label="$2" rows="6"></gmd-textarea>`,
  detail: 'Textarea with custom number of visible rows',
};

const textareaWithValidation: ComponentSuggestion = {
  label: 'Textarea - With Validation',
  insertText: `<gmd-textarea name="$1" label="$2" required="true" minLength="10" maxLength="500"></gmd-textarea>`,
  detail: 'Textarea with min/max length validation',
};

const formTextarea: ComponentSuggestion = {
  label: 'Textarea - Form Field',
  insertText: `<gmd-textarea name="$1" label="$2" fieldKey="$3" required="true"></gmd-textarea>`,
  detail: 'Textarea field with a field key for validation/data collection',
};

export const textareaConfiguration: ComponentSuggestionConfiguration = {
  suggestions: [basicTextarea, requiredTextarea, textareaWithRows, textareaWithValidation, formTextarea],
  attributeSuggestions: [
    {
      label: 'name="fieldName"',
      insertText: 'name="$1"',
      detail: 'HTML name/id for label association',
    },
    {
      label: 'label="Label Text"',
      insertText: 'label="$1"',
      detail: 'Display label for the textarea field',
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
      label: 'rows="4"',
      insertText: 'rows="$1"',
      detail: 'Number of visible text rows (default: 4)',
    },
    {
      label: 'minLength="10"',
      insertText: 'minLength="$1"',
      detail: 'Minimum character length',
    },
    {
      label: 'maxLength="500"',
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
    {
      label: 'readonly="true"',
      insertText: 'readonly="true"',
      detail: 'Makes the field read-only',
    },
    {
      label: 'disabled="true"',
      insertText: 'disabled="true"',
      detail: 'Disables the field',
    },
  ],
  hoverDocumentation: {
    label: 'Textarea',
    description:
      'A multi-line text input component for longer text entries. Supports validation options including required, minLength, maxLength, and pattern matching.',
  },
  attributeHoverDocumentation: {
    name: 'HTML name/id attribute for label association and accessibility',
    label: 'Display label shown above the textarea field',
    placeholder: 'Hint text displayed when the field is empty',
    required: 'Whether the field is required (true/false)',
    rows: 'Number of visible text rows (default: 4)',
    minLength: 'Minimum number of characters allowed',
    maxLength: 'Maximum number of characters allowed',
    pattern: 'Regular expression pattern for value validation',
    fieldKey: 'Field key used for validation and data collection',
    value: 'Initial/default value for the field',
    readonly: 'Whether the field is read-only (true/false)',
    disabled: 'Whether the field is disabled (true/false)',
  },
};
