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
import { importProvidersFrom } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Meta, moduleMetadata, StoryObj, applicationConfig } from '@storybook/angular';

import { GmdInputComponent } from './gmd-input.component';
import { GraviteeMarkdownEditorModule } from '../../gravitee-markdown-editor/gravitee-markdown-editor.module';
import { GmdFormEditorComponent } from '../../gravitee-markdown-form-editor/gmd-form-editor.component';
import { GraviteeMarkdownViewerModule } from '../../gravitee-markdown-viewer/gravitee-markdown-viewer.module';
import { provideGmdFormStore } from '../../services/gmd-form-state.store';

export default {
  title: 'Gravitee Markdown/Components/Input',
  component: GmdInputComponent,
  parameters: {
    layout: 'centered',
  },
  decorators: [
    moduleMetadata({
      imports: [GmdInputComponent, GmdFormEditorComponent, GraviteeMarkdownEditorModule, GraviteeMarkdownViewerModule, FormsModule],
    }),
    applicationConfig({
      providers: [importProvidersFrom(GraviteeMarkdownEditorModule.forRoot({ theme: 'vs', baseUrl: '.' })), provideGmdFormStore()],
    }),
  ],
} as Meta<GmdInputComponent>;

export const Basic: StoryObj<GmdInputComponent> = {
  render: () => ({
    template: `
      <div style="width: 400px;">
        <gmd-input name="basic" label="Basic Input" placeholder="Enter text..."></gmd-input>
      </div>
    `,
  }),
};

export const Required: StoryObj<GmdInputComponent> = {
  render: () => ({
    template: `
      <div style="width: 400px;">
        <gmd-input name="required" label="Required Input" required="true" placeholder="This field is required"></gmd-input>
      </div>
    `,
  }),
};

export const ReadonlyAndDisabled: StoryObj<GmdInputComponent> = {
  render: () => ({
    template: `
      <div style="width: 400px; display: flex; flex-direction: column; gap: 16px;">
        <gmd-input name="normal" label="Normal Input" value="Editable text" placeholder="You can edit this"></gmd-input>
        <gmd-input name="readonly" label="Readonly Input" value="Read-only text" readonly="true" placeholder="Cannot edit"></gmd-input>
        <gmd-input name="disabled" label="Disabled Input" value="Disabled text" disabled="true" placeholder="Disabled"></gmd-input>
      </div>
    `,
  }),
  parameters: {
    docs: {
      description: {
        story:
          'Comparison of normal, readonly, and disabled states. Readonly fields are focusable and their values are submitted, while disabled fields are not focusable and values are not submitted.',
      },
    },
  },
};

export const WithValidation: StoryObj<GmdInputComponent> = {
  render: () => ({
    template: `
      <div style="width: 400px; display: flex; flex-direction: column; gap: 16px;">
        <gmd-input
          name="minmax"
          label="Min/Max Length"
          minLength="3"
          maxLength="10"
          placeholder="3-10 characters">
        </gmd-input>
        <gmd-input
          name="pattern"
          label="Pattern Validation (letters only)"
          pattern="[A-Za-z]+"
          placeholder="Letters only">
        </gmd-input>
        <gmd-input
          name="email"
          label="Email (with pattern validation)"
          pattern="[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"
          required="true"
          placeholder="user@example.com">
        </gmd-input>
      </div>
    `,
  }),
};

export const PatternExamples: StoryObj<GmdInputComponent> = {
  render: () => ({
    template: `
      <div style="width: 400px; display: flex; flex-direction: column; gap: 16px;">
        <gmd-input
          name="email"
          label="Email"
          pattern="[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"
          placeholder="email@example.com">
        </gmd-input>
        <gmd-input
          name="number"
          label="Numbers only"
          pattern="[0-9]+"
          placeholder="123">
        </gmd-input>
        <gmd-input
          name="url"
          label="URL"
          pattern="https?://.*"
          placeholder="https://example.com">
        </gmd-input>
        <gmd-input
          name="tel"
          label="Phone"
          pattern="\\+?[0-9\\s-]+"
          placeholder="+1 234 567 8900">
        </gmd-input>
      </div>
    `,
  }),
  parameters: {
    docs: {
      description: {
        story:
          'Examples of using pattern validation for different input types. All inputs are type="text" with regex patterns for validation.',
      },
    },
  },
};

export const WithFieldKey: StoryObj<GmdInputComponent> = {
  render: () => ({
    template: `
      <div style="width: 400px; display: flex; flex-direction: column; gap: 16px;">
        <gmd-input
          name="email"
          label="Email"
          fieldKey="email"
          required="true"
          pattern="[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"
          placeholder="user@example.com">
        </gmd-input>
        <gmd-input
          name="company"
          label="Company Name"
          fieldKey="company"
          required="true"
          minLength="2"
          maxLength="50"
          placeholder="Your company name">
        </gmd-input>
      </div>
    `,
  }),
  parameters: {
    docs: {
      description: {
        story: 'Input fields configured with field keys. These fields emit validation events for form state aggregation.',
      },
    },
  },
};

export const WithFormEditor: StoryObj = {
  render: () => ({
    template: `
      <style>
        .form-editor-container {
          height: 600px;
        }
      </style>
      <div style="padding: 20px; display: flex; flex-direction: column; gap: 16px;">
        <h2>Input Component with Form Editor</h2>
        <p>Edit the markdown content below. The form editor shows real-time validation status and field values:</p>

        <div class="form-editor-container">
          <gmd-form-editor [(ngModel)]="formContent" />
        </div>
      </div>
    `,
    props: {
      formContent: `# Registration Form

Please fill in your details:

<gmd-input name="username" label="Username" fieldKey="username" required="true" minLength="3" maxLength="20" placeholder="Choose a username"></gmd-input>

<gmd-input name="email" label="Email Address" fieldKey="email" pattern="[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}" required="true" placeholder="your.email@example.com"></gmd-input>

<gmd-input name="website" label="Website" fieldKey="website" pattern="https?://.*" placeholder="https://example.com"></gmd-input>

<gmd-input name="company" label="Company Name" fieldKey="company" required="true" minLength="2" maxLength="100" placeholder="Your company"></gmd-input>
`,
    },
  }),
  parameters: {
    docs: {
      description: {
        story:
          'Form editor with live validation tracking. Shows how the gmd-form-editor component tracks field states and displays validation status in real-time.',
      },
    },
  },
};

export const InMarkdownViewer: StoryObj = {
  render: () => ({
    template: `
      <div style="width: 600px;">
        <gmd-viewer [content]="content"></gmd-viewer>
      </div>
    `,
    props: {
      content: `# Form Input Examples

<gmd-input name="username" label="Username" required="true" minLength="3" maxLength="20"></gmd-input>

<gmd-input name="email" label="Email Address" pattern="[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}" required="true"></gmd-input>

<gmd-input name="website" label="Website" pattern="https?://.*" placeholder="https://example.com"></gmd-input>

<gmd-input name="phone" label="Phone Number" pattern="\\+?[0-9\\s-]+" placeholder="+1 234 567 8900"></gmd-input>

<gmd-input name="company" label="Company Name" required="true" minLength="2" maxLength="100"></gmd-input>
`,
    },
  }),
  parameters: {
    docs: {
      description: {
        story: 'Input components rendered within the GMD viewer, showing how they appear in actual markdown content.',
      },
    },
  },
};
