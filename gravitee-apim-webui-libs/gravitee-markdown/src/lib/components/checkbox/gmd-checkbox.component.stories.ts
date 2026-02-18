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
import { importProvidersFrom } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Meta, moduleMetadata, StoryObj, applicationConfig } from '@storybook/angular';

import { GmdCheckboxComponent } from './gmd-checkbox.component';
import { GraviteeMarkdownEditorModule } from '../../gravitee-markdown-editor/gravitee-markdown-editor.module';
import { GmdFormEditorComponent } from '../../gravitee-markdown-form-editor/gmd-form-editor.component';
import { GraviteeMarkdownViewerModule } from '../../gravitee-markdown-viewer/gravitee-markdown-viewer.module';
import { provideGmdFormStore } from '../../services/gmd-form-state.store';

export default {
  title: 'Gravitee Markdown/Components/Checkbox',
  component: GmdCheckboxComponent,
  parameters: {
    layout: 'centered',
  },
  decorators: [
    moduleMetadata({
      imports: [GmdCheckboxComponent, GmdFormEditorComponent, GraviteeMarkdownEditorModule, GraviteeMarkdownViewerModule, FormsModule],
    }),
    applicationConfig({
      providers: [importProvidersFrom(GraviteeMarkdownEditorModule.forRoot({ theme: 'vs', baseUrl: '.' })), provideGmdFormStore()],
    }),
  ],
} as Meta<GmdCheckboxComponent>;

export const Basic: StoryObj<GmdCheckboxComponent> = {
  render: () => ({
    template: `
      <div style="width: 400px;">
        <gmd-checkbox name="basic" label="Basic Checkbox"></gmd-checkbox>
      </div>
    `,
  }),
};

export const Required: StoryObj<GmdCheckboxComponent> = {
  render: () => ({
    template: `
      <div style="width: 400px;">
        <gmd-checkbox name="required" label="I agree to the terms and conditions" required="true"></gmd-checkbox>
      </div>
    `,
  }),
};

export const PreChecked: StoryObj<GmdCheckboxComponent> = {
  render: () => ({
    template: `
      <div style="width: 400px; display: flex; flex-direction: column; gap: 16px;">
        <gmd-checkbox name="unchecked" label="Unchecked by default"></gmd-checkbox>
        <gmd-checkbox name="checked" label="Checked by default" value="true"></gmd-checkbox>
      </div>
    `,
  }),
};

export const ReadonlyAndDisabled: StoryObj<GmdCheckboxComponent> = {
  render: () => ({
    template: `
      <div style="width: 400px; display: flex; flex-direction: column; gap: 16px;">
        <gmd-checkbox name="normal" label="Normal Checkbox" value="true"></gmd-checkbox>
        <gmd-checkbox name="readonly" label="Readonly Checkbox" value="true" readonly="true"></gmd-checkbox>
        <gmd-checkbox name="disabled" label="Disabled Checkbox" value="true" disabled="true"></gmd-checkbox>
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

export const CommonUseCases: StoryObj<GmdCheckboxComponent> = {
  render: () => ({
    template: `
      <div style="width: 400px; display: flex; flex-direction: column; gap: 16px;">
        <gmd-checkbox name="terms" label="I agree to the Terms and Conditions" required="true"></gmd-checkbox>
        <gmd-checkbox name="privacy" label="I agree to the Privacy Policy" required="true"></gmd-checkbox>
        <gmd-checkbox name="newsletter" label="Subscribe to newsletter"></gmd-checkbox>
        <gmd-checkbox name="marketing" label="Receive marketing communications"></gmd-checkbox>
      </div>
    `,
  }),
};

export const WithFieldKey: StoryObj<GmdCheckboxComponent> = {
  render: () => ({
    template: `
      <div style="width: 400px; display: flex; flex-direction: column; gap: 16px;">
        <gmd-checkbox
          name="terms"
          label="I agree to the Terms and Conditions"
          fieldKey="termsAccepted"
          required="true">
        </gmd-checkbox>
        <gmd-checkbox
          name="newsletter"
          label="Subscribe to newsletter"
          fieldKey="newsletterSubscription">
        </gmd-checkbox>
      </div>
    `,
  }),
  parameters: {
    docs: {
      description: {
        story:
          'Checkbox fields configured with field keys. These fields emit validation events for form state aggregation. Values are saved as "true"/"false" strings.',
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
        <h2>Checkbox Component with Form Editor</h2>
        <p>Edit the markdown content below. The form editor shows real-time validation status and field values:</p>

        <div class="form-editor-container">
          <gmd-form-editor [(ngModel)]="formContent" />
        </div>
      </div>
    `,
    props: {
      formContent: `# Subscription Form

Please review and accept the following terms:

<gmd-checkbox name="terms" label="I agree to the Terms and Conditions" fieldKey="termsAccepted" required="true"></gmd-checkbox>

<gmd-checkbox name="privacy" label="I agree to the Privacy Policy" fieldKey="privacyAccepted" required="true"></gmd-checkbox>

<gmd-checkbox name="newsletter" label="Subscribe to newsletter" fieldKey="newsletterSubscription"></gmd-checkbox>

<gmd-checkbox name="marketing" label="Receive marketing communications" fieldKey="marketingConsent"></gmd-checkbox>
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
      content: `# Checkbox Examples

<gmd-checkbox name="terms" label="I agree to the Terms and Conditions" required="true"></gmd-checkbox>

<gmd-checkbox name="privacy" label="I agree to the Privacy Policy" required="true"></gmd-checkbox>

<gmd-checkbox name="newsletter" label="Subscribe to newsletter"></gmd-checkbox>
`,
    },
  }),
  parameters: {
    docs: {
      description: {
        story: 'Checkbox components rendered within the GMD viewer, showing how they appear in actual markdown content.',
      },
    },
  },
};
