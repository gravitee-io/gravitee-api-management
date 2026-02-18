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

import { GmdRadioComponent } from './gmd-radio.component';
import { GraviteeMarkdownEditorModule } from '../../gravitee-markdown-editor/gravitee-markdown-editor.module';
import { GmdFormEditorComponent } from '../../gravitee-markdown-form-editor/gmd-form-editor.component';
import { GraviteeMarkdownViewerModule } from '../../gravitee-markdown-viewer/gravitee-markdown-viewer.module';
import { provideGmdFormStore } from '../../services/gmd-form-state.store';

export default {
  title: 'Gravitee Markdown/Components/Radio',
  component: GmdRadioComponent,
  parameters: {
    layout: 'centered',
  },
  decorators: [
    moduleMetadata({
      imports: [GmdRadioComponent, GmdFormEditorComponent, GraviteeMarkdownEditorModule, GraviteeMarkdownViewerModule, FormsModule],
    }),
    applicationConfig({
      providers: [importProvidersFrom(GraviteeMarkdownEditorModule.forRoot({ theme: 'vs', baseUrl: '.' })), provideGmdFormStore()],
    }),
  ],
} as Meta<GmdRadioComponent>;

export const Basic: StoryObj<GmdRadioComponent> = {
  render: () => ({
    template: `
      <div style="width: 400px;">
        <gmd-radio name="basic" label="Basic Radio Group" options="Option 1,Option 2,Option 3"></gmd-radio>
      </div>
    `,
  }),
};

export const Required: StoryObj<GmdRadioComponent> = {
  render: () => ({
    template: `
      <div style="width: 400px;">
        <gmd-radio name="required" label="Required Radio Group" required="true" options="Option 1,Option 2,Option 3"></gmd-radio>
      </div>
    `,
  }),
};

export const WithJsonOptions: StoryObj<GmdRadioComponent> = {
  render: () => ({
    template: `
      <div style="width: 400px;">
        <gmd-radio name="json" label="Radio with JSON Options" options='["Small","Medium","Large","Extra Large"]'></gmd-radio>
      </div>
    `,
  }),
};

export const ReadonlyAndDisabled: StoryObj<GmdRadioComponent> = {
  render: () => ({
    template: `
      <div style="width: 400px; display: flex; flex-direction: column; gap: 24px;">
        <gmd-radio name="normal" label="Normal Radio" value="Option 2" options="Option 1,Option 2,Option 3"></gmd-radio>
        <gmd-radio name="readonly" label="Readonly Radio" value="Option 2" readonly="true" options="Option 1,Option 2,Option 3"></gmd-radio>
        <gmd-radio name="disabled" label="Disabled Radio" value="Option 2" disabled="true" options="Option 1,Option 2,Option 3"></gmd-radio>
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

export const CommonUseCases: StoryObj<GmdRadioComponent> = {
  render: () => ({
    template: `
      <div style="width: 400px; display: flex; flex-direction: column; gap: 24px;">
        <gmd-radio name="plan" label="Plan" required="true" options="Basic,Professional,Enterprise"></gmd-radio>
        <gmd-radio name="size" label="Size" required="true" options='["Small","Medium","Large"]'></gmd-radio>
        <gmd-radio name="frequency" label="Billing Frequency" required="true" options="Monthly,Quarterly,Yearly"></gmd-radio>
      </div>
    `,
  }),
};

export const WithFieldKey: StoryObj<GmdRadioComponent> = {
  render: () => ({
    template: `
      <div style="width: 400px; display: flex; flex-direction: column; gap: 24px;">
        <gmd-radio
          name="plan"
          label="Plan"
          fieldKey="plan"
          required="true"
          options="Basic,Professional,Enterprise">
        </gmd-radio>
        <gmd-radio
          name="frequency"
          label="Billing Frequency"
          fieldKey="billingFrequency"
          required="true"
          options='["Monthly","Quarterly","Yearly"]'>
        </gmd-radio>
      </div>
    `,
  }),
  parameters: {
    docs: {
      description: {
        story: 'Radio button groups configured with field keys. These fields emit validation events for form state aggregation.',
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
        <h2>Radio with Form Editor</h2>
        <p>Edit the markdown content below. The form editor shows real-time validation status and field values:</p>

        <div class="form-editor-container">
          <gmd-form-editor [(ngModel)]="formContent" />
        </div>
      </div>
    `,
    props: {
      formContent: `# API Subscription Plan

Choose the subscription plan that best fits your needs.

## Select Your Plan

<gmd-radio
  name="plan"
  label="Subscription Plan"
  fieldKey="plan"
  required="true"
  options="Free (10 requests/day),Starter ($9.99/month - 1000 requests/day),Professional ($49.99/month - 10000 requests/day),Enterprise ($199.99/month - Unlimited requests)">
</gmd-radio>

## Billing Frequency

<gmd-radio
  name="frequency"
  label="Billing Frequency"
  fieldKey="billingFrequency"
  required="true"
  options="Monthly (No discount),Quarterly (Save 10%),Yearly (Save 20%)">
</gmd-radio>

## Support Level

<gmd-radio
  name="support"
  label="Support Level"
  fieldKey="support"
  required="true"
  options="Community Support,Email Support (24h response),Priority Support (4h response),Dedicated Support (1h response)">
</gmd-radio>

## Auto-renewal

<gmd-radio
  name="renewal"
  label="Auto-renewal Preference"
  fieldKey="autoRenewal"
  options="Enable auto-renewal,Disable auto-renewal">
</gmd-radio>`,
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
      content: `# Radio Examples

<gmd-radio name="plan" label="Plan" required="true" options="Basic,Professional,Enterprise"></gmd-radio>

<gmd-radio name="size" label="Size" options='["Small","Medium","Large"]'></gmd-radio>

<gmd-radio name="frequency" label="Billing Frequency" required="true" options="Monthly,Quarterly,Yearly"></gmd-radio>
`,
    },
  }),
  parameters: {
    docs: {
      description: {
        story: 'Radio button groups rendered within the GMD viewer, showing how they appear in actual markdown content.',
      },
    },
  },
};
