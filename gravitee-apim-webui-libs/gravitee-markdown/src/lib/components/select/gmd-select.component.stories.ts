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

import { GmdSelectComponent } from './gmd-select.component';
import { GraviteeMarkdownEditorModule } from '../../gravitee-markdown-editor/gravitee-markdown-editor.module';
import { GmdFormEditorComponent } from '../../gravitee-markdown-form-editor/gmd-form-editor.component';
import { GraviteeMarkdownViewerModule } from '../../gravitee-markdown-viewer/gravitee-markdown-viewer.module';
import { provideGmdFormStore } from '../../services/gmd-form-state.store';

export default {
  title: 'Gravitee Markdown/Components/Select',
  component: GmdSelectComponent,
  parameters: {
    layout: 'centered',
  },
  decorators: [
    moduleMetadata({
      imports: [GmdSelectComponent, GmdFormEditorComponent, GraviteeMarkdownEditorModule, GraviteeMarkdownViewerModule, FormsModule],
    }),
    applicationConfig({
      providers: [importProvidersFrom(GraviteeMarkdownEditorModule.forRoot({ theme: 'vs', baseUrl: '.' })), provideGmdFormStore()],
    }),
  ],
} as Meta<GmdSelectComponent>;

export const Basic: StoryObj<GmdSelectComponent> = {
  render: () => ({
    template: `
      <div style="width: 400px;">
        <gmd-select name="basic" label="Basic Select" options="Option 1,Option 2,Option 3"></gmd-select>
      </div>
    `,
  }),
};

export const Required: StoryObj<GmdSelectComponent> = {
  render: () => ({
    template: `
      <div style="width: 400px;">
        <gmd-select name="required" label="Required Select" required="true" options="Option 1,Option 2,Option 3"></gmd-select>
      </div>
    `,
  }),
};

export const WithJsonOptions: StoryObj<GmdSelectComponent> = {
  render: () => ({
    template: `
      <div style="width: 400px;">
        <gmd-select name="json" label="Select with JSON Options" options='["United States","Canada","Mexico","United Kingdom"]'></gmd-select>
      </div>
    `,
  }),
};

export const CommonUseCases: StoryObj<GmdSelectComponent> = {
  render: () => ({
    template: `
      <div style="width: 400px; display: flex; flex-direction: column; gap: 16px;">
        <gmd-select name="country" label="Country" required="true" options="United States,Canada,Mexico,United Kingdom,Germany,France"></gmd-select>
        <gmd-select name="plan" label="Plan" required="true" options="Basic,Professional,Enterprise"></gmd-select>
        <gmd-select name="region" label="Region" options="North America,Europe,Asia Pacific,South America"></gmd-select>
      </div>
    `,
  }),
};

export const WithFieldKey: StoryObj<GmdSelectComponent> = {
  render: () => ({
    template: `
      <div style="width: 400px; display: flex; flex-direction: column; gap: 16px;">
        <gmd-select
          name="country"
          label="Country"
          fieldKey="country"
          required="true"
          options="United States,Canada,Mexico,United Kingdom">
        </gmd-select>
        <gmd-select
          name="plan"
          label="Plan"
          fieldKey="plan"
          required="true"
          options='["Basic","Professional","Enterprise"]'>
        </gmd-select>
      </div>
    `,
  }),
  parameters: {
    docs: {
      description: {
        story: 'Select fields configured with field keys. These fields emit validation events for form state aggregation.',
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
        <h2>Select with Form Editor</h2>
        <p>Edit the markdown content below. The form editor shows real-time validation status and field values:</p>

        <div class="form-editor-container">
          <gmd-form-editor [(ngModel)]="formContent" />
        </div>
      </div>
    `,
    props: {
      formContent: `# User Preferences

Configure your account preferences below.

## Language & Region

<gmd-select
  name="language"
  label="Preferred Language"
  fieldKey="language"
  required="true"
  options="English,Spanish,French,German,Italian,Portuguese,Japanese,Chinese">
</gmd-select>

<gmd-select
  name="timezone"
  label="Timezone"
  fieldKey="timezone"
  required="true"
  options="UTC-8 (PST),UTC-5 (EST),UTC+0 (GMT),UTC+1 (CET),UTC+9 (JST)">
</gmd-select>

## Notification Settings

<gmd-select
  name="frequency"
  label="Email Frequency"
  fieldKey="frequency"
  required="true"
  options="Immediately,Daily Digest,Weekly Digest,Monthly Digest">
</gmd-select>

<gmd-select
  name="format"
  label="Email Format"
  fieldKey="format"
  options="Plain Text,HTML,Rich Text">
</gmd-select>`,
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
      content: `# Select Examples

<gmd-select name="country" label="Country" required="true" options="United States,Canada,Mexico"></gmd-select>

<gmd-select name="plan" label="Plan" options='["Basic","Professional","Enterprise"]'></gmd-select>

<gmd-select name="region" label="Region" options="North America,Europe,Asia"></gmd-select>
`,
    },
  }),
  parameters: {
    docs: {
      description: {
        story: 'Select components rendered within the GMD viewer, showing how they appear in actual markdown content.',
      },
    },
  },
};
