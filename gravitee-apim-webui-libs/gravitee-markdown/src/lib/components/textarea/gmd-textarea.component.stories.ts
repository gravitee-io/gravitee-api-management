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

import { GmdTextareaComponent } from './gmd-textarea.component';
import { GraviteeMarkdownEditorModule } from '../../gravitee-markdown-editor/gravitee-markdown-editor.module';
import { GmdFormEditorComponent } from '../../gravitee-markdown-form-editor/gmd-form-editor.component';
import { GraviteeMarkdownViewerModule } from '../../gravitee-markdown-viewer/gravitee-markdown-viewer.module';
import { provideGmdFormStore } from '../../services/gmd-form-state.store';

export default {
  title: 'Gravitee Markdown/Components/Textarea',
  component: GmdTextareaComponent,
  parameters: {
    layout: 'centered',
  },
  decorators: [
    moduleMetadata({
      imports: [GmdTextareaComponent, GmdFormEditorComponent, GraviteeMarkdownEditorModule, GraviteeMarkdownViewerModule, FormsModule],
    }),
    applicationConfig({
      providers: [importProvidersFrom(GraviteeMarkdownEditorModule.forRoot({ theme: 'vs', baseUrl: '.' })), provideGmdFormStore()],
    }),
  ],
} as Meta<GmdTextareaComponent>;

export const Basic: StoryObj<GmdTextareaComponent> = {
  render: () => ({
    template: `
      <div style="width: 400px;">
        <gmd-textarea name="basic" label="Basic Textarea" placeholder="Enter your message..."></gmd-textarea>
      </div>
    `,
  }),
};

export const Required: StoryObj<GmdTextareaComponent> = {
  render: () => ({
    template: `
      <div style="width: 400px;">
        <gmd-textarea name="required" label="Required Textarea" required="true" placeholder="This field is required"></gmd-textarea>
      </div>
    `,
  }),
};

export const CustomRows: StoryObj<GmdTextareaComponent> = {
  render: () => ({
    template: `
      <div style="width: 400px; display: flex; flex-direction: column; gap: 16px;">
        <gmd-textarea name="small" label="Small (2 rows)" rows="2" placeholder="2 rows"></gmd-textarea>
        <gmd-textarea name="default" label="Default (4 rows)" rows="4" placeholder="4 rows (default)"></gmd-textarea>
        <gmd-textarea name="large" label="Large (8 rows)" rows="8" placeholder="8 rows"></gmd-textarea>
      </div>
    `,
  }),
};

export const ReadonlyAndDisabled: StoryObj<GmdTextareaComponent> = {
  render: () => ({
    template: `
      <div style="width: 400px; display: flex; flex-direction: column; gap: 16px;">
        <gmd-textarea name="normal" label="Normal Textarea" value="This is editable text" placeholder="You can edit this"></gmd-textarea>
        <gmd-textarea name="readonly" label="Readonly Textarea" value="This is read-only text that cannot be modified" readonly="true" placeholder="Cannot edit"></gmd-textarea>
        <gmd-textarea name="disabled" label="Disabled Textarea" value="This is disabled text" disabled="true" placeholder="Disabled"></gmd-textarea>
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

export const WithValidation: StoryObj<GmdTextareaComponent> = {
  render: () => ({
    template: `
      <div style="width: 400px; display: flex; flex-direction: column; gap: 16px;">
        <gmd-textarea
          name="minmax"
          label="Min/Max Length"
          minLength="10"
          maxLength="200"
          placeholder="10-200 characters">
        </gmd-textarea>
        <gmd-textarea
          name="required"
          label="Required with Validation"
          required="true"
          minLength="20"
          maxLength="500"
          placeholder="Required, 20-500 characters">
        </gmd-textarea>
      </div>
    `,
  }),
  parameters: {
    docs: {
      description: {
        story: 'Textarea with length validation. Note: pattern attribute is not supported for textarea as per HTML specification.',
      },
    },
  },
};

export const WithFieldKey: StoryObj<GmdTextareaComponent> = {
  render: () => ({
    template: `
      <div style="width: 400px; display: flex; flex-direction: column; gap: 16px;">
        <gmd-textarea
          name="description"
          label="Description"
          fieldKey="description"
          required="true"
          minLength="10"
          maxLength="500"
          placeholder="Describe your use case...">
        </gmd-textarea>
        <gmd-textarea
          name="notes"
          label="Additional Notes"
          fieldKey="notes"
          rows="6"
          placeholder="Any additional information...">
        </gmd-textarea>
      </div>
    `,
  }),
  parameters: {
    docs: {
      description: {
        story: 'Textarea fields configured with field keys. These fields emit validation events for form state aggregation.',
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
        <h2>Textarea with Form Editor</h2>
        <p>Edit the markdown content below. The form editor shows real-time validation status and field values:</p>

        <div class="form-editor-container">
          <gmd-form-editor [(ngModel)]="formContent" />
        </div>
      </div>
    `,
    props: {
      formContent: `# Feedback Form

Please share your feedback with us to help improve our services.

## Your Feedback

<gmd-textarea
  name="feedback"
  label="Feedback"
  fieldKey="feedback"
  required="true"
  minLength="20"
  maxLength="1000"
  rows="6"
  placeholder="Please provide detailed feedback about your experience...">
</gmd-textarea>

## Additional Comments

<gmd-textarea
  name="comments"
  label="Additional Comments"
  fieldKey="comments"
  rows="4"
  placeholder="Any other comments or suggestions...">
</gmd-textarea>

## Would you like us to follow up?

<gmd-textarea
  name="contact"
  label="Contact Information"
  fieldKey="contact"
  rows="3"
  placeholder="Email or phone number (optional)">
</gmd-textarea>`,
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
      content: `# Textarea Examples

<gmd-textarea name="message" label="Message" required="true" minLength="10" maxLength="500"></gmd-textarea>

<gmd-textarea name="description" label="Description" rows="6" placeholder="Enter a detailed description..."></gmd-textarea>

<gmd-textarea name="comments" label="Comments" rows="3" placeholder="Any additional comments..."></gmd-textarea>
`,
    },
  }),
  parameters: {
    docs: {
      description: {
        story: 'Textarea components rendered within the GMD viewer, showing how they appear in actual markdown content.',
      },
    },
  },
};
